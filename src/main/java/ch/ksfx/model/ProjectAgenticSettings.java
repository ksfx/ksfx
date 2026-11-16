package ch.ksfx.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * A project's Agentic policy (concept "Agentic settings per project"): default model and permission
 * mode are the project's resolution-time defaults (agent value wins when set, provider auto
 * default applies when neither is set). No inheritance: a project without a row here, or with
 * enabled=false, has no working agents - a turn fails with a clear message. The accounts the
 * agents run on - auth mode, key, rate-limit snapshot - are the project's {@link AgenticAccess}
 * rows since 2026-11-16 (their old columns here are unmapped and dropped one release later).
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

    @Column(name = "default_model")
    private String defaultModel;

    @Column(name = "default_permission_mode")
    private String defaultPermissionMode = "default";

    @Column(name = "enabled")
    private boolean enabled = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public String getDefaultPermissionMode() { return defaultPermissionMode; }
    public void setDefaultPermissionMode(String defaultPermissionMode) { this.defaultPermissionMode = defaultPermissionMode; }
    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
