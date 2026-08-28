import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { ArrowLeft, Mail, MessageSquare, Phone } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader } from "@/components/portal/StatCard";
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
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { crmService } from "@/services/crm.service";
import { leadStages, type LeadStage } from "@/types/ops";

export const Route = createFileRoute("/admin/crm/$leadId")({
  head: () => ({
    meta: [
      { title: "Lead profile — Learntrix CRM" },
      {
        name: "description",
        content:
          "Full lead history: communication, notes, follow-ups, demos, payments and enrollment status.",
      },
      { property: "og:title", content: "Lead profile — Learntrix CRM" },
      { property: "og:description", content: "Single-view lead record for counsellors." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: LeadProfile,
});

function LeadProfile() {
  const { leadId } = Route.useParams();
  const qc = useQueryClient();
  const lead = useQuery({
    queryKey: ["crm", "lead", leadId],
    queryFn: () => crmService.detail(leadId),
  });
  const staff = useQuery({ queryKey: ["crm", "staff"], queryFn: () => crmService.staff() });
  const [note, setNote] = useState("");
  const [followUp, setFollowUp] = useState({ date: "", time: "", notes: "", nextAction: "Call" });

  if (lead.isPending || !lead.data) return <Skeleton className="h-96 w-full" />;
  const data = lead.data;
  const refresh = () => void qc.invalidateQueries({ queryKey: ["crm"] });

  return (
    <>
      <Button variant="ghost" size="sm" className="mb-3" asChild>
        <Link to="/admin/crm">
          <ArrowLeft className="size-4" /> Back to CRM
        </Link>
      </Button>

      <PageHeader
        title={data.name}
        description={`${data.courseInterest} · ${data.source} · owned by ${data.assignedTo}`}
        action={
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => toast.success("Call logged.")}>
              <Phone className="size-4" /> Call
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => toast.success("WhatsApp message queued.")}
            >
              <MessageSquare className="size-4" /> WhatsApp
            </Button>
            <Button variant="outline" size="sm" onClick={() => toast.success("Email queued.")}>
              <Mail className="size-4" /> Email
            </Button>
          </div>
        }
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Panel title="Communication history">
            <ul className="space-y-3">
              {(data.communications ?? []).length > 0 ? (
                (data.communications ?? []).map((c) => (
                  <li key={c.id} className="rounded-md border border-border p-3">
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                      <span className="font-medium text-foreground">{c.channel}</span>
                      <span>{new Date(c.at).toLocaleString()}</span>
                    </div>
                    <p className="mt-1 text-sm">{c.summary}</p>
                    <p className="mt-1 text-xs text-muted-foreground">by {c.by}</p>
                  </li>
                ))
              ) : (
                <li className="text-xs text-muted-foreground">No communication records.</li>
              )}
            </ul>
          </Panel>

          <Panel title="Notes">
            <ul className="mb-4 space-y-2">
              {(data.notes ?? []).length > 0 ? (
                (data.notes ?? []).map((n) => (
                  <li key={n.id} className="rounded-md bg-muted/50 p-3 text-sm">
                    {n.body}
                    <span className="mt-1 block text-xs text-muted-foreground">
                      {n.by} · {new Date(n.at).toLocaleDateString()}
                    </span>
                  </li>
                ))
              ) : (
                <li className="text-xs text-muted-foreground">No notes added.</li>
              )}
            </ul>
            <Textarea
              rows={3}
              placeholder="Add a note…"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
            <Button
              className="mt-2"
              size="sm"
              onClick={async () => {
                await crmService.addNote(data.id, note);
                setNote("");
                toast.success("Note added.");
              }}
            >
              Add note
            </Button>
          </Panel>

          <Panel title="Follow-ups">
            <ul className="mb-4 space-y-2">
              {(data.followUps ?? []).length > 0 ? (
                (data.followUps ?? []).map((f) => (
                  <li
                    key={f.id}
                    className="flex flex-wrap items-center justify-between gap-2 rounded-md border border-border p-3 text-sm"
                  >
                    <div>
                      <p className="font-medium">
                        {f.date} · {f.time}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {f.notes} · next: {f.nextAction} · {f.assignedTo}
                      </p>
                    </div>
                    <StatusBadge value={f.status} />
                  </li>
                ))
              ) : (
                <li className="text-xs text-muted-foreground">No scheduled follow-ups.</li>
              )}
            </ul>
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <Label className="mb-1.5 block text-xs">Follow-up date</Label>
                <Input
                  type="date"
                  value={followUp.date}
                  onChange={(e) => setFollowUp({ ...followUp, date: e.target.value })}
                />
              </div>
              <div>
                <Label className="mb-1.5 block text-xs">Follow-up time</Label>
                <Input
                  type="time"
                  value={followUp.time}
                  onChange={(e) => setFollowUp({ ...followUp, time: e.target.value })}
                />
              </div>
              <div className="sm:col-span-2">
                <Label className="mb-1.5 block text-xs">Notes</Label>
                <Textarea
                  rows={2}
                  value={followUp.notes}
                  onChange={(e) => setFollowUp({ ...followUp, notes: e.target.value })}
                />
              </div>
            </div>
            <Button
              className="mt-3"
              size="sm"
              onClick={async () => {
                await crmService.scheduleFollowUp(data.id, followUp);
                refresh();
                toast.success("Follow-up scheduled.");
              }}
            >
              Schedule follow-up
            </Button>
          </Panel>
        </div>

        <div className="space-y-6">
          <Panel title="Lead record">
            <dl className="space-y-2 text-sm">
              <Row label="Phone" value={data.phone} />
              <Row label="Email" value={data.email} />
              <Row label="City" value={data.city ?? "—"} />
              <Row label="Course interest" value={data.courseInterest} />
              <Row label="Source" value={data.source} />
              <Row label="Lead score" value={`${data.score}/100`} />
              <Row label="Demo attended" value={data.demoAttended ? "Yes" : "No"} />
              <Row label="Enrollment" value={data.enrollmentStatus} />
            </dl>
          </Panel>

          <Panel title="Ownership & stage">
            <Label className="mb-1.5 block text-xs">Assigned staff</Label>
            <Select
              value={data.assignedToId}
              onValueChange={async (v) => {
                const member = (staff.data ?? []).find((s) => s.id === v);
                if (!member) return;
                await crmService.assign(data.id, member);
                refresh();
                toast.success(`Reassigned to ${member.name}.`);
              }}
            >
              <SelectTrigger aria-label="Assign lead">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {(staff.data ?? []).map((s) => (
                  <SelectItem key={s.id} value={s.id}>
                    {s.name} — {s.role.replace("_", " ")}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Label className="mb-1.5 mt-4 block text-xs">Stage</Label>
            <Select
              value={data.stage}
              onValueChange={async (v) => {
                await crmService.moveStage(data.id, v as LeadStage);
                refresh();
                toast.success(`Stage set to ${v}.`);
              }}
            >
              <SelectTrigger aria-label="Lead stage">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {leadStages.map((s) => (
                  <SelectItem key={s} value={s}>
                    {s}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Panel>

          <Panel title="Enquiry & payment history">
            <ul className="space-y-2 text-sm">
              {(data.enquiryHistory ?? []).map((e) => (
                <li key={e.id} className="rounded-md bg-muted/50 p-2 text-xs">
                  {e.subject} · {new Date(e.at).toLocaleDateString()}
                </li>
              ))}
              {(data.paymentHistory ?? []).length > 0 ? (
                (data.paymentHistory ?? []).map((p) => (
                  <li
                    key={p.id}
                    className="flex items-center justify-between rounded-md border border-border p-2 text-xs"
                  >
                    <span>₹{p.amount.toLocaleString("en-IN")}</span>
                    <StatusBadge value={p.status} />
                  </li>
                ))
              ) : (
                <li className="text-xs text-muted-foreground">No payments recorded.</li>
              )}
            </ul>
          </Panel>
        </div>
      </div>
    </>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}
