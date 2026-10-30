package ch.ksfx.model;

import javax.persistence.*;
import java.util.Date;

/**
 * A chat message sent while the agent's turn was still running - waits here until that turn ends,
 * then becomes a normal USER AgentMessage and runs as (part of) the next turn. See
 * ClaudeCliSessionService (runTurn / the hand-off at the end of executeTurn) and the migration
 * 20261031090000 for why queued messages are not stored in agent_message directly.
 */
@Entity
@Table(name = "agent_message_queue")
public class AgentMessageQueueEntry
{
    private Long id;
    private Agent agent;
    private String content;
    private String cliText;
    private String attachments;
    private boolean voiceInput;
    private Agent fromAgent;
    private boolean deliverReply;
    private boolean reply;
    private Date createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @ManyToOne
    @JoinColumn(name = "agent_id")
    public Agent getAgent() { return agent; }
    public void setAgent(Agent agent) { this.agent = agent; }

    /** What the chat shows (the - possibly voice-cleaned - message text). */
    @Lob
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    /** Exactly what goes to the CLI: the message plus the note listing uploaded files. */
    @Lob
    @Column(name = "cli_text")
    public String getCliText() { return cliText; }
    public void setCliText(String cliText) { this.cliText = cliText; }

    @Lob
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }

    @Column(name = "voice_input")
    public boolean getVoiceInput() { return voiceInput; }
    public void setVoiceInput(boolean voiceInput) { this.voiceInput = voiceInput; }

    /** Sending agent for agent-to-agent messages; null = a human in the chat. */
    @ManyToOne
    @JoinColumn(name = "from_agent_id")
    public Agent getFromAgent() { return fromAgent; }
    public void setFromAgent(Agent fromAgent) { this.fromAgent = fromAgent; }

    /** The answer of the turn that processes this entry goes back to fromAgent automatically. */
    @Column(name = "deliver_reply")
    public boolean getDeliverReply() { return deliverReply; }
    public void setDeliverReply(boolean deliverReply) { this.deliverReply = deliverReply; }

    /** This entry IS such a delivered answer - never forwarded back again (no loops). */
    @Column(name = "is_reply")
    public boolean getReply() { return reply; }
    public void setReply(boolean reply) { this.reply = reply; }

    @Column(name = "created_at")
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
