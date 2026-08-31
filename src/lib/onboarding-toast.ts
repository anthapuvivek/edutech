import { toast } from "sonner";

import type { OnboardingResult } from "@/services/admin.service";

/**
 * Reports the outcome of an onboarding / resend-invite call.
 *
 * The account is created even when the activation email cannot be delivered, so a
 * plain HTTP 200 says nothing about whether the recipient will ever see the link.
 * We key the message off `emailStatus` and, when delivery failed, surface the
 * activation URL and copy it to the clipboard so the admin can pass it on by hand.
 */
export function reportOnboardingOutcome(res: OnboardingResult | undefined, subject: string): void {
  const status = res?.emailStatus;
  const label = res?.identifier ? `${subject} ${res.identifier}` : subject;

  if (status === "SENT") {
    toast.success(`${label} onboarded — activation email sent to ${res?.email ?? "the recipient"}.`);
    return;
  }

  if (!status) {
    // Older backend build that predates delivery reporting: stay non-committal.
    toast.success(`${label} onboarded.`);
    return;
  }

  const reason =
    status === "NOT_CONFIGURED"
      ? "SMTP is not configured on the backend"
      : (res?.emailError ?? "the mail server rejected the message");

  if (res?.activationUrl) {
    void copyToClipboard(res.activationUrl);
  }

  toast.error(`${label} was created, but the activation email was NOT delivered — ${reason}.`, {
    description: res?.activationUrl
      ? `Activation link copied to your clipboard — send it to ${res.email ?? "them"} manually: ${res.activationUrl}`
      : "Fix the mail settings, then use Resend Invite.",
    duration: 15000,
  });
}

/** Reports a resend-invite outcome, which differs only in wording from onboarding. */
export function reportResendOutcome(res: OnboardingResult | undefined, email: string): void {
  const status = res?.emailStatus;

  if (status === "SENT" || !status) {
    toast.success(`Activation email resent to ${email}.`);
    return;
  }

  const reason =
    status === "NOT_CONFIGURED"
      ? "SMTP is not configured on the backend"
      : (res?.emailError ?? "the mail server rejected the message");

  if (res?.activationUrl) {
    void copyToClipboard(res.activationUrl);
  }

  toast.error(`Could not email ${email} — ${reason}.`, {
    description: res?.activationUrl
      ? `A fresh activation link was generated and copied to your clipboard: ${res.activationUrl}`
      : "Fix the mail settings and try again.",
    duration: 15000,
  });
}

async function copyToClipboard(text: string): Promise<void> {
  try {
    await navigator.clipboard?.writeText(text);
  } catch {
    // Clipboard access is best-effort — the link is still shown in the toast body.
  }
}
