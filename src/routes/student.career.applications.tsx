import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
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
import { jobService } from "@/services/job.service";
import type { ApplicationStatus } from "@/types/career";

export const Route = createFileRoute("/student/career/applications")({
  head: () => ({
    meta: [
      { title: "Application Tracker — Learntrix Careers" },
      {
        name: "description",
        content: "Track every job application from saved to offer with deadlines and next actions.",
      },
      { property: "og:title", content: "Application Tracker — Learntrix Careers" },
      { property: "og:description", content: "A personal CRM for your placement applications." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: ApplicationsPage,
});

const columns: ApplicationStatus[] = ["Saved", "Applied", "Interview", "Offer", "Rejected"];

function statusVariant(status: ApplicationStatus) {
  if (status === "Offer") return "success" as const;
  if (status === "Rejected" || status === "Withdrawn") return "outline" as const;
  if (status === "Interview" || status === "Final Round") return "info" as const;
  return "warning" as const;
}

function columnFor(status: ApplicationStatus): ApplicationStatus {
  if (status === "Assessment" || status === "Shortlisted") return "Applied";
  if (status === "Final Round") return "Interview";
  if (status === "Withdrawn") return "Rejected";
  return status;
}

function ApplicationsPage() {
  const apps = useQuery({
    queryKey: ["career", "applications"],
    queryFn: () => jobService.applications(),
  });

  if (apps.isLoading) return <Skeleton className="h-64 w-full" />;
  const rows = apps.data ?? [];

  return (
    <>
      <PageHeader
        title="Applications"
        description="Applications are tracked separately from course enrolments. Offers appear only once confirmed."
      />

      {!rows.length ? (
        <EmptyState
          title="No applications yet"
          description="Apply from the job portal to start tracking progress here."
        />
      ) : (
        <Tabs defaultValue="board">
          <TabsList className="mb-4">
            <TabsTrigger value="board">Board</TabsTrigger>
            <TabsTrigger value="table">Table</TabsTrigger>
          </TabsList>

          <TabsContent value="board">
            <div className="grid gap-4 md:grid-cols-3 xl:grid-cols-5">
              {columns.map((col) => {
                const items = rows.filter((r) => columnFor(r.status) === col);
                return (
                  <section key={col} className="surface-panel p-4">
                    <h2 className="mb-3 text-xs font-medium uppercase tracking-[0.14em] text-muted-foreground">
                      {col} · {items.length}
                    </h2>
                    <ul className="space-y-3">
                      {items.map((a) => (
                        <li key={a.id} className="rounded-md border border-border p-3 text-sm">
                          <p className="font-medium">{a.role}</p>
                          <p className="text-xs text-muted-foreground">{a.companyName}</p>
                          <Badge variant={statusVariant(a.status)} className="mt-2">
                            {a.status}
                          </Badge>
                          {a.nextAction ? (
                            <p className="mt-2 text-xs text-muted-foreground">{a.nextAction}</p>
                          ) : null}
                        </li>
                      ))}
                      {!items.length ? (
                        <li className="text-xs text-muted-foreground">Nothing here yet</li>
                      ) : null}
                    </ul>
                  </section>
                );
              })}
            </div>
          </TabsContent>

          <TabsContent value="table">
            <div className="surface-panel overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Company</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Applied</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Next action</TableHead>
                    <TableHead>Interview</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows.map((a) => (
                    <TableRow key={a.id}>
                      <TableCell className="font-medium">{a.companyName}</TableCell>
                      <TableCell>{a.role}</TableCell>
                      <TableCell>{new Date(a.appliedAt).toLocaleDateString()}</TableCell>
                      <TableCell>
                        <Badge variant={statusVariant(a.status)}>{a.status}</Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{a.nextAction ?? "—"}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {a.interviewAt ? new Date(a.interviewAt).toLocaleString() : "—"}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </TabsContent>
        </Tabs>
      )}
    </>
  );
}
