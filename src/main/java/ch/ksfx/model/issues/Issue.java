package ch.ksfx.model.issues;

import ch.ksfx.model.Agent;
import ch.ksfx.model.user.User;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * One issue in an {@link IssueTracker} - GitHub-style, but deliberately without any versioning
 * (unlike WikiPage/WikiPageVersion): {@link #description} is edited in place, per the explicit
 * design decision when this was built. Displayed as "#&lt;id&gt;" - ids are global, not
 * per-tracker (no counter to race on; a KSFX instance is small enough that gapless per-tracker
 * numbering isn't worth the transaction gymnastics).
 *
 * Creator and assignee can each be a human OR an agent - same either/or pair-of-nullable-FKs
 * pattern as WikiPageVersion's editedByUser/editedByAgent: at most one of each pair is set, and
 * for API-created issues the creator comes strictly from the authenticated bearer token, never
 * the request body.
 */
@Entity
@Table(name = "issue")
public class Issue
{
    private Long id;
    private IssueTracker issueTracker;
    private String title;
    private String description;
    private IssueStatus status = IssueStatus.OPEN;
    private IssuePriority priority = IssuePriority.MEDIUM;

    // Deliberately NOT initialized to an ArrayList: Ebean's enhanced load path casts the field to
    // its own BeanCollection when fetching the many-to-many, and a plain pre-initialized ArrayList
    // makes every fetch("labels") query blow up with a ClassCastException. Callers must null-guard
    // (Thymeleaf's th:each already treats null as empty).
    private List<IssueLabel> labels;
    private User assigneeUser;
    private Agent assigneeAgent;
    private User createdByUser;
    private Agent createdByAgent;
    private Date createdAt;
    private Date updatedAt;

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
    @JoinColumn(name = "issue_tracker_id")
    public IssueTracker getIssueTracker()
    {
        return issueTracker;
    }

    public void setIssueTracker(IssueTracker issueTracker)
    {
        this.issueTracker = issueTracker;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @Lob
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Enumerated(EnumType.STRING)
    public IssueStatus getStatus()
    {
        return status;
    }

    public void setStatus(IssueStatus status)
    {
        this.status = status;
    }

    @Enumerated(EnumType.STRING)
    public IssuePriority getPriority()
    {
        return priority;
    }

    public void setPriority(IssuePriority priority)
    {
        this.priority = priority;
    }

    @ManyToMany
    @JoinTable(name = "issue_issue_label",
            joinColumns = @JoinColumn(name = "issue_id"),
            inverseJoinColumns = @JoinColumn(name = "issue_label_id"))
    public List<IssueLabel> getLabels()
    {
        return labels;
    }

    public void setLabels(List<IssueLabel> labels)
    {
        this.labels = labels;
    }

    @ManyToOne
    @JoinColumn(name = "assignee_user_id")
    public User getAssigneeUser()
    {
        return assigneeUser;
    }

    public void setAssigneeUser(User assigneeUser)
    {
        this.assigneeUser = assigneeUser;
    }

    @ManyToOne
    @JoinColumn(name = "assignee_agent_id")
    public Agent getAssigneeAgent()
    {
        return assigneeAgent;
    }

    public void setAssigneeAgent(Agent assigneeAgent)
    {
        this.assigneeAgent = assigneeAgent;
    }

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    public User getCreatedByUser()
    {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser)
    {
        this.createdByUser = createdByUser;
    }

    @ManyToOne
    @JoinColumn(name = "created_by_agent_id")
    public Agent getCreatedByAgent()
    {
        return createdByAgent;
    }

    public void setCreatedByAgent(Agent createdByAgent)
    {
        this.createdByAgent = createdByAgent;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt)
    {
        this.updatedAt = updatedAt;
    }
}
