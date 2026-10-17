package ch.ksfx.model.issues;

import ch.ksfx.model.Agent;
import ch.ksfx.model.user.User;

import javax.persistence.*;
import java.util.Date;

/**
 * One comment on an {@link Issue} - markdown content, human or agent author (same either/or
 * attribution pattern as the issue itself), no versioning: append and delete only.
 */
@Entity
@Table(name = "issue_comment")
public class IssueComment
{
    private Long id;
    private Issue issue;
    private String content;
    private User createdByUser;
    private Agent createdByAgent;
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
    @JoinColumn(name = "issue_id")
    public Issue getIssue()
    {
        return issue;
    }

    public void setIssue(Issue issue)
    {
        this.issue = issue;
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
}
