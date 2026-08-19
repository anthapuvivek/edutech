import { useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Lock, Send } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { supportService } from "@/services/support.service";
import type { TicketStatus } from "@/types/ops";

export const Route = createFileRoute("/admin/support")({
  head: () => ({
    meta: [
      { title: "Support Desk — Learntrix Admin" },
      { name: "description", content: "Triage, assign and resolve student support tickets with internal staff notes and escalation." },
      { property: "og:title", content: "Support Desk — Learntrix Admin" },
      { property: "og:description", content: "Ticket triage, assignment, escalation and resolution tracking." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminSupport,
});

const statuses: TicketStatus[] = ["Open", "Assigned", "In Progress", "Waiting for Student", "Resolved", "Closed"];
const priorities = ["Low", "Medium", "High", "Urgent"];
const categories = ["Course", "Technical Issue", "Payment", "Account", "Certificate", "Class", "Attendance", "Assignment", "Coding", "Career", "Job", "Resume", "Other"];

function AdminSupport() {
  const qc = useQueryClient();
  const [filters, setFilters] = useState({ status: "all", priority: "all", category: "all", search: "" });
  const [activeId, setActiveId] = useState<string | null>(null);

  const overview = useQuery({ queryKey: ["support", "overview"], queryFn: () => supportService.overview() });
  const agents = useQuery({ queryKey: ["support", "agents"], queryFn: () => supportService.agents() });
  const tickets = useQuery({ queryKey: ["support", "tickets", filters], queryFn: () => supportService.list(filters) });

  const refresh = () => void qc.invalidateQueries({ queryKey: ["support"] });

  return (
    <>
      <PageHeader title="Support Desk" description="Ticket permissions, visibility and escalation paths are enforced by the backend." />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <StatCard label="Open tickets" value={overview.data?.openTickets ?? "—"} />
        <StatCard label="Urgent" value={overview.data?.urgentTickets ?? "—"} />
        <StatCard label="Unassigned" value={overview.data?.unassignedTickets ?? "—"} />
        <StatCard label="My tickets" value={overview.data?.myTickets ?? "—"} />
        <StatCard label="Resolved today" value={overview.data?.resolvedToday ?? "—"} />
        <StatCard label="Avg. resolution" value={overview.data ? `${overview.data.averageResolutionHours}h` : "—"} />
      </section>

      <Panel
        title="Tickets"
        action={
          <div className="flex flex-wrap gap-2">
            <Input className="w-44" placeholder="Search ref or student" value={filters.search} onChange={(e) => setFilters({ ...filters, search: e.target.value })} />
            <FilterSelect label="status" value={filters.status} options={statuses} onChange={(v) => setFilters({ ...filters, status: v })} />
            <FilterSelect label="priority" value={filters.priority} options={priorities} onChange={(v) => setFilters({ ...filters, priority: v })} />
            <FilterSelect label="category" value={filters.category} options={categories} onChange={(v) => setFilters({ ...filters, category: v })} />
          </div>
        }
      >
        {tickets.isPending ? (
          <Skeleton className="h-80 w-full" />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Ticket</TableHead>
                  <TableHead>Student</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Priority</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Assigned</TableHead>
                  <TableHead>Updated</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(tickets.data ?? []).map((t) => (
                  <TableRow key={t.id}>
                    <TableCell>
                      <div className="font-medium">{t.subject}</div>
                      <div className="text-xs text-muted-foreground">{t.reference}</div>
                    </TableCell>
                    <TableCell className="text-sm">{t.studentName}</TableCell>
                    <TableCell className="text-xs">{t.category}</TableCell>
                    <TableCell>
                      <StatusBadge value={t.priority} />
                    </TableCell>
                    <TableCell>
                      <Select value={t.status} onValueChange={async (v) => { await supportService.updateStatus(t.id, v as TicketStatus); refresh(); toast.success("Status updated."); }}>
                        <SelectTrigger className="w-44 text-xs" aria-label={`Status of ${t.reference}`}>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {statuses.map((s) => (
                            <SelectItem key={s} value={s}>
                              {s}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </TableCell>
                    <TableCell>
                      <Select
                        value={(agents.data ?? []).find((a) => a.name === t.assignedTo)?.id ?? ""}
                        onValueChange={async (v) => {
                          const agent = (agents.data ?? []).find((a) => a.id === v);
                          if (!agent) return;
                          await supportService.assign(t.id, agent);
                          refresh();
                          toast.success(`Assigned to ${agent.name}.`);
                        }}
                      >
                        <SelectTrigger className="w-40 text-xs" aria-label={`Assign ${t.reference}`}>
                          <SelectValue placeholder="Unassigned" />
                        </SelectTrigger>
                        <SelectContent>
                          {(agents.data ?? []).map((a) => (
                            <SelectItem key={a.id} value={a.id}>
                              {a.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </TableCell>
                    <TableCell className="text-xs">{new Date(t.updatedAt).toLocaleDateString()}</TableCell>
                    <TableCell className="text-right">
                      <Button size="sm" variant="outline" onClick={() => setActiveId(t.id)}>
                        Open
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Panel>

      <Dialog open={Boolean(activeId)} onOpenChange={(o) => !o && setActiveId(null)}>
        <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
          {activeId ? <TicketThread id={activeId} onChanged={refresh} /> : null}
        </DialogContent>
      </Dialog>
    </>
  );
}

function FilterSelect({ label, value, options, onChange }: { label: string; value: string; options: string[]; onChange: (v: string) => void }) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-40" aria-label={`Filter by ${label}`}>
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="all">All {label}</SelectItem>
        {options.map((o) => (
          <SelectItem key={o} value={o}>
            {o}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

function TicketThread({ id, onChanged }: { id: string; onChanged: () => void }) {
  const ticket = useQuery({ queryKey: ["support", "ticket", id, "staff"], queryFn: () => supportService.detail(id, "staff") });
  const [body, setBody] = useState("");
  const [internal, setInternal] = useState(false);

  if (ticket.isPending || !ticket.data) return <Skeleton className="h-64 w-full" />;
  const t = ticket.data;

  return (
    <>
      <DialogHeader>
        <DialogTitle>
          {t.subject} <span className="ml-2 text-xs font-normal text-muted-foreground">{t.reference}</span>
        </DialogTitle>
      </DialogHeader>

      <div className="flex flex-wrap gap-2 text-xs">
        <StatusBadge value={t.status} />
        <StatusBadge value={t.priority} />
        <span className="text-muted-foreground">
          {t.category} · {t.studentName} · created {new Date(t.createdAt).toLocaleDateString()}
        </span>
      </div>

      <ul className="my-4 space-y-3">
        {t.messages.map((m) => (
          <li
            key={m.id}
            className={
              m.internal
                ? "rounded-md border border-warning/40 bg-warning/10 p-3"
                : m.authorRole === "student"
                  ? "rounded-md bg-muted/60 p-3"
                  : "rounded-md border border-border p-3"
            }
          >
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span className="flex items-center gap-1 font-medium text-foreground">
                {m.internal ? <Lock className="size-3" /> : null}
                {m.author} {m.internal ? "· internal note (staff only)" : m.authorRole === "student" ? "· student" : "· staff reply"}
              </span>
              <span>{new Date(m.at).toLocaleString()}</span>
            </div>
            <p className="mt-1 text-sm">{m.body}</p>
            {m.attachments.map((a) => (
              <p key={a.id} className="mt-1 text-xs text-muted-foreground">
                📎 {a.name} ({a.sizeKb} KB)
              </p>
            ))}
          </li>
        ))}
      </ul>

      <Textarea rows={3} placeholder={internal ? "Internal note — not visible to the student" : "Reply to the student"} value={body} onChange={(e) => setBody(e.target.value)} />
      <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
        <label className="flex items-center gap-2 text-xs">
          <Switch checked={internal} onCheckedChange={setInternal} aria-label="Internal note" />
          Internal note (staff only)
        </label>
        <div className="flex gap-2">
          <Select
            onValueChange={async (v) => {
              await supportService.escalate(t.id, v);
              onChanged();
              toast.success(`Escalated to ${v}.`);
            }}
          >
            <SelectTrigger className="w-40 text-xs" aria-label="Escalate ticket">
              <SelectValue placeholder="Escalate to" />
            </SelectTrigger>
            <SelectContent>
              {["Mentor", "Teacher", "Placement Officer", "Finance Staff", "Admin"].map((r) => (
                <SelectItem key={r} value={r}>
                  {r}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button
            size="sm"
            onClick={async () => {
              await supportService.reply(t.id, body, { internal, author: "Support Desk", authorRole: "staff" });
              setBody("");
              onChanged();
              toast.success(internal ? "Internal note added." : "Reply sent to the student.");
            }}
          >
            <Send className="size-4" /> {internal ? "Add note" : "Send reply"}
          </Button>
        </div>
      </div>

      <div className="mt-4">
        <Label className="text-xs text-muted-foreground">
          Attachments are uploaded through a signed URL and validated server-side before storage.
        </Label>
      </div>
    </>
  );
}
