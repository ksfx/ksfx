package ch.ksfx.model;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * A recurring background task for an {@link Agent}: when {@link #cronSchedule} fires, {@link #taskPrompt}
 * is sent to the agent exactly as if the user had typed it (same chat history, same --resume
 * session continuity). An agent can have many of these. {@link #lastRunAt}/{@link #lastRunStatus}/
 * {@link #lastRunError} are a flat "last run" summary for the schedule list - the full detail trail
 * for every run (scheduled or manual) already lives in {@link AgentMessage}.
 */
@Entity
@Table(name = "agent_schedule")
public class AgentSchedule
{
    private Long id;
    private Agent agent;

    @NotNull
    @Size(min = 2, max = 255)
    @NotEmpty
    private String name;
    private String taskPrompt;
    private String cronSchedule;
    private boolean cronScheduleEnabled = false;
    private Date lastRunAt;
    private String lastRunStatus;
    private String lastRunError;
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
    @JoinColumn(name = "agent_id")
    public Agent getAgent()
    {
        return agent;
    }

    public void setAgent(Agent agent)
    {
        this.agent = agent;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Lob
    public String getTaskPrompt()
    {
        return taskPrompt;
    }

    public void setTaskPrompt(String taskPrompt)
    {
        this.taskPrompt = taskPrompt;
    }

    public String getCronSchedule()
    {
        return cronSchedule;
    }

    public void setCronSchedule(String cronSchedule)
    {
        this.cronSchedule = cronSchedule;
    }

    public boolean getCronScheduleEnabled()
    {
        return cronScheduleEnabled;
    }

    public void setCronScheduleEnabled(boolean cronScheduleEnabled)
    {
        this.cronScheduleEnabled = cronScheduleEnabled;
    }

    public Date getLastRunAt()
    {
        return lastRunAt;
    }

    public void setLastRunAt(Date lastRunAt)
    {
        this.lastRunAt = lastRunAt;
    }

    public String getLastRunStatus()
    {
        return lastRunStatus;
    }

    public void setLastRunStatus(String lastRunStatus)
    {
        this.lastRunStatus = lastRunStatus;
    }

    @Lob
    public String getLastRunError()
    {
        return lastRunError;
    }

    public void setLastRunError(String lastRunError)
    {
        this.lastRunError = lastRunError;
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
