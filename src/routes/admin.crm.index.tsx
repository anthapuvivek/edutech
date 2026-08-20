import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { Mail, MessageSquare, Phone } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { crmService } from "@/services/crm.service";
import { leadKanbanStages, leadStages, type Lead, type LeadStage } from "@/types/ops";

export const Route = createFileRoute("/admin/crm/")({
  head: () => ({
    meta: [
      { title: "CRM & Leads — Learntrix Admin" },
      {
        name: "description",
        content:
          "Track enquiries from first touch to enrollment with counsellor ownership and follow-ups.",
      },
      { property: "og:title", content: "CRM & Leads — Learntrix Admin" },
      { property: "og:description", content: "Lead pipeline, follow-ups and conversion tracking." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminCrm,
});

function AdminCrm() {
  const qc = useQueryClient();
  const [stage, setStage] = useState<LeadStage | "all">("all");
  const [source, setSource] = useState("all");
  const [owner, setOwner] = useState("all");
  const [search, setSearch] = useState("");

  const overview = useQuery({
    queryKey: ["crm", "overview"],
    queryFn: () => crmService.overview(),
  });
  const staff = useQuery({ queryKey: ["crm", "staff"], queryFn: () => crmService.staff() });
  const leads = useQuery({
    queryKey: ["crm", "leads", stage, source, owner, search],
    queryFn: () => crmService.list({ stage, source, assignedToId: owner, search }),
  });

  const counsellors = (staff.data ?? []).filter(
    (s) => s.role === "counsellor" || s.role === "placement_officer" || s.role === "support_agent",
  );
  const rows = leads.data ?? [];

  async function move(lead: Lead, next: LeadStage) {
    await crmService.moveStage(lead.id, next);
    void qc.invalidateQueries({ queryKey: ["crm"] });
    toast.success(`${lead.name} moved to ${next}.`);
  }

  async function assign(lead: Lead, staffId: string) {
    const member = (staff.data ?? []).find((s) => s.id === staffId);
    if (!member) return;
    await crmService.assign(lead.id, member);
    void qc.invalidateQueries({ queryKey: ["crm"] });
    toast.success(`${lead.name} assigned to ${member.name}.`);
  }

  return (
    <>
      <PageHeader
        title="CRM & Lead Management"
        description="Every enquiry from the public website lands here. Lead ownership and visibility are enforced by the backend."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Total leads" value={overview.data?.totalLeads ?? "—"} />
        <StatCard label="New leads" value={overview.data?.newLeads ?? "—"} />
        <StatCard label="Follow-ups today" value={overview.data?.followUpsToday ?? "—"} />
        <StatCard label="Demo registrations" value={overview.data?.demoRegistrations ?? "—"} />
        <StatCard label="Demo attendees" value={overview.data?.demoAttendees ?? "—"} />
        <StatCard label="Payment pending" value={overview.data?.paymentPending ?? "—"} />
        <StatCard label="Converted" value={overview.data?.convertedLeads ?? "—"} />
        <StatCard
          label="Conversion rate"
          value={overview.data ? `${overview.data.conversionRate}%` : "—"}
        />
      </section>

      <Tabs defaultValue="table">
        <TabsList className="mb-4">
          <TabsTrigger value="table">Leads</TabsTrigger>
          <TabsTrigger value="kanban">Pipeline</TabsTrigger>
          <TabsTrigger value="followups">Follow-ups due today</TabsTrigger>
        </TabsList>

        <TabsContent value="table">
          <Panel
            title="All leads"
            description="Filter by stage, source and owner."
            action={
              <div className="flex flex-wrap gap-2">
                <Input
                  className="w-48"
                  placeholder="Search name, phone, email"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
                <Select value={stage} onValueChange={(v) => setStage(v as LeadStage | "all")}>
                  <SelectTrigger className="w-40" aria-label="Filter by stage">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All stages</SelectItem>
                    {leadStages.map((s) => (
                      <SelectItem key={s} value={s}>
                        {s}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Select value={source} onValueChange={setSource}>
                  <SelectTrigger className="w-40" aria-label="Filter by source">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All sources</SelectItem>
                    {[
                      "Website",
                      "Course Enquiry",
                      "Demo Class",
                      "Webinar",
                      "WhatsApp",
                      "Phone",
                      "Referral",
                      "Social Media",
                      "Advertisement",
                      "Other",
                    ].map((s) => (
                      <SelectItem key={s} value={s}>
                        {s}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Select value={owner} onValueChange={setOwner}>
                  <SelectTrigger className="w-44" aria-label="Filter by counsellor">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All counsellors</SelectItem>
                    {counsellors.map((c) => (
                      <SelectItem key={c.id} value={c.id}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            }
          >
            {leads.isPending ? (
              <Skeleton className="h-80 w-full" />
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Name</TableHead>
                      <TableHead>Contact</TableHead>
                      <TableHead>Course interest</TableHead>
                      <TableHead>Source</TableHead>
                      <TableHead>Counsellor</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Last contact</TableHead>
                      <TableHead>Next follow-up</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {rows.map((lead) => (
                      <TableRow key={lead.id}>
                        <TableCell className="font-medium">
                          <Link
                            to="/admin/crm/$leadId"
                            params={{ leadId: lead.id }}
                            className="hover:underline"
                          >
                            {lead.name}
                          </Link>
                          <div className="text-xs text-muted-foreground">{lead.city}</div>
                        </TableCell>
                        <TableCell className="text-xs">
                          {lead.phone}
                          <div className="text-muted-foreground">{lead.email}</div>
                        </TableCell>
                        <TableCell className="text-sm">{lead.courseInterest}</TableCell>
                        <TableCell className="text-xs">{lead.source}</TableCell>
                        <TableCell>
                          <Select
                            value={lead.assignedToId}
                            onValueChange={(v) => void assign(lead, v)}
                          >
                            <SelectTrigger className="w-40" aria-label={`Assign ${lead.name}`}>
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {counsellors.map((c) => (
                                <SelectItem key={c.id} value={c.id}>
                                  {c.name}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </TableCell>
                        <TableCell>
                          <StatusBadge value={lead.stage} />
                        </TableCell>
                        <TableCell className="text-xs">
                          {new Date(lead.lastContactAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell className="text-xs">
                          {lead.nextFollowUpAt
                            ? new Date(lead.nextFollowUpAt).toLocaleDateString()
                            : "—"}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-1">
                            <Button
                              size="icon"
                              variant="ghost"
                              aria-label="Call lead"
                              onClick={() => toast.success(`Call logged for ${lead.name}.`)}
                            >
                              <Phone className="size-4" />
                            </Button>
                            <Button
                              size="icon"
                              variant="ghost"
                              aria-label="WhatsApp lead"
                              onClick={() =>
                                toast.success(`WhatsApp message queued for ${lead.name}.`)
                              }
                            >
                              <MessageSquare className="size-4" />
                            </Button>
                            <Button
                              size="icon"
                              variant="ghost"
                              aria-label="Email lead"
                              onClick={() => toast.success(`Email queued for ${lead.name}.`)}
                            >
                              <Mail className="size-4" />
                            </Button>
                            <Button size="sm" variant="outline" asChild>
                              <Link to="/admin/crm/$leadId" params={{ leadId: lead.id }}>
                                View
                              </Link>
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </Panel>
        </TabsContent>

        <TabsContent value="kanban">
          <div className="flex gap-4 overflow-x-auto pb-4">
            {leadKanbanStages.map((col) => {
              const items = rows.filter((l) => l.stage === col);
              return (
                <div
                  key={col}
                  className="w-72 shrink-0 rounded-lg border border-border bg-card p-3"
                >
                  <div className="mb-3 flex items-center justify-between">
                    <h3 className="text-sm font-semibold">{col}</h3>
                    <span className="text-xs text-muted-foreground">{items.length}</span>
                  </div>
                  <div className="space-y-2">
                    {items.map((lead) => (
                      <div
                        key={lead.id}
                        className="rounded-md border border-border/70 bg-background p-3"
                      >
                        <Link
                          to="/admin/crm/$leadId"
                          params={{ leadId: lead.id }}
                          className="text-sm font-medium hover:underline"
                        >
                          {lead.name}
                        </Link>
                        <p className="mt-0.5 text-xs text-muted-foreground">
                          {lead.courseInterest}
                        </p>
                        <p className="mt-1 text-[11px] text-muted-foreground">
                          {lead.source} · {lead.assignedTo}
                        </p>
                        <Select
                          value={lead.stage}
                          onValueChange={(v) => void move(lead, v as LeadStage)}
                        >
                          <SelectTrigger
                            className="mt-2 h-8 text-xs"
                            aria-label={`Move ${lead.name}`}
                          >
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
                      </div>
                    ))}
                    {items.length === 0 ? (
                      <p className="py-6 text-center text-xs text-muted-foreground">No leads</p>
                    ) : null}
                  </div>
                </div>
              );
            })}
          </div>
        </TabsContent>

        <TabsContent value="followups">
          <Panel
            title="Follow-ups due today"
            description="Counsellors see only the leads assigned to them; admins see everything."
          >
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Lead</TableHead>
                    <TableHead>Course</TableHead>
                    <TableHead>Counsellor</TableHead>
                    <TableHead>Stage</TableHead>
                    <TableHead className="text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows
                    .filter(
                      (l) =>
                        l.nextFollowUpAt?.slice(0, 10) === new Date().toISOString().slice(0, 10),
                    )
                    .map((lead) => (
                      <TableRow key={lead.id}>
                        <TableCell className="font-medium">{lead.name}</TableCell>
                        <TableCell className="text-sm">{lead.courseInterest}</TableCell>
                        <TableCell className="text-sm">{lead.assignedTo}</TableCell>
                        <TableCell>
                          <StatusBadge value={lead.stage} />
                        </TableCell>
                        <TableCell className="text-right">
                          <Button size="sm" variant="outline" asChild>
                            <Link to="/admin/crm/$leadId" params={{ leadId: lead.id }}>
                              Open
                            </Link>
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            </div>
          </Panel>
        </TabsContent>
      </Tabs>
    </>
  );
}
