package ch.ksfx.model;

/**
 * API_KEY and SETUP_TOKEN both put {@link AgenticConfig#getApiKey()} into the CLI subprocess's
 * environment - just under different variable names, because the Claude CLI treats them as two
 * genuinely different, mutually exclusive credential kinds (confirmed against the installed CLI
 * binary: it has a dedicated error path for "an ANTHROPIC_API_KEY is set but you meant to use your
 * subscription instead"). ANTHROPIC_API_KEY is a console.anthropic.com API key (usage-based
 * billing); a SETUP_TOKEN value is the long-lived token `claude setup-token` mints against a
 * Claude subscription (Pro/Max) and must go into CLAUDE_CODE_OAUTH_TOKEN instead - putting one in
 * the other's slot doesn't silently fall back, it fails outright ("Invalid API key" for the
 * reverse case). See ClaudeCliSessionService#authEnvironmentVariableName for where this is decided,
 * and its two call sites for where the result actually gets set on the CLI subprocess.
 */
public enum AgenticAuthMode
{
    API_KEY, OAUTH, SETUP_TOKEN
}
