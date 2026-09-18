package ch.ksfx.model.wiki;

import javax.persistence.*;
import java.util.Date;

/**
 * A real, manageable folder within a {@link Wiki} - unlike the first cut of this feature, folders
 * are no longer purely derived from page paths: they're their own rows, so an empty folder can
 * exist, be renamed (without touching any page), and be a target for organizing pages into (see
 * WikiController's folder actions in the sidebar). {@link #parent} null means top-level (directly
 * under the wiki root).
 */
@Entity
@Table(name = "wiki_folder")
public class WikiFolder
{
    private Long id;
    private Wiki wiki;
    private WikiFolder parent;
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
    @JoinColumn(name = "parent_folder_id")
    public WikiFolder getParent()
    {
        return parent;
    }

    public void setParent(WikiFolder parent)
    {
        this.parent = parent;
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
}
