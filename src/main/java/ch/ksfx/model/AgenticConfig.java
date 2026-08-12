package ch.ksfx.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Per-instance configuration for the "Agentic" feature (headless Claude Code CLI sessions).
 * There is exactly one row per KSFX instance, mirroring {@link GitSyncConfig}.
 */
@Entity
@Table(name = "agentic_config")
public class AgenticConfig
{
    private Long id;
    private AgenticAuthMode authMode = AgenticAuthMode.API_KEY;
    private String apiKey;
    private String claudeCliPath = "claude";
    private String defaultPermissionMode = "default";
    private String workspaceRoot;
    private boolean enabled = false;

    // Latest known Claude subscription-plan rate limit snapshot (see ClaudeCliSessionService,
    // captured from the CLI's "rate_limit_event" stream-json events) - not user-editable, so
    // AgenticConfigController.save() must carry these forward explicitly on every form submit.
    private String claudeRateLimitStatus;
    private String claudeRateLimitType;
    private Date claudeRateLimitResetsAt;
    private String claudeRateLimitOverageStatus;
    private Date claudeRateLimitOverageResetsAt;
    private Boolean claudeRateLimitUsingOverage;
    private Date claudeRateLimitUpdatedAt;

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

    @Enumerated(EnumType.STRING)
    public AgenticAuthMode getAuthMode()
    {
        return authMode;
    }

    public void setAuthMode(AgenticAuthMode authMode)
    {
        this.authMode = authMode;
    }

    @Lob
    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public String getClaudeCliPath()
    {
        return claudeCliPath;
    }

    public void setClaudeCliPath(String claudeCliPath)
    {
        this.claudeCliPath = claudeCliPath;
    }

    public String getDefaultPermissionMode()
    {
        return defaultPermissionMode;
    }

    public void setDefaultPermissionMode(String defaultPermissionMode)
    {
        this.defaultPermissionMode = defaultPermissionMode;
    }

    public String getWorkspaceRoot()
    {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(String workspaceRoot)
    {
        this.workspaceRoot = workspaceRoot;
    }

    public boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getClaudeRateLimitStatus()
    {
        return claudeRateLimitStatus;
    }

    public void setClaudeRateLimitStatus(String claudeRateLimitStatus)
    {
        this.claudeRateLimitStatus = claudeRateLimitStatus;
    }

    public String getClaudeRateLimitType()
    {
        return claudeRateLimitType;
    }

    public void setClaudeRateLimitType(String claudeRateLimitType)
    {
        this.claudeRateLimitType = claudeRateLimitType;
    }

    public Date getClaudeRateLimitResetsAt()
    {
        return claudeRateLimitResetsAt;
    }

    public void setClaudeRateLimitResetsAt(Date claudeRateLimitResetsAt)
    {
        this.claudeRateLimitResetsAt = claudeRateLimitResetsAt;
    }

    public String getClaudeRateLimitOverageStatus()
    {
        return claudeRateLimitOverageStatus;
    }

    public void setClaudeRateLimitOverageStatus(String claudeRateLimitOverageStatus)
    {
        this.claudeRateLimitOverageStatus = claudeRateLimitOverageStatus;
    }

    public Date getClaudeRateLimitOverageResetsAt()
    {
        return claudeRateLimitOverageResetsAt;
    }

    public void setClaudeRateLimitOverageResetsAt(Date claudeRateLimitOverageResetsAt)
    {
        this.claudeRateLimitOverageResetsAt = claudeRateLimitOverageResetsAt;
    }

    public Boolean getClaudeRateLimitUsingOverage()
    {
        return claudeRateLimitUsingOverage;
    }

    public void setClaudeRateLimitUsingOverage(Boolean claudeRateLimitUsingOverage)
    {
        this.claudeRateLimitUsingOverage = claudeRateLimitUsingOverage;
    }

    public Date getClaudeRateLimitUpdatedAt()
    {
        return claudeRateLimitUpdatedAt;
    }

    public void setClaudeRateLimitUpdatedAt(Date claudeRateLimitUpdatedAt)
    {
        this.claudeRateLimitUpdatedAt = claudeRateLimitUpdatedAt;
    }
}
