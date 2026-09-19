package ch.ksfx.model.issues;

import javax.persistence.*;
import java.util.Date;

/**
 * A label defined per {@link IssueTracker} (name + hex color, GitHub-style), attached to issues
 * via the issue_issue_label join table (see {@link Issue#getLabels()}). Names are unique within a
 * tracker; the agent API auto-creates unknown label names on use with a neutral default color
 * (see AgentIssueApiController), so agents never have to manage label ids.
 */
@Entity
@Table(name = "issue_label")
public class IssueLabel
{
    private Long id;
    private IssueTracker issueTracker;
    private String name;
    private String color;
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
    @JoinColumn(name = "issue_tracker_id")
    public IssueTracker getIssueTracker()
    {
        return issueTracker;
    }

    public void setIssueTracker(IssueTracker issueTracker)
    {
        this.issueTracker = issueTracker;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /** Hex color like "#5b5bd6" - rendered as the chip background, with white text. */
    public String getColor()
    {
        return color;
    }

    public void setColor(String color)
    {
        this.color = color;
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
