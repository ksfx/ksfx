package ch.ksfx.model.issues;

import ch.ksfx.model.Project;
import javax.persistence.*;
import java.util.Date;

/**
 * A named, independent issue tracker (e.g. one per project) - the Issues sibling of
 * {@link ch.ksfx.model.wiki.Wiki}: every {@link Issue} and {@link IssueLabel} belongs to exactly
 * one tracker, and the sidebar dropdown switches between them (see IssueController).
 */
@Entity
@Table(name = "issue_tracker")
public class IssueTracker
{
    private Long id;
    private Project project;
    private String name;
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

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    @ManyToOne
    @JoinColumn(name = "project_id")
    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }
}
