package ch.ksfx.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Date;

/**
 * A project's Agentic account and policy (concept "Agentic settings per project"): auth mode and
 * key decide whose Anthropic account the project's agents run on, default model and permission
 * mode are the project's resolution-time defaults (agent value wins when set, provider auto
 * default applies when neither is set). No inheritance: a project without a row here, or with
 * enabled=false, has no working agents - a turn fails with a clear message. The rate-limit
 * snapshot lives here because it describes the account, and the account belongs to the project.
 * {@link AgenticConfig} keeps only instance infrastructure (workspace root, CLI path, kill switch).
 */
@Entity
@Table(name = "project_agentic_settings")
public class ProjectAgenticSettings
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_mode")
    private AgenticAuthMode authMode = AgenticAuthMode.API_KEY;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "default_model")
    private String defaultModel;

    @Column(name = "default_permission_mode")
    private String defaultPermissionMode = "default";

    @Column(name = "enabled")
    private boolean enabled = false;

    @Column(name = "claude_rate_limit_status")
    private String claudeRateLimitStatus;

    @Column(name = "claude_rate_limit_type")
    private String claudeRateLimitType;

    @Column(name = "claude_rate_limit_resets_at")
    private Date claudeRateLimitResetsAt;

    @Column(name = "claude_rate_limit_overage_status")
    private String claudeRateLimitOverageStatus;

    @Column(name = "claude_rate_limit_overage_resets_at")
    private Date claudeRateLimitOverageResetsAt;

    @Column(name = "claude_rate_limit_using_overage")
    private Boolean claudeRateLimitUsingOverage;

    @Column(name = "claude_rate_limit_updated_at")
    private Date claudeRateLimitUpdatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public AgenticAuthMode getAuthMode() { return authMode; }
    public void setAuthMode(AgenticAuthMode authMode) { this.authMode = authMode; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public String getDefaultPermissionMode() { return defaultPermissionMode; }
    public void setDefaultPermissionMode(String defaultPermissionMode) { this.defaultPermissionMode = defaultPermissionMode; }
    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getClaudeRateLimitStatus() { return claudeRateLimitStatus; }
    public void setClaudeRateLimitStatus(String v) { this.claudeRateLimitStatus = v; }
    public String getClaudeRateLimitType() { return claudeRateLimitType; }
    public void setClaudeRateLimitType(String v) { this.claudeRateLimitType = v; }
    public Date getClaudeRateLimitResetsAt() { return claudeRateLimitResetsAt; }
    public void setClaudeRateLimitResetsAt(Date v) { this.claudeRateLimitResetsAt = v; }
    public String getClaudeRateLimitOverageStatus() { return claudeRateLimitOverageStatus; }
    public void setClaudeRateLimitOverageStatus(String v) { this.claudeRateLimitOverageStatus = v; }
    public Date getClaudeRateLimitOverageResetsAt() { return claudeRateLimitOverageResetsAt; }
    public void setClaudeRateLimitOverageResetsAt(Date v) { this.claudeRateLimitOverageResetsAt = v; }
    public Boolean getClaudeRateLimitUsingOverage() { return claudeRateLimitUsingOverage; }
    public void setClaudeRateLimitUsingOverage(Boolean v) { this.claudeRateLimitUsingOverage = v; }
    public Date getClaudeRateLimitUpdatedAt() { return claudeRateLimitUpdatedAt; }
    public void setClaudeRateLimitUpdatedAt(Date v) { this.claudeRateLimitUpdatedAt = v; }
}
