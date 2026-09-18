package ch.ksfx.model.wiki;

import ch.ksfx.model.Agent;
import ch.ksfx.model.AgentMessage;
import ch.ksfx.model.user.User;

import javax.persistence.*;
import java.util.Date;

/**
 * One immutable snapshot of a {@link WikiPage}'s markdown content. Saving a page (whether from the
 * web editor or the agent API) always inserts a new row rather than updating an existing one, so the
 * full edit history is just "every WikiPageVersion for this page, newest id first" - a restore is
 * therefore also an insert (copying an old version's content into a new row), never a delete, so the
 * history itself can never be edited away.
 *
 * Exactly one of {@link #editedByUser}/{@link #editedByAgent} is set, matching whichever caller
 * authenticated the save (a human via session login, or an agent via its own bearer token - see
 * AgentWikiApiController). {@link #sourceAgentMessage} is an optional extra pointer, set only for
 * agent-authored versions, to the specific chat turn that produced this edit - useful for tracing
 * "why did this page change" back to a conversation, but not load-bearing: the content itself always
 * lives on this row, never solely in the referenced message, so history survives even if that chat
 * (or the whole agent) is later deleted.
 */
@Entity
@Table(name = "wiki_page_version")
public class WikiPageVersion
{
    private Long id;
    private WikiPage wikiPage;
    private String content;
    private User editedByUser;
    private Agent editedByAgent;
    private AgentMessage sourceAgentMessage;
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

    @Lob
    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    @ManyToOne
    @JoinColumn(name = "edited_by_user_id")
    public User getEditedByUser()
    {
        return editedByUser;
    }

    public void setEditedByUser(User editedByUser)
    {
        this.editedByUser = editedByUser;
    }

    @ManyToOne
    @JoinColumn(name = "edited_by_agent_id")
    public Agent getEditedByAgent()
    {
        return editedByAgent;
    }

    public void setEditedByAgent(Agent editedByAgent)
    {
        this.editedByAgent = editedByAgent;
    }

    @ManyToOne
    @JoinColumn(name = "source_agent_message_id")
    public AgentMessage getSourceAgentMessage()
    {
        return sourceAgentMessage;
    }

    public void setSourceAgentMessage(AgentMessage sourceAgentMessage)
    {
        this.sourceAgentMessage = sourceAgentMessage;
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
