package ch.ksfx.model;

import ch.ksfx.model.user.User;

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
import java.util.Date;

/**
 * A user's membership in a project at a level (READER / WRITER / OWNER). One row per user and
 * project. Instance administration is NOT a membership but the ADMIN role on the user (see
 * UserScope). Managed only by instance administrators (concept, Q2).
 */
@Entity
@Table(name = "project_membership")
public class ProjectMembership
{
    private Long id;
    private User user;
    private Project project;
    private ProjectMembershipLevel level = ProjectMembershipLevel.WRITER;
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
    @JoinColumn(name = "user_id")
    public User getUser()
    {
        return user;
    }

    public void setUser(User user)
    {
        this.user = user;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "level")
    public ProjectMembershipLevel getLevel()
    {
        return level;
    }

    public void setLevel(ProjectMembershipLevel level)
    {
        this.level = level;
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
