package com.learntrix.edtech.service;

import com.learntrix.edtech.entity.User;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.client-url:http://localhost:8080}")
    private String clientUrl;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

<<<<<<< HEAD
    @Value("${spring.mail.from:${spring.mail.username:anthapuvivekananda@gmail.com}}")
    private String fromAddress;
=======
    @Value("${app.mail.from:}")
    private String configuredFrom;

    @Value("${app.mail.from-name:LearntriX Platform}")
    private String fromName;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuthRequired;

    /**
     * True only when outbound mail can realistically be sent. Spring Boot still builds a
     * JavaMailSender bean when the host property is present but blank, so the bean's
     * existence proves nothing; and a host with SMTP auth enabled but no credentials is
     * guaranteed to be rejected, so that counts as unconfigured too.
     */
    public boolean isConfigured() {
        if (mailSender == null || isBlank(mailHost)) return false;
        return !smtpAuthRequired || !isBlank(mailUsername);
    }

    /** Diagnostic snapshot for the admin mail-status endpoint. Never exposes the password. */
    public Map<String, Object> describeConfiguration() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("configured", isConfigured());
        out.put("host", isBlank(mailHost) ? null : mailHost);
        out.put("username", isBlank(mailUsername) ? null : mailUsername);
        out.put("from", resolveFrom());
        out.put("fromName", fromName);
        out.put("clientUrl", clientUrl);
        if (mailSender instanceof JavaMailSenderImpl impl) {
            out.put("port", impl.getPort());
        }
        if (!isConfigured()) {
            out.put("hint", isBlank(mailHost)
                    ? "MAIL_HOST is empty. Set MAIL_HOST, MAIL_PORT, MAIL_USERNAME and MAIL_PASSWORD in "
                            + "backend/.env, then restart with backend/run.ps1 so the values reach Spring."
                    : "MAIL_USERNAME/MAIL_PASSWORD are empty while SMTP auth is on, so " + mailHost
                            + " will reject every message. Fill them in backend/.env (Gmail needs a 16-character "
                            + "App Password) and restart with backend/run.ps1.");
        }
        return out;
    }

    public String buildActivationUrl(String activationToken) {
        return clientUrl + "/reset-password?token=" + activationToken;
    }
>>>>>>> b72e728 (application updated)

    /**
     * Sends the welcome activation email to a newly onboarded student or teacher.
     * Contains the one-time secure activation token link for them to set their password.
     */
<<<<<<< HEAD
    public boolean sendWelcomeActivationEmail(User user, String roleName, String identifier, String activationToken) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            LOGGER.warn("Cannot send activation email: User or email is null/empty");
            return false;
        }

        String activationUrl = clientUrl + "/reset-password?token=" + activationToken;
=======
    public MailDispatchResult sendWelcomeActivationEmail(User user, String roleName, String identifier,
                                                         String activationToken) {
        String activationUrl = buildActivationUrl(activationToken);
>>>>>>> b72e728 (application updated)
        String subject = "Welcome to LearntriX - Activate Your Account";
        String id = identifier != null ? identifier : "N/A";

        String textContent = String.format(
                "Welcome to LearntriX!%n%n"
                        + "Your account has been created by the administrator.%n%n"
                        + "Account Details:%n"
                        + " - Email: %s%n"
                        + " - Role: %s%n"
                        + " - Identifier: %s%n%n"
                        + "Please activate your account and choose your password by visiting this secure link:%n"
                        + "%s%n%n"
                        + "Note: This activation link expires in 24 hours.%n%n"
                        + "- The LearntriX Team",
                user.getEmail(), roleName, id, activationUrl);

        String htmlContent = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;\">"
                        + "<h2 style=\"color: #0f172a;\">Welcome to LearntriX</h2>"
                        + "<p style=\"color: #475569;\">Your account has been created by the administrator.</p>"
                        + "<div style=\"background: #f8fafc; padding: 16px; border-radius: 6px; margin: 20px 0;\">"
                        + "<p style=\"margin: 4px 0;\"><strong>Email:</strong> %s</p>"
                        + "<p style=\"margin: 4px 0;\"><strong>Role:</strong> %s</p>"
                        + "<p style=\"margin: 4px 0;\"><strong>ID:</strong> %s</p>"
                        + "</div>"
                        + "<p style=\"color: #475569;\">Please activate your account and choose your password using the button below:</p>"
                        + "<div style=\"text-align: center; margin: 28px 0;\">"
                        + "<a href=\"%s\" style=\"background: #2563eb; color: #ffffff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: bold; display: inline-block;\">Activate Account</a>"
                        + "</div>"
                        + "<p style=\"color: #64748b; font-size: 13px;\">If the button does not work, copy and paste this link in your browser:<br>"
                        + "<a href=\"%s\" style=\"color: #2563eb;\">%s</a></p>"
                        + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;\" />"
                        + "<p style=\"color: #94a3b8; font-size: 12px;\">This activation link expires in 24 hours. If you did not expect this invitation, please ignore this email.</p>"
                        + "</div>",
                user.getEmail(), roleName, id, activationUrl, activationUrl, activationUrl);

<<<<<<< HEAD
        LOGGER.info("Attempting to send activation email to {}", user.getEmail());

        if (mailSender == null || mailHost == null || mailHost.trim().isEmpty()) {
            LOGGER.warn("JavaMailSender / SMTP host is not configured (MAIL_HOST unset). Failed to send activation email to {}", user.getEmail());
            return false;
        }

        String sender = (fromAddress != null && !fromAddress.trim().isEmpty()) ? fromAddress.trim() : mailUsername;
        if (sender == null || sender.trim().isEmpty()) {
            sender = "anthapuvivekananda@gmail.com";
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sender, "LearntriX Platform");
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);
            mailSender.send(message);
            LOGGER.info("Activation email sent successfully to {} from {}", user.getEmail(), sender);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to send activation email to {} from {}. Error: {}", user.getEmail(), sender, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sends the password reset email to a user who requested a password reset.
     * Contains the secure reset token link.
     */
    public boolean sendPasswordResetEmail(User user, String resetToken) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            LOGGER.warn("Cannot send password reset email: User or email is null/empty");
            return false;
        }

        String resetUrl = clientUrl + "/reset-password?token=" + resetToken;
        String subject = "LearntriX - Password Reset Request";

        String textContent = String.format(
                "Hello %s,\n\n" +
                "We received a request to reset your password for your LearntriX account.\n\n" +
                "Please reset your password by visiting this link:\n" +
                "%s\n\n" +
                "Note: This password reset link expires in 1 hour. If you did not request this, please ignore this email.\n\n" +
                "— The LearntriX Team",
                user.getName() != null ? user.getName() : "LearntriX User", resetUrl
        );

        String htmlContent = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;\">" +
                "<h2 style=\"color: #0f172a;\">Reset Your LearntriX Password</h2>" +
                "<p style=\"color: #475569;\">Hello %s,</p>" +
                "<p style=\"color: #475569;\">We received a request to reset your password. Click the button below to choose a new password:</p>" +
                "<div style=\"text-align: center; margin: 28px 0;\">" +
                "<a href=\"%s\" style=\"background: #2563eb; color: #ffffff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: bold; display: inline-block;\">Reset Password</a>" +
                "</div>" +
                "<p style=\"color: #64748b; font-size: 13px;\">If the button does not work, copy and paste this link in your browser:<br>" +
                "<a href=\"%s\" style=\"color: #2563eb;\">%s</a></p>" +
                "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 12px;\">This password reset link expires in 1 hour. If you did not make this request, you can safely ignore this email.</p>" +
                "</div>",
                user.getName() != null ? user.getName() : "LearntriX User", resetUrl, resetUrl, resetUrl
        );

        LOGGER.info("Attempting to send password reset email to {}", user.getEmail());

        if (mailSender == null || mailHost == null || mailHost.trim().isEmpty()) {
            LOGGER.warn("JavaMailSender / SMTP host is not configured (MAIL_HOST unset). Failed to send password reset email to {}", user.getEmail());
            return false;
        }

        String sender = (fromAddress != null && !fromAddress.trim().isEmpty()) ? fromAddress.trim() : mailUsername;
        if (sender == null || sender.trim().isEmpty()) {
            sender = "anthapuvivekananda@gmail.com";
=======
        return dispatch(user.getEmail(), subject, textContent, htmlContent, activationUrl, "activation");
    }

    /**
     * Sends the self-service password reset email triggered from the forgot-password screen.
     */
    public MailDispatchResult sendPasswordResetEmail(User user, String resetToken, long validForMinutes) {
        String resetUrl = clientUrl + "/reset-password?token=" + resetToken;
        String subject = "LearntriX - Reset Your Password";

        String textContent = String.format(
                "We received a request to reset the password for %s.%n%n"
                        + "Choose a new password using this secure link:%n%s%n%n"
                        + "This link expires in %d minutes. If you did not request a reset you can ignore this email.%n%n"
                        + "- The LearntriX Team",
                user.getEmail(), resetUrl, validForMinutes);

        String htmlContent = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;\">"
                        + "<h2 style=\"color: #0f172a;\">Reset your password</h2>"
                        + "<p style=\"color: #475569;\">We received a request to reset the password for <strong>%s</strong>.</p>"
                        + "<div style=\"text-align: center; margin: 28px 0;\">"
                        + "<a href=\"%s\" style=\"background: #2563eb; color: #ffffff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: bold; display: inline-block;\">Choose a new password</a>"
                        + "</div>"
                        + "<p style=\"color: #64748b; font-size: 13px;\">If the button does not work, copy and paste this link:<br>"
                        + "<a href=\"%s\" style=\"color: #2563eb;\">%s</a></p>"
                        + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;\" />"
                        + "<p style=\"color: #94a3b8; font-size: 12px;\">This link expires in %d minutes. If you did not request a reset, ignore this email.</p>"
                        + "</div>",
                user.getEmail(), resetUrl, resetUrl, resetUrl, validForMinutes);

        return dispatch(user.getEmail(), subject, textContent, htmlContent, resetUrl, "password reset");
    }

    /** Sends a plain diagnostic email so an admin can prove SMTP works end to end. */
    public MailDispatchResult sendTestEmail(String to) {
        String subject = "LearntriX SMTP test";
        String text = "This is a test message from your LearntriX backend. If you are reading it, outbound "
                + "email is working and activation links will reach their recipients.";
        String html = "<div style=\"font-family: Arial, sans-serif; padding: 16px;\">"
                + "<h3 style=\"color:#0f172a;\">LearntriX SMTP test</h3>"
                + "<p style=\"color:#475569;\">" + text + "</p></div>";
        return dispatch(to, subject, text, html, null, "test");
    }

    /**
     * Single funnel for every outbound message. Returns an honest result — callers must not
     * report success unless the SMTP server actually accepted the message.
     */
    private MailDispatchResult dispatch(String to, String subject, String text, String html,
                                        String link, String kind) {
        if (link != null) {
            LOGGER.info("--------------------------------------------------------------------------------");
            LOGGER.info("{} link for {} -> {}", kind, to, link);
            LOGGER.info("--------------------------------------------------------------------------------");
        }

        if (!isConfigured()) {
            LOGGER.error("SMTP is not configured (spring.mail.host is empty). The {} email to {} was NOT "
                    + "delivered. Set MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD in backend/.env.", kind, to);
            return MailDispatchResult.notConfigured(link);
>>>>>>> b72e728 (application updated)
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
<<<<<<< HEAD
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(sender, "LearntriX Platform");
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);
            mailSender.send(message);
            LOGGER.info("Password reset email sent successfully to {} from {}", user.getEmail(), sender);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to send password reset email to {} from {}. Error: {}", user.getEmail(), sender, e.getMessage(), e);
            return false;
=======
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(resolveFrom(), fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
            LOGGER.info("{} email delivered to {} via {}", kind, to, mailHost);
            return MailDispatchResult.sent(link);
        } catch (Exception e) {
            String reason = rootMessage(e);
            LOGGER.error("SMTP dispatch of the {} email to {} FAILED: {}", kind, to, reason, e);
            return MailDispatchResult.failed(reason, link);
>>>>>>> b72e728 (application updated)
        }
    }

    private String resolveFrom() {
        if (!isBlank(configuredFrom)) return configuredFrom;
        if (!isBlank(mailUsername)) return mailUsername;
        return "no-reply@learntrix.com";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return isBlank(msg) ? cur.getClass().getSimpleName() : msg;
    }
}
