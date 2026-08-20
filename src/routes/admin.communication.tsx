import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { adminService } from "@/services/admin.service";
import type { WhatsAppSettings } from "@/types/admin";

export const Route = createFileRoute("/admin/communication")({
  head: () => ({
    meta: [
      { title: "WhatsApp & Communication — Learntrix Admin" },
      {
        name: "description",
        content:
          "Configure the WhatsApp business number, floating button and pre-filled message templates used across the platform.",
      },
      { property: "og:title", content: "WhatsApp & Communication — Learntrix Admin" },
      { property: "og:description", content: "Central WhatsApp and communication configuration." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminCommunication,
});

const templateFields: Array<{ key: keyof WhatsAppSettings; label: string; hint: string }> = [
  {
    key: "defaultMessage",
    label: "Default message",
    hint: "Used by the floating button on the public website.",
  },
  {
    key: "courseEnquiryMessage",
    label: "Course enquiry",
    hint: "Course detail pages. Supports {course} placeholder.",
  },
  { key: "studentSupportMessage", label: "Student support", hint: "Student portal help CTA." },
  { key: "careerSupportMessage", label: "Career support", hint: "Career dashboard CTA." },
  {
    key: "jobEnquiryMessage",
    label: "Job enquiry",
    hint: "Job listings. Supports {job} placeholder.",
  },
];

function AdminCommunication() {
  const settings = useQuery({
    queryKey: ["admin", "whatsapp"],
    queryFn: () => adminService.whatsappSettings(),
  });
  const [draft, setDraft] = useState<WhatsAppSettings | null>(null);

  useEffect(() => {
    if (settings.data) setDraft(settings.data);
  }, [settings.data]);

  if (!draft) return <Skeleton className="h-[60vh] w-full" />;

  return (
    <>
      <PageHeader
        title="WhatsApp & Communication"
        description="One number, one set of templates — reused by every WhatsApp CTA on the platform."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Business number" value={draft.businessNumber} />
        <StatCard label="Floating button" value={draft.enabled ? "Enabled" : "Disabled"} />
        <StatCard label="Templates" value={templateFields.length} />
        <StatCard label="Chats this month" value="1,842" />
      </section>

      <form
        className="grid gap-4 lg:grid-cols-2"
        onSubmit={(e) => {
          e.preventDefault();
          const digits = draft.businessNumber.replace(/[^\d+]/g, "");
          if (digits.length < 10) {
            toast.error("Enter a valid WhatsApp business number with country code.");
            return;
          }
          void adminService
            .saveWhatsappSettings(draft)
            .then(() => toast.success("WhatsApp settings saved."));
        }}
      >
        <Panel title="Business account">
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="businessNumber">WhatsApp business number</Label>
              <Input
                id="businessNumber"
                value={draft.businessNumber}
                maxLength={20}
                onChange={(e) => setDraft({ ...draft, businessNumber: e.target.value })}
              />
              <p className="text-xs text-muted-foreground">
                Include the country code, e.g. +91 90000 00000.
              </p>
            </div>
            <div className="flex items-start justify-between gap-4 rounded-lg border border-border p-4">
              <div>
                <Label htmlFor="enabled" className="text-sm">
                  Floating WhatsApp button
                </Label>
                <p className="mt-1 text-xs text-muted-foreground">
                  Shows on every public page and inside the student portal.
                </p>
              </div>
              <Switch
                id="enabled"
                checked={draft.enabled}
                onCheckedChange={(v) => setDraft({ ...draft, enabled: v })}
              />
            </div>
            <Button type="submit">Save communication settings</Button>
          </div>
        </Panel>

        <Panel
          title="Message templates"
          description="Pre-filled text used when a user opens a WhatsApp chat."
        >
          <div className="space-y-4">
            {templateFields.map((f) => (
              <div key={String(f.key)} className="space-y-1.5">
                <Label htmlFor={String(f.key)}>{f.label}</Label>
                <Textarea
                  id={String(f.key)}
                  rows={2}
                  maxLength={400}
                  value={String(draft[f.key] ?? "")}
                  onChange={(e) => setDraft({ ...draft, [f.key]: e.target.value })}
                />
                <p className="text-xs text-muted-foreground">{f.hint}</p>
              </div>
            ))}
          </div>
        </Panel>
      </form>
    </>
  );
}
