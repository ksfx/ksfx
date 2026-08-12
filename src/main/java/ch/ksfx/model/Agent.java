package ch.ksfx.model;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * A named agent, backed by a headless Claude Code CLI session. {@link #workspacePath} is the
 * per-agent directory the CLI process runs in; {@link #claudeSessionId} is the CLI's own session
 * id, captured after the first turn and passed via --resume on subsequent turns.
 */
@Entity
@Table(name = "agent")
public class Agent
{
    private Long id;
    private AgenticProject agenticProject;

    @NotNull
    @Size(min = 2, max = 200)
    @NotEmpty
    private String name;
    private String description;
    private String systemPrompt;
    private String claudeSessionId;
    private String workspacePath;
    private String permissionMode;
    private String apiToken;
    private boolean enabled = true;
    private Date createdAt;

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
    @JoinColumn(name = "agentic_project_id")
    public AgenticProject getAgenticProject()
    {
        return agenticProject;
    }

    public void setAgenticProject(AgenticProject agenticProject)
    {
        this.agenticProject = agenticProject;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Lob
    public String getSystemPrompt()
    {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt)
    {
        this.systemPrompt = systemPrompt;
    }

    public String getClaudeSessionId()
    {
        return claudeSessionId;
    }

    public void setClaudeSessionId(String claudeSessionId)
    {
        this.claudeSessionId = claudeSessionId;
    }

    public String getWorkspacePath()
    {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath)
    {
        this.workspacePath = workspacePath;
    }

    /**
     * Overrides {@link AgenticConfig#getDefaultPermissionMode()} for this agent when set; blank/null
     * means "use the global default".
     */
    public String getPermissionMode()
    {
        return permissionMode;
    }

    public void setPermissionMode(String permissionMode)
    {
        this.permissionMode = permissionMode;
    }

    /**
     * Bearer token for this agent's own /agentic/api/** calls (self-service scheduling). Generated
     * once at creation (or lazily backfilled for older rows); no rotation in v1.
     */
    public String getApiToken()
    {
        return apiToken;
    }

    public void setApiToken(String apiToken)
    {
        this.apiToken = apiToken;
    }

    public boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }
}
