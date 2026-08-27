package ch.ksfx.services.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentMessage;
import ch.ksfx.model.AgentMessageRole;
import ch.ksfx.model.AgenticAuthMode;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.model.AgenticProject;
import ch.ksfx.services.systemlogger.SystemLogger;
import ch.ksfx.util.StacktraceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spawns the headless Claude Code CLI ({@code claude -p ... --output-format stream-json}) as a
 * child process per chat turn, streams its output to the browser via SSE, and persists the turn
 * as {@link AgentMessage} rows. The CLI's own --resume mechanism (via {@link Agent#getClaudeSessionId()})
 * provides conversation continuity across turns; KSFX only keeps a display copy of the transcript.
 *
 * Only one turn per agent may run at a time ({@link #runningStatus}) - deliberately in-memory
 * rather than a DB column, so a crashed/restarted server can't leave an agent stuck "busy"
 * forever. The same map doubles as a live "what is this agent doing right now" status, polled by
 * the Agentic sidebar (GET /agentic/status) so activity is visible even for agents you're not
 * currently chatting with.
 *
 * Three entry points share the same validation/busy-guard/execution core: {@link #runTurn} (browser,
 * streams via SseEmitter, spawns a Thread so the controller can return the emitter immediately),
 * {@link #runScheduledTurn} (Quartz jobs from AgentScheduleJob, no browser attached, runs
 * synchronously on Quartz's own worker thread - a scheduled trigger is otherwise identical to the
 * user having typed the message themselves, including landing in the same AgentMessage history),
 * and {@link #runAgentTriggeredTurn} (another Agent messaging this one via AgentMessageApiController,
 * also synchronous/headless, persisted with role AGENT instead of USER so the chat UI can tell it
 * apart from a human).
 */
@Service
public class ClaudeCliSessionService
{
    public static final String SKIPPED_RESULT = "SKIPPED";

    private final AgenticConfigDAO agenticConfigDAO;
    private final AgentDAO agentDAO;
    private final AgentMessageDAO agentMessageDAO;
    private final AgentWorkspaceService agentWorkspaceService;
    private final AgenticDockerService agenticDockerService;
    private final SystemLogger systemLogger;
    private final ObjectMapper objectMapper;
    private final int serverPort;

    private final ConcurrentHashMap<Long, String> runningStatus = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Process> runningProcesses = new ConcurrentHashMap<>();
    private final Set<Long> stopRequested = ConcurrentHashMap.newKeySet();

    // Output accumulated so far for a running turn (text streamed + tool activity), and every
    // SseEmitter currently watching it live - see RunningTurnState, getPartialTurn and
    // attachToRunningTurn. Both exist purely to survive a page navigation: without them, a chat
    // page reload while a turn is in flight had nothing to show (the turn itself keeps running
    // server-side regardless - see trySend's comment - but its output only ever lived in the DOM
    // of whichever single page/request started it) until the turn finished and persisted. Keyed
    // separately from runningStatus/runningProcesses since those track different things (a short
    // status string; the OS process) with different lifetimes than "everything streamed so far."
    private final ConcurrentHashMap<Long, RunningTurnState> runningTurnState = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public ClaudeCliSessionService(AgenticConfigDAO agenticConfigDAO,
                                    AgentDAO agentDAO,
                                    AgentMessageDAO agentMessageDAO,
                                    AgentWorkspaceService agentWorkspaceService,
                                    AgenticDockerService agenticDockerService,
                                    SystemLogger systemLogger,
                                    ObjectMapper objectMapper,
                                    @Value("${server.port:8080}") int serverPort)
    {
        this.agenticConfigDAO = agenticConfigDAO;
        this.agentDAO = agentDAO;
        this.agentMessageDAO = agentMessageDAO;
        this.agentWorkspaceService = agentWorkspaceService;
        this.agenticDockerService = agenticDockerService;
        this.systemLogger = systemLogger;
        this.objectMapper = objectMapper;
        this.serverPort = serverPort;
    }

    public boolean isRunning(Long agentId)
    {
        return runningStatus.containsKey(agentId);
    }

    public String getStatus(Long agentId)
    {
        return runningStatus.get(agentId);
    }

    /**
     * Snapshot of every currently-running agent's status text, keyed by agent id. Used by the
     * sidebar's polling endpoint - a copy so callers don't hold a live view into the internal map.
     */
    public Map<Long, String> getAllStatuses()
    {
        return new HashMap<>(runningStatus);
    }

    /** Text and tool-activity streamed so far for a running turn - see {@link #runningTurnState}. */
    public static final class PartialTurn
    {
        private final String text;
        private final String toolActivityJson;

        private PartialTurn(String text, String toolActivityJson)
        {
            this.text = text;
            this.toolActivityJson = toolActivityJson;
        }

        public String getText()
        {
            return text;
        }

        public String getToolActivityJson()
        {
            return toolActivityJson;
        }
    }

    /**
     * Snapshot of a currently-running turn's output so far, for AgentController.chat() to render
     * immediately on page load instead of the page showing nothing until the turn completes - see
     * {@link #runningTurnState}. Null if the agent isn't currently running.
     */
    public PartialTurn getPartialTurn(Long agentId)
    {
        RunningTurnState state = runningTurnState.get(agentId);

        return state == null ? null : new PartialTurn(state.snapshotText(), state.snapshotToolActivityJson());
    }

    /**
     * Lets a freshly (re)loaded chat page attach to a turn that's already running - e.g. after
     * navigating away mid-turn and back, or simply looking at an agent while one of its scheduled/
     * agent-triggered turns happens to be in flight. The emitter then receives the same live events
     * {@link #executeTurn} broadcasts to every other subscriber (including whichever request
     * originally started the turn, if still connected) - see {@link #broadcastSend}. Returns false
     * (caller should just complete the emitter itself) if the agent isn't running, covering both
     * "never was" and the narrow race of the turn finishing in the gap between this check and
     * subscribing, which the follow-up isRunning check below self-heals.
     */
    public boolean attachToRunningTurn(Long agentId, SseEmitter emitter)
    {
        if (!isRunning(agentId)) {
            return false;
        }

        addSubscriber(agentId, emitter);

        if (!isRunning(agentId)) {
            CopyOnWriteArrayList<SseEmitter> list = subscribers.get(agentId);

            if (list != null) {
                list.remove(emitter);
            }

            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }

        return true;
    }

    /**
     * Manually ends a running turn (chat/scheduled/agent-triggered alike, all funnel through the
     * same {@link #executeTurn} and its {@link #runningProcesses} entry) instead of waiting for it
     * to finish or hang forever - there is no automatic timeout any more. Returns false if the
     * agent isn't currently running (e.g. a race where the turn just finished on its own).
     */
    public boolean stopTurn(Long agentId)
    {
        Process process = runningProcesses.get(agentId);

        if (process == null) {
            return false;
        }

        stopRequested.add(agentId);
        process.destroyForcibly();

        Agent agent = agentDAO.getAgentForId(agentId);
        AgenticProject project = agent != null ? agent.getAgenticProject() : null;

        if (project != null && project.getDockerIsolationEnabled()) {
            killDockerClaudeProcess(project);
        }

        return true;
    }

    public void resetSession(Agent agent)
    {
        agent.setClaudeSessionId(null);
        agentDAO.saveOrUpdateAgent(agent);

        AgentMessage resetMessage = new AgentMessage();
        resetMessage.setAgent(agent);
        resetMessage.setRole(AgentMessageRole.SYSTEM);
        resetMessage.setContent("Session reset. The Claude CLI's memory of this conversation was cleared; the next message starts a new conversation.");
        resetMessage.setCreatedAt(new Date());
        agentMessageDAO.saveAgentMessage(resetMessage);
    }

    public void runTurn(Long agentId, String userMessage, MultipartFile[] files, SseEmitter emitter)
    {
        Agent agent = agentDAO.getAgentForId(agentId);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        String validationError = validateAgentAndConfig(agent, config);

        if (validationError != null) {
            completeWithError(emitter, validationError);
            return;
        }

        if (runningStatus.putIfAbsent(agentId, "Starting…") != null) {
            completeWithError(emitter, "A request is already running for this agent.");
            return;
        }

        String[] attachmentsJson = new String[1];
        String messageForCli;

        try {
            messageForCli = saveAttachmentsAndBuildNote(agent, config, files, userMessage, attachmentsJson);
        } catch (IOException e) {
            runningStatus.remove(agentId);
            completeWithError(emitter, "File upload failed: " + e.getMessage());
            return;
        }

        persistUserMessage(agent, userMessage, attachmentsJson[0]);

        // Completion/error is now broadcast from inside executeTurn itself (to every subscriber,
        // not just this one emitter) - see completeAllSubscribers - so this thread doesn't need to
        // react to executeTurn's return value itself any more.
        new Thread(() -> executeTurn(agent, config, messageForCli, emitter)).start();
    }

    /**
     * Headless counterpart of {@link #runTurn} for scheduled background tasks (see
     * AgentScheduleJob) - no browser attached, so it runs synchronously (Quartz's own worker pool
     * already provides concurrency; no Thread spawn needed) and returns the result directly instead
     * of streaming it. Returns {@code null} on success, {@link #SKIPPED_RESULT} if the agent already
     * has a turn in flight (manual or another schedule - callers must check for this distinct case,
     * it is not a failure), or an error message string.
     */
    public String runScheduledTurn(Long agentId, String taskPrompt)
    {
        Agent agent = agentDAO.getAgentForId(agentId);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        String validationError = validateAgentAndConfig(agent, config);

        if (validationError != null) {
            return validationError;
        }

        if (runningStatus.putIfAbsent(agentId, "Starting… (scheduled)") != null) {
            return SKIPPED_RESULT;
        }

        persistUserMessage(agent, taskPrompt, null);

        return executeTurn(agent, config, taskPrompt, null);
    }

    /**
     * Outcome of {@link #runAgentTriggeredTurn}. Exactly one of getReply()/isSkipped()/getErrorMessage()
     * is meaningful. Deliberately a typed result rather than {@link #runScheduledTurn}'s plain-String
     * null/{@link #SKIPPED_RESULT}/error-text contract - that contract only stays unambiguous because
     * a scheduled turn's success case never has to carry payload text back through the same channel;
     * this one does (the reply text), so overloading String further would make a genuine reply that
     * happens to start with "Error:" or equal "SKIPPED" indistinguishable from a real failure/skip.
     * Only one caller (AgentMessageApiController), so the extra type costs nothing.
     */
    public static final class AgentTriggeredTurnResult
    {
        private final String reply;
        private final boolean skipped;
        private final String errorMessage;

        private AgentTriggeredTurnResult(String reply, boolean skipped, String errorMessage)
        {
            this.reply = reply;
            this.skipped = skipped;
            this.errorMessage = errorMessage;
        }

        static AgentTriggeredTurnResult success(String reply)
        {
            return new AgentTriggeredTurnResult(reply, false, null);
        }

        static AgentTriggeredTurnResult skipped()
        {
            return new AgentTriggeredTurnResult(null, true, null);
        }

        static AgentTriggeredTurnResult error(String message)
        {
            return new AgentTriggeredTurnResult(null, false, message);
        }

        public String getReply()
        {
            return reply;
        }

        public boolean isSkipped()
        {
            return skipped;
        }

        public String getErrorMessage()
        {
            return errorMessage;
        }
    }

    /**
     * Headless counterpart of {@link #runTurn} for agent-to-agent messaging (see
     * AgentMessageApiController) - {@code fromAgent} synchronously triggers a turn on
     * {@code targetAgentId} and gets the reply text back. Mirrors {@link #runScheduledTurn}'s
     * validate -> busy-guard -> executeTurn shape, except the incoming message is persisted with
     * role=AGENT/fromAgent=&lt;caller&gt; (see {@link #persistIncomingAgentMessage}) instead of
     * role=USER, and on success the just-persisted ASSISTANT reply is read back via
     * agentMessageDAO rather than restructuring executeTurn's own null-on-success return contract,
     * which runTurn/runScheduledTurn's other call sites depend on unchanged.
     *
     * The busy-guard ({@link #runningStatus}) doubles as the circuit-breaker against the most
     * dangerous loop shape: fromAgent is itself marked "running" for this whole synchronous call, so
     * if targetAgent's own turn tries to message fromAgent back while fromAgent is still blocked
     * here, that inner call immediately sees fromAgent as busy and gets isSkipped()=true rather than
     * hanging. Slower-burning ping-pong across separate top-level turns is not prevented here - see
     * the loop-avoidance guidance in buildAutoAppendedSystemPrompt instead; a hop-count/depth limit
     * is a possible future addition, not v1.
     */
    public AgentTriggeredTurnResult runAgentTriggeredTurn(Long targetAgentId, Agent fromAgent, String message)
    {
        Agent targetAgent = agentDAO.getAgentForId(targetAgentId);
        AgenticConfig config = agenticConfigDAO.getAgenticConfig();

        String validationError = validateAgentAndConfig(targetAgent, config);

        if (validationError != null) {
            return AgentTriggeredTurnResult.error(validationError);
        }

        if (runningStatus.putIfAbsent(targetAgentId, "Starting… (message from " + fromAgent.getName() + ")") != null) {
            return AgentTriggeredTurnResult.skipped();
        }

        persistIncomingAgentMessage(targetAgent, fromAgent, message);

        String error = executeTurn(targetAgent, config, message, null);

        if (error != null) {
            return AgentTriggeredTurnResult.error(error);
        }

        List<AgentMessage> messages = agentMessageDAO.getMessagesForAgent(targetAgentId);
        String reply = messages.isEmpty() ? "" : messages.get(messages.size() - 1).getContent();

        return AgentTriggeredTurnResult.success(reply);
    }

    private String validateAgentAndConfig(Agent agent, AgenticConfig config)
    {
        if (agent == null || !agent.getEnabled()) {
            return "Agent not found or disabled.";
        }

        if (config == null || !config.getEnabled() || isBlank(config.getClaudeCliPath()) || isBlank(config.getWorkspaceRoot())) {
            return "Agentic is not fully configured. Please set and enable the CLI path and workspace root under /agentic/config/.";
        }

        if (config.getAuthMode() != AgenticAuthMode.OAUTH && isBlank(config.getApiKey())) {
            return "API key missing (Auth Mode = API Key). Either set a key under /agentic/config/ or switch to OAuth.";
        }

        return null;
    }

    private void persistUserMessage(Agent agent, String content, String attachmentsJson)
    {
        AgentMessage userAgentMessage = new AgentMessage();
        userAgentMessage.setAgent(agent);
        userAgentMessage.setRole(AgentMessageRole.USER);
        userAgentMessage.setContent(content);
        userAgentMessage.setAttachments(attachmentsJson);
        userAgentMessage.setCreatedAt(new Date());
        agentMessageDAO.saveAgentMessage(userAgentMessage);
    }

    /** Sibling of {@link #persistUserMessage} for an agent-to-agent message - see {@link #runAgentTriggeredTurn}. */
    private void persistIncomingAgentMessage(Agent targetAgent, Agent fromAgent, String content)
    {
        AgentMessage message = new AgentMessage();
        message.setAgent(targetAgent);
        message.setRole(AgentMessageRole.AGENT);
        message.setFromAgent(fromAgent);
        message.setContent(content);
        message.setCreatedAt(new Date());
        agentMessageDAO.saveAgentMessage(message);
    }

    /**
     * Saves any uploaded files into the agent's own workspace (under uploads/, since that's
     * already the CLI process's cwd - the agent can Read/view them with its normal tools, no
     * special handling needed for images vs documents) and returns the message text to actually
     * send to the CLI, with a note appended pointing at the saved paths. {@code content} passed to
     * {@link #persistUserMessage} stays the clean user-typed text - only the note-appended version
     * goes to the CLI/its --resume session transcript. attachmentsJsonOut[0] is set to a JSON array
     * of {fileName, path} for the chat UI's attachment chips, or left null if no files were sent.
     */
    private String saveAttachmentsAndBuildNote(Agent agent, AgenticConfig config, MultipartFile[] files, String userMessage, String[] attachmentsJsonOut) throws IOException
    {
        if (files == null || files.length == 0) {
            return userMessage;
        }

        Path uploadsDir = agentWorkspaceService.ensureWorkspace(agent, config).resolve("uploads");
        Files.createDirectories(uploadsDir);

        ArrayNode attachments = objectMapper.createArrayNode();
        StringBuilder note = new StringBuilder(userMessage);
        note.append("\n\n[Angehängte Dateien in deinem Arbeitsverzeichnis - lies/betrachte sie bei Bedarf mit deinen Tools:");

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String safeName = sanitizeFilename(file.getOriginalFilename());
            String storedName = System.currentTimeMillis() + "-" + safeName;
            file.transferTo(uploadsDir.resolve(storedName));

            String relativePath = "uploads/" + storedName;
            note.append("\n- ").append(relativePath).append(" (").append(safeName).append(")");

            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("fileName", safeName);
            entry.put("path", relativePath);
            attachments.add(entry);
        }

        note.append("]");

        attachmentsJsonOut[0] = attachments.size() > 0 ? objectMapper.writeValueAsString(attachments) : null;

        return note.toString();
    }

    /**
     * Strips any directory components (path traversal) and replaces anything but a safe character
     * set, so an uploaded file's original name can't escape the uploads/ directory or collide with
     * shell-special characters when the agent later references it via Bash.
     */
    private String sanitizeFilename(String originalFilename)
    {
        if (isBlank(originalFilename)) {
            return "datei";
        }

        String baseName = Paths.get(originalFilename).getFileName().toString();
        String sanitized = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");

        return isBlank(sanitized) ? "datei" : sanitized;
    }

    /**
     * Runs one full turn (spawn -> stream -> persist). {@code emitter} may be null for headless/
     * scheduled runs - every emitter interaction below is guarded accordingly. Returns {@code null}
     * on success or an error message string on failure; never throws.
     */
    private String executeTurn(Agent agent, AgenticConfig config, String userMessage, SseEmitter emitter)
    {
        RunningTurnState turnState = new RunningTurnState(objectMapper);
        runningTurnState.put(agent.getId(), turnState);
        addSubscriber(agent.getId(), emitter);

        StringBuilder stderrOutput = new StringBuilder();
        String[] capturedSessionId = new String[1];
        RateLimitSnapshot rateLimitSnapshot = new RateLimitSnapshot();
        TurnUsage turnUsage = new TurnUsage();
        Process process = null;
        Path systemPromptFile = null;
        String turnError = null;

        AgenticProject project = agent.getAgenticProject();
        boolean useDocker = project != null && project.getDockerIsolationEnabled();

        try {
            runningStatus.put(agent.getId(), "Thinking…");

            Path workspace = agentWorkspaceService.ensureWorkspace(agent, config);
            Map<String, FileTime> filesBefore;

            try {
                filesBefore = snapshotDownloadsFolder(workspace);
            } catch (IOException e) {
                filesBefore = new HashMap<>();
            }

            if (useDocker) {
                try {
                    agenticDockerService.ensureContainer(project, config);
                } catch (IOException e) {
                    throw new IOException("Docker container for project '" + project.getName() + "' is not available: " + e.getMessage(), e);
                }
            }

            // The appended system prompt (scheduling instructions + downloads/ convention + the
            // agent's own prompt) is written to a file and passed via --append-system-prompt-file
            // instead of --append-system-prompt <text> directly - Java's ProcessBuilder on Windows
            // (confirmed empirically, JDK 8) silently truncates long command-line arguments
            // containing many embedded double quotes (this prompt's curl/JSON examples have several),
            // dropping everything after a certain point with no error. A short file path as the
            // actual argument sidesteps that entirely. For a Docker-isolated agent the file has to
            // live inside the bind-mounted workspace (a host temp file wouldn't be visible to `docker
            // exec`); for a host agent a plain temp file is simplest. Deleted again in the finally
            // block below either way.
            systemPromptFile = useDocker
                    ? workspace.resolve(".agentic-system-prompt.txt")
                    : Files.createTempFile("agentic-system-prompt-", ".txt");
            Files.write(systemPromptFile, buildAppendedSystemPrompt(agent).getBytes(StandardCharsets.UTF_8));

            ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(agent, config, userMessage, systemPromptFile));
            processBuilder.directory(workspace.toFile());

            if (!useDocker) {
                if (config.getAuthMode() != AgenticAuthMode.OAUTH) {
                    processBuilder.environment().put("ANTHROPIC_API_KEY", config.getApiKey());
                }
                // OAUTH mode: no ANTHROPIC_API_KEY set, CLI falls back to credentials from `claude
                // login` run interactively, once, as the same OS user that starts the KSFX process.

                // Passed as an env var (not a literal in the prompt text) so it never appears in
                // stream-json output, AgentMessage.toolActivity, the chat UI, or the CLI's own on-disk
                // transcript - see buildAppendedSystemPrompt, which tells the agent to reference
                // $KSFX_AGENT_TOKEN rather than write the value itself.
                processBuilder.environment().put("KSFX_AGENT_TOKEN", agent.getApiToken());
            }
            // useDocker: the same two values are passed as `-e` args to `docker exec` in
            // buildCommand instead - `docker exec` doesn't forward this (host) process's env to the
            // container without them being named explicitly.

            process = processBuilder.start();
            runningProcesses.put(agent.getId(), process);

            final Process startedProcess = process;
            Thread stderrDrain = new Thread(() -> drainStream(startedProcess.getErrorStream(), stderrOutput));
            stderrDrain.start();

            readStdout(process, agent.getId(), turnState, capturedSessionId, rateLimitSnapshot, turnUsage);

            process.waitFor();

            stderrDrain.join(TimeUnit.SECONDS.toMillis(5));

            if (process.exitValue() != 0) {
                if (stopRequested.remove(agent.getId())) {
                    throw new IOException("Turn stopped by user.");
                }

                throw new IOException("Claude CLI process exited with exit code " + process.exitValue() + ": " + stderrOutput);
            }

            if (capturedSessionId[0] != null) {
                agent.setClaudeSessionId(capturedSessionId[0]);
                agentDAO.saveOrUpdateAgent(agent);
            }

            ArrayNode generatedFiles = objectMapper.createArrayNode();

            try {
                Map<String, FileTime> filesAfter = snapshotDownloadsFolder(workspace);

                for (Map.Entry<String, FileTime> entry : filesAfter.entrySet()) {
                    FileTime previous = filesBefore.get(entry.getKey());

                    if (previous == null || entry.getValue().compareTo(previous) > 0) {
                        ObjectNode fileEntry = objectMapper.createObjectNode();
                        fileEntry.put("fileName", Paths.get(entry.getKey()).getFileName().toString());
                        fileEntry.put("path", entry.getKey());
                        generatedFiles.add(fileEntry);
                    }
                }
            } catch (IOException e) {
                // best-effort - a scan failure shouldn't take down an otherwise-successful turn
            }

            AgentMessage assistantMessage = new AgentMessage();
            assistantMessage.setAgent(agent);
            assistantMessage.setRole(AgentMessageRole.ASSISTANT);
            assistantMessage.setContent(turnState.snapshotText());
            assistantMessage.setToolActivity(turnState.snapshotToolActivityJson());
            assistantMessage.setGeneratedFiles(generatedFiles.size() > 0 ? objectMapper.writeValueAsString(generatedFiles) : null);
            assistantMessage.setInputTokens(turnUsage.inputTokens);
            assistantMessage.setOutputTokens(turnUsage.outputTokens);
            assistantMessage.setCacheCreationInputTokens(turnUsage.cacheCreationInputTokens);
            assistantMessage.setCacheReadInputTokens(turnUsage.cacheReadInputTokens);
            assistantMessage.setDurationMs(turnUsage.durationMs);
            assistantMessage.setCreatedAt(new Date());
            agentMessageDAO.saveAgentMessage(assistantMessage);

            if (rateLimitSnapshot.status != null) {
                config.setClaudeRateLimitStatus(rateLimitSnapshot.status);
                config.setClaudeRateLimitType(rateLimitSnapshot.rateLimitType);
                config.setClaudeRateLimitResetsAt(rateLimitSnapshot.resetsAt != null ? new Date(rateLimitSnapshot.resetsAt * 1000L) : null);
                config.setClaudeRateLimitOverageStatus(rateLimitSnapshot.overageStatus);
                config.setClaudeRateLimitOverageResetsAt(rateLimitSnapshot.overageResetsAt != null ? new Date(rateLimitSnapshot.overageResetsAt * 1000L) : null);
                config.setClaudeRateLimitUsingOverage(rateLimitSnapshot.usingOverage);
                config.setClaudeRateLimitUpdatedAt(new Date());
                agenticConfigDAO.saveOrUpdateAgenticConfig(config);
            }

            if (turnUsage.inputTokens != null) {
                broadcastSend(agent.getId(), SseEmitter.event().name("usage").data(objectMapper.writeValueAsString(turnUsage.inputTokens + turnUsage.outputTokens)));
            }

            if (generatedFiles.size() > 0) {
                broadcastSend(agent.getId(), SseEmitter.event().name("generated_files").data(objectMapper.writeValueAsString(generatedFiles)));
            }

            return null;
        } catch (Exception e) {
            systemLogger.logMessage("AGENTIC", "Agent turn failed for agent " + agent.getId(), e);

            AgentMessage errorMessage = new AgentMessage();
            errorMessage.setAgent(agent);
            errorMessage.setRole(AgentMessageRole.SYSTEM);
            errorMessage.setContent("Error: " + e.getMessage());
            errorMessage.setCreatedAt(new Date());
            agentMessageDAO.saveAgentMessage(errorMessage);

            turnError = "Error: " + e.getMessage() + StacktraceUtil.getStackTrace(e);
            return turnError;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();

                if (useDocker) {
                    killDockerClaudeProcess(project);
                }
            }

            if (systemPromptFile != null) {
                try {
                    Files.deleteIfExists(systemPromptFile);
                } catch (IOException ignored) {
                    // best-effort - a leftover temp file here is harmless clutter, not a functional problem
                }
            }

            // Order matters: clear runningStatus (which isRunning()/attachToRunningTurn key off of)
            // *before* broadcasting completion, so a concurrent attachToRunningTurn call either sees
            // "not running" up front and never subscribes, or - the narrow remaining race - subscribes
            // just after completeAllSubscribers already ran and self-heals via its own follow-up
            // isRunning() check (which by then correctly reflects "not running" too, since this
            // ordering guarantees runningStatus is already cleared). The other order (broadcast first,
            // clear after) would leave a window where isRunning() still says true right after
            // completion already fired, and a subscriber added in that window would never be told
            // it's over.
            runningStatus.remove(agent.getId());
            runningProcesses.remove(agent.getId());
            stopRequested.remove(agent.getId());
            runningTurnState.remove(agent.getId());
            completeAllSubscribers(agent.getId(), turnError);
        }
    }

    private void addSubscriber(Long agentId, SseEmitter emitter)
    {
        if (emitter == null) {
            return;
        }

        subscribers.computeIfAbsent(agentId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    /** Sends to every current subscriber for {@code agentId} - see {@link #subscribers}. No-op (not an error) if nobody's watching right now. */
    private void broadcastSend(Long agentId, SseEmitter.SseEventBuilder event)
    {
        CopyOnWriteArrayList<SseEmitter> list = subscribers.get(agentId);

        if (list == null) {
            return;
        }

        for (SseEmitter subscriber : list) {
            try {
                subscriber.send(event);
            } catch (Exception ignored) {
                // Gone (browser navigated away, tab closed, etc.) - drop it rather than let it keep
                // failing every subsequent send for the rest of this turn.
                list.remove(subscriber);
            }
        }
    }

    /**
     * Ends every current subscriber for {@code agentId} (the request that started the turn, plus
     * any that attached mid-flight via {@link #attachToRunningTurn}) - {@code errorMessage} null for
     * a clean finish, or the same text {@link #executeTurn} returns to its own (headless) callers
     * for a failed one. Always removes the subscriber list, even if it was empty/absent, so nothing
     * lingers in {@link #subscribers} past the turn's own lifetime.
     */
    private void completeAllSubscribers(Long agentId, String errorMessage)
    {
        CopyOnWriteArrayList<SseEmitter> list = subscribers.remove(agentId);

        if (list == null) {
            return;
        }

        for (SseEmitter subscriber : list) {
            try {
                if (errorMessage != null) {
                    subscriber.send(SseEmitter.event().name("error").data(errorMessage));
                    subscriber.completeWithError(new IllegalStateException(errorMessage));
                } else {
                    subscriber.complete();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Killing the {@code docker exec} client process (via destroyForcibly above) does not kill the
     * process tree it spawned inside the container - so on a timeout/abort for a Docker-isolated
     * agent, this best-effort follow-up asks the container itself to kill any running `claude`
     * process, rather than leaving it running unattended in the background.
     */
    private void killDockerClaudeProcess(AgenticProject project)
    {
        try {
            new ProcessBuilder("docker", "exec", agenticDockerService.containerNameFor(project.getId()), "pkill", "-f", "claude")
                    .start().waitFor(5, TimeUnit.SECONDS);
        } catch (IOException ignored) {
            // best-effort - docker itself may be unreachable
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The workspace can accumulate arbitrary amounts of scratch/tool/dependency clutter that has
     * nothing to do with what the user actually asked for (node_modules/, log files, package-manager
     * caches, whatever a script happens to write) - a name-based *exclusion* list was tried first
     * (see git history) and abandoned: it's an unbounded blocklist that can never enumerate every
     * kind of noise a tool might create, and each gap re-surfaces as a fresh flood of junk download
     * chips. This flips it to a small, explicit *inclusion* allowlist instead - a single named
     * folder, taught to the agent via the system prompt (see buildAutoAppendedSystemPrompt): only
     * files under downloads/ are ever offered as download chips, nothing else in the workspace is
     * scanned or considered, regardless of what else accumulates there.
     */
    private static final String DOWNLOADS_DIRECTORY_NAME = "downloads";

    /**
     * Snapshots workspace/downloads/ only (not the rest of the workspace - see
     * {@link #DOWNLOADS_DIRECTORY_NAME}), returning each file's path (relative to the workspace,
     * forward-slash-separated, so it still starts with "downloads/") mapped to its last-modified
     * time. Called once before and once after a turn; the diff between the two snapshots is how
     * "generated files" are detected.
     */
    private Map<String, FileTime> snapshotDownloadsFolder(Path workspace) throws IOException
    {
        Map<String, FileTime> snapshot = new HashMap<>();
        Path downloads = workspace.resolve(DOWNLOADS_DIRECTORY_NAME);

        if (!Files.isDirectory(downloads)) {
            return snapshot;
        }

        try (Stream<Path> walk = Files.walk(downloads)) {
            walk.filter(Files::isRegularFile)
                .forEach(p -> {
                    try {
                        snapshot.put(workspace.relativize(p).toString().replace('\\', '/'), Files.getLastModifiedTime(p));
                    } catch (IOException ignored) {
                        // file vanished mid-walk or unreadable - just omit it from the snapshot
                    }
                });
        }

        return snapshot;
    }

    /**
     * Flags verified against the installed CLI's --help before go-live; kept isolated here so a
     * version mismatch is a one-line fix. systemPromptFile must already exist and contain the
     * appended system prompt as UTF-8 - see the caller (executeTurn) and the class comment on
     * --append-system-prompt-file above for why this is a file, not an inline argument.
     *
     * For a Docker-isolated agent (see AgenticDockerService), the whole `claude` invocation is
     * wrapped in `docker exec` against the agent's project container instead of running directly on
     * the host - same flags either way, only how the process is launched (and where its cwd/env come
     * from) differs. No `-t`/`-i`: a pseudo-tty would corrupt the --output-format stream-json framing
     * this class's stdout parser relies on.
     */
    private List<String> buildCommand(Agent agent, AgenticConfig config, String userMessage, Path systemPromptFile)
    {
        AgenticProject project = agent.getAgenticProject();
        boolean useDocker = project != null && project.getDockerIsolationEnabled();

        List<String> claudeArgs = new ArrayList<>();
        claudeArgs.add(useDocker ? "claude" : config.getClaudeCliPath());
        claudeArgs.add("-p");
        claudeArgs.add(userMessage);
        claudeArgs.add("--output-format");
        claudeArgs.add("stream-json");
        claudeArgs.add("--verbose");
        claudeArgs.add("--permission-mode");
        claudeArgs.add(!isBlank(agent.getPermissionMode()) ? agent.getPermissionMode() : config.getDefaultPermissionMode());

        if (!isBlank(agent.getClaudeSessionId())) {
            claudeArgs.add("--resume");
            claudeArgs.add(agent.getClaudeSessionId());
        }

        claudeArgs.add("--append-system-prompt-file");

        if (!useDocker) {
            claudeArgs.add(systemPromptFile.toAbsolutePath().toString());
            return claudeArgs;
        }

        // Inside the container the workspace is bind-mounted at /workspace/agent-<id> (see
        // AgentWorkspaceService/AgenticDockerService) - systemPromptFile is a host path under that
        // same folder, so this is just its container-side equivalent, not a second file.
        claudeArgs.add("/workspace/agent-" + agent.getId() + "/" + systemPromptFile.getFileName());

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("exec");
        // Runs as AgenticDockerService.CONTAINER_USER, not root - the claude CLI itself refuses
        // --dangerously-skip-permissions (bypassPermissions mode) under UID 0, so the container
        // boots/is-administered as root but the actual claude process runs unprivileged instead
        // (with passwordless sudo available inside the container for anything still needing root).
        command.add("-u");
        command.add(agenticDockerService.containerUser());
        command.add("-w");
        command.add("/workspace/agent-" + agent.getId());

        if (config.getAuthMode() != AgenticAuthMode.OAUTH) {
            command.add("-e");
            command.add("ANTHROPIC_API_KEY=" + config.getApiKey());
        }

        command.add("-e");
        command.add("KSFX_AGENT_TOKEN=" + agent.getApiToken());
        command.add(agenticDockerService.containerNameFor(project.getId()));
        command.addAll(claudeArgs);

        return command;
    }

    /**
     * Every agent gets self-service scheduling instructions appended to its system prompt (not
     * just agents with a custom prompt - this is a built-in capability now), teaching it to manage
     * its own AgentSchedule rows via curl against /agentic/api/schedule. The token is referenced as
     * $KSFX_AGENT_TOKEN (an env var set on this process, see executeTurn) rather than written as a
     * literal value, so it never ends up in AgentMessage.toolActivity/the chat UI/the CLI's own
     * transcript when the agent's tool call gets recorded.
     */
    private String buildAppendedSystemPrompt(Agent agent)
    {
        if (isBlank(agent.getApiToken())) {
            agent.setApiToken(java.util.UUID.randomUUID().toString().replace("-", ""));
            agentDAO.saveOrUpdateAgent(agent);
        }

        return buildAutoAppendedSystemPrompt(agent);
    }

    /**
     * The complete text appended (via --append-system-prompt-file) ahead of/around whatever the
     * CLI's own default system prompt already contains - a three-tier hierarchy, each tier optional
     * except the first:
     * <ol>
     *     <li>Hardcoded platform boilerplate (scheduling API instructions; the code/ organizational
     *     convention for coding-task work, purely a tidiness convention with no functional effect
     *     since the downloads/ change below; the downloads/ convention - see
     *     {@link #DOWNLOADS_DIRECTORY_NAME} - which is what actually determines what surfaces as a
     *     download suggestion; and, if the agent has an AgenticProject, notes about its shared/
     *     folder and Docker isolation/sudo)</li>
     *     <li>{@link AgenticProject#getSystemPrompt()}, if the agent has a project and it's set</li>
     *     <li>{@link Agent#getSystemPrompt()}, if set</li>
     * </ol>
     * All three are joined the same bare way - see the comment on the final concatenation below for
     * why. Pure/side-effect-free (unlike {@link #buildAppendedSystemPrompt}, which also lazily
     * backfills agent.apiToken - a backfill that never affects this method's *output*, since the
     * token value itself is never inlined into the prompt text, only referenced by env-var name) so
     * it doubles as the exact text shown as a read-only hint on the agent edit page - see
     * AgentController.edit()/submit() - keeping that preview from ever drifting out of sync with
     * what's actually sent to the CLI.
     */
    public String buildAutoAppendedSystemPrompt(Agent agent)
    {
        // A Docker-isolated agent's curl calls originate from inside its container, where
        // "localhost" is the container itself, not the KSFX host - host.docker.internal is Docker's
        // standard way to reach the host from inside a container (works with the --add-host flag
        // AgenticDockerService.ensureContainer sets on `docker run`). Non-isolated agents keep
        // calling plain localhost exactly as before. Shared by both the scheduling and the
        // agent-messaging sections below - both are /agentic/api/** self-service calls.
        String agentApiBaseUrl = (agent.getAgenticProject() != null && agent.getAgenticProject().getDockerIsolationEnabled())
                ? "http://host.docker.internal:" + serverPort
                : "http://localhost:" + serverPort;

        String schedulingPrompt = "Du kannst eigene wiederkehrende Hintergrundaufgaben (Scheduled Tasks) verwalten, "
                + "indem du mit dem Bash-Tool curl gegen die lokale KSFX-API aufrufst. Authentifiziere dich dabei "
                + "IMMER über die bereits gesetzte Umgebungsvariable $KSFX_AGENT_TOKEN (NIEMALS den Wert selbst "
                + "aufschreiben oder raten - referenziere ausschließlich $KSFX_AGENT_TOKEN im curl-Aufruf). "
                + "Basis-URL: " + agentApiBaseUrl + "/agentic/api/schedule\n\n"
                + "Endpunkte:\n"
                + "- GET  /agentic/api/schedule            -> Liste deiner eigenen geplanten Aufgaben\n"
                + "- POST /agentic/api/schedule            -> neue Aufgabe anlegen, JSON-Body: "
                + "{\"name\":\"...\",\"taskPrompt\":\"...\",\"cronSchedule\":\"0 0 9 * * ?\",\"cronScheduleEnabled\":true}\n"
                + "- PUT  /agentic/api/schedule/{id}        -> bestehende Aufgabe aktualisieren (gleicher Body)\n"
                + "- DELETE /agentic/api/schedule/{id}      -> Aufgabe löschen\n\n"
                + "WICHTIG: cronSchedule verwendet Quartz-Cron-Syntax mit 6-7 Feldern (Sekunde zuerst), NICHT "
                + "Standard-5-Feld-Cron. Beispiel für 'täglich um 9 Uhr': \"0 0 9 * * ?\". Beispiel-Aufruf:\n"
                + "curl -s -X POST " + agentApiBaseUrl + "/agentic/api/schedule "
                + "-H \"Authorization: Bearer $KSFX_AGENT_TOKEN\" -H \"Content-Type: application/json\" "
                + "-d '{\"name\":\"Tägliche Erinnerung\",\"taskPrompt\":\"Prüfe X\",\"cronSchedule\":\"0 0 9 * * ?\",\"cronScheduleEnabled\":true}'\n";

        List<Agent> otherAgents = agentDAO.getAllAgents().stream()
                .filter(a -> a.getEnabled() && !a.getId().equals(agent.getId()))
                .sorted(Comparator.comparing(Agent::getName))
                .collect(Collectors.toList());

        schedulingPrompt += "\nDu kannst auch andere Agenten direkt ansprechen (synchron - du wartest auf ihre "
                + "Antwort), indem du mit dem Bash-Tool curl gegen die lokale KSFX-API aufrufst. Authentifiziere "
                + "dich dabei IMMER über $KSFX_AGENT_TOKEN. Endpunkt: POST " + agentApiBaseUrl + "/agentic/api/message\n"
                + "Body: {\"targetAgentId\":<id>,\"message\":\"...\"}\n"
                + "Die Antwort des Ziel-Agenten kommt direkt als JSON zurück: "
                + "{\"targetAgentId\":<id>,\"targetAgentName\":\"...\",\"reply\":\"...\"}\n"
                + "Beispiel-Aufruf:\n"
                + "curl -s -X POST " + agentApiBaseUrl + "/agentic/api/message "
                + "-H \"Authorization: Bearer $KSFX_AGENT_TOKEN\" -H \"Content-Type: application/json\" "
                + "-d '{\"targetAgentId\":123,\"message\":\"...\"}'\n"
                + "WICHTIG: Antworte nicht reflexhaft auf eine eingehende Nachricht eines anderen Agenten, indem du "
                + "sofort wieder zurückschreibst - das kann zu einer Endlosschleife führen (A wartet auf B, B "
                + "antwortet sofort wieder an A, usw.). Nutze diese Funktion gezielt, nicht automatisch.\n";

        if (!otherAgents.isEmpty()) {
            StringBuilder directory = new StringBuilder("\nVerfügbare Ziel-Agenten (id: Name):\n");

            for (Agent other : otherAgents) {
                directory.append("- ").append(other.getId()).append(": ").append(other.getName()).append("\n");
            }

            schedulingPrompt += directory;
        }

        schedulingPrompt += "\nFür Coding-Aufgaben (z.B. Git-Checkouts, größere Projektstrukturen) legst du diese, "
                + "sofern nicht explizit anders gewünscht, unter einem Unterordner code/ in deinem Arbeitsverzeichnis "
                + "an, nicht direkt im Hauptverzeichnis.\n";

        schedulingPrompt += "\nWenn der Nutzer dich explizit um eine fertige Datei zum Download bittet (z.B. einen "
                + "Bericht, eine PowerPoint, ein Bild, eine CSV), legst du GENAU DIESE fertige Datei zusätzlich unter "
                + "einem Unterordner downloads/ in deinem Arbeitsverzeichnis ab (z.B. downloads/bericht.pptx) - "
                + "NUR Dateien dort werden dir im Chat als Download-Vorschlag angezeigt, alles andere in deinem "
                + "Arbeitsverzeichnis (Zwischenstände, Abhängigkeiten, Logs, Caches etc.) bleibt unsichtbar für den "
                + "Nutzer. Lege dort nichts ab, worum der Nutzer nicht explizit gebeten hat.\n";

        if (agent.getAgenticProject() != null) {
            schedulingPrompt += "\nGeteilte Ressourcen deines Agentic Projects findest du unter ../shared "
                    + "(relativ zu deinem eigenen Arbeitsverzeichnis).\n";

            if (agent.getAgenticProject().getDockerIsolationEnabled()) {
                schedulingPrompt += "\nDu läufst isoliert in einem eigenen Docker-Container (Ubuntu) als "
                        + "normaler Benutzer, nicht als root. Für System-Installationen (z.B. apt-get, "
                        + "Paketmanager) steht dir passwortloses sudo zur Verfügung - stelle Bash-Befehlen, "
                        + "die root-Rechte benötigen, einfach sudo voran.\n";
            }
        }

        // Middle tier of the hardcoded-boilerplate -> AgenticProject.systemPrompt -> Agent.systemPrompt
        // hierarchy - only present for an agent actually assigned to a project, same gating as the
        // structural notes just above. Joined the same bare, seamless way as agent.systemPrompt
        // below (see that comment) - a blank-line paragraph break, no "---"/label/attribution, since
        // that's what was found to actually work rather than trigger prompt-injection suspicion.
        String agenticProjectSystemPrompt = agent.getAgenticProject() != null ? agent.getAgenticProject().getSystemPrompt() : null;

        if (!isBlank(agenticProjectSystemPrompt)) {
            schedulingPrompt += "\n\n" + agenticProjectSystemPrompt;
        }

        if (isBlank(agent.getSystemPrompt())) {
            return schedulingPrompt;
        }

        // No separator, no label, no attribution sentence - both were tried (a bare "---" block and
        // a "this is legitimate, not injected" label) and both measurably backfired: the model
        // treated any special framing around the custom text as itself a sign of tampering, more so
        // for the label than the plain separator. Simplest option turned out best: just concatenate,
        // exactly like two paragraphs of the same document, no visual or textual seam at all.
        return schedulingPrompt + "\n\n" + agent.getSystemPrompt();
    }

    private void readStdout(Process process, Long agentId, RunningTurnState turnState, String[] capturedSessionId, RateLimitSnapshot rateLimitSnapshot, TurnUsage turnUsage) throws IOException
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    handleStreamEvent(objectMapper.readTree(line), agentId, turnState, capturedSessionId, rateLimitSnapshot, turnUsage);
                } catch (IOException parseException) {
                    systemLogger.logMessage("AGENTIC", "Konnte stream-json Zeile nicht parsen: " + line, parseException);
                }
            }
        }
    }

    /**
     * Tool activity is forwarded (live, via SSE broadcast - see {@link #broadcastSend}) and
     * persisted (in {@link AgentMessage#getToolActivity()}) as structured JSON -
     * {"type":"tool_use","tool":...,"input":...} / {"type":"tool_result","result":...} - rather than
     * pre-formatted text, so the browser (agentic-chat.js) can render one rich representation for
     * both the live stream and history reloaded from the DB, instead of duplicating formatting
     * logic in Java and JS. Broadcasting is a no-op if nobody's currently subscribed (headless/
     * scheduled runs, or a browser tab that navigated away) - {@code turnState} still accumulates
     * everything regardless, so a page loaded/reloaded later can still catch up via
     * {@link #getPartialTurn}.
     */
    private void handleStreamEvent(JsonNode event, Long agentId, RunningTurnState turnState, String[] capturedSessionId, RateLimitSnapshot rateLimitSnapshot, TurnUsage turnUsage) throws IOException
    {
        String type = event.path("type").asText("");

        if ("assistant".equals(type)) {
            for (JsonNode contentBlock : event.path("message").path("content")) {
                String blockType = contentBlock.path("type").asText("");

                if ("text".equals(blockType)) {
                    String text = contentBlock.path("text").asText("");
                    turnState.appendText(text);
                    runningStatus.put(agentId, "Responding…");

                    // JSON-encoded (like tool_use/tool_result below), not sent raw: a raw
                    // multi-line string here breaks the client's naive SSE event-boundary
                    // parsing (it splits on a blank line, which a paragraph break inside the
                    // text also produces) and truncates the live-rendered response, even
                    // though turnState/the DB copy stays complete either way.
                    broadcastSend(agentId, SseEmitter.event().name("text").data(objectMapper.writeValueAsString(text)));
                } else if ("tool_use".equals(blockType)) {
                    String toolName = contentBlock.path("name").asText("tool");

                    ObjectNode toolUseEntry = objectMapper.createObjectNode();
                    toolUseEntry.put("type", "tool_use");
                    toolUseEntry.put("tool", toolName);
                    toolUseEntry.set("input", contentBlock.path("input"));
                    turnState.addToolActivity(toolUseEntry);

                    runningStatus.put(agentId, "Tool: " + toolName);

                    broadcastSend(agentId, SseEmitter.event().name("tool_use").data(objectMapper.writeValueAsString(toolUseEntry)));
                }
            }
        } else if ("user".equals(type)) {
            for (JsonNode contentBlock : event.path("message").path("content")) {
                if ("tool_result".equals(contentBlock.path("type").asText(""))) {
                    String result = contentBlock.path("content").isTextual()
                            ? contentBlock.path("content").asText()
                            : contentBlock.path("content").toString();

                    ObjectNode toolResultEntry = objectMapper.createObjectNode();
                    toolResultEntry.put("type", "tool_result");
                    toolResultEntry.put("result", result);
                    turnState.addToolActivity(toolResultEntry);

                    runningStatus.put(agentId, "Evaluating tool result…");

                    broadcastSend(agentId, SseEmitter.event().name("tool_result").data(objectMapper.writeValueAsString(toolResultEntry)));
                }
            }
        } else if ("rate_limit_event".equals(type)) {
            JsonNode info = event.path("rate_limit_info");
            rateLimitSnapshot.status = info.path("status").asText(null);
            rateLimitSnapshot.rateLimitType = info.path("rateLimitType").asText(null);
            rateLimitSnapshot.resetsAt = info.hasNonNull("resetsAt") ? info.path("resetsAt").asLong() : null;
            rateLimitSnapshot.overageStatus = info.path("overageStatus").asText(null);
            rateLimitSnapshot.overageResetsAt = info.hasNonNull("overageResetsAt") ? info.path("overageResetsAt").asLong() : null;
            rateLimitSnapshot.usingOverage = info.hasNonNull("isUsingOverage") ? info.path("isUsingOverage").asBoolean() : null;
        } else if ("result".equals(type)) {
            if (event.hasNonNull("session_id")) {
                capturedSessionId[0] = event.path("session_id").asText();
            }

            JsonNode usage = event.path("usage");
            turnUsage.inputTokens = usage.hasNonNull("input_tokens") ? usage.path("input_tokens").asInt() : null;
            turnUsage.outputTokens = usage.hasNonNull("output_tokens") ? usage.path("output_tokens").asInt() : null;
            turnUsage.cacheCreationInputTokens = usage.hasNonNull("cache_creation_input_tokens") ? usage.path("cache_creation_input_tokens").asInt() : null;
            turnUsage.cacheReadInputTokens = usage.hasNonNull("cache_read_input_tokens") ? usage.path("cache_read_input_tokens").asInt() : null;
            turnUsage.durationMs = event.hasNonNull("duration_ms") ? event.path("duration_ms").asInt() : null;
        }
    }

    /** Latest Claude subscription-plan rate limit seen during a turn - see stream-json's "rate_limit_event". */
    /**
     * Output accumulated so far for one running turn - text and tool activity, mirroring exactly
     * what ends up on the persisted {@link AgentMessage} once the turn finishes (see executeTurn's
     * success path). Appends all happen on the single thread executing that one turn (only one turn
     * per agent runs at a time - see runningStatus.putIfAbsent), but snapshots can be read
     * concurrently from an HTTP request thread (AgentController.chat() rendering a page mid-turn -
     * see getPartialTurn) - synchronized so a snapshot never observes a StringBuilder/ArrayNode
     * half-way through a concurrent append.
     */
    private static class RunningTurnState
    {
        private final StringBuilder assistantText = new StringBuilder();
        private final ArrayNode toolActivity;

        RunningTurnState(ObjectMapper objectMapper)
        {
            this.toolActivity = objectMapper.createArrayNode();
        }

        synchronized void appendText(String text)
        {
            assistantText.append(text);
        }

        synchronized void addToolActivity(ObjectNode entry)
        {
            toolActivity.add(entry);
        }

        synchronized String snapshotText()
        {
            return assistantText.toString();
        }

        synchronized String snapshotToolActivityJson()
        {
            return toolActivity.size() > 0 ? toolActivity.toString() : null;
        }
    }

    private static class RateLimitSnapshot
    {
        String status;
        String rateLimitType;
        Long resetsAt;
        String overageStatus;
        Long overageResetsAt;
        Boolean usingOverage;
    }

    /** Token/duration usage from a turn's final "result" event. */
    private static class TurnUsage
    {
        Integer inputTokens;
        Integer outputTokens;
        Integer cacheCreationInputTokens;
        Integer cacheReadInputTokens;
        Integer durationMs;
    }

    private void drainStream(InputStream inputStream, StringBuilder target)
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                target.append(line).append("\n");
            }
        } catch (IOException e) {
            // best-effort diagnostic capture only, process outcome is judged by exit code
        }
    }

    private void completeWithError(SseEmitter emitter, String message)
    {
        trySend(emitter, SseEmitter.event().name("error").data(message));

        try {
            emitter.completeWithError(new IllegalStateException(message));
        } catch (Exception ignored) {
        }
    }

    /**
     * Sends an SSE event if an emitter is attached, silently ignoring failures. The emitter is
     * null for headless/scheduled/agent-triggered runs (no browser attached); it's non-null but
     * already completed if the browser navigated away mid-turn (Spring throws
     * IllegalStateException on the next send). Either way the turn should keep running the CLI
     * process to completion and persist normally, not treat "nobody's watching anymore" as a
     * turn failure - that used to abort the read loop, land in executeTurn's catch block, and
     * kill the still-working CLI process via its finally block.
     */
    private void trySend(SseEmitter emitter, SseEmitter.SseEventBuilder event)
    {
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(event);
        } catch (Exception ignored) {
        }
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
