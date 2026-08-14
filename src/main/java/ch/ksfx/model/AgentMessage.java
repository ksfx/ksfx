package ch.ksfx.model;

import javax.persistence.*;
import java.util.Date;

/**
 * One turn of a chat with an {@link Agent}: either the user's message, the assistant's final
 * reply for that turn, or a system-role error. {@link #toolActivity} holds a pre-formatted,
 * human-readable summary of any tool calls made during the turn (not raw JSON), so it can be
 * rendered directly in Thymeleaf without a JSON-parsing helper.
 */
@Entity
@Table(name = "agent_message")
public class AgentMessage
{
    private Long id;
    private Agent agent;
    private AgentMessageRole role;
    private String content;
    private String toolActivity;
    private String attachments;
    private String generatedFiles;
    private Date createdAt;

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
}
