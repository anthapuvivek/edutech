import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
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
import { teacherService } from "@/services/teacher.service";

export const Route = createFileRoute("/teacher/batches")({
  head: () => ({
    meta: [
      { title: "My Batches — Teacher Portal | Learntrix" },
      {
        name: "description",
        content: "Every batch you teach, with its course, schedule, capacity and student count.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: TeacherBatchesPage,
});

function formatDate(value?: string | null) {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString();
}

function TeacherBatchesPage() {
  const [search, setSearch] = useState("");

  // GET /teacher/batches resolves the teacher from the JWT and returns only batches whose
  // teacher_id is theirs, so there is no client-side ownership filtering to get wrong.
  const batchesQuery = useQuery({
    queryKey: ["teacher", "batches"],
    queryFn: () => teacherService.batches(),
  });

  const batches = batchesQuery.data ?? [];

  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return batches;
    return batches.filter(
      (b) =>
        b.name.toLowerCase().includes(q) ||
        (b.courseTitle ?? "").toLowerCase().includes(q),
    );
  }, [batches, search]);

  const totalStudents = batches.reduce((sum, b) => sum + (b.studentCount ?? 0), 0);
  const totalCapacity = batches.reduce((sum, b) => sum + (b.capacity ?? 0), 0);
  const activeCount = batches.filter((b) => (b.status ?? "").toUpperCase() === "ACTIVE").length;
  const courseCount = new Set(batches.map((b) => b.courseId)).size;

  return (
    <>
      <PageHeader
        title="My Batches"
        description="Every cohort assigned to you. A batch is the boundary for classes and student visibility — the same course can run several batches at once."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Batches" value={batches.length} />
        <StatCard label="Active" value={activeCount} />
        <StatCard label="Courses" value={courseCount} />
        <StatCard label="Students" value={`${totalStudents}/${totalCapacity}`} />
      </section>

      <Panel
        title="Assigned batches"
        description="Resolved from your account on the server. You only ever see batches you teach."
        action={
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search batch or course"
            className="sm:w-64"
            aria-label="Search batches"
          />
        }
      >
        {batchesQuery.isLoading ? (
          <div className="space-y-2 py-2">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : batchesQuery.isError ? (
          <p className="py-8 text-center text-sm text-destructive">
            Could not load your batches. Please refresh, or contact an administrator if this
            continues.
          </p>
        ) : batches.length === 0 ? (
          <EmptyState
            title="No batches assigned yet"
            description="An administrator assigns you to a batch from Admin → Batches. Once assigned, your cohorts and their students appear here."
          />
        ) : rows.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            No batches match “{search}”.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Batch</TableHead>
                  <TableHead>Course</TableHead>
                  <TableHead>Trainer</TableHead>
                  <TableHead>Schedule</TableHead>
                  <TableHead className="text-right">Students</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((b) => (
                  <TableRow key={b.id}>
                    <TableCell className="font-medium">{b.name}</TableCell>
                    <TableCell className="max-w-52 truncate">{b.courseTitle ?? "—"}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {b.teacherName ?? "—"}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {formatDate(b.startDate)} → {formatDate(b.endDate)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {b.studentCount}
                      <span className="text-muted-foreground">/{b.capacity}</span>
                    </TableCell>
                    <TableCell>
                      <StatusBadge value={b.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Panel>
    </>
  );
}
