package com.learntrix.edtech.service;

/**
 * Outcome of a single outbound email attempt.
 *
 * <p>Deliberately distinguishes NOT_CONFIGURED from FAILED: the first means no SMTP
 * host was ever set (nothing was even attempted), the second means the SMTP server
 * rejected or timed out. Both are surfaced to the admin UI so it can never claim an
 * activation mail was delivered when it was not.</p>
 */
public class MailDispatchResult {

    public enum Status { SENT, FAILED, NOT_CONFIGURED }

    private final Status status;
    private final String detail;
    private final String link;

    private MailDispatchResult(Status status, String detail, String link) {
        this.status = status;
        this.detail = detail;
        this.link = link;
    }

    public static MailDispatchResult sent(String link) {
        return new MailDispatchResult(Status.SENT, null, link);
    }

    public static MailDispatchResult failed(String detail, String link) {
        return new MailDispatchResult(Status.FAILED, detail, link);
    }

    public static MailDispatchResult notConfigured(String link) {
        return new MailDispatchResult(Status.NOT_CONFIGURED,
                "SMTP is not configured. Set MAIL_HOST / MAIL_USERNAME / MAIL_PASSWORD in backend/.env.", link);
    }

    public boolean isSent() {
        return status == Status.SENT;
    }

    public Status getStatus() {
        return status;
    }

    /** Machine-readable status string for API responses: SENT | FAILED | NOT_CONFIGURED. */
    public String getStatusName() {
        return status.name();
    }

    /** Human-readable reason when the send did not succeed; null on success. */
    public String getDetail() {
        return detail;
    }

    /** The activation / reset URL that was (or would have been) mailed. */
    public String getLink() {
        return link;
    }
}
