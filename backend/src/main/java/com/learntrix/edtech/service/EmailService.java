package com.learntrix.edtech.service;

import com.learntrix.edtech.entity.User;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

    @Value("${spring.mail.from:${spring.mail.username:anthapuvivekananda@gmail.com}}")
    private String fromAddress;

    /**
     * Sends the welcome activation email to a newly onboarded student or teacher.
     * Contains the one-time secure activation token link for them to set their password.
     */
    public boolean sendWelcomeActivationEmail(User user, String roleName, String identifier, String activationToken) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            LOGGER.warn("Cannot send activation email: User or email is null/empty");
            return false;
        }

        String activationUrl = clientUrl + "/reset-password?token=" + activationToken;
        String subject = "Welcome to LearntriX - Activate Your Account";

        String textContent = String.format(
                "Welcome to LearntriX!\n\n" +
                "Your account has been created by the administrator.\n\n" +
                "Account Details:\n" +
                " - Email: %s\n" +
                " - Role: %s\n" +
                " - Identifier: %s\n\n" +
                "Please activate your account and choose your password by visiting this secure link:\n" +
                "%s\n\n" +
                "Note: This activation link expires in 24 hours.\n\n" +
                "— The LearntriX Team",
                user.getEmail(), roleName, identifier != null ? identifier : "N/A", activationUrl
        );

        String htmlContent = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;\">" +
                "<h2 style=\"color: #0f172a;\">Welcome to LearntriX</h2>" +
                "<p style=\"color: #475569;\">Your account has been created by the administrator.</p>" +
                "<div style=\"background: #f8fafc; padding: 16px; border-radius: 6px; margin: 20px 0;\">" +
                "<p style=\"margin: 4px 0;\"><strong>Email:</strong> %s</p>" +
                "<p style=\"margin: 4px 0;\"><strong>Role:</strong> %s</p>" +
                "<p style=\"margin: 4px 0;\"><strong>ID:</strong> %s</p>" +
                "</div>" +
                "<p style=\"color: #475569;\">Please activate your account and choose your password using the button below:</p>" +
                "<div style=\"text-align: center; margin: 28px 0;\">" +
                "<a href=\"%s\" style=\"background: #2563eb; color: #ffffff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: bold; display: inline-block;\">Activate Account</a>" +
                "</div>" +
                "<p style=\"color: #64748b; font-size: 13px;\">If the button does not work, copy and paste this link in your browser:<br>" +
                "<a href=\"%s\" style=\"color: #2563eb;\">%s</a></p>" +
                "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 12px;\">This activation link expires in 24 hours. If you did not expect this invitation, please ignore this email.</p>" +
                "</div>",
                user.getEmail(), roleName, identifier != null ? identifier : "N/A", activationUrl, activationUrl, activationUrl
        );

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
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
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
        }
    }
}
