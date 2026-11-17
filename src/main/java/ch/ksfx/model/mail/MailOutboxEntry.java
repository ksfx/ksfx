package ch.ksfx.model.mail;

import ch.ksfx.model.Agent;
import ch.ksfx.model.Project;

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
 * One mail in the outbox - which is also the log (concept "Mail service"): written when a mail is
 * accepted, sent by MailService's background job, kept afterwards. Recipients are stored as
 * comma-separated addresses, the body as the Markdown the caller sent.
 */
@Entity
@Table(name = "mail_outbox")
public class MailOutboxEntry
{
    public enum Status { QUEUED, SENT, FAILED, BLOCKED }

    public enum SourceType { AGENT, SERVICE, SYSTEM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private SourceType sourceType;

    /** Who sent it, for the log and the display name: the agent's name, or e.g. "Issues". */
    @Column(name = "source_label")
    private String sourceLabel;

    @ManyToOne
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "to_addresses")
    private String toAddresses;

    @Column(name = "cc_addresses")
    private String ccAddresses;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body")
    private String body;

    @Column(name = "attempts")
    private int attempts = 0;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "next_attempt_at")
    private Date nextAttemptAt;

    @Column(name = "sent_at")
    private Date sentAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
    public String getSourceLabel() { return sourceLabel; }
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }
    public Agent getAgent() { return agent; }
    public void setAgent(Agent agent) { this.agent = agent; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getToAddresses() { return toAddresses; }
    public void setToAddresses(String toAddresses) { this.toAddresses = toAddresses; }
    public String getCcAddresses() { return ccAddresses; }
    public void setCcAddresses(String ccAddresses) { this.ccAddresses = ccAddresses; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Date nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }
}
