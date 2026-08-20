import { createFileRoute } from "@tanstack/react-router";
import { Send } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/notifications")({
  head: () => ({
    meta: [
      { title: "Notifications — Learntrix Admin" },
      {
        name: "description",
        content:
          "Send targeted announcements to students, batches, trainers or career-eligible cohorts.",
      },
      { property: "og:title", content: "Notifications — Learntrix Admin" },
      { property: "og:description", content: "Targeted in-app, email and WhatsApp announcements." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminNotifications,
});

const audiences = [
  "All students",
  "Specific batch",
  "Specific course",
  "Low attendance students",
  "Career eligible students",
  "Placed students",
  "All trainers",
];

const recent = [
  {
    id: "n1",
    title: "Placement drive · Wipro",
    audience: "Career eligible students",
    channel: "In-app + WhatsApp",
    sentAt: "2 hours ago",
    reach: 412,
  },
  {
    id: "n2",
    title: "Attendance below 75% — action needed",
    audience: "Low attendance students",
    channel: "Email + WhatsApp",
    sentAt: "Yesterday",
    reach: 86,
  },
  {
    id: "n3",
    title: "New module released · Spring Boot",
    audience: "Full Stack batch",
    channel: "In-app",
    sentAt: "3 days ago",
    reach: 150,
  },
];

function AdminNotifications() {
  const [audience, setAudience] = useState(audiences[0]!);
  const [channel, setChannel] = useState("In-app");

  return (
    <>
      <PageHeader
        title="Notifications"
        description="Compose once, deliver across in-app, email and WhatsApp channels."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Sent this month" value={148} />
        <StatCard label="Average reach" value="1,204" />
        <StatCard label="Open rate" value="63%" />
        <StatCard label="Scheduled" value={4} />
      </section>

      <div className="grid gap-4 lg:grid-cols-[1.2fr_1fr]">
        <Panel title="Compose announcement">
          <form
            className="grid gap-4"
            onSubmit={(e) => {
              e.preventDefault();
              const form = new FormData(e.currentTarget);
              const title = String(form.get("title") ?? "").trim();
              if (!title) {
                toast.error("Title is required.");
                return;
              }
              void adminService
                .sendNotification({ title, audience, channel })
                .then(() => toast.success("Announcement queued for delivery."));
            }}
          >
            <div className="space-y-1.5">
              <Label htmlFor="title">Title</Label>
              <Input id="title" name="title" maxLength={120} required />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="message">Message</Label>
              <Textarea id="message" name="message" rows={6} maxLength={2000} required />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label>Audience</Label>
                <Select value={audience} onValueChange={setAudience}>
                  <SelectTrigger aria-label="Audience">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {audiences.map((a) => (
                      <SelectItem key={a} value={a}>
                        {a}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Channel</Label>
                <Select value={channel} onValueChange={setChannel}>
                  <SelectTrigger aria-label="Channel">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {["In-app", "Email", "WhatsApp", "All channels"].map((c) => (
                      <SelectItem key={c} value={c}>
                        {c}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div>
              <Button type="submit">
                <Send className="size-4" aria-hidden /> Send announcement
              </Button>
            </div>
          </form>
        </Panel>

        <Panel title="Recent announcements">
          <ul className="space-y-3">
            {recent.map((n) => (
              <li key={n.id} className="rounded-lg border border-border p-4">
                <div className="flex items-start justify-between gap-3">
                  <p className="text-sm font-medium">{n.title}</p>
                  <Badge variant="outline">{n.reach}</Badge>
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  {n.audience} · {n.channel} · {n.sentAt}
                </p>
              </li>
            ))}
          </ul>
        </Panel>
      </div>
    </>
  );
}
