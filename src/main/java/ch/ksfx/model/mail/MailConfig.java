package ch.ksfx.model.mail;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

/**
 * The platform's one SMTP configuration (concept "Mail service"), set by the platform admin on
 * Admin -> Mail. Single row. {@link #dailyLimit} is the emergency brake across the whole platform.
 */
@Entity
@Table(name = "mail_config")
public class MailConfig
{
    public static final String SECURITY_NONE = "NONE";
    public static final String SECURITY_STARTTLS = "STARTTLS";
    public static final String SECURITY_SSL = "SSL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enabled")
    private boolean enabled = false;

    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private int smtpPort = 587;

    @Column(name = "smtp_security")
    private String smtpSecurity = SECURITY_STARTTLS;

    @Column(name = "smtp_username")
    private String smtpUsername;

    @Column(name = "smtp_password")
    private String smtpPassword;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "from_name")
    private String fromName;

    @Column(name = "reply_to")
    private String replyTo;

    @Column(name = "daily_limit")
    private int dailyLimit = 200;

    /** The day the admins were last told that the daily limit was reached - once per day is enough. */
    @Column(name = "limit_notice_sent_on")
    private Date limitNoticeSentOn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }
    public String getSmtpSecurity() { return smtpSecurity; }
    public void setSmtpSecurity(String smtpSecurity) { this.smtpSecurity = smtpSecurity; }
    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }
    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public String getReplyTo() { return replyTo; }
    public void setReplyTo(String replyTo) { this.replyTo = replyTo; }
    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
    public Date getLimitNoticeSentOn() { return limitNoticeSentOn; }
    public void setLimitNoticeSentOn(Date limitNoticeSentOn) { this.limitNoticeSentOn = limitNoticeSentOn; }

    /** Enough set to actually send: switched on, with a host and a sender address. */
    @Transient
    public boolean isUsable()
    {
        return enabled && smtpHost != null && !smtpHost.trim().isEmpty() && fromAddress != null && !fromAddress.trim().isEmpty();
    }
}
