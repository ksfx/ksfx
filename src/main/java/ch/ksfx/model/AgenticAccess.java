package ch.ksfx.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

/**
 * One named account of a project (concept "Agentic accesses"): auth mode and key decide whose
 * Anthropic account a turn runs on. A project can hold several - "company API key", "Kejo's Max
 * subscription" - and each agent picks one (Agent.agenticAccess); agents without a choice use the
 * project's default access. Project-bound: managed by the project's OWNERs on its Agentic Settings
 * page, never shared across projects.
 *
 * The rate-limit snapshot lives here because it describes the account: captured from the CLI after
 * every turn that ran on this access. OAUTH accesses all use the host's one `claude login`, so two
 * OAUTH accesses are the same account under two names.
 */
@Entity
@Table(name = "agentic_access")
public class AgenticAccess
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_mode")
    private AgenticAuthMode authMode = AgenticAuthMode.API_KEY;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "is_default")
    private boolean defaultAccess = false;

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

    @Column(name = "created_at")
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AgenticAuthMode getAuthMode() { return authMode; }
    public void setAuthMode(AgenticAuthMode authMode) { this.authMode = authMode; }
    @JsonIgnore
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public boolean getDefaultAccess() { return defaultAccess; }
    public void setDefaultAccess(boolean defaultAccess) { this.defaultAccess = defaultAccess; }
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
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    /** Whether a turn can run on this access at all: OAUTH needs no key, the others do. */
    @Transient
    public boolean isUsable()
    {
        return authMode == AgenticAuthMode.OAUTH || (apiKey != null && !apiKey.trim().isEmpty());
    }

    /** The key for display: first and last characters only. */
    @Transient
    public String getMaskedKey()
    {
        if (authMode == AgenticAuthMode.OAUTH) {
            return "host login";
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "no key set";
        }

        String key = apiKey.trim();

        return key.length() <= 12 ? "••••" : key.substring(0, 7) + "…" + key.substring(key.length() - 4);
    }

    @Transient
    public String getAuthModeLabel()
    {
        if (authMode == AgenticAuthMode.SETUP_TOKEN) {
            return "Setup token";
        }

        return authMode == AgenticAuthMode.OAUTH ? "OAuth" : "API key";
    }

    /**
     * Status of the last rate-limit snapshot as a CSS modifier: ok / warning / blocked / unknown.
     * A "rejected" snapshot whose reset time has passed is shown as unknown - the block is over,
     * but no turn has confirmed the new state yet.
     */
    @Transient
    public String getStatusKey()
    {
        if (claudeRateLimitStatus == null) {
            return "unknown";
        }

        if ("rejected".equals(claudeRateLimitStatus)) {
            return claudeRateLimitResetsAt != null && claudeRateLimitResetsAt.before(new Date()) ? "unknown" : "blocked";
        }

        return claudeRateLimitStatus.contains("warning") ? "warning" : "ok";
    }
}
