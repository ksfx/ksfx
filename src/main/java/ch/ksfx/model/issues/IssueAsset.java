package ch.ksfx.model.issues;

import ch.ksfx.model.Agent;
import ch.ksfx.model.user.User;

import javax.persistence.*;
import java.util.Date;

/**
 * An image/file uploaded into the issue tracker, stored as a DB blob - the exact WikiAsset pattern
 * (see that class's comment for the backup-friendliness rationale): nullable {@link #issue}
 * distinguishes an inline editor image (null - referenced only by the /issues/assets/{id} URL
 * embedded in markdown, survives everything) from a file in an issue's "Attachments" section
 * (non-null - cascade-deleted with the issue). {@link #fileSize} denormalized at upload time so
 * listings never load blobs.
 */
@Entity
@Table(name = "issue_asset")
public class IssueAsset
{
    private Long id;
    private Issue issue;
    private String fileName;
    private String contentType;
    private byte[] content;
    private Long fileSize;
    private User uploadedByUser;
    private Agent uploadedByAgent;
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

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    @Lob
    public byte[] getContent()
    {
        return content;
    }

    public void setContent(byte[] content)
    {
        this.content = content;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    @ManyToOne
    @JoinColumn(name = "uploaded_by_user_id")
    public User getUploadedByUser()
    {
        return uploadedByUser;
    }

    public void setUploadedByUser(User uploadedByUser)
    {
        this.uploadedByUser = uploadedByUser;
    }

    @ManyToOne
    @JoinColumn(name = "uploaded_by_agent_id")
    public Agent getUploadedByAgent()
    {
        return uploadedByAgent;
    }

    public void setUploadedByAgent(Agent uploadedByAgent)
    {
        this.uploadedByAgent = uploadedByAgent;
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
