package ch.ksfx.services.agentic;

import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.services.systemlogger.SystemLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@link VoiceCompletionClient} that spawns the {@code claude} CLI as a subprocess - no
 * {@code --resume}, no tool access, no agent system prompt, just a one-shot {@code -p} prompt.
 * Works under any {@link ch.ksfx.model.AgenticAuthMode}, including OAUTH (where the CLI is the
 * only thing that can authenticate at all - see ClaudeApiCompletionClient's Javadoc), which is why
 * this is the fallback implementation whenever the faster direct-API one can't be used. Costs
 * thousands of tokens per call regardless of how short the input text is - see
 * ClaudeCliSessionService.selectVoiceCompletionClient for why that's not the whole story.
 */
@Service
public class ClaudeCliCompletionClient implements VoiceCompletionClient
{
    private final AgentWorkspaceService agentWorkspaceService;
    private final SystemLogger systemLogger;
    private final ObjectMapper objectMapper;

    public ClaudeCliCompletionClient(AgentWorkspaceService agentWorkspaceService, SystemLogger systemLogger, ObjectMapper objectMapper)
    {
        this.agentWorkspaceService = agentWorkspaceService;
        this.systemLogger = systemLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    public VoiceCompletionResult complete(String prompt, Agent agent, AgenticConfig config)
    {
        Path workspace;

        try {
            workspace = agentWorkspaceService.ensureWorkspace(agent, config);
        } catch (IOException e) {
            return null;
        }

        List<String> command = new ArrayList<>();
        command.add(config.getClaudeCliPath());
        command.add("-p");
        command.add(prompt);
        command.add("--output-format");
        command.add("stream-json");
        command.add("--verbose");
        command.add("--permission-mode");
        command.add("default"); // never wants/needs tool access, regardless of the agent's own permission mode

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workspace.toFile());
        processBuilder.redirectInput(ProcessBuilder.Redirect.from(new File(ClaudeCliEnvironment.nullDevicePath())));

        String authEnvVar = ClaudeCliEnvironment.authEnvironmentVariableName(config.getAuthMode());

        if (authEnvVar != null) {
            processBuilder.environment().put(authEnvVar, config.getApiKey());
        }

        StringBuilder resultText = new StringBuilder();
        Integer[] usage = new Integer[5]; // input, output, cacheCreation, cacheRead, durationMs
        StringBuilder stderrOutput = new StringBuilder();

        try {
            Process process = processBuilder.start();

            Thread stderrDrain = new Thread(() -> ClaudeCliEnvironment.drainStream(process.getErrorStream(), stderrOutput));
            stderrDrain.start();

            Thread stdoutReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) {
                            continue;
                        }

                        JsonNode event = objectMapper.readTree(line);
                        String type = event.path("type").asText("");

                        if ("assistant".equals(type)) {
                            for (JsonNode contentBlock : event.path("message").path("content")) {
                                if ("text".equals(contentBlock.path("type").asText(""))) {
                                    resultText.append(contentBlock.path("text").asText(""));
                                }
                            }
                        } else if ("result".equals(type)) {
                            JsonNode usageNode = event.path("usage");
                            usage[0] = usageNode.hasNonNull("input_tokens") ? usageNode.path("input_tokens").asInt() : null;
                            usage[1] = usageNode.hasNonNull("output_tokens") ? usageNode.path("output_tokens").asInt() : null;
                            usage[2] = usageNode.hasNonNull("cache_creation_input_tokens") ? usageNode.path("cache_creation_input_tokens").asInt() : null;
                            usage[3] = usageNode.hasNonNull("cache_read_input_tokens") ? usageNode.path("cache_read_input_tokens").asInt() : null;
                            usage[4] = event.hasNonNull("duration_ms") ? event.path("duration_ms").asInt() : null;
                        }
                    }
                } catch (IOException ignored) {
                    // process died/pipe closed - the waitFor(timeout) below decides the outcome
                }
            });
            stdoutReader.start();

            boolean finished = process.waitFor(20, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                systemLogger.logMessage("AGENTIC", "Voice cleanup CLI call timed out after 20s, keeping raw dictated text");
                return null;
            }

            stdoutReader.join(TimeUnit.SECONDS.toMillis(2));
            stderrDrain.join(TimeUnit.SECONDS.toMillis(2));

            if (process.exitValue() != 0 || resultText.length() == 0) {
                systemLogger.logMessage("AGENTIC", "Voice cleanup CLI call failed (exit " + process.exitValue() + "), keeping raw dictated text: " + stderrOutput);
                return null;
            }
        } catch (IOException | InterruptedException e) {
            systemLogger.logMessage("AGENTIC", "Voice cleanup CLI call failed, keeping raw dictated text", e);
            return null;
        }

        String cleaned = resultText.toString().trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        return new VoiceCompletionResult(cleaned, usage[0], usage[1], usage[2], usage[3], usage[4], "cli");
    }
}
