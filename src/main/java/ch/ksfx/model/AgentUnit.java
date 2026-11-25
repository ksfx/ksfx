package ch.ksfx.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;

/**
 * A unit of a project's agents ("Marketing", "Ops"): agents can optionally belong to one
 * (Agent.agentUnit) and are then shown as a collapsible group in the agent sidebar. One level, no
 * nesting, project-bound; managed by the project's OWNERs on its Agentic Settings page or created
 * on the fly from an agent's edit page. UI only - agents themselves do not see units.
 */
@Entity
@Table(name = "agent_unit")
public class AgentUnit
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "name")
    private String name;

    /** Position in the sidebar and on the settings page, ascending. */
    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at")
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
