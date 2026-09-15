package ch.ksfx.services.agentic;

import ch.ksfx.model.AgenticAuthMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Small, provider-agnostic OS/process helpers shared by anything that spawns the {@code claude}
 * CLI as a subprocess - today {@link ClaudeCliSessionService} (the main turn) and
 * {@link ClaudeCliCompletionClient} (the voice-cleanup call, see {@link VoiceCompletionClient}).
 * Pulled out as static methods rather than instance methods on either caller so the second caller
 * doesn't need a reference to the first just to reuse this.
 */
final class ClaudeCliEnvironment
{
    private ClaudeCliEnvironment()
    {
    }

    static String authEnvironmentVariableName(AgenticAuthMode authMode)
    {
        switch (authMode) {
            case API_KEY:
                return "ANTHROPIC_API_KEY";
            case SETUP_TOKEN:
                return "CLAUDE_CODE_OAUTH_TOKEN";
            default:
                return null;
        }
    }

    /** "NUL" on Windows, "/dev/null" everywhere else - see the stdin-redirect call sites. */
    static String nullDevicePath()
    {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? "NUL" : "/dev/null";
    }

    static void drainStream(InputStream inputStream, StringBuilder target)
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
}
