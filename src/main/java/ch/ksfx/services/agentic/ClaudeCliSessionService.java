package ch.ksfx.services.agentic;

import ch.ksfx.dao.AgentDAO;
import ch.ksfx.dao.AgentMessageDAO;
import ch.ksfx.dao.AgenticConfigDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentMessage;
import ch.ksfx.model.AgentMessageRole;
import ch.ksfx.model.AgenticAuthMode;
import ch.ksfx.model.AgenticConfig;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
 * Two entry points share the same validation/busy-guard/execution core: {@link #runTurn} (browser,
 * streams via SseEmitter, spawns a Thread so the controller can return the emitter immediately) and
 * {@link #runScheduledTurn} (Quartz jobs from AgentScheduleJob, no browser attached, runs
 * synchronously on Quartz's own worker thread - a scheduled trigger is otherwise identical to the
 * user having typed the message themselves, including landing in the same AgentMessage history).
 */
@Service
public class ClaudeCliSessionService
{
    public static final String SKIPPED_RESULT = "SKIPPED";

    private static final long PROCESS_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(30);

    private final AgenticConfigDAO agenticConfigDAO;
    private final AgentDAO agentDAO;
    private final AgentMessageDAO agentMessageDAO;
    private final AgentWorkspaceService agentWorkspaceService;
    private final SystemLogger systemLogger;
    private final ObjectMapper objectMapper;
    private final int serverPort;

    private final ConcurrentHashMap<Long, String> runningStatus = new ConcurrentHashMap<>();

    public ClaudeCliSessionService(AgenticConfigDAO agenticConfigDAO,
                                    AgentDAO agentDAO,
                                    AgentMessageDAO agentMessageDAO,
                                    AgentWorkspaceService agentWorkspaceService,
                                    SystemLogger systemLogger,
                                    ObjectMapper objectMapper,
                                    @Value("${server.port:8080}") int serverPort)
    {
        this.agenticConfigDAO = agenticConfigDAO;
        this.agentDAO = agentDAO;
        this.agentMessageDAO = agentMessageDAO;
        this.agentWorkspaceService = agentWorkspaceService;
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

        new Thread(() -> {
            String error = executeTurn(agent, config, messageForCli, emitter);

            if (error == null) {
                emitter.complete();
            } else {
                completeWithError(emitter, error);
            }
        }).start();
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
        StringBuilder assistantText = new StringBuilder();
        ArrayNode toolActivity = objectMapper.createArrayNode();
        StringBuilder stderrOutput = new StringBuilder();
        String[] capturedSessionId = new String[1];
        RateLimitSnapshot rateLimitSnapshot = new RateLimitSnapshot();
        TurnUsage turnUsage = new TurnUsage();
        Process process = null;

        try {
            runningStatus.put(agent.getId(), "Thinking…");

            Path workspace = agentWorkspaceService.ensureWorkspace(agent, config);

            ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(agent, config, userMessage));
            processBuilder.directory(workspace.toFile());

            if (config.getAuthMode() != AgenticAuthMode.OAUTH) {
                processBuilder.environment().put("ANTHROPIC_API_KEY", config.getApiKey());
            }
            // OAUTH mode: no ANTHROPIC_API_KEY set, CLI falls back to credentials from `claude login`
            // run interactively, once, as the same OS user that starts the KSFX process.

            // Passed as an env var (not a literal in the prompt text) so it never appears in
            // stream-json output, AgentMessage.toolActivity, the chat UI, or the CLI's own on-disk
            // transcript - see buildAppendedSystemPrompt, which tells the agent to reference
            // $KSFX_AGENT_TOKEN rather than write the value itself.
            processBuilder.environment().put("KSFX_AGENT_TOKEN", agent.getApiToken());

            process = processBuilder.start();

            final Process startedProcess = process;
            Thread stderrDrain = new Thread(() -> drainStream(startedProcess.getErrorStream(), stderrOutput));
            stderrDrain.start();

            readStdout(process, emitter, agent.getId(), assistantText, toolActivity, capturedSessionId, rateLimitSnapshot, turnUsage);

            boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Claude CLI process exceeded the timeout of " + PROCESS_TIMEOUT_SECONDS + "s and was terminated.");
            }

            stderrDrain.join(TimeUnit.SECONDS.toMillis(5));

            if (process.exitValue() != 0) {
                throw new IOException("Claude CLI process exited with exit code " + process.exitValue() + ": " + stderrOutput);
            }

            if (capturedSessionId[0] != null) {
                agent.setClaudeSessionId(capturedSessionId[0]);
                agentDAO.saveOrUpdateAgent(agent);
            }

            AgentMessage assistantMessage = new AgentMessage();
            assistantMessage.setAgent(agent);
            assistantMessage.setRole(AgentMessageRole.ASSISTANT);
            assistantMessage.setContent(assistantText.toString());
            assistantMessage.setToolActivity(toolActivity.size() > 0 ? objectMapper.writeValueAsString(toolActivity) : null);
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

            if (emitter != null && turnUsage.inputTokens != null) {
                emitter.send(SseEmitter.event().name("usage").data(objectMapper.writeValueAsString(turnUsage.inputTokens + turnUsage.outputTokens)));
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

            return "Error: " + e.getMessage() + StacktraceUtil.getStackTrace(e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }

            runningStatus.remove(agent.getId());
        }
    }

    /**
     * Flags verified against the installed CLI's --help before go-live; kept isolated here so a
     * version mismatch is a one-line fix.
     */
    private List<String> buildCommand(Agent agent, AgenticConfig config, String userMessage)
    {
        List<String> command = new ArrayList<>();

        command.add(config.getClaudeCliPath());
        command.add("-p");
        command.add(userMessage);
        command.add("--output-format");
        command.add("stream-json");
        command.add("--verbose");
        command.add("--permission-mode");
        command.add(!isBlank(agent.getPermissionMode()) ? agent.getPermissionMode() : config.getDefaultPermissionMode());

        if (!isBlank(agent.getClaudeSessionId())) {
            command.add("--resume");
            command.add(agent.getClaudeSessionId());
        }

        command.add("--append-system-prompt");
        command.add(buildAppendedSystemPrompt(agent));

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

        String schedulingPrompt = "Du kannst eigene wiederkehrende Hintergrundaufgaben (Scheduled Tasks) verwalten, "
                + "indem du mit dem Bash-Tool curl gegen die lokale KSFX-API aufrufst. Authentifiziere dich dabei "
                + "IMMER über die bereits gesetzte Umgebungsvariable $KSFX_AGENT_TOKEN (NIEMALS den Wert selbst "
                + "aufschreiben oder raten - referenziere ausschließlich $KSFX_AGENT_TOKEN im curl-Aufruf). "
                + "Basis-URL: http://localhost:" + serverPort + "/agentic/api/schedule\n\n"
                + "Endpunkte:\n"
                + "- GET  /agentic/api/schedule            -> Liste deiner eigenen geplanten Aufgaben\n"
                + "- POST /agentic/api/schedule            -> neue Aufgabe anlegen, JSON-Body: "
                + "{\"name\":\"...\",\"taskPrompt\":\"...\",\"cronSchedule\":\"0 0 9 * * ?\",\"cronScheduleEnabled\":true}\n"
                + "- PUT  /agentic/api/schedule/{id}        -> bestehende Aufgabe aktualisieren (gleicher Body)\n"
                + "- DELETE /agentic/api/schedule/{id}      -> Aufgabe löschen\n\n"
                + "WICHTIG: cronSchedule verwendet Quartz-Cron-Syntax mit 6-7 Feldern (Sekunde zuerst), NICHT "
                + "Standard-5-Feld-Cron. Beispiel für 'täglich um 9 Uhr': \"0 0 9 * * ?\". Beispiel-Aufruf:\n"
                + "curl -s -X POST http://localhost:" + serverPort + "/agentic/api/schedule "
                + "-H \"Authorization: Bearer $KSFX_AGENT_TOKEN\" -H \"Content-Type: application/json\" "
                + "-d '{\"name\":\"Tägliche Erinnerung\",\"taskPrompt\":\"Prüfe X\",\"cronSchedule\":\"0 0 9 * * ?\",\"cronScheduleEnabled\":true}'\n";

        if (agent.getAgenticProject() != null) {
            schedulingPrompt += "\nGeteilte Ressourcen deines Agentic Projects findest du unter ../shared "
                    + "(relativ zu deinem eigenen Arbeitsverzeichnis).\n";
        }

        return isBlank(agent.getSystemPrompt()) ? schedulingPrompt : schedulingPrompt + "\n---\n\n" + agent.getSystemPrompt();
    }

    private void readStdout(Process process, SseEmitter emitter, Long agentId, StringBuilder assistantText, ArrayNode toolActivity, String[] capturedSessionId, RateLimitSnapshot rateLimitSnapshot, TurnUsage turnUsage) throws IOException
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    handleStreamEvent(objectMapper.readTree(line), emitter, agentId, assistantText, toolActivity, capturedSessionId, rateLimitSnapshot, turnUsage);
                } catch (IOException parseException) {
                    systemLogger.logMessage("AGENTIC", "Konnte stream-json Zeile nicht parsen: " + line, parseException);
                }
            }
        }
    }

    /**
     * Tool activity is forwarded (live, via SSE) and persisted (in {@link AgentMessage#getToolActivity()})
     * as structured JSON - {"type":"tool_use","tool":...,"input":...} / {"type":"tool_result","result":...}
     * - rather than pre-formatted text, so the browser (agentic-chat.js) can render one rich
     * representation for both the live stream and history reloaded from the DB, instead of
     * duplicating formatting logic in Java and JS. {@code emitter} may be null (scheduled/headless
     * runs) - every send is guarded.
     */
    private void handleStreamEvent(JsonNode event, SseEmitter emitter, Long agentId, StringBuilder assistantText, ArrayNode toolActivity, String[] capturedSessionId, RateLimitSnapshot rateLimitSnapshot, TurnUsage turnUsage) throws IOException
    {
        String type = event.path("type").asText("");

        if ("assistant".equals(type)) {
            for (JsonNode contentBlock : event.path("message").path("content")) {
                String blockType = contentBlock.path("type").asText("");

                if ("text".equals(blockType)) {
                    String text = contentBlock.path("text").asText("");
                    assistantText.append(text);
                    runningStatus.put(agentId, "Responding…");

                    if (emitter != null) {
                        // JSON-encoded (like tool_use/tool_result below), not sent raw: a raw
                        // multi-line string here breaks the client's naive SSE event-boundary
                        // parsing (it splits on a blank line, which a paragraph break inside the
                        // text also produces) and truncates the live-rendered response, even
                        // though assistantText/the DB copy stays complete either way.
                        emitter.send(SseEmitter.event().name("text").data(objectMapper.writeValueAsString(text)));
                    }
                } else if ("tool_use".equals(blockType)) {
                    String toolName = contentBlock.path("name").asText("tool");

                    ObjectNode toolUseEntry = objectMapper.createObjectNode();
                    toolUseEntry.put("type", "tool_use");
                    toolUseEntry.put("tool", toolName);
                    toolUseEntry.set("input", contentBlock.path("input"));
                    toolActivity.add(toolUseEntry);

                    runningStatus.put(agentId, "Tool: " + toolName);

                    if (emitter != null) {
                        emitter.send(SseEmitter.event().name("tool_use").data(objectMapper.writeValueAsString(toolUseEntry)));
                    }
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
                    toolActivity.add(toolResultEntry);

                    runningStatus.put(agentId, "Evaluating tool result…");

                    if (emitter != null) {
                        emitter.send(SseEmitter.event().name("tool_result").data(objectMapper.writeValueAsString(toolResultEntry)));
                    }
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
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
        } catch (IOException ignored) {
        }

        emitter.completeWithError(new IllegalStateException(message));
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
