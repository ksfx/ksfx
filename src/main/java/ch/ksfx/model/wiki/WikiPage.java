package ch.ksfx.model.wiki;

import javax.persistence.*;
import java.util.Date;

/**
 * A single wiki page, living in a {@link WikiFolder} ({@link #folder} null means top-level) within
 * its {@link Wiki}. {@link #slug} (e.g. "setup-guide.md") is generated once from the title at
 * creation time (see WikiService#createPage) and never changes afterward, even if the title is
 * later edited - so a page's identity/URL survives a rename instead of link-rotting. Uniqueness is
 * per (wiki, folder), not global - two different folders (or two different wikis) can each have
 * their own "home.md". The page's actual markdown lives in {@link WikiPageVersion}, never here -
 * this row is just the stable identity plus denormalized fields cheap to show in a listing without
 * joining to the latest version.
 *
 * {@link #parentPage} makes a page a "subpage" of another page - see WikiService#validateParentPage
 * for the one invariant that keeps this from becoming a second, independent hierarchy alongside
 * folders: a page's parent must always live in the exact same folder as the page itself. Folders
 * therefore stay the only real containment structure; the parent-page chain is just a finer-grained
 * ordering *within* one folder's pages, not an alternative to it.
 */
@Entity
@Table(name = "wiki_page")
public class WikiPage
{
    private Long id;
    private Wiki wiki;
    private WikiFolder folder;
    private WikiPage parentPage;
    private String slug;
    private String title;
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
    @JoinColumn(name = "wiki_id")
    public Wiki getWiki()
    {
        return wiki;
    }

    public void setWiki(Wiki wiki)
    {
        this.wiki = wiki;
    }

    @ManyToOne
    @JoinColumn(name = "folder_id")
    public WikiFolder getFolder()
    {
        return folder;
    }

    public void setFolder(WikiFolder folder)
    {
        this.folder = folder;
    }

    @ManyToOne
    @JoinColumn(name = "parent_page_id")
    public WikiPage getParentPage()
    {
        return parentPage;
    }

    public void setParentPage(WikiPage parentPage)
    {
        this.parentPage = parentPage;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(String slug)
    {
        this.slug = slug;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
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
