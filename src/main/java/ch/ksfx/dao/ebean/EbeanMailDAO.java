package ch.ksfx.dao.ebean;

import ch.ksfx.dao.MailDAO;
import ch.ksfx.model.mail.MailAttachment;
import ch.ksfx.model.mail.MailConfig;
import ch.ksfx.model.mail.MailOutboxEntry;
import io.ebean.Ebean;
import io.ebean.ExpressionList;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class EbeanMailDAO implements MailDAO
{
    @Override
    public MailConfig getMailConfig()
    {
        return Ebean.find(MailConfig.class).orderBy("id asc").setMaxRows(1).findOne();
    }

    @Override
    public void saveOrUpdateMailConfig(MailConfig config)
    {
        if (config.getId() != null) {
            Ebean.update(config);
        } else {
            Ebean.save(config);
        }
    }

    @Override
    public MailOutboxEntry getOutboxEntryForId(Long id)
    {
        return id != null ? Ebean.find(MailOutboxEntry.class, id) : null;
    }

    @Override
    public void saveOrUpdateOutboxEntry(MailOutboxEntry entry)
    {
        if (entry.getId() != null) {
            Ebean.update(entry);
        } else {
            Ebean.save(entry);
        }
    }

    @Override
    public List<MailOutboxEntry> getDueEntries(Date now, int limit)
    {
        return Ebean.find(MailOutboxEntry.class).where()
                .eq("status", MailOutboxEntry.Status.QUEUED)
                .or().isNull("nextAttemptAt").le("nextAttemptAt", now).endOr()
                .orderBy("id asc")
                .setMaxRows(limit)
                .findList();
    }

    @Override
    public int countAcceptedSince(Date since)
    {
        return Ebean.find(MailOutboxEntry.class).where()
                .ge("createdAt", since)
                .ne("status", MailOutboxEntry.Status.BLOCKED)
                .findCount();
    }

    @Override
    public List<MailOutboxEntry> getRecentEntries(Long projectId, int limit)
    {
        ExpressionList<MailOutboxEntry> where = Ebean.find(MailOutboxEntry.class).fetch("agent", "name").fetch("project", "name").where();

        if (projectId != null) {
            where.eq("project.id", projectId);
        }

        return where.orderBy("id desc").setMaxRows(limit).findList();
    }

    @Override
    public void saveAttachment(MailAttachment attachment)
    {
        Ebean.save(attachment);
    }

    @Override
    public List<MailAttachment> getAttachments(Long outboxId)
    {
        return Ebean.find(MailAttachment.class).where().eq("outboxEntry.id", outboxId).orderBy("id asc").findList();
    }
}
