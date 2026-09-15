package ch.ksfx.services.agentic;

import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticAuthMode;
import ch.ksfx.model.AgenticConfig;
import ch.ksfx.services.systemlogger.SystemLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * {@link VoiceCompletionClient} that calls the Anthropic Messages API directly over HTTPS, instead
 * of going through the full {@code claude} CLI harness ({@link ClaudeCliCompletionClient}). Skips
 * the CLI's own baked-in system prompt and full tool-definition set entirely - those cost several
 * thousand tokens on every single CLI invocation regardless of task size (confirmed empirically,
 * 2026-09-15: a one-sentence cleanup still showed ~2500 input + several thousand cache tokens),
 * which for a plain text-cleanup task that never needs a tool is pure waste.
 *
 * Deliberately uses a small, cheap model (Haiku) rather than whatever the agent itself is
 * configured to use - punctuation/typo cleanup doesn't need a top-tier model, and this call is
 * charged independently of the agent's own conversation.
 *
 * Only usable for {@link AgenticAuthMode#API_KEY} and {@link AgenticAuthMode#SETUP_TOKEN}: both
 * hand KSFX an actual reusable bearer credential (see AgenticConfig.apiKey). AgenticAuthMode#OAUTH
 * has no equivalent - those credentials live in the CLI's own local login state (from an
 * interactive `claude login`), not something this service can read out and reuse safely - so this
 * client returns null immediately for OAUTH and the caller falls back to
 * {@link ClaudeCliCompletionClient} instead (see ClaudeCliSessionService.selectVoiceCompletionClient).
 *
 * API-key auth uses the standard `x-api-key` header; the setup-token flavor is an OAuth-style
 * token and needs `Authorization: Bearer` plus the `anthropic-beta: oauth-2025-04-20` header
 * instead - confirmed by grepping the claude CLI's own embedded documentation strings for exactly
 * this distinction ("OAuth tokens go on Authorization: Bearer, not x-api-key").
 */
@Service
public class ClaudeApiCompletionClient implements VoiceCompletionClient
{
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String OAUTH_BETA_HEADER_VALUE = "oauth-2025-04-20";
    private static final int TIMEOUT_MS = 20_000;

    private final SystemLogger systemLogger;
    private final ObjectMapper objectMapper;

    public ClaudeApiCompletionClient(SystemLogger systemLogger, ObjectMapper objectMapper)
    {
        this.systemLogger = systemLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    public VoiceCompletionResult complete(String prompt, Agent agent, AgenticConfig config)
    {
        if (config.getAuthMode() != AgenticAuthMode.API_KEY && config.getAuthMode() != AgenticAuthMode.SETUP_TOKEN) {
            return null;
        }

        long startedAt = System.currentTimeMillis();

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", MODEL);
            requestBody.put("max_tokens", 1024);
            ObjectNode message = requestBody.putArray("messages").addObject();
            message.put("role", "user");
            message.put("content", prompt);

            HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("content-type", "application/json");
            connection.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);

            if (config.getAuthMode() == AgenticAuthMode.SETUP_TOKEN) {
                connection.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
                connection.setRequestProperty("anthropic-beta", OAUTH_BETA_HEADER_VALUE);
            } else {
                connection.setRequestProperty("x-api-key", config.getApiKey());
            }

            try (OutputStream out = connection.getOutputStream()) {
                out.write(objectMapper.writeValueAsBytes(requestBody));
            }

            int status = connection.getResponseCode();
            String body = readFully(status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream());

            if (status < 200 || status >= 300) {
                systemLogger.logMessage("AGENTIC", "Voice cleanup API call failed (HTTP " + status + "), keeping raw dictated text: " + body);
                return null;
            }

            JsonNode responseJson = objectMapper.readTree(body);
            StringBuilder text = new StringBuilder();

            for (JsonNode block : responseJson.path("content")) {
                if ("text".equals(block.path("type").asText(""))) {
                    text.append(block.path("text").asText(""));
                }
            }

            String cleaned = text.toString().trim();

            if (cleaned.isEmpty()) {
                return null;
            }

            JsonNode usage = responseJson.path("usage");

            return new VoiceCompletionResult(
                    cleaned,
                    usage.hasNonNull("input_tokens") ? usage.path("input_tokens").asInt() : null,
                    usage.hasNonNull("output_tokens") ? usage.path("output_tokens").asInt() : null,
                    usage.hasNonNull("cache_creation_input_tokens") ? usage.path("cache_creation_input_tokens").asInt() : null,
                    usage.hasNonNull("cache_read_input_tokens") ? usage.path("cache_read_input_tokens").asInt() : null,
                    (int) (System.currentTimeMillis() - startedAt),
                    "api");
        } catch (IOException e) {
            systemLogger.logMessage("AGENTIC", "Voice cleanup API call failed, keeping raw dictated text", e);
            return null;
        }
    }

    private String readFully(java.io.InputStream inputStream) throws IOException
    {
        if (inputStream == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }
}
