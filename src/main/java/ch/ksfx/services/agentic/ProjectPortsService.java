package ch.ksfx.services.agentic;

import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.dao.ProjectDAO;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.Project;
import ch.ksfx.services.project.ProjectContextService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The published ports of a Docker-isolated project, for the breadcrumb's ports dropdown: the
 * mappings themselves come from {@link AgenticDockerService#portMappingsFor} (one truth, same as
 * the container run command and the setup preview), and {@link #knock} answers "does anything
 * LISTEN on that port right now".
 *
 * Knocking happens INSIDE the container, not against the published host port: Docker's own proxy
 * listens on every published host port for as long as the container runs, whether or not a
 * process inside listens on the target port - a host-side TCP connect therefore always succeeds
 * and showed every dot green (found on HQ, 2026-10-23; the dev box has no Docker, which is why
 * the original host-side probe looked correct there). One `docker exec` probes all container-side
 * ports in a single bash loop via /dev/tcp (connects to 127.0.0.1 inside the container net
 * namespace: instant accept or instant refusal), so the whole check is one exec of ~100 ms. A
 * container that is not running fails the exec and reports every port closed, matching reality.
 *
 * Runs only when the dropdown is opened, asynchronously from the browser (ProjectPortsController)
 * - never during page rendering - and is cached briefly so repeated opening does not re-knock.
 * Green means "a process listens", not "the app answers sensibly".
 */
@Service("projectPortsService")
public class ProjectPortsService
{
    private static final int EXEC_TIMEOUT_SECONDS = 5;
    private static final long CACHE_TTL_MS = 5000;

    public static final String PORT_PLACEHOLDER = "{port}";

    private final AgenticDockerService agenticDockerService;
    private final ProjectContextService projectContextService;
    private final AgenticConfigDAO agenticConfigDAO;
    private final ProjectDAO projectDAO;
    private final Map<Long, CachedKnock> knockCache = new ConcurrentHashMap<>();

    public ProjectPortsService(AgenticDockerService agenticDockerService, ProjectContextService projectContextService,
                               AgenticConfigDAO agenticConfigDAO, ProjectDAO projectDAO)
    {
        this.agenticDockerService = agenticDockerService;
        this.projectContextService = projectContextService;
        this.agenticConfigDAO = agenticConfigDAO;
        this.projectDAO = projectDAO;
    }

    /** container port -> host port of the CURRENT project, empty unless it is Docker-isolated - the breadcrumb fragment's render-time call. */
    public Map<Integer, Integer> currentProjectPorts()
    {
        Project project = projectContextService.current();

        if (project == null || project.getId() == null || !project.getDockerIsolationEnabled()) {
            return Collections.emptyMap();
        }

        return agenticDockerService.portMappingsFor(project.getId());
    }

    /**
     * The breadcrumb dropdown's links: through the reverse proxy when a port link template is
     * configured (HSTS on the KSFX domain force-upgrades plain http://domain:port links to https,
     * which a bare dev server cannot answer - the proxy route is real HTTPS), else plain
     * http://<request host>:<port> as before.
     */
    public List<PortLink> currentProjectPortLinks()
    {
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();
        String template = config != null ? config.getPortLinkTemplate() : null;
        boolean useTemplate = template != null && template.contains(PORT_PLACEHOLDER);
        List<PortLink> links = new ArrayList<>();

        currentProjectPorts().forEach((containerPort, hostPort) -> {
            String url = useTemplate
                    ? template.trim().replace(PORT_PLACEHOLDER, String.valueOf(hostPort))
                    : "http://" + requestServerName() + ":" + hostPort;
            links.add(new PortLink(containerPort, hostPort, url));
        });

        return links;
    }

    /**
     * Caddy's on-demand-TLS "ask" check (public, see ProjectPortsController): is {@code domain} a
     * port link domain of this instance? Valid iff the configured template matches it AND the
     * embedded host port is mapped by SOME Docker-isolated project - deliberately regardless of
     * whether the container runs or anything listens (decided 2026-10-24): the certificate should
     * exist before the first dev server starts, and a stopped container is not a reason to refuse
     * a cert. Without a template (or one without the placeholder) everything is refused.
     */
    public boolean isValidPortLinkDomain(String domain)
    {
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();
        String template = config != null ? config.getPortLinkTemplate() : null;

        if (domain == null || domain.trim().isEmpty() || template == null || !template.contains(PORT_PLACEHOLDER)) {
            return false;
        }

        String host = template.trim();

        if (host.contains("://")) {
            host = host.substring(host.indexOf("://") + 3);
        }

        int slash = host.indexOf('/');

        if (slash >= 0) {
            host = host.substring(0, slash);
        }

        Pattern pattern = Pattern.compile(Pattern.quote(host).replace(PORT_PLACEHOLDER, "\\E(\\d{1,5})\\Q"), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(domain.trim());

        if (!matcher.matches()) {
            return false;
        }

        int hostPort;

        try {
            hostPort = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return false;
        }

        for (Project project : projectDAO.getAllProjects()) {
            if (project.getDockerIsolationEnabled() && agenticDockerService.portMappingsFor(project.getId()).containsValue(hostPort)) {
                return true;
            }
        }

        return false;
    }

    private String requestServerName()
    {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        return attributes instanceof ServletRequestAttributes ? ((ServletRequestAttributes) attributes).getRequest().getServerName() : "localhost";
    }

    /** One link of the breadcrumb dropdown. */
    public static class PortLink
    {
        private final Integer containerPort;
        private final Integer hostPort;
        private final String url;

        PortLink(Integer containerPort, Integer hostPort, String url)
        {
            this.containerPort = containerPort;
            this.hostPort = hostPort;
            this.url = url;
        }

        public Integer getContainerPort() { return containerPort; }
        public Integer getHostPort() { return hostPort; }
        public String getUrl() { return url; }
        public String getLabel() { return url.contains("://") ? url.substring(url.indexOf("://") + 3) : url; }
    }

    /** host port -> something listens on the matching container port, for exactly the project's derived ports. */
    public Map<Integer, Boolean> knock(Project project)
    {
        CachedKnock cached = knockCache.get(project.getId());

        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            return cached.result;
        }

        Map<Integer, Integer> mappings = agenticDockerService.portMappingsFor(project.getId());
        Set<Integer> openContainerPorts = probeContainerPorts(project, mappings.keySet());
        Map<Integer, Boolean> result = new LinkedHashMap<>();

        mappings.forEach((containerPort, hostPort) -> result.put(hostPort, openContainerPorts.contains(containerPort)));
        knockCache.put(project.getId(), new CachedKnock(result));

        return result;
    }

    /** One `docker exec` for all ports: bash prints "<port>:open|closed" per /dev/tcp connect inside the container. */
    private Set<Integer> probeContainerPorts(Project project, Collection<Integer> containerPorts)
    {
        Set<Integer> open = new HashSet<>();
        String ports = containerPorts.stream().map(String::valueOf).collect(Collectors.joining(" "));
        String script = "for p in " + ports + "; do (exec 3<>/dev/tcp/127.0.0.1/$p) 2>/dev/null && echo $p:open || echo $p:closed; done";

        try {
            Process process = new ProcessBuilder("docker", "exec", agenticDockerService.containerNameFor(project.getId()), "bash", "-c", script)
                    .redirectErrorStream(true).start();
            String output = new String(readAll(process.getInputStream()), StandardCharsets.UTF_8);

            if (!process.waitFor(EXEC_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return open;
            }

            if (process.exitValue() != 0 && !output.contains(":open") && !output.contains(":closed")) {
                return open; // container not running / docker unavailable - every port reads closed, which matches reality
            }

            for (String line : output.split("\\r?\\n")) {
                if (line.endsWith(":open")) {
                    open.add(Integer.valueOf(line.substring(0, line.indexOf(':'))));
                }
            }
        } catch (Exception e) {
            // no docker / interrupted: all closed
        }

        return open;
    }

    private byte[] readAll(java.io.InputStream in) throws java.io.IOException
    {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;

        while ((n = in.read(chunk)) > 0) {
            buffer.write(chunk, 0, n);
        }

        return buffer.toByteArray();
    }

    private static class CachedKnock
    {
        final Map<Integer, Boolean> result;
        final long timestamp = System.currentTimeMillis();

        CachedKnock(Map<Integer, Boolean> result) { this.result = result; }
    }
}
