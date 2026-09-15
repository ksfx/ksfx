package ch.ksfx.services.agentic;

import ch.ksfx.model.Agent;
import ch.ksfx.model.AgenticConfig;

/**
 * A one-shot, stateless text completion used to clean up voice-dictated messages before they're
 * persisted/sent (see ClaudeCliSessionService.cleanupVoiceTranscript) - deliberately named after
 * what it's *for*, not after Claude: today's implementations both happen to call Claude (one via
 * the CLI, one via the Anthropic API directly), but nothing about this feature requires that to
 * stay true, and callers shouldn't need to know or care which one actually ran.
 */
public interface VoiceCompletionClient
{
    /**
     * @return the completion, or {@code null} if it couldn't be obtained for any reason (timeout,
     * non-2xx/non-zero exit, empty reply, unsupported auth mode, ...) - callers fall back to the
     * original dictated text rather than treating this as fatal.
     */
    VoiceCompletionResult complete(String prompt, Agent agent, AgenticConfig config);
}
