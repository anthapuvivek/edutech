import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";

import { MetricBar, Panel, StatusBadge } from "@/components/portal/AdminBits";
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
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/performance")({
  head: () => ({
    meta: [
      { title: "Learning & Performance — Learntrix Admin" },
      {
        name: "description",
        content:
          "Student scorecards covering course progress, quizzes, assignments, coding practice and points.",
      },
      { property: "og:title", content: "Learning & Performance — Learntrix Admin" },
      { property: "og:description", content: "Platform-wide learning performance scorecards." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminPerformance,
});

function AdminPerformance() {
  const students = useQuery({
    queryKey: ["admin", "students"],
    queryFn: () => adminService.students(),
  });
  const [q, setQ] = useState("");
  const data = students.data ?? [];
  const rows = useMemo(() => {
    const needle = q.trim().toLowerCase();
    if (!needle) return data;
    return data.filter((s) =>
      [s.name, s.email, s.courseTitle].some((v) => String(v).toLowerCase().includes(needle)),
    );
  }, [data, q]);

  const avg = (key: "quizScore" | "codingScore" | "progressPercent") =>
    data.length ? Math.round(data.reduce((s, r) => s + (r[key] ?? 0), 0) / data.length) : 0;

  return (
    <>
      <PageHeader
        title="Learning & Performance"
        description="Scorecards are computed by the backend from lessons, quizzes, assignments and coding submissions."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Students tracked" value={data.length} />
        <StatCard label="Average progress" value={`${avg("progressPercent")}%`} />
        <StatCard label="Average quiz score" value={`${avg("quizScore")}%`} />
        <StatCard label="Average coding score" value={`${avg("codingScore")}%`} />
      </section>

      <Panel
        title="Student scorecards"
        action={
          <Input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search student or course"
            maxLength={80}
            className="w-full sm:w-72"
            aria-label="Search students"
          />
        }
      >
        {students.isPending ? (
          <Skeleton className="h-80 w-full" />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Student</TableHead>
                  <TableHead>Course</TableHead>
                  <TableHead className="w-52">Progress</TableHead>
                  <TableHead className="w-52">Quiz</TableHead>
                  <TableHead className="w-52">Coding</TableHead>
                  <TableHead className="text-right">Points</TableHead>
                  <TableHead>Career</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((s) => (
                  <TableRow key={s.id}>
                    <TableCell>
                      <div className="font-medium">{s.name}</div>
                      <div className="text-xs text-muted-foreground">{s.studentId}</div>
                    </TableCell>
                    <TableCell className="text-sm">{s.courseTitle}</TableCell>
                    <TableCell>
                      <MetricBar label="Progress" value={s.progressPercent} />
                    </TableCell>
                    <TableCell>
                      <MetricBar label="Quiz" value={s.quizScore} />
                    </TableCell>
                    <TableCell>
                      <MetricBar label="Coding" value={s.codingScore} />
                    </TableCell>
                    <TableCell className="text-right">{s.points.toLocaleString()}</TableCell>
                    <TableCell>
                      <StatusBadge value={s.careerStatus} />
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
