package ch.ksfx.model;

import javax.persistence.*;
import java.util.Date;

/**
 * One turn of a chat with an {@link Agent}: the user's message, the assistant's final reply for
 * that turn, a system-role error, or an incoming message from another Agent (see {@link #fromAgent}).
 * {@link #toolActivity} holds a pre-formatted, human-readable summary of any tool calls made during
 * the turn (not raw JSON), so it can be rendered directly in Thymeleaf without a JSON-parsing helper.
 */
@Entity
@Table(name = "agent_message")
public class AgentMessage
{
    private Long id;
    private Agent agent;
    private Agent fromAgent;
    private AgentMessageRole role;
    private String content;
    private String toolActivity;
    private String attachments;
    private String generatedFiles;
    private Date createdAt;

    /**
     * True for a row that isn't a real conversation turn - currently just the voice-input cleanup
     * call (see ClaudeCliSessionService.cleanupVoiceTranscript), persisted purely so its token usage
     * shows up in the usage stats instead of silently not being tracked. Excluded from the chat
     * transcript queries (AgentMessageDAO) so it never renders as a message bubble.
     */
    private boolean internal = false;

    /**
     * True for a USER-role message that was composed via the mic button and went through
     * ClaudeCliSessionService.cleanupVoiceTranscript before being stored - lets the chat UI show a
     * small indicator, so a cleanup mistake (wrong word, over-corrected phrasing) is easy to spot
     * and attribute to voice input rather than to something the user actually typed.
     */
    private boolean voiceInput = false;

    // Usage stats for this turn (ASSISTANT-role messages only) - captured from the CLI's
    // stream-json "result" event (see ClaudeCliSessionService). Generic/provider-neutral names,
    // unlike AgenticConfig's claudeRateLimit* fields - see plan discussion for the rationale.
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer cacheCreationInputTokens;
    private Integer cacheReadInputTokens;
    private Integer durationMs;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name = "agent_id")
    public Agent getAgent()
    {
        return agent;
    }

    public void setAgent(Agent agent)
    {
        this.agent = agent;
    }

    /**
     * The Agent that sent this message, set only when {@link #role} is {@link AgentMessageRole#AGENT}
     * (an agent-to-agent message via AgentMessageApiController) - null for USER/ASSISTANT/SYSTEM rows.
     */
    @ManyToOne
    @JoinColumn(name = "from_agent_id")
    public Agent getFromAgent()
    {
        return fromAgent;
    }

    public void setFromAgent(Agent fromAgent)
    {
        this.fromAgent = fromAgent;
    }

    @Enumerated(EnumType.STRING)
    public AgentMessageRole getRole()
    {
        return role;
    }

    public void setRole(AgentMessageRole role)
    {
        this.role = role;
    }

    @Lob
    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    @Lob
    public String getToolActivity()
    {
        return toolActivity;
    }

    public void setToolActivity(String toolActivity)
    {
        this.toolActivity = toolActivity;
    }

    /**
     * JSON array of files attached to this message (e.g. [{"fileName":"...","path":"uploads/..."}]) -
     * saved into the agent's own workspace directory so the CLI can read/see them via its normal
     * tools; this column only holds the display metadata for rendering attachment chips in the UI.
     */
    @Lob
    public String getAttachments()
    {
        return attachments;
    }

    public void setAttachments(String attachments)
    {
        this.attachments = attachments;
    }

    /**
     * JSON array of files the agent produced during this turn (new/modified since the turn
     * started), detected by diffing the workspace before and after - see ClaudeCliSessionService.
     * Same {fileName, path} shape as {@link #attachments}, just for the other direction.
     */
    @Lob
    public String getGeneratedFiles()
    {
        return generatedFiles;
    }

    public void setGeneratedFiles(String generatedFiles)
    {
        this.generatedFiles = generatedFiles;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    public Integer getInputTokens()
    {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens)
    {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens()
    {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens)
    {
        this.outputTokens = outputTokens;
    }

    public Integer getCacheCreationInputTokens()
    {
        return cacheCreationInputTokens;
    }

    public void setCacheCreationInputTokens(Integer cacheCreationInputTokens)
    {
        this.cacheCreationInputTokens = cacheCreationInputTokens;
    }

    public Integer getCacheReadInputTokens()
    {
        return cacheReadInputTokens;
    }

    public void setCacheReadInputTokens(Integer cacheReadInputTokens)
    {
        this.cacheReadInputTokens = cacheReadInputTokens;
    }

    public Integer getDurationMs()
    {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs)
    {
        this.durationMs = durationMs;
    }

    public boolean getInternal()
    {
        return internal;
    }

    public void setInternal(boolean internal)
    {
        this.internal = internal;
    }

    public boolean getVoiceInput()
    {
        return voiceInput;
    }

    public void setVoiceInput(boolean voiceInput)
    {
        this.voiceInput = voiceInput;
    }
}
