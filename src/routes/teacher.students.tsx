import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { teacherService } from "@/services/teacher.service";

export const Route = createFileRoute("/teacher/students")({
  head: () => ({
    meta: [
      { title: "Student Tracking — Learntrix Trainer" },
      { name: "description", content: "Track course progress, quiz scores, coding practice and engagement for every student in your batches." },
      { property: "og:title", content: "Student Tracking — Learntrix Trainer" },
      { property: "og:description", content: "Per-student progress across lessons, quizzes and coding practice." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: TeacherStudents,
});

const statusVariant = { active: "default", at_risk: "outline", inactive: "secondary" } as const;

function TeacherStudents() {
  const [search, setSearch] = useState("");
  const students = useQuery({ queryKey: ["teacher", "students"], queryFn: () => teacherService.students() });
  const rows = (students.data ?? []).filter(
    (s) =>
      s.name.toLowerCase().includes(search.toLowerCase()) ||
      s.courseTitle.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <>
      <PageHeader title="Students" description="Students enrolled in the courses assigned to you." />

      <div className="mb-5 max-w-sm">
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by student or course"
          aria-label="Search students"
        />
      </div>

      {students.isPending ? (
        <Skeleton className="h-80 w-full" />
      ) : students.isError ? (
        <p className="text-sm text-destructive">Unable to load your students. Please try again.</p>
      ) : rows.length === 0 ? (
        <EmptyState title="No students found" description="Try a different search term or course filter." />
      ) : (
        <div className="surface-panel overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Student</TableHead>
                <TableHead>Course</TableHead>
                <TableHead className="w-40">Progress</TableHead>
                <TableHead className="text-right">Quiz</TableHead>
                <TableHead className="text-right">Assignments</TableHead>
                <TableHead className="text-right">Solved</TableHead>
                <TableHead className="text-right">Points</TableHead>
                <TableHead className="text-right">Rank</TableHead>
                <TableHead>Last active</TableHead>
                <TableHead>Status</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((s) => (
                <TableRow key={s.id}>
                  <TableCell>
                    <div className="font-medium">{s.name}</div>
                    <div className="text-xs text-muted-foreground">{s.email}</div>
                  </TableCell>
                  <TableCell className="max-w-52 truncate">{s.courseTitle}</TableCell>
                  <TableCell>
                    <Progress value={s.progressPercent} />
                    <span className="mt-1 block text-xs text-muted-foreground">
                      {s.progressPercent}% · {s.lessonsCompleted} lessons
                    </span>
                  </TableCell>
                  <TableCell className="text-right">{s.quizScore}%</TableCell>
                  <TableCell className="text-right">{s.assignments}</TableCell>
                  <TableCell className="text-right">{s.problemsSolved}</TableCell>
                  <TableCell className="text-right">{s.points.toLocaleString()}</TableCell>
                  <TableCell className="text-right">#{s.rank}</TableCell>
                  <TableCell className="text-muted-foreground">{s.lastActive}</TableCell>
                  <TableCell>
                    <Badge variant={statusVariant[s.status]}>{s.status.replace("_", " ")}</Badge>
                  </TableCell>
                  <TableCell>
                    <Button size="sm" variant="outline" disabled>
                      View student
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </>
  );
}
