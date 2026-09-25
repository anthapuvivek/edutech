import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { ArrowLeft, CheckCircle2, ChevronLeft, ChevronRight, Flag, XCircle } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { quizService } from "@/services/quiz.service";
import type { QuizAttemptResult } from "@/types/quiz";

export const Route = createFileRoute("/student/quizzes/$quizId")({
  head: () => ({
    meta: [
      { title: "Quiz — Learntrix Student" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: StudentQuizAttemptPage,
});

type AttemptStage = "READY" | "ANSWERING" | "REVIEW";

function StudentQuizAttemptPage() {
  const { quizId } = Route.useParams();
  const queryClient = useQueryClient();
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [result, setResult] = useState<QuizAttemptResult | null>(null);
  const [stage, setStage] = useState<AttemptStage>("READY");
  const [questionIndex, setQuestionIndex] = useState(0);

  const detailQuery = useQuery({
    queryKey: ["student", "quiz", quizId],
    queryFn: () => quizService.studentQuizDetail(quizId),
  });

  // A submitted attempt is loaded from the caller-scoped endpoint so returning to this
  // route shows the full server result without introducing an attempt-id IDOR surface.
  const attemptsQuery = useQuery({
    queryKey: ["student", "quiz-attempts"],
    queryFn: () => quizService.myAttempts(),
    enabled: detailQuery.data?.quiz.attempted === true,
  });

  const savedResult = useMemo(
    () => attemptsQuery.data?.find((attempt) => attempt.quizId === quizId) ?? null,
    [attemptsQuery.data, quizId],
  );
  const displayedResult = result ?? savedResult;

  const startMutation = useMutation({
    mutationFn: () => quizService.startAttempt(quizId),
    onSuccess: () => {
      setStage("ANSWERING");
      setQuestionIndex(0);
    },
    onError: (err: unknown) => {
      toast.error(err instanceof Error && err.message ? err.message : "Could not start this quiz.");
    },
  });

  const submitMutation = useMutation({
    mutationFn: () =>
      quizService.submitAttempt(quizId, {
        answers: Object.entries(answers).map(([questionId, selectedOptionId]) => ({
          questionId,
          selectedOptionId,
        })),
      }),
    onSuccess: (res) => {
      setResult(res);
      void queryClient.invalidateQueries({ queryKey: ["student", "quizzes"] });
      void queryClient.invalidateQueries({ queryKey: ["student", "quiz-attempts"] });
      void queryClient.invalidateQueries({ queryKey: ["student", "quiz", quizId] });
      toast.success(
        res.passed ? `Passed with ${res.score}%` : `Scored ${res.score}% — not a pass this time`,
      );
    },
    onError: (err: unknown) => {
      toast.error(err instanceof Error && err.message ? err.message : "Could not submit your answers.");
    },
  });

  if (detailQuery.isLoading) {
    return <div className="space-y-3"><Skeleton className="h-12 w-2/3" /><Skeleton className="h-40 w-full" /></div>;
  }

  if (detailQuery.isError || !detailQuery.data) {
    return (
      <><PageHeader title="Quiz unavailable" description="" /><Panel title="Cannot open this quiz" description="">
        <p className="py-6 text-sm text-muted-foreground">This quiz is not available to you. It may be unpublished, or it may belong to a different course or batch.</p>
        <Button variant="outline" asChild><Link to="/student/quizzes"><ArrowLeft className="mr-1.5 size-4" aria-hidden /> Back to quizzes</Link></Button>
      </Panel></>
    );
  }

  const { quiz, questions } = detailQuery.data;
  const answeredCount = Object.keys(answers).length;

  if (quiz.attempted && attemptsQuery.isLoading && !displayedResult) {
    return <div className="space-y-3"><Skeleton className="h-12 w-2/3" /><Skeleton className="h-40 w-full" /></div>;
  }

  if (displayedResult) return <ResultView result={displayedResult} />;

  if (quiz.attempted && attemptsQuery.isError) {
    return (
      <><PageHeader title={quiz.title} description="You have already submitted this quiz." /><Panel title="Could not load your result" description="">
        <p className="py-4 text-sm text-muted-foreground">Your submission is saved, but its result could not be loaded. Refresh and try again.</p>
        <Button variant="outline" asChild><Link to="/student/quizzes">Back to quizzes</Link></Button>
      </Panel></>
    );
  }

  if (questions.length === 0) {
    return <><PageHeader title={quiz.title} description="" /><Panel title="Nothing to answer" description=""><p className="py-6 text-sm text-muted-foreground">This quiz has no questions yet. Please check back later.</p></Panel></>;
  }

  if (stage === "READY") {
    return (
      <><PageHeader title={quiz.title} description={quiz.courseTitle} action={<Button variant="outline" asChild><Link to="/student/quizzes"><ArrowLeft className="mr-1.5 size-4" aria-hidden /> Back</Link></Button>} />
        <Panel title="Ready to begin?" description="Your attempt starts only when you select Start quiz.">
          {quiz.description ? <p className="mb-5 whitespace-pre-wrap text-sm text-muted-foreground">{quiz.description}</p> : null}
          <div className="mb-6 grid gap-4 sm:grid-cols-3">
            <StatCard label="Questions" value={questions.length} />
            <StatCard label="Time limit" value={`${quiz.timeLimitMinutes ?? 30} min`} />
            <StatCard label="Pass mark" value={`${quiz.passingScore ?? 60}%`} />
          </div>
          <p className="mb-4 text-sm text-muted-foreground">You may move between questions and review your answers before submitting. A submitted quiz cannot be attempted again.</p>
          <Button onClick={() => startMutation.mutate()} disabled={startMutation.isPending}>{startMutation.isPending ? "Starting…" : "Start quiz"}</Button>
        </Panel></>
    );
  }

  if (stage === "REVIEW") {
    return (
      <><PageHeader title={`${quiz.title} — Review`} description={`${answeredCount} of ${questions.length} answered`} />
        <Panel title="Review your answers" description="Select a question to change its answer before submitting.">
          <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
            {questions.map((question, index) => (
              <Button key={question.id} variant={answers[question.id] ? "outline" : "secondary"} className="justify-start" onClick={() => { setQuestionIndex(index); setStage("ANSWERING"); }}>
                {answers[question.id] ? <CheckCircle2 className="mr-2 size-4 text-primary" /> : <Flag className="mr-2 size-4" />} Question {index + 1}
              </Button>
            ))}
          </div>
          {answeredCount < questions.length ? <p className="mt-4 text-sm text-muted-foreground">Unanswered questions will be marked incorrect.</p> : null}
          <div className="mt-6 flex flex-wrap justify-between gap-3 border-t pt-4">
            <Button variant="outline" onClick={() => setStage("ANSWERING")}>Continue editing</Button>
            <Button onClick={() => submitMutation.mutate()} disabled={submitMutation.isPending || answeredCount === 0}>{submitMutation.isPending ? "Submitting…" : "Submit quiz"}</Button>
          </div>
        </Panel></>
    );
  }

  // questions.length is checked above and the index is clamped by every navigation action.
  const question = questions[questionIndex]!;
  return (
    <><PageHeader title={quiz.title} description={`Question ${questionIndex + 1} of ${questions.length}`} />
      <Panel title={`Question ${questionIndex + 1}`} description={`${answeredCount} of ${questions.length} answered`}>
        <p className="text-base font-medium">{question.questionText}{question.points ? <span className="ml-2 text-xs text-muted-foreground">({question.points} marks)</span> : null}</p>
        <div className="mt-4 flex flex-col gap-2">
          {question.options.map((option) => {
            const selected = answers[question.id] === option.id;
            return <label key={option.id} className={`flex cursor-pointer items-center gap-3 rounded-md border p-3 text-sm ${selected ? "border-primary bg-primary/5" : "hover:bg-muted/50"}`}>
              <input type="radio" name={`q-${question.id}`} value={option.id} checked={selected} onChange={() => setAnswers((previous) => ({ ...previous, [question.id]: option.id }))} className="size-4" />
              <span>{option.optionText}</span>
            </label>;
          })}
        </div>
        <div className="mt-6 flex flex-wrap items-center justify-between gap-3 border-t pt-4">
          <Button variant="outline" onClick={() => setQuestionIndex((value) => Math.max(0, value - 1))} disabled={questionIndex === 0}><ChevronLeft className="mr-1 size-4" /> Previous</Button>
          <div className="flex gap-2"><Button variant="secondary" onClick={() => setStage("REVIEW")}>Review</Button>{questionIndex < questions.length - 1 ? <Button onClick={() => setQuestionIndex((value) => Math.min(questions.length - 1, value + 1))}>Next <ChevronRight className="ml-1 size-4" /></Button> : <Button onClick={() => setStage("REVIEW")}>Review & submit</Button>}</div>
        </div>
      </Panel></>
  );
}

function ResultView({ result }: { result: QuizAttemptResult }) {
  return (
    <><PageHeader title={result.quizTitle} description="Your result. Scoring was performed on the server." action={<Button variant="outline" asChild><Link to="/student/quizzes"><ArrowLeft className="mr-1.5 size-4" aria-hidden /> All quizzes</Link></Button>} />
      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><StatCard label="Score" value={`${result.score ?? 0}%`} /><StatCard label="Correct" value={`${result.correctAnswers ?? 0}/${result.totalQuestions ?? 0}`} /><StatCard label="Pass mark" value={`${result.passingScore ?? 0}%`} /><StatCard label="Outcome" value={result.passed ? "Passed" : "Not passed"} /></section>
      <Panel title="Answer review" description="Correct answers are revealed only after submission.">
        <ol className="flex flex-col gap-4">{result.answers.map((answer, index) => <li key={answer.questionId} className="rounded-md border p-3"><div className="flex items-start justify-between gap-3"><p className="font-medium">{index + 1}. {answer.questionText}</p>{answer.correct ? <Badge className="shrink-0 gap-1"><CheckCircle2 className="size-3" aria-hidden /> Correct</Badge> : <Badge variant="destructive" className="shrink-0 gap-1"><XCircle className="size-3" aria-hidden /> Incorrect</Badge>}</div>{answer.explanation ? <p className="mt-2 text-sm text-muted-foreground">{answer.explanation}</p> : null}</li>)}</ol>
      </Panel></>
  );
}
