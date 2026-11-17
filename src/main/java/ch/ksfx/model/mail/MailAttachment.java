package ch.ksfx.model.mail;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/** A file attached to an outbox mail - copied in when the mail is accepted (see mail_attachment). */
@Entity
@Table(name = "mail_attachment")
public class MailAttachment
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "outbox_id")
    private MailOutboxEntry outboxEntry;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Lob
    @Column(name = "data")
    private byte[] data;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MailOutboxEntry getOutboxEntry() { return outboxEntry; }
    public void setOutboxEntry(MailOutboxEntry outboxEntry) { this.outboxEntry = outboxEntry; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
