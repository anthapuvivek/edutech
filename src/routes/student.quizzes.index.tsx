import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { CheckCircle2, ListChecks, XCircle } from "lucide-react";
import { useMemo, useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { Panel } from "@/components/portal/AdminBits";
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
import { quizService } from "@/services/quiz.service";

export const Route = createFileRoute("/student/quizzes/")({
  head: () => ({
    meta: [
      { title: "Quizzes — Learntrix Student" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: StudentQuizzesPage,
});

function StudentQuizzesPage() {
  const [search, setSearch] = useState("");

  // The backend returns only published quizzes for courses this student is enrolled in,
  // and for a batch quiz only if they belong to that batch. No filtering is done here.
  const quizzesQuery = useQuery({
    queryKey: ["student", "quizzes"],
    queryFn: () => quizService.studentQuizzes(),
  });

  const quizzes = quizzesQuery.data ?? [];

  const rows = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return quizzes;
    return quizzes.filter(
      (z) =>
        z.title.toLowerCase().includes(q) || (z.courseTitle ?? "").toLowerCase().includes(q),
    );
  }, [quizzes, search]);

  const attempted = quizzes.filter((z) => z.attempted).length;
  const passed = quizzes.filter((z) => z.attemptStatus === "PASSED").length;

  return (
    <>
      <PageHeader
        title="Quizzes"
        description="Assessments your trainer has published for your course or batch."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Available" value={quizzes.length} />
        <StatCard label="Attempted" value={attempted} />
        <StatCard label="Passed" value={passed} />
        <StatCard label="Not attempted" value={quizzes.length - attempted} />
      </section>

      <Panel
        title="Available quizzes"
        description="A quiz appears here once your trainer publishes it."
        action={
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search quiz or course"
            className="sm:w-64"
            aria-label="Search quizzes"
          />
        }
      >
        {quizzesQuery.isLoading ? (
          <div className="space-y-2 py-2">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : quizzesQuery.isError ? (
          <p className="py-8 text-center text-sm text-destructive">
            Could not load your quizzes. Please refresh and try again.
          </p>
        ) : quizzes.length === 0 ? (
          <EmptyState
            title="No quizzes yet"
            description="When your trainer publishes a quiz for your course or batch, it will appear here."
          />
        ) : rows.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            No quizzes match &ldquo;{search}&rdquo;.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Quiz</TableHead>
                  <TableHead>Course</TableHead>
                  <TableHead className="text-right">Questions</TableHead>
                  <TableHead className="text-right">Pass mark</TableHead>
                  <TableHead>Result</TableHead>
                  <TableHead className="text-right">Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((z) => (
                  <TableRow key={z.id}>
                    <TableCell>
                      <div className="font-medium">{z.title}</div>
                      {z.batchName ? (
                        <div className="text-xs text-muted-foreground">{z.batchName}</div>
                      ) : null}
                    </TableCell>
                    <TableCell className="max-w-52 truncate">{z.courseTitle}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      {z.questionCount ?? "—"}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {z.passingScore != null ? `${z.passingScore}%` : "—"}
                    </TableCell>
                    <TableCell>
                      {!z.attempted ? (
                        <Badge variant="outline">Not attempted</Badge>
                      ) : z.attemptStatus === "PASSED" ? (
                        <Badge className="gap-1">
                          <CheckCircle2 className="size-3" aria-hidden /> {z.score}% passed
                        </Badge>
                      ) : (
                        <Badge variant="destructive" className="gap-1">
                          <XCircle className="size-3" aria-hidden /> {z.score}% failed
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button size="sm" variant={z.attempted ? "outline" : "default"} asChild>
                        <Link to="/student/quizzes/$quizId" params={{ quizId: z.id }}>
                          <ListChecks className="mr-1.5 size-4" aria-hidden />
                          {z.attempted ? "View result" : "Start quiz"}
                        </Link>
                      </Button>
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
