package ch.ksfx.model.wiki;

import ch.ksfx.model.Agent;
import ch.ksfx.model.user.User;

import javax.persistence.*;
import java.util.Date;

/**
 * An image/file uploaded into the wiki, stored as a DB blob rather than on disk - per the explicit
 * reason this was asked for: a single DB backup then covers the whole wiki (pages, versions, and
 * assets alike), instead of pages living in MySQL while their images live in some separate
 * filesystem path that needs its own backup story. Served back out via WikiController's
 * /wiki/assets/{id}.
 *
 * Two kinds of asset share this table, distinguished by {@link #wikiPage}: null means an inline
 * editor image (uploaded mid-edit, possibly before its page even exists - referenced only by the
 * URL embedded in the markdown); non-null means a file attached to that page's "Attachments"
 * section (uploaded from the page view, deleted along with the page via the DB FK cascade).
 * {@link #fileSize} is denormalized at upload time so attachment listings don't have to load the
 * blob just to show a size.
 */
@Entity
@Table(name = "wiki_asset")
public class WikiAsset
{
    private Long id;
    private WikiPage wikiPage;
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
    @JoinColumn(name = "wiki_page_id")
    public WikiPage getWikiPage()
    {
        return wikiPage;
    }

    public void setWikiPage(WikiPage wikiPage)
    {
        this.wikiPage = wikiPage;
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
