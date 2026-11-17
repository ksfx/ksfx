package ch.ksfx.dao;

import ch.ksfx.model.mail.MailAttachment;
import ch.ksfx.model.mail.MailConfig;
import ch.ksfx.model.mail.MailOutboxEntry;

import java.util.Date;
import java.util.List;

public interface MailDAO
{
    /** The single configuration row, or null if Admin -> Mail was never saved. */
    public MailConfig getMailConfig();

    public void saveOrUpdateMailConfig(MailConfig config);

    public MailOutboxEntry getOutboxEntryForId(Long id);

    public void saveOrUpdateOutboxEntry(MailOutboxEntry entry);

    /** QUEUED mails whose next attempt is due, oldest first. */
    public List<MailOutboxEntry> getDueEntries(Date now, int limit);

    /** Mails accepted (everything but BLOCKED) since {@code since} - what counts against the daily limit. */
    public int countAcceptedSince(Date since);

    /** The latest mails, newest first - all of them, or only one project's when {@code projectId} is set. */
    public List<MailOutboxEntry> getRecentEntries(Long projectId, int limit);

    public void saveAttachment(MailAttachment attachment);

    public List<MailAttachment> getAttachments(Long outboxId);
}
