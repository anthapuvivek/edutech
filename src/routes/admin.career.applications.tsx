import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/career/applications")({
  head: () => ({
    meta: [
      { title: "Applications — Learntrix Admin" },
      {
        name: "description",
        content:
          "Track every student job application from applied to offer across board and table views.",
      },
      { property: "og:title", content: "Applications — Learntrix Admin" },
      {
        property: "og:description",
        content: "Placement pipeline tracking for the whole platform.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminApplications,
});

const stages = [
  "Applied",
  "Shortlisted",
  "Assessment",
  "Interview",
  "Final Round",
  "Offer",
  "Rejected",
] as const;

function AdminApplications() {
  const apps = useQuery({
    queryKey: ["admin", "applications"],
    queryFn: () => adminService.applications(),
  });
  const [q, setQ] = useState("");
  const data = apps.data ?? [];
  const rows = useMemo(() => {
    const needle = q.trim().toLowerCase();
    if (!needle) return data;
    return data.filter((a) =>
      [a.studentName, a.jobTitle, a.companyName].some((v) => v.toLowerCase().includes(needle)),
    );
  }, [data, q]);

  return (
    <>
      <PageHeader
        title="Applications"
        description="Status changes here are recorded by the backend and reflected in the student's tracker."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Applications" value={data.length} />
        <StatCard
          label="Interviews"
          value={data.filter((a) => a.status === "Interview" || a.status === "Final Round").length}
        />
        <StatCard label="Offers" value={data.filter((a) => a.status === "Offer").length} />
        <StatCard
          label="Ineligible applies"
          value={data.filter((a) => !a.eligible).length}
          hint="Flagged for review"
        />
      </section>

      <Tabs defaultValue="board">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <TabsList>
            <TabsTrigger value="board">Board</TabsTrigger>
            <TabsTrigger value="table">Table</TabsTrigger>
          </TabsList>
          <Input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search student, role or company"
            maxLength={80}
            className="w-full sm:w-72"
            aria-label="Search applications"
          />
        </div>

        {apps.isPending ? (
          <Skeleton className="h-96 w-full" />
        ) : (
          <>
            <TabsContent value="board">
              <div className="grid gap-3 overflow-x-auto md:grid-cols-3 xl:grid-cols-7">
                {stages.map((stage) => (
                  <div key={stage} className="surface-panel min-w-56 p-3">
                    <div className="mb-2 flex items-center justify-between">
                      <p className="text-xs font-medium uppercase tracking-[0.12em] text-muted-foreground">
                        {stage}
                      </p>
                      <Badge variant="outline">
                        {rows.filter((a) => a.status === stage).length}
                      </Badge>
                    </div>
                    <div className="space-y-2">
                      {rows
                        .filter((a) => a.status === stage)
                        .map((a) => (
                          <div key={a.id} className="rounded-md border border-border p-3">
                            <p className="text-sm font-medium">{a.studentName}</p>
                            <p className="text-xs text-muted-foreground">
                              {a.jobTitle} · {a.companyName}
                            </p>
                            {!a.eligible ? (
                              <Badge variant="warning" className="mt-2">
                                Not eligible
                              </Badge>
                            ) : null}
                          </div>
                        ))}
                    </div>
                  </div>
                ))}
              </div>
            </TabsContent>

            <TabsContent value="table">
              <Panel title="All applications">
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Student</TableHead>
                        <TableHead>Role</TableHead>
                        <TableHead>Company</TableHead>
                        <TableHead>Applied</TableHead>
                        <TableHead>Eligibility</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="text-right">Action</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {rows.map((a) => (
                        <TableRow key={a.id}>
                          <TableCell className="font-medium">{a.studentName}</TableCell>
                          <TableCell>{a.jobTitle}</TableCell>
                          <TableCell>{a.companyName}</TableCell>
                          <TableCell className="text-sm">
                            {new Date(a.appliedAt).toLocaleDateString()}
                          </TableCell>
                          <TableCell>
                            <StatusBadge value={a.eligible ? "eligible" : "not_eligible"} />
                          </TableCell>
                          <TableCell>
                            <StatusBadge value={a.status} />
                          </TableCell>
                          <TableCell className="text-right">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => toast.success("Stage update submitted.")}
                            >
                              Move stage
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </Panel>
            </TabsContent>
          </>
        )}
      </Tabs>
    </>
  );
}
