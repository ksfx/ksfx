package ch.ksfx.services.agentic;

/**
 * One completion from a {@link VoiceCompletionClient} - just the text plus whatever token usage
 * the provider reported for it, so the caller can persist accurate cost tracking (see
 * ClaudeCliSessionService.persistVoiceCleanupUsage) regardless of which implementation produced it.
 */
public final class VoiceCompletionResult
{
    private final String text;
    private final Integer inputTokens;
    private final Integer outputTokens;
    private final Integer cacheCreationInputTokens;
    private final Integer cacheReadInputTokens;
    private final Integer durationMs;
    private final String source;

    /**
     * @param source short, human-readable label of which implementation produced this (e.g. "cli",
     * "api") - persisted into the visible tracking message so it's obvious in hindsight which one
     * actually ran for a given call, not just inferable from token counts alone.
     */
    public VoiceCompletionResult(String text, Integer inputTokens, Integer outputTokens,
                                  Integer cacheCreationInputTokens, Integer cacheReadInputTokens, Integer durationMs, String source)
    {
        this.text = text;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.cacheCreationInputTokens = cacheCreationInputTokens;
        this.cacheReadInputTokens = cacheReadInputTokens;
        this.durationMs = durationMs;
        this.source = source;
    }

    public String getText()
    {
        return text;
    }

    public Integer getInputTokens()
    {
        return inputTokens;
    }

    public Integer getOutputTokens()
    {
        return outputTokens;
    }

    public Integer getCacheCreationInputTokens()
    {
        return cacheCreationInputTokens;
    }

    public Integer getCacheReadInputTokens()
    {
        return cacheReadInputTokens;
    }

    public Integer getDurationMs()
    {
        return durationMs;
    }

    public String getSource()
    {
        return source;
    }
}
