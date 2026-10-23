package ch.ksfx.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Per-instance configuration for the "Agentic" feature - since the per-project split (concept
 * "Agentic settings per project") instance INFRASTRUCTURE only: workspace root, CLI path and the
 * global kill switch. Account and policy live in {@link ProjectAgenticSettings} per project. The
 * moved DB columns still exist until the deferred drop changeset ships (rollback safety for the
 * previous jar) but are no longer mapped here. Exactly one row per instance.
 */
@Entity
@Table(name = "agentic_config")
public class AgenticConfig
{
    private Long id;
    private String claudeCliPath = "claude";
    private String workspaceRoot;
    private boolean enabled = false;


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





    public String getClaudeCliPath()
    {
        return claudeCliPath;
    }

    public void setClaudeCliPath(String claudeCliPath)
    {
        this.claudeCliPath = claudeCliPath;
    }



    /**
     * Model for all agents without an explicit {@link Agent#getModel()} override; blank/null means
     * "let the CLI pick its own default".
     */


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














}
