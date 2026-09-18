package ch.ksfx.model.wiki;

import javax.persistence.*;
import java.util.Date;

/**
 * A named, independent wiki (e.g. one per project/team) - every {@link WikiPage} belongs to exactly
 * one Wiki, and page paths are only unique within a Wiki, not globally, so two wikis can each have
 * their own "home.md" without colliding. The sidebar dropdown (see wiki_index.html and friends)
 * switches between these; there is no cross-wiki page listing or search yet.
 */
@Entity
@Table(name = "wiki")
public class Wiki
{
    private Long id;
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
}
