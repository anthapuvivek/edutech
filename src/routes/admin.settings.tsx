import { createFileRoute } from "@tanstack/react-router";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";

export const Route = createFileRoute("/admin/settings")({
  head: () => ({
    meta: [
      { title: "Platform Settings — Learntrix Admin" },
      { name: "description", content: "Configure organisation identity, contact details, branding, integrations and platform-wide defaults." },
      { property: "og:title", content: "Platform Settings — Learntrix Admin" },
      { property: "og:description", content: "Central configuration for the Learntrix platform." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminSettings,
});

const org = [
  { id: "orgName", label: "Organisation name", value: "Learntrix" },
  { id: "supportEmail", label: "Support email", value: "support@learntrix.com", type: "email" },
  { id: "supportPhone", label: "Support phone", value: "+91 90000 00000" },
  { id: "address", label: "Address", value: "Hyderabad, India" },
  { id: "timezone", label: "Timezone", value: "Asia/Kolkata" },
  { id: "currency", label: "Currency", value: "INR" },
];

const toggles = [
  { id: "registration", label: "Public student registration", hint: "Allow students to self-register from the website.", on: true },
  { id: "autoEligibility", label: "Automatic career eligibility", hint: "Re-evaluate eligibility nightly using the active rule sets.", on: true },
  { id: "attendanceAlerts", label: "Attendance alerts", hint: "Notify students and trainers when attendance drops below threshold.", on: true },
  { id: "teacherArticles", label: "Teacher article submissions", hint: "Teachers can submit articles for admin review.", on: true },
  { id: "maintenance", label: "Maintenance mode", hint: "Show a maintenance notice on the public website.", on: false },
];

function AdminSettings() {
  return (
    <>
      <PageHeader title="Platform Settings" description="Organisation-level configuration. Changes are recorded in the audit log." />

      <div className="grid gap-4 lg:grid-cols-2">
        <Panel title="Organisation">
          <form
            className="grid gap-4 sm:grid-cols-2"
            onSubmit={(e) => {
              e.preventDefault();
              toast.success("Organisation settings saved.");
            }}
          >
            {org.map((f) => (
              <div key={f.id} className="space-y-1.5">
                <Label htmlFor={f.id}>{f.label}</Label>
                <Input id={f.id} name={f.id} defaultValue={f.value} type={f.type ?? "text"} maxLength={160} />
              </div>
            ))}
            <div className="space-y-1.5 sm:col-span-2">
              <Label htmlFor="tagline">Tagline</Label>
              <Textarea id="tagline" name="tagline" rows={2} maxLength={300} defaultValue="Enterprise learning and placement platform." />
            </div>
            <div className="sm:col-span-2">
              <Button type="submit">Save settings</Button>
            </div>
          </form>
        </Panel>

        <Panel title="Platform toggles" description="Feature switches applied across the platform.">
          <ul className="space-y-3">
            {toggles.map((t) => (
              <li key={t.id} className="flex items-start justify-between gap-4 rounded-lg border border-border p-4">
                <div>
                  <Label htmlFor={t.id} className="text-sm">
                    {t.label}
                  </Label>
                  <p className="mt-1 text-xs text-muted-foreground">{t.hint}</p>
                </div>
                <Switch id={t.id} defaultChecked={t.on} onCheckedChange={() => toast.success(`${t.label} updated.`)} />
              </li>
            ))}
          </ul>
        </Panel>
      </div>
    </>
  );
}
