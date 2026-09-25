import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { EmptyState } from "@/components/common/EmptyState";
import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { quizService } from "@/services/quiz.service";
import { teacherService } from "@/services/teacher.service";

export const Route = createFileRoute("/teacher/quizzes")({
  head: () => ({
    meta: [
      { title: "Quizzes — Teacher Portal | Learntrix" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: TeacherQuizzesPage,
});

const NONE = "none";

function TeacherQuizzesPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [courseId, setCourseId] = useState("");
  const [batchId, setBatchId] = useState(NONE);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [resultsFor, setResultsFor] = useState<string | null>(null);

  const quizzesQuery = useQuery({
    queryKey: ["teacher", "quizzes"],
    queryFn: () => quizService.teacherQuizzes(),
  });
  // Only courses and batches this teacher is authorised for; both resolve from the JWT.
  const coursesQuery = useQuery({
    queryKey: ["teacher", "courses"],
    queryFn: () => teacherService.courses(),
  });
  const batchesQuery = useQuery({
    queryKey: ["teacher", "batches"],
    queryFn: () => teacherService.batches(),
  });

  const quizzes = quizzesQuery.data ?? [];
  const published = quizzes.filter((q) => q.status === "PUBLISHED").length;

  const publishMutation = useMutation({
    mutationFn: ({ id, publish }: { id: string; publish: boolean }) =>
      publish ? quizService.publishQuiz(id) : quizService.unpublishQuiz(id),
    onSuccess: (_d, vars) => {
      toast.success(vars.publish ? "Quiz published." : "Quiz moved back to draft.");
      void queryClient.invalidateQueries({ queryKey: ["teacher", "quizzes"] });
    },
    onError: (err: unknown) => {
      // The backend refuses to publish a quiz with no questions — show that reason.
      toast.error(
        err instanceof Error && err.message ? err.message : "Could not change the quiz status.",
      );
    },
  });

  async function handleCreate(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const title = ((form.get("title") as string | null) ?? "").trim();
    if (!title) {
      toast.error("Give the quiz a title.");
      return;
    }
    if (!courseId) {
      toast.error("Select a course.");
      return;
    }

    setSaving(true);
    try {
      await quizService.createQuiz({
        courseId,
        batchId: batchId !== NONE ? batchId : undefined,
        title,
        description: ((form.get("description") as string | null) ?? "").trim() || undefined,
        passingScore: Number(form.get("passingScore")) || 60,
        timeLimitMinutes: Number(form.get("timeLimitMinutes")) || 30,
      });
      toast.success(`${title} created as a draft. Add questions, then publish.`);
      setOpen(false);
      setCourseId("");
      setBatchId(NONE);
      await quizzesQuery.refetch();
    } catch (err: unknown) {
      toast.error(
        err instanceof Error && err.message ? err.message : "Could not create the quiz.",
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <PageHeader
        title="Quizzes"
        description="Assessments for your courses and batches. A quiz starts as a draft and is only visible to students once published."
        action={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="mr-1.5 size-4" aria-hidden /> Create quiz
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-lg">
              <DialogHeader>
                <DialogTitle>Create quiz</DialogTitle>
                <DialogDescription>
                  Saved as a draft. Add at least one question before you can publish it.
                </DialogDescription>
              </DialogHeader>
              <form id="create-quiz" onSubmit={handleCreate} className="space-y-4">
                <div className="space-y-1.5">
                  <Label htmlFor="quiz-title">
                    Title <span className="text-destructive">*</span>
                  </Label>
                  <Input id="quiz-title" name="title" required maxLength={200} />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="quiz-course">
                    Course <span className="text-destructive">*</span>
                  </Label>
                  <Select value={courseId} onValueChange={(v) => { setCourseId(v); setBatchId(NONE); }}>
                    <SelectTrigger id="quiz-course">
                      <SelectValue placeholder="Select course" />
                    </SelectTrigger>
                    <SelectContent>
                      {(coursesQuery.data ?? []).map((c: any) => (
                        <SelectItem key={c.id} value={c.id}>
                          {c.title}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="quiz-batch">Batch</Label>
                  <Select value={batchId} onValueChange={setBatchId} disabled={!courseId}>
                    <SelectTrigger id="quiz-batch">
                      <SelectValue placeholder="Whole course" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={NONE}>Whole course</SelectItem>
                      {(batchesQuery.data ?? [])
                        .filter((b) => !courseId || b.courseId === courseId)
                        .map((b) => (
                          <SelectItem key={b.id} value={b.id}>
                            {b.name}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                  <p className="text-xs text-muted-foreground">
                    Leave as &ldquo;Whole course&rdquo; for every enrolled student, or pick one
                    batch to restrict it to that cohort.
                  </p>
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="quiz-pass">Pass mark (%)</Label>
                    <Input id="quiz-pass" name="passingScore" type="number" min={0} max={100} defaultValue={60} />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="quiz-time">Time limit (min)</Label>
                    <Input id="quiz-time" name="timeLimitMinutes" type="number" min={1} defaultValue={30} />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="quiz-desc">Description</Label>
                  <Textarea id="quiz-desc" name="description" rows={2} />
                </div>
              </form>
              <DialogFooter>
                <Button variant="outline" onClick={() => setOpen(false)} disabled={saving}>
                  Cancel
                </Button>
                <Button type="submit" form="create-quiz" disabled={saving}>
                  {saving ? "Creating…" : "Create draft"}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Quizzes" value={quizzes.length} />
        <StatCard label="Published" value={published} />
        <StatCard label="Drafts" value={quizzes.length - published} />
        <StatCard
          label="Questions"
          value={quizzes.reduce((s, q) => s + (q.questionCount ?? 0), 0)}
        />
      </section>

      <Panel title="Your quizzes" description="Only quizzes for courses and batches you manage.">
        {quizzesQuery.isLoading ? (
          <div className="space-y-2 py-2">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : quizzesQuery.isError ? (
          <p className="py-8 text-center text-sm text-destructive">Could not load your quizzes.</p>
        ) : quizzes.length === 0 ? (
          <EmptyState
            title="No quizzes yet"
            description="Create a quiz, add questions, then publish it to your students."
          />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Quiz</TableHead>
                  <TableHead>Course</TableHead>
                  <TableHead>Scope</TableHead>
                  <TableHead className="text-right">Questions</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {quizzes.map((q) => (
                  <TableRow key={q.id}>
                    <TableCell className="font-medium">{q.title}</TableCell>
                    <TableCell className="max-w-52 truncate">{q.courseTitle}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {q.batchName ?? "Whole course"}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      <button
                        type="button"
                        className="underline-offset-2 hover:underline"
                        onClick={() => setExpanded((c) => (c === q.id ? null : q.id))}
                      >
                        {q.questionCount ?? 0}
                      </button>
                    </TableCell>
                    <TableCell>
                      <StatusBadge value={q.status} />
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={publishMutation.isPending}
                        onClick={() =>
                          publishMutation.mutate({
                            id: q.id,
                            publish: q.status !== "PUBLISHED",
                          })
                        }
                      >
                        {q.status === "PUBLISHED" ? "Unpublish" : "Publish"}
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setResultsFor((c) => (c === q.id ? null : q.id))}
                      >
                        Results
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Panel>

      {expanded ? <QuizQuestions quizId={expanded} /> : null}
      {resultsFor ? <QuizResults quizId={resultsFor} /> : null}
    </>
  );
}

/** Question authoring for one quiz. The answer key is teacher-only and never sent to students. */
function QuizQuestions({ quizId }: { quizId: string }) {
  const queryClient = useQueryClient();
  const [text, setText] = useState("");
  const [options, setOptions] = useState<string[]>(["", ""]);
  const [correctIndex, setCorrectIndex] = useState(0);

  const questionsQuery = useQuery({
    queryKey: ["teacher", "quiz-questions", quizId],
    queryFn: () => quizService.teacherQuestions(quizId),
  });

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ["teacher", "quiz-questions", quizId] });
    void queryClient.invalidateQueries({ queryKey: ["teacher", "quizzes"] });
  };

  const addMutation = useMutation({
    mutationFn: () =>
      quizService.addQuestion(quizId, {
        questionText: text.trim(),
        options: options.map((o, i) => ({ optionText: o.trim(), correct: i === correctIndex })),
      }),
    onSuccess: () => {
      toast.success("Question added.");
      setText("");
      setOptions(["", ""]);
      setCorrectIndex(0);
      invalidate();
    },
    onError: (err: unknown) => {
      toast.error(err instanceof Error && err.message ? err.message : "Could not add the question.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (questionId: string) => quizService.deleteQuestion(quizId, questionId),
    onSuccess: () => {
      toast.success("Question removed.");
      invalidate();
    },
    onError: () => toast.error("Could not remove the question."),
  });

  const canAdd =
    text.trim().length > 0 && options.filter((o) => o.trim().length > 0).length >= 2;

  return (
    <Panel
      title="Questions"
      description="Exactly one option must be marked correct. Students never receive the answer key."
    >
      {questionsQuery.isLoading ? (
        <Skeleton className="h-20 w-full" />
      ) : (questionsQuery.data ?? []).length === 0 ? (
        <p className="text-sm text-muted-foreground">
          No questions yet. Add one below — a quiz cannot be published until it has at least one.
        </p>
      ) : (
        <ol className="mb-5 flex flex-col gap-3">
          {(questionsQuery.data ?? []).map((q, i) => (
            <li key={q.id} className="rounded-md border p-3">
              <div className="flex items-start justify-between gap-3">
                <p className="font-medium">
                  {i + 1}. {q.questionText}
                </p>
                <Button
                  size="sm"
                  variant="ghost"
                  className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                  onClick={() => deleteMutation.mutate(q.id)}
                  title="Remove question"
                >
                  <Trash2 className="size-4" aria-hidden />
                </Button>
              </div>
              <ul className="mt-2 flex flex-col gap-1 text-sm">
                {q.options.map((o) => (
                  <li key={o.id} className={o.correct ? "font-medium text-primary" : "text-muted-foreground"}>
                    {o.correct ? "✓ " : "• "}
                    {o.optionText}
                  </li>
                ))}
              </ul>
            </li>
          ))}
        </ol>
      )}

      <div className="space-y-3 border-t pt-4">
        <div className="space-y-1.5">
          <Label htmlFor="new-question">New question</Label>
          <Input
            id="new-question"
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder="e.g. Which keyword makes a Java field immutable?"
          />
        </div>

        <div className="flex flex-col gap-2">
          {options.map((o, i) => (
            <div key={i} className="flex items-center gap-2">
              <input
                type="radio"
                name="correct-option"
                checked={correctIndex === i}
                onChange={() => setCorrectIndex(i)}
                className="size-4"
                aria-label={`Mark option ${i + 1} correct`}
              />
              <Input
                value={o}
                onChange={(e) =>
                  setOptions((prev) => prev.map((p, j) => (j === i ? e.target.value : p)))
                }
                placeholder={`Option ${i + 1}`}
              />
              {options.length > 2 ? (
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setOptions((prev) => prev.filter((_, j) => j !== i));
                    if (correctIndex >= i && correctIndex > 0) setCorrectIndex((c) => c - 1);
                  }}
                >
                  <Trash2 className="size-4" aria-hidden />
                </Button>
              ) : null}
            </div>
          ))}
        </div>

        <div className="flex flex-wrap gap-2">
          <Button size="sm" variant="outline" onClick={() => setOptions((p) => [...p, ""])}>
            Add option
          </Button>
          <Button size="sm" onClick={() => addMutation.mutate()} disabled={!canAdd || addMutation.isPending}>
            {addMutation.isPending ? "Adding…" : "Add question"}
          </Button>
        </div>
        <p className="text-xs text-muted-foreground">
          The radio button marks the correct answer. Two options minimum.
        </p>
      </div>
    </Panel>
  );
}

/**
 * Attempt results for one quiz.
 *
 * The backend refuses this endpoint unless the caller can manage the quiz, so a teacher
 * cannot read another teacher's cohort by guessing a quiz id.
 */
function QuizResults({ quizId }: { quizId: string }) {
  const resultsQuery = useQuery({
    queryKey: ["teacher", "quiz-performance", quizId],
    queryFn: () => quizService.quizPerformance(quizId),
  });

  if (resultsQuery.isLoading) {
    return (
      <Panel title="Results" description="">
        <Skeleton className="h-32 w-full" />
      </Panel>
    );
  }

  if (resultsQuery.isError || !resultsQuery.data) {
    return (
      <Panel title="Results" description="">
        <p className="py-6 text-sm text-muted-foreground">
          Could not load results for this quiz.
        </p>
      </Panel>
    );
  }

  const attempts = resultsQuery.data;
  const passed = attempts.filter((a) => a.passed).length;
  // Averaged over attempts only; students who never attempted would drag it to zero.
  const average =
    attempts.length > 0
      ? Math.round(attempts.reduce((sum, a) => sum + (a.score ?? 0), 0) / attempts.length)
      : 0;

  return (
    <Panel
      title={`Results — ${attempts[0]?.quizTitle ?? "Quiz"}`}
      description="Scores are computed on the server from its own answer key."
    >
      <div className="mb-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Attempts" value={String(attempts.length)} />
        <StatCard label="Passed" value={String(passed)} />
        <StatCard label="Failed" value={String(attempts.length - passed)} />
        <StatCard label="Average score" value={`${average}%`} />
      </div>

      {attempts.length === 0 ? (
        <p className="py-6 text-sm text-muted-foreground">
          No student has attempted this quiz yet.
        </p>
      ) : (
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Student</TableHead>
                <TableHead className="text-right">Correct</TableHead>
                <TableHead className="text-right">Score</TableHead>
                <TableHead>Outcome</TableHead>
                <TableHead>Submitted</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {attempts.map((a) => (
                <TableRow key={a.attemptId}>
                  <TableCell className="font-medium">
                    {a.studentName}
                    {a.studentEmail ? (
                      <span className="block text-xs text-muted-foreground">{a.studentEmail}</span>
                    ) : null}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">
                    {a.correctAnswers ?? 0}/{a.totalQuestions ?? 0}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">{a.score ?? 0}%</TableCell>
                  <TableCell>
                    <Badge variant={a.passed ? "default" : "destructive"}>
                      {a.passed ? "Passed" : "Failed"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {a.submittedAt ? new Date(a.submittedAt).toLocaleString() : "—"}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </Panel>
  );
}
