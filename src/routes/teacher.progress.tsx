import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Info } from "lucide-react";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { progressService } from "@/services/progress.service";
import { pct } from "@/types/progress";

export const Route = createFileRoute("/teacher/progress")({
  head: () => ({
    meta: [
      { title: "Student progress — Learntrix Trainer" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: TeacherProgressPage,
});

function TeacherProgressPage() {
  const query = useQuery({
    queryKey: ["teacher", "progress"],
    queryFn: () => progressService.batchProgress(),
  });

  if (query.isLoading) {
    return (
      <>
        <PageHeader title="Student progress" description="" />
        <Skeleton className="h-48 w-full" />
      </>
    );
  }

  if (query.isError) {
    return (
      <>
        <PageHeader title="Student progress" description="" />
        <Panel title="Could not load progress" description="">
          <p className="py-6 text-sm text-muted-foreground">
            Something went wrong. Refresh the page to try again.
          </p>
        </Panel>
      </>
    );
  }

  const batches = query.data ?? [];
  const unavailable = batches[0]?.unavailableMetrics ?? [];

  return (
    <>
      <PageHeader
        title="Student progress"
        description="Only the batches you run. Every figure is computed from stored activity."
      />

      {batches.length === 0 ? (
        <Panel title="No batches" description="">
          <p className="py-8 text-center text-sm text-muted-foreground">
            You do not run any batches yet, so there is no progress to show.
          </p>
        </Panel>
      ) : (
        batches.map((b) => (
          <Panel
            key={b.batchId}
            title={`${b.batchName ?? "Batch"} — ${b.courseTitle ?? ""}`}
            description={`${b.studentCount} student${b.studentCount === 1 ? "" : "s"}`}
          >
            <div className="mb-4 grid gap-4 sm:grid-cols-3">
              <StatCard label="Average progress" value={pct(b.averageProgress)} />
              <StatCard label="Average quiz score" value={pct(b.averageQuizScore)} />
              <StatCard label="Average assignment score" value={pct(b.averageAssignmentScore)} />
            </div>

            {b.students.length === 0 ? (
              <p className="py-6 text-sm text-muted-foreground">
                No students in this batch yet.
              </p>
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Student</TableHead>
                      <TableHead className="w-48">Progress</TableHead>
                      <TableHead className="text-right">Quiz</TableHead>
                      <TableHead className="text-right">Assignments</TableHead>
                      <TableHead className="text-right">Coding</TableHead>
                      <TableHead>Last active</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {b.students.map((s) => (
                      <TableRow key={s.studentId}>
                        <TableCell className="font-medium">
                          {s.studentName}
                          {s.studentEmail ? (
                            <span className="block text-xs text-muted-foreground">
                              {s.studentEmail}
                            </span>
                          ) : null}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <Progress value={s.overallProgress} className="flex-1" />
                            <span className="w-10 shrink-0 text-right text-sm tabular-nums">
                              {s.overallProgress}%
                            </span>
                          </div>
                        </TableCell>
                        <TableCell className="text-right tabular-nums">
                          {pct(s.averageQuizScore)}
                        </TableCell>
                        <TableCell className="text-right tabular-nums">
                          {s.assignmentsSubmitted ?? 0}/{s.assignmentsTotal ?? 0}
                        </TableCell>
                        <TableCell className="text-right tabular-nums">
                          {s.codingSolved ?? 0}/{s.codingTotal ?? 0}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {s.lastActivityAt
                            ? new Date(s.lastActivityAt).toLocaleDateString()
                            : "Never"}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </Panel>
        ))
      )}

      {unavailable.length > 0 ? (
        <Panel title="Not measured yet" description="">
          <ul className="flex flex-col gap-2">
            {unavailable.map((m) => (
              <li key={m} className="flex items-start gap-2 text-sm text-muted-foreground">
                <Info className="mt-0.5 size-4 shrink-0" aria-hidden />
                {m}
              </li>
            ))}
          </ul>
        </Panel>
      ) : null}
    </>
  );
}
