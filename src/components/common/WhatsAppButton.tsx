import { useQuery } from "@tanstack/react-query";
import { MessageCircle } from "lucide-react";

import { Button } from "@/components/ui/button";
import { adminService } from "@/services/admin.service";
import type { WhatsAppSettings } from "@/types/admin";

export type WhatsAppContext = "default" | "course" | "student" | "career" | "job";

function messageFor(settings: WhatsAppSettings, context: WhatsAppContext, subject?: string) {
  const raw =
    context === "course"
      ? settings.courseEnquiryMessage
      : context === "student"
        ? settings.studentSupportMessage
        : context === "career"
          ? settings.careerSupportMessage
          : context === "job"
            ? settings.jobEnquiryMessage
            : settings.defaultMessage;
  return raw
    .replace("{course}", subject ?? "your programs")
    .replace("{job}", subject ?? "this role");
}

export function useWhatsAppLink(context: WhatsAppContext = "default", subject?: string) {
  const settings = useQuery({
    queryKey: ["admin", "whatsapp"],
    queryFn: () => adminService.whatsappSettings(),
    staleTime: 5 * 60_000,
  });
  if (!settings.data?.enabled) return null;
  const digits = settings.data.businessNumber.replace(/\D/g, "");
  return `https://wa.me/${digits}?text=${encodeURIComponent(messageFor(settings.data, context, subject))}`;
}

/** Inline CTA — use inside pages (course detail, contact, career, job cards). */
export function WhatsAppCta({
  context = "default",
  subject,
  label = "Chat on WhatsApp",
  className,
}: {
  context?: WhatsAppContext;
  subject?: string;
  label?: string;
  className?: string;
}) {
  const href = useWhatsAppLink(context, subject);
  if (!href) return null;
  return (
    <Button variant="outline" asChild className={className}>
      <a href={href} target="_blank" rel="noreferrer noopener">
        <MessageCircle className="size-4" aria-hidden /> {label}
      </a>
    </Button>
  );
}

/** Floating, unobtrusive site-wide WhatsApp entry point. */
export function WhatsAppFloatingButton({ context = "default" }: { context?: WhatsAppContext }) {
  const href = useWhatsAppLink(context);
  if (!href) return null;
  return (
    <a
      href={href}
      target="_blank"
      rel="noreferrer noopener"
      aria-label="Chat with us on WhatsApp"
      className="fixed bottom-5 right-5 z-50 inline-flex items-center gap-2 rounded-full border border-border bg-background/95 px-4 py-3 text-sm font-medium shadow-lg backdrop-blur transition-transform hover:-translate-y-0.5 hover:bg-muted"
    >
      <MessageCircle className="size-4 text-accent" aria-hidden />
      <span className="hidden sm:inline">Chat with us on WhatsApp</span>
    </a>
  );
}
