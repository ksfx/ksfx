package ch.ksfx.services.agentic;

import ch.ksfx.dao.AgenticProjectDAO;
import ch.ksfx.model.AgenticAuthMode;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.AgenticProject;
import ch.ksfx.model.DockerContainerStatus;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Optional, per-AgenticProject Docker isolation for the headless claude CLI subprocess a turn
 * spawns - see ksfx/ksfx#25 and the Agentic wiki page for the full design rationale. Deliberately
 * opt-in: {@link #isTrustedDockerAddress} and every lifecycle method here are only ever called for
 * an agent/project that has {@code dockerIsolationEnabled} set, so KSFX never shells out to
 * {@code docker} at all unless at least one AgenticProject has actually turned this on.
 *
 * One long-lived container per AgenticProject (not per Agent, not per-turn), named
 * "ksfx-agentic-&lt;projectId&gt;", built from a single plain ubuntu:24.04 image with no custom
 * per-project images - see {@link #ensureContainer}. The project's workspace folder is bind-mounted
 * in at /workspace, so uploads/downloads/code/shared all survive a {@link #throwAway} rebuild;
 * everything an agent installs directly into the container's own filesystem (apt packages, etc.)
 * does not, which is the intended "reset a broken toolchain" semantics of throwing a container away.
 * A fixed, project-derived set of ports is also published at creation - see {@link #portMappingsFor} -
 * so a dev server an agent starts is reachable from the host; like everything else `docker run`-time,
 * that set can't be changed without a {@link #throwAway} either.
 *
 * All `docker` invocations go through {@link #runProcess}, the only pattern in this codebase for
 * shelling out to and monitoring an external process (mirrors ClaudeCliSessionService.executeTurn's
 * stdout/stderr-drain + waitFor(timeout) + exit-code-check shape, generalized into a small helper
 * here since this class needs it at half a dozen call sites instead of just one).
 */
@Service
public class AgenticDockerService
{
    private static final String BOOTSTRAP_MARKER_FILE = "/root/.ksfx-claude-installed";
    private static final long TRUSTED_SUBNET_CACHE_MS = TimeUnit.MINUTES.toMillis(5);

    // The `claude` CLI itself refuses --dangerously-skip-permissions (bypassPermissions mode) when
    // running as UID 0 ("cannot be used with root/sudo privileges for security reasons") - so the
    // container boots and is administered as root (root owns the bootstrap, apt installs, etc.), but
    // the claude process a turn actually execs (see ClaudeCliSessionService.buildCommand's `-u`) runs
    // as this unprivileged user instead, with passwordless sudo available for anything it still needs
    // root for. Preserves "agents may install whatever they want" while keeping bypassPermissions usable.
    static final String CONTAINER_USER = "agent";

    // Fixed set of container-side ports published for every Docker-isolated project, so an agent can
    // just point a dev server at one of these (told which via the system prompt - see
    // ClaudeCliSessionService) without KSFX having to support arbitrary/dynamic port publishing,
    // which Docker doesn't allow adding to an already-running container anyway (publishing is fixed
    // at `docker run` time - see hostPortFor). Not framework-default ports (Vite's 5173 etc.) on
    // purpose: a small contiguous range is simpler to document/remember than a grab-bag of every
    // framework's own default, and the agent has to be told explicitly which port(s) are reachable
    // either way, so nothing is lost by picking our own numbers instead.
    static final int FIRST_AGENT_PORT = 8080;
    static final int LAST_AGENT_PORT = 8085;

    // Host-side base for the published range - deliberately far from common dev-machine ports
    // (KSFX's own 8080 included) so a project's host ports never collide with anything else running
    // on this machine. Combined with the *10-per-project stride below, project 1 lands on
    // 18090-18095, project 2 on 18100-18105, etc. - human-readable at a glance, with headroom
    // (10 slots reserved per project, only 6 used) for FIRST_AGENT_PORT..LAST_AGENT_PORT to grow
    // later without shifting every existing project's host ports.
    static final int HOST_PORT_BASE = 18080;
    static final int HOST_PORT_STRIDE_PER_PROJECT = 10;

    private final AgenticProjectDAO agenticProjectDAO;
    private final AgentWorkspaceService agentWorkspaceService;
    private final SystemLogger systemLogger;

    // Docker bridge subnet(s) trusted to call the self-service scheduling API as if they were
    // localhost - see isTrustedDockerAddress/AgenticApiAccessInterceptor. Refreshed from `docker
    // network inspect bridge` rather than hardcoding e.g. 172.17.0.0/16, since Docker Desktop's
    // actual bridge subnet is backend-dependent (WSL2/Hyper-V/plain dockerd) and not reliably that
    // default. Fails closed: an empty/stale cache trusts nothing.
    private volatile List<SubnetUtils.SubnetInfo> trustedDockerSubnets = new ArrayList<>();
    private volatile long trustedDockerSubnetsCheckedAt = 0;

    public AgenticDockerService(AgenticProjectDAO agenticProjectDAO, AgentWorkspaceService agentWorkspaceService, SystemLogger systemLogger)
    {
        this.agenticProjectDAO = agenticProjectDAO;
        this.agentWorkspaceService = agentWorkspaceService;
        this.systemLogger = systemLogger;
    }

    public String containerNameFor(Long agenticProjectId)
    {
        return "ksfx-agentic-" + agenticProjectId;
    }

    /** The unprivileged OS user inside the container the claude process is exec'd as - see {@link #CONTAINER_USER}. */
    public String containerUser()
    {
        return CONTAINER_USER;
    }

    /**
     * Container-port -&gt; host-port for every port published on this project's container - see the
     * {@link #FIRST_AGENT_PORT}/{@link #HOST_PORT_BASE} field comments for the derivation. Pure
     * arithmetic on {@code agenticProjectId}, no Docker call involved, so this is safe to show on a
     * project's page (e.g. as a preview/status table) even while its container isn't running, and is
     * exactly what {@link #buildRunCommand} publishes - the single source of truth for both, so a UI
     * display of this can never drift from what's actually running.
     */
    public Map<Integer, Integer> portMappingsFor(Long agenticProjectId)
    {
        Map<Integer, Integer> mappings = new LinkedHashMap<>();
        int hostBase = HOST_PORT_BASE + (int) (agenticProjectId * HOST_PORT_STRIDE_PER_PROJECT);

        for (int containerPort = FIRST_AGENT_PORT; containerPort <= LAST_AGENT_PORT; containerPort++) {
            mappings.put(containerPort, hostBase + (containerPort - FIRST_AGENT_PORT));
        }

        return mappings;
    }

    /**
     * Creates the container if it doesn't exist yet (pulling ubuntu:24.04 and bootstrapping Node.js
     * + the claude CLI into it - see {@link #bootstrap}), starts it if it's stopped (re-bootstrapping
     * only if the marker file is missing, e.g. a crash mid-bootstrap), or no-ops if already running.
     * Also serves as the "Start" action from the UI - the three cases collapse to the same call.
     */
    public void ensureContainer(AgenticProject project, AgenticConfig config) throws IOException
    {
        String name = containerNameFor(project.getId());

        try {
            ProcessResult inspect = runProcess(30, "docker", "inspect", "-f", "{{.State.Status}}", name);

            if (inspect.exitCode == 0) {
                if ("running".equals(inspect.stdout.trim())) {
                    syncOAuthCredentials(name, config);
                    persistStatus(project, DockerContainerStatus.RUNNING, name);
                    return;
                }

                requireSuccess(runProcess(30, "docker", "start", name));

                ProcessResult marker = runProcess(10, "docker", "exec", name, "test", "-f", BOOTSTRAP_MARKER_FILE);

                if (marker.exitCode != 0) {
                    bootstrap(name);
                }

                syncOAuthCredentials(name, config);
                persistStatus(project, DockerContainerStatus.RUNNING, name);
                return;
            }

            // Non-zero inspect covers both "no such object" (expected - not created yet) and a
            // genuinely unreachable daemon; either way the right next step is the same "try to
            // create" attempt below, which will itself fail clearly if Docker isn't actually up.
            Path hostWorkspace = agentWorkspaceService.resolveAgenticProjectWorkspace(project, config).toAbsolutePath();
            Files.createDirectories(hostWorkspace);

            requireSuccess(runProcess(120, buildRunCommand(name, hostWorkspace.toString(), config, project.getId()).toArray(new String[0])));

            bootstrap(name);
            syncOAuthCredentials(name, config);
            persistStatus(project, DockerContainerStatus.RUNNING, name);
        } catch (IOException e) {
            persistStatusBestEffort(project, DockerContainerStatus.UNREACHABLE, name);
            throw e;
        }
    }

    /**
     * The exact `docker run` argv used to create a project's container - factored out of
     * {@link #ensureContainer} so {@link #describeSetup} can show the user precisely this, with zero
     * risk of the preview drifting from what actually runs (same trick as
     * ClaudeCliSessionService.buildAutoAppendedSystemPrompt for the system-prompt preview).
     */
    private List<String> buildRunCommand(String containerName, String hostWorkspaceMount, AgenticConfig config)
    {
        return buildRunCommand(containerName, hostWorkspaceMount, config, null);
    }

    /**
     * {@code agenticProjectId} may be null only for {@link #describeSetup}'s unsaved-new-project
     * preview (no id yet to derive host ports from) - every real invocation (from {@link
     * #ensureContainer}) always has one, so port publishing there is unconditional.
     */
    private List<String> buildRunCommand(String containerName, String hostWorkspaceMount, AgenticConfig config, Long agenticProjectId)
    {
        List<String> runCommand = new ArrayList<>(Arrays.asList("docker", "run", "-d", "--name", containerName,
                "--add-host=host.docker.internal:host-gateway",
                "-v", hostWorkspaceMount + ":/workspace",
                "-w", "/workspace",
                "--memory=2g", "--cpus=2"));

        if (agenticProjectId != null) {
            for (Map.Entry<Integer, Integer> mapping : portMappingsFor(agenticProjectId).entrySet()) {
                runCommand.add("-p");
                runCommand.add(mapping.getValue() + ":" + mapping.getKey());
            }
        }

        // OAuth credentials are NOT bind-mounted here (see syncOAuthCredentials) - a `docker run -v`
        // of the single host credentials file used to be mounted to /root/.claude/.credentials.json,
        // with a symlink from the unprivileged agent user's home pointing at it. That broke: the
        // `claude` CLI itself refreshes its credentials file via an atomic write (temp file + rename
        // over the target), which *replaces* whatever was at that path - including a symlink - with
        // a plain file. The very first turn an agent ran silently severed it from the host's live,
        // periodically-refreshed OAuth token and froze it at whatever the CLI happened to write at
        // that moment; from then on it never picked up a host-side token refresh again, eventually
        // going stale and failing with "Not logged in" with no obvious cause (see ksfx/ksfx#41).
        runCommand.add("ubuntu:24.04");
        runCommand.add("sleep");
        runCommand.add("infinity");

        return runCommand;
    }

    /**
     * Copies the host's live OAuth credentials file into the container's unprivileged agent user's
     * home directory via {@code docker cp}, run fresh before every turn (see every {@link
     * #ensureContainer} success path) rather than mounted/symlinked once at container creation - see
     * {@link #buildRunCommand}'s comment for why a mount+symlink doesn't survive the CLI's own
     * atomic-write credential refresh. {@code docker cp} always reads the host file's *current*
     * content at copy time, so this self-heals from that clobbering on every turn instead of freezing
     * a stale/invalid copy in place permanently. No-op for API-key mode or if the host hasn't done an
     * interactive {@code claude login} yet. Best-effort: logs and continues rather than failing the
     * whole turn, since a sync failure just means the turn itself will fail clearly downstream with
     * the CLI's own "Not logged in" error rather than KSFX guessing at one here.
     */
    private void syncOAuthCredentials(String containerName, AgenticConfig config)
    {
        if (config.getAuthMode() != AgenticAuthMode.OAUTH) {
            return;
        }

        Path hostCredentials = Paths.get(System.getProperty("user.home"), ".claude", ".credentials.json");

        if (!Files.isRegularFile(hostCredentials)) {
            return;
        }

        try {
            requireSuccess(runProcess(15, "docker", "exec", containerName, "mkdir", "-p", "/home/" + CONTAINER_USER + "/.claude"));
            requireSuccess(runProcess(15, "docker", "cp", hostCredentials.toAbsolutePath().toString(),
                    containerName + ":/home/" + CONTAINER_USER + "/.claude/.credentials.json"));
            requireSuccess(runProcess(15, "docker", "exec", containerName, "chown", CONTAINER_USER + ":" + CONTAINER_USER,
                    "/home/" + CONTAINER_USER + "/.claude/.credentials.json"));
            requireSuccess(runProcess(15, "docker", "exec", containerName, "chmod", "600",
                    "/home/" + CONTAINER_USER + "/.claude/.credentials.json"));
        } catch (IOException e) {
            systemLogger.logMessage("AGENTIC", "Could not sync OAuth credentials into container '" + containerName + "' - "
                    + "the next turn will likely fail with 'Not logged in' if this doesn't clear up on retry.", e);
        }
    }

    /**
     * The exact bootstrap script run once per fresh/thrown-away container - factored out of
     * {@link #bootstrap} for the same zero-drift reason as {@link #buildRunCommand}.
     */
    private String bootstrapScript()
    {
        return "apt-get update -qq && DEBIAN_FRONTEND=noninteractive apt-get install -y -qq curl ca-certificates sudo "
                + "&& curl -fsSL https://deb.nodesource.com/setup_20.x | bash - >/dev/null "
                + "&& apt-get install -y -qq nodejs && npm install -g @anthropic-ai/claude-code "
                + "&& useradd -m -s /bin/bash " + CONTAINER_USER + " "
                + "&& echo '" + CONTAINER_USER + " ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/" + CONTAINER_USER + " "
                + "&& mkdir -p /home/" + CONTAINER_USER + "/.claude "
                + "&& chown -R " + CONTAINER_USER + ":" + CONTAINER_USER + " /home/" + CONTAINER_USER + " "
                + "&& touch " + BOOTSTRAP_MARKER_FILE;
    }

    /**
     * Exact, human-readable description of what enabling Docker isolation actually does for this
     * project - the container-name/creation/bootstrap commands verbatim (via {@link #buildRunCommand}/
     * {@link #bootstrapScript}, so this can never drift from what {@link #ensureContainer} really
     * runs), plus a short note on how each chat turn then execs into it. Pure/side-effect-free (like
     * ClaudeCliSessionService.buildAutoAppendedSystemPrompt) so it doubles as a read-only preview on
     * the project edit page - see AgenticProjectController.edit()/submit(). Works for an unsaved new
     * project too (id == null), using a placeholder container name/workspace path in that case.
     */
    public String describeSetup(AgenticProject project, AgenticConfig config)
    {
        String containerName = project.getId() != null ? containerNameFor(project.getId()) : "ksfx-agentic-<project-id>";
        // Windows rejects '<'/'>' at Path-construction time (not just on real I/O), so the
        // placeholder for an unsaved project is built as a plain string, not via Paths.get.
        String hostWorkspaceMount = project.getId() != null
                ? agentWorkspaceService.resolveAgenticProjectWorkspace(project, config).toAbsolutePath().toString()
                : Paths.get(config.getWorkspaceRoot()).toAbsolutePath() + java.io.File.separator + "project-<project-id>";

        String runCommand = String.join(" ", quoteIfNeeded(buildRunCommand(containerName, hostWorkspaceMount, config, project.getId())));

        StringBuilder portTable = new StringBuilder();

        if (project.getId() != null) {
            for (Map.Entry<Integer, Integer> mapping : portMappingsFor(project.getId()).entrySet()) {
                portTable.append("  localhost:").append(mapping.getValue()).append(" -> container:").append(mapping.getKey()).append("\n");
            }
        } else {
            portTable.append("  (assigned once this project is saved and has an id)\n");
        }

        return "Container name: " + containerName + "\n\n"
                + "1) Created once, the first time this project's isolation is used (or after Throw Away):\n"
                + runCommand + "\n\n"
                + "Published ports (fixed container-side " + FIRST_AGENT_PORT + "-" + LAST_AGENT_PORT + ", host-side derived from this\n"
                + "project's id so it never collides with another project's - tell the agent which of these to bind\n"
                + "its dev server to, e.g. via its custom system prompt):\n"
                + portTable + "\n"
                + "2) Bootstrapped once right after creation (installs Node.js + the claude CLI, and sets up the\n"
                + "unprivileged '" + CONTAINER_USER + "' user - see below):\n"
                + "docker exec " + containerName + " bash -lc \"" + bootstrapScript() + "\"\n\n"
                + "3) For OAuth mode, before every turn (not just once - see syncOAuthCredentials): the host's\n"
                + "current ~/.claude/.credentials.json is freshly `docker cp`'d into the container as '" + CONTAINER_USER + "',\n"
                + "since the claude CLI's own credential refresh would otherwise silently detach a one-time-mounted\n"
                + "copy from the host's live token.\n\n"
                + "4) Every chat turn then runs inside that same container as:\n"
                + "docker exec -u " + CONTAINER_USER + " -w /workspace/agent-<agent-id> [-e ANTHROPIC_API_KEY=... if not OAuth]"
                + " -e KSFX_AGENT_TOKEN=... " + containerName + " claude -p \"<message>\" --output-format stream-json --verbose"
                + " --permission-mode <mode> [--resume <session-id>] --append-system-prompt-file /workspace/agent-<agent-id>/.agentic-system-prompt.txt\n\n"
                + "The container itself is administered as root (that's what runs the bootstrap above), but the\n"
                + "claude process in step 4 runs as the unprivileged '" + CONTAINER_USER + "' user, with passwordless\n"
                + "sudo available inside the container for anything that still needs root - the claude CLI refuses\n"
                + "to run unattended (--dangerously-skip-permissions / bypassPermissions mode) as root itself.\n\n"
                + "/workspace is your project's workspace folder bind-mounted from the host, so uploads/downloads/\n"
                + "code/shared survive a Throw Away rebuild - only what an agent installs into the container's own\n"
                + "filesystem (apt packages etc.) is lost when it's thrown away.";
    }

    private List<String> quoteIfNeeded(List<String> args)
    {
        List<String> quoted = new ArrayList<>();

        for (String arg : args) {
            quoted.add(arg.contains(" ") ? "\"" + arg + "\"" : arg);
        }

        return quoted;
    }

    /**
     * Read-only status refresh for the edit page - a plain `docker inspect`, never starting/creating
     * anything. Deliberately NOT {@link #ensureContainer}: that method's "start if stopped" behavior
     * is exactly right for a pre-turn check, but calling it just to display current status would
     * silently resurrect a container the user just explicitly Stopped, defeating that action. Persists
     * whatever status is observed (RUNNING/STOPPED, or UNREACHABLE if the container doesn't exist /
     * Docker itself is unreachable) and never throws - a failed refresh just leaves the previous
     * persisted status on screen.
     */
    public void refreshStatus(AgenticProject project)
    {
        String name = containerNameFor(project.getId());

        try {
            ProcessResult inspect = runProcess(30, "docker", "inspect", "-f", "{{.State.Status}}", name);

            if (inspect.exitCode != 0) {
                persistStatus(project, DockerContainerStatus.UNREACHABLE, name);
                return;
            }

            String status = inspect.stdout.trim();
            persistStatus(project, "running".equals(status) ? DockerContainerStatus.RUNNING : DockerContainerStatus.STOPPED, name);
        } catch (IOException ignored) {
            // best-effort - leave whatever status was already persisted
        }
    }

    /**
     * Installs Node.js + the claude CLI into a freshly created/recreated container, creates the
     * unprivileged {@link #CONTAINER_USER} the actual claude process runs as (see the field comment
     * for why), then drops a marker file so a later ensureContainer call knows not to repeat this
     * (~30-90s) step. A single shared stock Ubuntu image plus this bootstrap - rather than a custom
     * pre-built image - is the deliberate choice here: zero registry/build-pipeline maintenance for
     * KSFX, at the cost of this one-time latency on create/throw-away only, never per turn.
     *
     * OAuth credentials are not part of this bootstrap - see {@link #syncOAuthCredentials}, called
     * separately (and repeatedly, every turn) from every {@link #ensureContainer} success path.
     */
    private void bootstrap(String containerName) throws IOException
    {
        requireSuccess(runProcess(480, "docker", "exec", containerName, "bash", "-lc", bootstrapScript()));
    }

    public void stop(AgenticProject project) throws IOException
    {
        String name = containerNameFor(project.getId());

        try {
            requireSuccess(runProcess(30, "docker", "stop", name));
            persistStatus(project, DockerContainerStatus.STOPPED, name);
        } catch (IOException e) {
            persistStatusBestEffort(project, DockerContainerStatus.UNREACHABLE, name);
            throw e;
        }
    }

    /**
     * Ends a stopped/abandoned turn's {@code claude} process inside the container - tried
     * surgically first ({@code docker exec ... pkill}, given a generous 40s before giving up on
     * it - escalating to a full container kill+restart is disruptive to every agent sharing this
     * project's container, not just the one being stopped, so it's worth waiting out a container
     * that's merely slow rather than truly stuck), escalating to killing and restarting the
     * *whole* container if that doesn't complete within its own timeout. The surgical path
     * depends on the container's exec subsystem being responsive - exactly what a runaway,
     * resource-exhausted container (a real one: ksfx/ksfx#28, a container that ran two JVMs,
     * Postgres and MySQL simultaneously inside a 2g memory limit) often is not, silently leaving
     * the stopped-by-the-user turn's process running anyway with nothing telling you it's still
     * there. `docker kill` operates at the kernel/cgroup level and doesn't need a working exec
     * session, so it gets through regardless. Restarts the container afterward (`docker start`)
     * so it's immediately usable again rather than left stopped until the next turn happens to
     * call {@link #ensureContainer} - the bootstrap marker survives a kill+start, so this
     * doesn't repeat the ~30-90s Node/claude install.
     *
     * Returns whether it had to escalate, so {@link ch.ksfx.services.agentic.ClaudeCliSessionService}
     * knows to tell every agent sharing this project's container (not just whichever turn
     * triggered this) that it just got reset out from under them - a project's container is
     * shared across every Agent assigned to it (see the class javadoc), so a `docker kill` here
     * ends their in-flight work too, not only the turn this call is cleaning up after.
     */
    public boolean recoverStuckContainer(AgenticProject project)
    {
        String name = containerNameFor(project.getId());

        try {
            // Never throws for "found nothing to kill" (a plain non-zero exit, not a timeout/
            // exec failure - see runProcess) - that's the common, unremarkable case where the
            // claude process had already exited on its own by the time this runs.
            runProcess(40, "docker", "exec", name, "pkill", "-f", "claude");
            return false;
        } catch (IOException e) {
            systemLogger.logMessage("AGENTIC", "Surgical kill of claude process in '" + name
                    + "' didn't complete - container looks stuck, escalating to a full container kill+restart.", e);
        }

        try {
            runProcess(20, "docker", "kill", name);
        } catch (IOException ignored) {
            // best-effort - if even `docker kill` doesn't get through, the daemon itself is in
            // trouble; the restart attempt right below will surface that clearly if so.
        }

        try {
            requireSuccess(runProcess(30, "docker", "start", name));
            persistStatus(project, DockerContainerStatus.RUNNING, name);
        } catch (IOException e) {
            persistStatusBestEffort(project, DockerContainerStatus.UNREACHABLE, name);
        }

        return true;
    }

    /**
     * Discards the container and recreates it clean from the base image - the "reset a broken
     * toolchain" action. The project's workspace is bind-mounted, not part of the container's own
     * filesystem, so uploads/downloads/code/shared are untouched by this.
     */
    public void throwAway(AgenticProject project, AgenticConfig config) throws IOException
    {
        try {
            runProcess(30, "docker", "rm", "-f", containerNameFor(project.getId()));
        } catch (IOException ignored) {
            // "no such container" (or any other removal hiccup) is a fine starting point for a rebuild.
        }

        ensureContainer(project, config);
    }

    /**
     * Best-effort container teardown for AgenticProjectController's delete cascade - deliberately
     * never throws, so a broken/unreachable Docker install can never block deleting an
     * AgenticProject (matching that cascade's existing "never block on cleanup" behavior for the
     * agent-workspace side). A leftover container in that edge case needs manual `docker rm`.
     */
    public void deleteContainer(AgenticProject project)
    {
        String name = containerNameFor(project.getId());

        try {
            runProcess(30, "docker", "rm", "-f", name);
        } catch (Exception e) {
            systemLogger.logMessage("AGENTIC", "Could not remove Docker container '" + name + "' for deleted AgenticProject "
                    + project.getId() + " - may need manual cleanup.", e);
        }
    }

    /**
     * Used by AgenticApiAccessInterceptor to let a containerized agent's self-service scheduling
     * curl calls (routed via host.docker.internal, arriving from the Docker bridge network rather
     * than literal localhost) through, without loosening that interceptor's unconditional
     * X-Forwarded-For rejection.
     */
    public boolean isTrustedDockerAddress(String remoteAddr)
    {
        if (agenticProjectDAO.getAllAgenticProjects().stream().noneMatch(AgenticProject::getDockerIsolationEnabled)) {
            return false;
        }

        refreshTrustedSubnetsIfStale();

        for (SubnetUtils.SubnetInfo subnet : trustedDockerSubnets) {
            try {
                if (subnet.isInRange(remoteAddr)) {
                    return true;
                }
            } catch (Exception ignored) {
                // remoteAddr isn't a plain dotted-quad IPv4 address (e.g. an IPv6 literal) - can't be in an IPv4 CIDR.
            }
        }

        return false;
    }

    private synchronized void refreshTrustedSubnetsIfStale()
    {
        if (System.currentTimeMillis() - trustedDockerSubnetsCheckedAt < TRUSTED_SUBNET_CACHE_MS) {
            return;
        }

        try {
            ProcessResult result = runProcess(10, "docker", "network", "inspect", "bridge",
                    "--format", "{{range .IPAM.Config}}{{.Subnet}} {{end}}");

            if (result.exitCode == 0) {
                List<SubnetUtils.SubnetInfo> subnets = new ArrayList<>();

                for (String cidr : result.stdout.trim().split("\\s+")) {
                    if (!cidr.isEmpty()) {
                        subnets.add(new SubnetUtils(cidr).getInfo());
                    }
                }

                trustedDockerSubnets = subnets;
            }
            // Non-zero exit: keep whatever's already cached (possibly the empty default) rather
            // than wipe a previously-good cache over one transient failure.
        } catch (Exception ignored) {
        } finally {
            trustedDockerSubnetsCheckedAt = System.currentTimeMillis();
        }
    }

    private void persistStatus(AgenticProject project, DockerContainerStatus status, String containerName)
    {
        project.setDockerContainerName(containerName);
        project.setDockerContainerStatus(status);
        project.setDockerContainerLastCheckedAt(new Date());
        agenticProjectDAO.saveOrUpdateAgenticProject(project);
    }

    private void persistStatusBestEffort(AgenticProject project, DockerContainerStatus status, String containerName)
    {
        try {
            persistStatus(project, status, containerName);
        } catch (Exception ignored) {
        }
    }

    private ProcessResult requireSuccess(ProcessResult result) throws IOException
    {
        if (result.exitCode != 0) {
            throw new IOException("Docker command failed (exit " + result.exitCode + "): " + result.command
                    + (result.stderr.length() > 0 ? " - " + result.stderr.trim() : ""));
        }

        return result;
    }

    /**
     * Runs an external command to completion, capturing stdout/stderr on separate drain threads
     * (same shape as ClaudeCliSessionService.executeTurn's stderr-drain pattern, generalized since
     * this class needs it at several call sites). Throws only for genuine execution problems -
     * couldn't start, timed out, interrupted - never for a non-zero exit code, since several callers
     * (the "docker inspect" existence check, "docker rm" during throwAway) treat a non-zero exit as
     * an expected, meaningful outcome rather than an error. Callers that want "non-zero = failure"
     * wrap the result in {@link #requireSuccess}.
     */
    private ProcessResult runProcess(int timeoutSeconds, String... command) throws IOException
    {
        Process process = new ProcessBuilder(command).start();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Thread stdoutDrain = new Thread(() -> drainStream(process.getInputStream(), stdout));
        Thread stderrDrain = new Thread(() -> drainStream(process.getErrorStream(), stderr));
        stdoutDrain.start();
        stderrDrain.start();

        boolean finished;

        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Interrupted while running: " + String.join(" ", command), e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out after " + timeoutSeconds + "s: " + String.join(" ", command));
        }

        try {
            stdoutDrain.join(TimeUnit.SECONDS.toMillis(5));
            stderrDrain.join(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return new ProcessResult(process.exitValue(), stdout.toString(), stderr.toString(), String.join(" ", command));
    }

    private void drainStream(InputStream stream, StringBuilder into)
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                into.append(line).append('\n');
            }
        } catch (IOException ignored) {
            // stream closed under us (process already gone) - nothing left worth reading.
        }
    }

    private static class ProcessResult
    {
        final int exitCode;
        final String stdout;
        final String stderr;
        final String command;

        ProcessResult(int exitCode, String stdout, String stderr, String command)
        {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.command = command;
        }
    }
}
