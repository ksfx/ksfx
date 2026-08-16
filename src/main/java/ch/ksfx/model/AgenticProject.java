package ch.ksfx.model;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * Groups {@link Agent}s that share a workspace root folder (and optionally shared resources
 * within it). An Agent's assignment to an AgenticProject is optional.
 */
@Entity
@Table(name = "agentic_project")
public class AgenticProject
{
    private Long id;

    @NotNull
    @Size(min = 2, max = 200)
    @NotEmpty
    private String name;
    private String description;
    private Date createdAt;

    // Docker isolation is opt-in and off by default - see AgenticDockerService. When disabled (the
    // default), none of the other docker* fields are ever populated or read, and ClaudeCliSessionService
    // spawns the claude CLI directly on the host exactly as for a project with isolation never touched.
    private boolean dockerIsolationEnabled = false;
    private String dockerContainerName;
    private DockerContainerStatus dockerContainerStatus = DockerContainerStatus.NOT_CREATED;
    private Date dockerContainerLastCheckedAt;

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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    public boolean getDockerIsolationEnabled()
    {
        return dockerIsolationEnabled;
    }

    public void setDockerIsolationEnabled(boolean dockerIsolationEnabled)
    {
        this.dockerIsolationEnabled = dockerIsolationEnabled;
    }

    public String getDockerContainerName()
    {
        return dockerContainerName;
    }

    public void setDockerContainerName(String dockerContainerName)
    {
        this.dockerContainerName = dockerContainerName;
    }

    public DockerContainerStatus getDockerContainerStatus()
    {
        return dockerContainerStatus;
    }

    public void setDockerContainerStatus(DockerContainerStatus dockerContainerStatus)
    {
        this.dockerContainerStatus = dockerContainerStatus;
    }

    public Date getDockerContainerLastCheckedAt()
    {
        return dockerContainerLastCheckedAt;
    }

    public void setDockerContainerLastCheckedAt(Date dockerContainerLastCheckedAt)
    {
        this.dockerContainerLastCheckedAt = dockerContainerLastCheckedAt;
    }
}
