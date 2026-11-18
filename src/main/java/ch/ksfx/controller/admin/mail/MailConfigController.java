package ch.ksfx.controller.admin.mail;

import ch.ksfx.dao.MailDAO;
import ch.ksfx.model.mail.MailConfig;
import ch.ksfx.model.user.User;
import ch.ksfx.services.mail.MailService;
import ch.ksfx.services.project.UserScope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin -> Mail (concept "Mail service"): the platform's SMTP settings, the daily emergency brake,
 * a test mail, and the mail log of the whole platform. /admin/** is ADMIN-only (WebSecurityConfig).
 * The password is never sent back to the browser: an empty field on save keeps the stored one.
 */
@Controller
@RequestMapping("/admin/mail")
public class MailConfigController
{
    private static final int LOG_SIZE = 100;

    private final MailDAO mailDAO;
    private final MailService mailService;
    private final UserScope userScope;

    public MailConfigController(MailDAO mailDAO, MailService mailService, UserScope userScope)
    {
        this.mailDAO = mailDAO;
        this.mailService = mailService;
        this.userScope = userScope;
    }

    @GetMapping("/")
    public String index(Model model)
    {
        MailConfig config = mailDAO.getMailConfig();
        User user = userScope.currentUser();

        model.addAttribute("mailConfig", config != null ? config : new MailConfig());
        model.addAttribute("passwordSet", config != null && config.getSmtpPassword() != null && !config.getSmtpPassword().isEmpty());
        model.addAttribute("testAddress", user != null ? user.getEmail() : null);
        model.addAttribute("mails", mailDAO.getRecentEntries(null, LOG_SIZE));

        return "admin/mail/mail_config";
    }

    @PostMapping("/")
    public String save(@RequestParam(value = "enabled", defaultValue = "false") boolean enabled,
                       @RequestParam("smtpHost") String smtpHost,
                       @RequestParam("smtpPort") int smtpPort,
                       @RequestParam("smtpSecurity") String smtpSecurity,
                       @RequestParam(value = "smtpUsername", required = false) String smtpUsername,
                       @RequestParam(value = "smtpPassword", required = false) String smtpPassword,
                       @RequestParam(value = "clearPassword", defaultValue = "false") boolean clearPassword,
                       @RequestParam("fromAddress") String fromAddress,
                       @RequestParam(value = "fromName", required = false) String fromName,
                       @RequestParam(value = "replyTo", required = false) String replyTo,
                       @RequestParam("dailyLimit") int dailyLimit,
                       @RequestParam(value = "action", defaultValue = "save") String action,
                       @RequestParam(value = "testAddress", required = false) String testAddress,
                       RedirectAttributes redirectAttributes)
    {
        MailConfig config = mailDAO.getMailConfig();

        if (config == null) {
            config = new MailConfig();
        }

        config.setEnabled(enabled);
        config.setSmtpHost(trimToNull(smtpHost));
        config.setSmtpPort(smtpPort);
        config.setSmtpSecurity(smtpSecurity);
        config.setSmtpUsername(trimToNull(smtpUsername));

        if (clearPassword) {
            config.setSmtpPassword(null);
        } else if (smtpPassword != null && !smtpPassword.isEmpty()) {
            config.setSmtpPassword(smtpPassword);
        }

        config.setFromAddress(trimToNull(fromAddress));
        config.setFromName(trimToNull(fromName));
        config.setReplyTo(trimToNull(replyTo));
        config.setDailyLimit(Math.max(0, dailyLimit));

        mailDAO.saveOrUpdateMailConfig(config);

        if ("test".equals(action)) {
            if (trimToNull(testAddress) == null || config.getSmtpHost() == null || config.getFromAddress() == null) {
                redirectAttributes.addFlashAttribute("mailError", "For a test mail, SMTP host, from address and a recipient are needed.");
            } else {
                String error = mailService.sendTestMail(config, testAddress.trim());

                if (error == null) {
                    redirectAttributes.addFlashAttribute("message", "Settings saved - test mail sent to " + testAddress.trim() + ".");
                } else {
                    redirectAttributes.addFlashAttribute("mailError", "Settings saved, but the test mail failed: " + error);
                }
            }
        } else {
            redirectAttributes.addFlashAttribute("message", "Mail settings saved.");
        }

        return "redirect:/admin/mail/";
    }

    private static String trimToNull(String value)
    {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
