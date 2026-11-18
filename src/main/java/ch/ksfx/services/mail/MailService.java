package ch.ksfx.services.mail;

import ch.ksfx.dao.MailDAO;
import ch.ksfx.dao.user.UserDAO;
import ch.ksfx.model.Agent;
import ch.ksfx.model.Project;
import ch.ksfx.model.mail.MailAttachment;
import ch.ksfx.model.mail.MailConfig;
import ch.ksfx.model.mail.MailOutboxEntry;
import ch.ksfx.model.user.User;
import ch.ksfx.services.project.UserScope;
import ch.ksfx.services.systemlogger.SystemLogger;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * The platform's one way to send mail (concept "Mail service", KSFX wiki) - for agents (their
 * /agentic/api/mail endpoint) and later for services like the issues and the wiki.
 *
 * {@link #submit} only checks and writes the mail to the outbox ({@code mail_outbox}, which is also
 * the log); {@link #processOutbox} sends it in the background, with retries - callers never wait
 * for SMTP. The sender is always the platform address from Admin -> Mail; who sent it shows in the
 * display name ("Lexi via KSFX") and in a footer. The only limit is the platform-wide daily
 * emergency brake: once reached, mails are rejected (logged as BLOCKED) and the admins are told
 * once per day.
 */
@Service
public class MailService
{
    public static final int MAX_RECIPIENTS = 50;
    public static final int MAX_BODY_CHARS = 200_000;
    public static final long MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024;
    private static final int MAX_ATTEMPTS = 5;
    private static final int[] RETRY_MINUTES = { 1, 5, 15, 60 };
    private static final int SMTP_TIMEOUT_MS = 20_000;

    private final MailDAO mailDAO;
    private final UserDAO userDAO;
    private final UserScope userScope;
    private final SystemLogger systemLogger;

    private final Parser markdownParser = Parser.builder().extensions(Arrays.asList(TablesExtension.create())).build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().extensions(Arrays.asList(TablesExtension.create())).escapeHtml(true).build();

    public MailService(MailDAO mailDAO, UserDAO userDAO, UserScope userScope, SystemLogger systemLogger)
    {
        this.mailDAO = mailDAO;
        this.userDAO = userDAO;
        this.userScope = userScope;
        this.systemLogger = systemLogger;
    }

    /** Whether mail can be sent at all right now - also decides whether agents are told about it. */
    public boolean isAvailable()
    {
        MailConfig config = mailDAO.getMailConfig();

        return config != null && config.isUsable();
    }

    /**
     * Checks a mail and puts it into the outbox. Throws {@link MailRejectedException} with a readable
     * reason when mail is off, the mail is invalid, or the daily limit is reached (then the mail is
     * still logged, as BLOCKED). Synchronized so two callers cannot both slip past the limit.
     */
    public synchronized MailOutboxEntry submit(MailRequest request)
    {
        MailConfig config = mailDAO.getMailConfig();

        if (config == null || !config.isUsable()) {
            throw new MailRejectedException(MailRejectedException.Reason.DISABLED, "Mail is not enabled on this platform (Admin -> Mail).");
        }

        List<String> to = normalizeAddresses(request.to, "to");
        List<String> cc = normalizeAddresses(request.cc, "cc");

        if (to.isEmpty()) {
            throw invalid("At least one recipient in \"to\" is required.");
        }

        if (to.size() + cc.size() > MAX_RECIPIENTS) {
            throw invalid("Too many recipients - at most " + MAX_RECIPIENTS + ".");
        }

        String subject = request.subject != null ? request.subject.trim() : "";

        if (subject.isEmpty()) {
            throw invalid("A subject is required.");
        }

        if (subject.contains("\r") || subject.contains("\n")) {
            throw invalid("The subject must be a single line.");
        }

        if (subject.length() > 500) {
            throw invalid("The subject is too long - at most 500 characters.");
        }

        String body = request.markdownBody != null ? request.markdownBody : "";

        if (body.length() > MAX_BODY_CHARS) {
            throw invalid("The body is too long - at most " + MAX_BODY_CHARS + " characters.");
        }

        long attachmentBytes = request.attachments.stream().mapToLong(a -> a.data.length).sum();

        if (attachmentBytes > MAX_ATTACHMENT_BYTES) {
            throw invalid("Attachments too large - at most " + (MAX_ATTACHMENT_BYTES / 1024 / 1024) + " MB in total.");
        }

        MailOutboxEntry entry = new MailOutboxEntry();
        entry.setSourceType(request.sourceType);
        entry.setSourceLabel(request.sourceLabel);
        entry.setAgent(request.agent);
        entry.setProject(request.project);
        entry.setToAddresses(String.join(", ", to));
        entry.setCcAddresses(cc.isEmpty() ? null : String.join(", ", cc));
        entry.setSubject(subject);
        entry.setBody(body);
        entry.setCreatedAt(new Date());

        boolean overLimit = !request.bypassLimit && mailDAO.countAcceptedSince(startOfToday()) >= config.getDailyLimit();

        if (overLimit) {
            entry.setStatus(MailOutboxEntry.Status.BLOCKED);
            entry.setLastError("Daily limit of " + config.getDailyLimit() + " mails reached.");
            mailDAO.saveOrUpdateOutboxEntry(entry);
            notifyAdminsOfLimit(config);

            throw new MailRejectedException(MailRejectedException.Reason.LIMIT,
                    "The platform's daily mail limit (" + config.getDailyLimit() + ") is reached - no more mails today. The admins have been told.");
        }

        entry.setStatus(MailOutboxEntry.Status.QUEUED);
        entry.setNextAttemptAt(new Date());
        mailDAO.saveOrUpdateOutboxEntry(entry);

        for (Attachment attachment : request.attachments) {
            MailAttachment row = new MailAttachment();
            row.setOutboxEntry(entry);
            row.setFileName(attachment.fileName);
            row.setContentType(attachment.contentType);
            row.setData(attachment.data);
            mailDAO.saveAttachment(row);
        }

        return entry;
    }

    /** Sends what is due. Runs in the background; a failure is retried a few times, then marked FAILED. */
    @Scheduled(initialDelay = 30_000, fixedDelay = 15_000)
    public void processOutbox()
    {
        MailConfig config = mailDAO.getMailConfig();

        if (config == null || !config.isUsable()) {
            return; // queued mails wait until mail is switched on again
        }

        for (MailOutboxEntry entry : mailDAO.getDueEntries(new Date(), 20)) {
            try {
                send(config, entry, mailDAO.getAttachments(entry.getId()));
                entry.setStatus(MailOutboxEntry.Status.SENT);
                entry.setSentAt(new Date());
                entry.setLastError(null);
                entry.setNextAttemptAt(null);
            } catch (Exception e) {
                int attempts = entry.getAttempts() + 1;
                entry.setLastError(describe(e));

                if (attempts >= MAX_ATTEMPTS) {
                    entry.setStatus(MailOutboxEntry.Status.FAILED);
                    entry.setNextAttemptAt(null);
                    systemLogger.logMessage("MAIL", "Mail " + entry.getId() + " failed for good after " + attempts + " attempts: " + entry.getLastError());
                } else {
                    Calendar next = Calendar.getInstance();
                    next.add(Calendar.MINUTE, RETRY_MINUTES[Math.min(attempts - 1, RETRY_MINUTES.length - 1)]);
                    entry.setNextAttemptAt(next.getTime());
                }
            }

            entry.setAttempts(entry.getAttempts() + 1);
            mailDAO.saveOrUpdateOutboxEntry(entry);
        }
    }

    /** Sends a test mail right away with the given (possibly unsaved) settings; returns null or the error. */
    public String sendTestMail(MailConfig config, String toAddress)
    {
        MailOutboxEntry entry = new MailOutboxEntry();
        entry.setSourceType(MailOutboxEntry.SourceType.SYSTEM);
        entry.setToAddresses(toAddress);
        entry.setSubject("KSFX test mail");
        entry.setBody("This is a test mail from **Admin -> Mail**. If you can read it, the SMTP settings work.");

        try {
            send(config, entry, new ArrayList<>());
            return null;
        } catch (Exception e) {
            return describe(e);
        }
    }

    /** "user:{id}" -> the user's mail address, or null if there is no such user or no address. */
    public String addressOfUser(Long userId)
    {
        User user = userDAO.getUser(userId);

        return user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty() ? user.getEmail().trim() : null;
    }

    private void send(MailConfig config, MailOutboxEntry entry, List<MailAttachment> attachments) throws MessagingException, UnsupportedEncodingException
    {
        Session session = sessionFor(config);
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(config.getFromAddress().trim(), displayName(config, entry), "UTF-8"));

        if (config.getReplyTo() != null && !config.getReplyTo().trim().isEmpty()) {
            message.setReplyTo(InternetAddress.parse(config.getReplyTo().trim()));
        }

        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(entry.getToAddresses()));

        if (entry.getCcAddresses() != null && !entry.getCcAddresses().trim().isEmpty()) {
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(entry.getCcAddresses()));
        }

        message.setSubject(entry.getSubject(), "UTF-8");
        message.setSentDate(new Date());

        String footer = footer(entry);
        String body = entry.getBody() != null ? entry.getBody() : "";

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body + (footer != null ? "\n\n--\n" + footer : ""), "UTF-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(toHtml(body, footer), "text/html; charset=UTF-8");

        MimeMultipart alternative = new MimeMultipart("alternative");
        alternative.addBodyPart(textPart);
        alternative.addBodyPart(htmlPart);

        if (attachments.isEmpty()) {
            message.setContent(alternative);
        } else {
            MimeBodyPart alternativeWrapper = new MimeBodyPart();
            alternativeWrapper.setContent(alternative);

            MimeMultipart mixed = new MimeMultipart("mixed");
            mixed.addBodyPart(alternativeWrapper);

            for (MailAttachment attachment : attachments) {
                MimeBodyPart filePart = new MimeBodyPart();
                String contentType = attachment.getContentType() != null ? attachment.getContentType() : "application/octet-stream";
                filePart.setDataHandler(new DataHandler(new ByteArrayDataSource(attachment.getData(), contentType)));
                filePart.setFileName(attachment.getFileName());
                mixed.addBodyPart(filePart);
            }

            message.setContent(mixed);
        }

        Transport.send(message);
    }

    private Session sessionFor(MailConfig config)
    {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", config.getSmtpHost().trim());
        properties.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
        properties.put("mail.smtp.connectiontimeout", String.valueOf(SMTP_TIMEOUT_MS));
        properties.put("mail.smtp.timeout", String.valueOf(SMTP_TIMEOUT_MS));
        properties.put("mail.smtp.writetimeout", String.valueOf(SMTP_TIMEOUT_MS));

        if (MailConfig.SECURITY_STARTTLS.equals(config.getSmtpSecurity())) {
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
        } else if (MailConfig.SECURITY_SSL.equals(config.getSmtpSecurity())) {
            properties.put("mail.smtp.ssl.enable", "true");
        }

        boolean auth = config.getSmtpUsername() != null && !config.getSmtpUsername().trim().isEmpty();
        properties.put("mail.smtp.auth", String.valueOf(auth));

        if (!auth) {
            return Session.getInstance(properties);
        }

        final String username = config.getSmtpUsername().trim();
        final String password = config.getSmtpPassword() != null ? config.getSmtpPassword() : "";

        return Session.getInstance(properties, new Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    /** "Lexi via KSFX HQ" for an agent's mail, else just the configured name. */
    private String displayName(MailConfig config, MailOutboxEntry entry)
    {
        String platform = config.getFromName() != null && !config.getFromName().trim().isEmpty() ? config.getFromName().trim() : "KSFX";

        if (entry.getSourceType() == MailOutboxEntry.SourceType.AGENT && entry.getSourceLabel() != null) {
            return entry.getSourceLabel() + " via " + platform;
        }

        return platform;
    }

    private String footer(MailOutboxEntry entry)
    {
        if (entry.getSourceType() == MailOutboxEntry.SourceType.AGENT) {
            return "Sent by agent " + entry.getSourceLabel()
                    + (entry.getProject() != null ? " in project " + entry.getProject().getName() : "") + " via KSFX.";
        }

        if (entry.getSourceType() == MailOutboxEntry.SourceType.SERVICE) {
            return "Sent by KSFX" + (entry.getSourceLabel() != null ? " (" + entry.getSourceLabel() + ")" : "") + ".";
        }

        return null;
    }

    private String toHtml(String markdown, String footer)
    {
        String content = htmlRenderer.render(markdownParser.parse(markdown));

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + "body{font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;font-size:15px;line-height:1.5;color:#222;}"
                + "table{border-collapse:collapse;margin:8px 0;}th,td{border:1px solid #ccc;padding:5px 10px;text-align:left;vertical-align:top;}"
                + "th{background:#f1f3f6;}code{background:#f1f3f6;padding:1px 4px;border-radius:3px;}pre{background:#f1f3f6;padding:10px;border-radius:4px;overflow:auto;}"
                + ".ksfx-footer{margin-top:24px;padding-top:8px;border-top:1px solid #ddd;color:#888;font-size:12px;}"
                + "</style></head><body>" + content
                + (footer != null ? "<div class=\"ksfx-footer\">" + HtmlUtils.htmlEscape(footer) + "</div>" : "")
                + "</body></html>";
    }

    /** One clean address per entry, validated strictly; throws on anything invalid. */
    private List<String> normalizeAddresses(List<String> addresses, String field)
    {
        List<String> result = new ArrayList<>();

        if (addresses == null) {
            return result;
        }

        for (String raw : addresses) {
            String address = raw != null ? raw.trim() : "";

            if (address.isEmpty()) {
                continue;
            }

            if (address.contains(",") || address.contains(";") || address.contains("\r") || address.contains("\n")) {
                throw invalid("One address per entry in \"" + field + "\": " + address);
            }

            try {
                InternetAddress parsed = new InternetAddress(address, true);
                parsed.validate();

                if (result.stream().noneMatch(existing -> existing.equalsIgnoreCase(parsed.getAddress()))) {
                    result.add(parsed.getAddress()); // the same person twice (e.g. two admin accounts, one address) gets one mail
                }
            } catch (AddressException e) {
                throw invalid("Invalid address in \"" + field + "\": " + address);
            }
        }

        return result;
    }

    /** Tells every admin with a mail address once per day that the limit was hit - bypassing the limit itself. */
    private void notifyAdminsOfLimit(MailConfig config)
    {
        Date today = startOfToday();

        if (config.getLimitNoticeSentOn() != null && !config.getLimitNoticeSentOn().before(today)) {
            return;
        }

        config.setLimitNoticeSentOn(today);
        mailDAO.saveOrUpdateMailConfig(config);

        List<String> admins = userDAO.getAllUsers().stream()
                .filter(userScope::isAdmin)
                .map(User::getEmail)
                .filter(email -> email != null && !email.trim().isEmpty())
                .collect(Collectors.toList());

        systemLogger.logMessage("MAIL", "Daily mail limit of " + config.getDailyLimit() + " reached - further mails are blocked until tomorrow.");

        if (admins.isEmpty()) {
            return;
        }

        MailRequest notice = new MailRequest();
        notice.to = admins;
        notice.subject = "KSFX: daily mail limit reached";
        notice.markdownBody = "The platform has sent **" + config.getDailyLimit() + "** mails today - the daily limit set on Admin -> Mail. "
                + "Further mails are rejected until tomorrow and show up as BLOCKED in the mail log.\n\n"
                + "If this was not expected, check the log for which agent or service sent them.";
        notice.sourceType = MailOutboxEntry.SourceType.SYSTEM;
        notice.sourceLabel = "System";
        notice.bypassLimit = true;

        try {
            submit(notice);
        } catch (MailRejectedException e) {
            systemLogger.logMessage("MAIL", "Could not queue the limit notice: " + e.getMessage());
        }
    }

    private static Date startOfToday()
    {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static String describe(Exception e)
    {
        StringBuilder text = new StringBuilder(e.getClass().getSimpleName() + ": " + e.getMessage());
        Throwable cause = e.getCause();

        while (cause != null && text.length() < 1000) {
            text.append(" / ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
            cause = cause.getCause();
        }

        return text.toString();
    }

    private static MailRejectedException invalid(String message)
    {
        return new MailRejectedException(MailRejectedException.Reason.INVALID, message);
    }

    /** What a caller wants to send. Recipients are plain addresses - resolve "user:{id}" before. */
    public static class MailRequest
    {
        public List<String> to = new ArrayList<>();
        public List<String> cc = new ArrayList<>();
        public String subject;
        public String markdownBody;
        public List<Attachment> attachments = new ArrayList<>();
        public MailOutboxEntry.SourceType sourceType = MailOutboxEntry.SourceType.SERVICE;
        public String sourceLabel;
        public Agent agent;
        public Project project;
        /** System notices only - must never be set for agent or service mails. */
        boolean bypassLimit = false;
    }

    public static class Attachment
    {
        public final String fileName;
        public final String contentType;
        public final byte[] data;

        public Attachment(String fileName, String contentType, byte[] data)
        {
            this.fileName = fileName;
            this.contentType = contentType;
            this.data = data;
        }
    }

    public static class MailRejectedException extends RuntimeException
    {
        public enum Reason { DISABLED, INVALID, LIMIT }

        private final Reason reason;

        public MailRejectedException(Reason reason, String message)
        {
            super(message);
            this.reason = reason;
        }

        public Reason getReason() { return reason; }
    }
}
