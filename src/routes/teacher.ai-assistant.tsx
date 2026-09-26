import { useMutation, useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Check, Loader2, RefreshCw, Sparkles, Trash2, Wand2 } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import { Textarea } from "@/components/ui/textarea";
import { aiService } from "@/services/ai.service";
import { assignmentService } from "@/services/assignment.service";
import { codingService } from "@/services/coding.service";
import { quizService } from "@/services/quiz.service";
import { teacherService } from "@/services/teacher.service";
import type {
  AiAssignmentDraft,
  AiCodingProblemDraft,
  AiContentType,
  AiDifficulty,
  AiQuizQuestionDraft,
} from "@/types/ai";

export const Route = createFileRoute("/teacher/ai-assistant")({
  head: () => ({
    meta: [
      { title: "AI Question Assistant — Learntrix Trainer" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AiAssistantPage,
});

const DIFFICULTIES: AiDifficulty[] = ["EASY", "MEDIUM", "HARD"];

function AiAssistantPage() {
  // ---------- generation form ----------
  const [courseId, setCourseId] = useState("");
  const [batchId, setBatchId] = useState("");
  const [type, setType] = useState<AiContentType>("QUIZ");
  const [difficulty, setDifficulty] = useState<AiDifficulty>("MEDIUM");
  const [topic, setTopic] = useState("");
  const [count, setCount] = useState(5);
  const [instructions, setInstructions] = useState("");
  const [language, setLanguage] = useState("java");

  // ---------- review state ----------
  const [questions, setQuestions] = useState<AiQuizQuestionDraft[]>([]);
  const [problems, setProblems] = useState<AiCodingProblemDraft[]>([]);
  const [assignments, setAssignments] = useState<AiAssignmentDraft[]>([]);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [warnings, setWarnings] = useState<string[]>([]);
  const [refineText, setRefineText] = useState("");

  const coursesQuery = useQuery({
    queryKey: ["teacher", "courses"],
    queryFn: () => teacherService.courses(),
  });
  const batchesQuery = useQuery({
    queryKey: ["teacher", "batches"],
    queryFn: () => teacherService.batches(),
  });

  const courses = coursesQuery.data ?? [];
  // A batch belongs to exactly one course; offering the others would only produce a
  // BATCH_COURSE_MISMATCH from the backend.
  const batches = useMemo(
    () => (batchesQuery.data ?? []).filter((b) => !courseId || b.courseId === courseId),
    [batchesQuery.data, courseId],
  );

  function resetDrafts() {
    setQuestions([]);
    setProblems([]);
    setAssignments([]);
    setSelected(new Set());
    setWarnings([]);
  }

  const generateMutation = useMutation({
    mutationFn: (refine?: string) =>
      aiService.generate({
        courseId,
        batchId: batchId || undefined,
        type,
        difficulty,
        topic,
        count,
        instructions: instructions || undefined,
        language: type === "CODING" ? language : undefined,
        // Regeneration sends what is on screen plus the change, so the backend needs no
        // conversation table.
        ...(refine
          ? {
              regenerateInstruction: refine,
              existingQuestions: type === "QUIZ" ? questions : undefined,
              existingProblems: type === "CODING" ? problems : undefined,
              existingAssignments: type === "ASSIGNMENT" ? assignments : undefined,
            }
          : {}),
      }),
    onSuccess: (res) => {
      setQuestions(res.questions ?? []);
      setProblems(res.problems ?? []);
      setAssignments(res.assignments ?? []);
      // Everything comes back selected: the common case is keeping most of it.
      const n = (res.questions ?? res.problems ?? res.assignments ?? []).length;
      setSelected(new Set(Array.from({ length: n }, (_, i) => i)));
      setWarnings(res.warnings ?? []);
      setRefineText("");
      toast.success(`Generated ${n} item${n === 1 ? "" : "s"} for review`);
    },
    onError: (err: unknown) => {
      toast.error(
        err instanceof Error && err.message ? err.message : "Generation failed. Please try again.",
      );
    },
  });

  /** Creates a DRAFT quiz, adds the selected questions, and stops. Never publishes. */
  const addQuizMutation = useMutation({
    mutationFn: async () => {
      const chosen = questions.filter((_, i) => selected.has(i));
      const quiz = await quizService.createQuiz({
        courseId,
        batchId: batchId || undefined,
        title: topic ? `${topic} — AI generated` : "AI generated quiz",
        description: `Generated with the AI assistant. Review before publishing.`,
        status: "DRAFT",
      });
      for (const [i, q] of chosen.entries()) {
        await quizService.addQuestion(quiz.id, {
          questionText: q.question,
          points: q.marks,
          sequenceNumber: i,
          explanation: q.explanation,
          options: q.options.map((text, idx) => ({
            optionText: text,
            correct: idx === q.correctOption,
          })),
        });
      }
      return { quiz, added: chosen.length };
    },
    onSuccess: ({ added }) => {
      toast.success(`Added ${added} question${added === 1 ? "" : "s"} to a new draft quiz`);
      resetDrafts();
    },
    onError: (err: unknown) => {
      toast.error(err instanceof Error ? err.message : "Could not add the questions.");
    },
  });

  /** Creates DRAFT coding problems with their test cases. Never publishes. */
  const addCodingMutation = useMutation({
    mutationFn: async () => {
      const chosen = problems.filter((_, i) => selected.has(i));
      for (const p of chosen) {
        const created = await codingService.createProblem({
          courseId,
          batchId: batchId || undefined,
          title: p.title,
          description: p.description,
          difficulty: p.difficulty,
          language: (p.language ?? language).toLowerCase(),
          constraintsText: (p.constraints ?? []).join("\n") || undefined,
          status: "DRAFT",
        });
        for (const [i, tc] of p.testCases.entries()) {
          await codingService.addTestCase(created.id, {
            inputData: tc.input ?? undefined,
            expectedOutput: tc.expectedOutput,
            sample: tc.sample,
            sequenceNumber: i,
          });
        }
      }
      return chosen.length;
    },
    onSuccess: (added) => {
      toast.success(`Added ${added} problem${added === 1 ? "" : "s"} as drafts with test cases`);
      resetDrafts();
    },
    onError: (err: unknown) => {
      toast.error(err instanceof Error ? err.message : "Could not add the problems.");
    },
  });

  /** Creates DRAFT assignments through the existing assignment API. Never publishes. */
  const addAssignmentsMutation = useMutation({
    mutationFn: async () => {
      const chosen = assignments.filter((_, i) => selected.has(i));
      for (const a of chosen) {
        await assignmentService.createAssignment({
          courseId,
          batchId: batchId || undefined,
          title: a.title,
          description: a.description,
          points: a.points,
          dueDate: a.suggestedDueInDays
            ? new Date(Date.now() + a.suggestedDueInDays * 86400000).toISOString()
            : undefined,
          status: "DRAFT",
        });
      }
      return chosen.length;
    },
    onSuccess: (added) => {
      toast.success(`Added ${added} assignment${added === 1 ? "" : "s"} as drafts`);
      resetDrafts();
    },
    onError: (err: unknown) => {
      toast.error(err instanceof Error ? err.message : "Could not add the assignments.");
    },
  });

  const busy = generateMutation.isPending;
  const saving =
    addQuizMutation.isPending || addCodingMutation.isPending || addAssignmentsMutation.isPending;
  const hasDrafts = questions.length > 0 || problems.length > 0 || assignments.length > 0;
  const canGenerate = Boolean(courseId && topic.trim()) && !busy;

  function toggle(i: number) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(i)) next.delete(i);
      else next.add(i);
      return next;
    });
  }

  function removeAt(i: number) {
    if (type === "QUIZ") setQuestions((qs) => qs.filter((_, idx) => idx !== i));
    else if (type === "CODING") setProblems((ps) => ps.filter((_, idx) => idx !== i));
    else setAssignments((as) => as.filter((_, idx) => idx !== i));
    setSelected((prev) => {
      // Indices shift left after a removal, so rebuild rather than delete.
      const next = new Set<number>();
      prev.forEach((s) => {
        if (s < i) next.add(s);
        else if (s > i) next.add(s - 1);
      });
      return next;
    });
  }

  return (
    <>
      <PageHeader
        title="AI Question Assistant"
        description="Generate draft questions and coding problems for a course or batch you teach. Nothing is saved or published until you add it."
      />

      <Panel title="What should I generate?" description="">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <div className="space-y-1.5">
            <Label>Course</Label>
            {coursesQuery.isLoading ? (
              <Skeleton className="h-9 w-full" />
            ) : (
              <Select
                value={courseId}
                onValueChange={(v) => {
                  setCourseId(v);
                  setBatchId("");
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select course" />
                </SelectTrigger>
                <SelectContent>
                  {courses.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>

          <div className="space-y-1.5">
            <Label>Batch</Label>
            <Select value={batchId} onValueChange={setBatchId} disabled={!courseId}>
              <SelectTrigger>
                <SelectValue placeholder="Whole course" />
              </SelectTrigger>
              <SelectContent>
                {batches.map((b) => (
                  <SelectItem key={b.id} value={b.id}>
                    {b.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label>Type</Label>
            <div className="flex gap-2">
              {(["QUIZ", "CODING", "ASSIGNMENT"] as AiContentType[]).map((t) => (
                <Button
                  key={t}
                  type="button"
                  variant={type === t ? "default" : "outline"}
                  size="sm"
                  onClick={() => {
                    setType(t);
                    resetDrafts();
                  }}
                >
                  {t === "QUIZ" ? "Quiz" : t === "CODING" ? "Coding" : "Assignment"}
                </Button>
              ))}
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>Difficulty</Label>
            <div className="flex gap-2">
              {DIFFICULTIES.map((d) => (
                <Button
                  key={d}
                  type="button"
                  variant={difficulty === d ? "default" : "outline"}
                  size="sm"
                  onClick={() => setDifficulty(d)}
                >
                  {d.charAt(0) + d.slice(1).toLowerCase()}
                </Button>
              ))}
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="ai-topic">Topic</Label>
            <Input
              id="ai-topic"
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              placeholder="e.g. Java inheritance"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="ai-count">How many</Label>
            <Input
              id="ai-count"
              type="number"
              min={1}
              max={10}
              value={count}
              onChange={(e) => setCount(Math.min(10, Math.max(1, Number(e.target.value) || 1)))}
            />
          </div>

          {type === "CODING" ? (
            <div className="space-y-1.5">
              <Label htmlFor="ai-language">Language</Label>
              <Input
                id="ai-language"
                value={language}
                onChange={(e) => setLanguage(e.target.value)}
                placeholder="java"
              />
            </div>
          ) : null}

          <div className="space-y-1.5 md:col-span-2 xl:col-span-3">
            <Label htmlFor="ai-instructions">Additional instructions (optional)</Label>
            <Textarea
              id="ai-instructions"
              rows={2}
              value={instructions}
              onChange={(e) => setInstructions(e.target.value)}
              placeholder="e.g. focus on application-oriented questions rather than definitions"
            />
          </div>
        </div>

        <div className="mt-4 flex items-center gap-3 border-t pt-4">
          <Button onClick={() => generateMutation.mutate(undefined)} disabled={!canGenerate}>
            {busy ? (
              <>
                <Loader2 className="mr-1.5 size-4 animate-spin" aria-hidden /> Generating…
              </>
            ) : (
              <>
                <Wand2 className="mr-1.5 size-4" aria-hidden /> Generate
              </>
            )}
          </Button>
          {!courseId || !topic.trim() ? (
            <p className="text-sm text-muted-foreground">Pick a course and enter a topic.</p>
          ) : null}
        </div>
      </Panel>

      {warnings.length > 0 ? (
        <Panel title="Notes" description="">
          <ul className="list-inside list-disc text-sm text-muted-foreground">
            {warnings.map((w, i) => (
              <li key={i}>{w}</li>
            ))}
          </ul>
        </Panel>
      ) : null}

      {hasDrafts ? (
        <Panel
          title="Review"
          description="Edit, deselect or delete anything you don't want. Nothing is saved until you add it."
        >
          <div className="mb-4 flex flex-wrap items-end gap-2 border-b pb-4">
            <div className="min-w-64 flex-1 space-y-1.5">
              <Label htmlFor="ai-refine">Refine</Label>
              <Input
                id="ai-refine"
                value={refineText}
                onChange={(e) => setRefineText(e.target.value)}
                placeholder="e.g. make question 2 harder, or generate 3 more"
              />
            </div>
            <Button
              variant="outline"
              disabled={busy || !refineText.trim()}
              onClick={() => generateMutation.mutate(refineText)}
            >
              <RefreshCw className="mr-1.5 size-4" aria-hidden /> Regenerate
            </Button>
          </div>

          {type === "ASSIGNMENT" ? (
            <AssignmentDrafts
              assignments={assignments}
              selected={selected}
              onToggle={toggle}
              onRemove={removeAt}
              onChange={setAssignments}
            />
          ) : type === "QUIZ" ? (
            <QuizDrafts
              questions={questions}
              selected={selected}
              onToggle={toggle}
              onRemove={removeAt}
              onChange={setQuestions}
            />
          ) : (
            <CodingDrafts
              problems={problems}
              selected={selected}
              onToggle={toggle}
              onRemove={removeAt}
            />
          )}

          <div className="mt-6 flex flex-wrap items-center justify-between gap-3 border-t pt-4">
            <p className="text-sm text-muted-foreground">
              {selected.size} of{" "}
              {type === "QUIZ"
                ? questions.length
                : type === "CODING"
                  ? problems.length
                  : assignments.length}{" "}
              selected.
              Added as a <strong>draft</strong> — you publish from the existing page.
            </p>
            <Button
              disabled={selected.size === 0 || saving}
              onClick={() => {
                if (type === "QUIZ") addQuizMutation.mutate();
                else if (type === "CODING") addCodingMutation.mutate();
                else addAssignmentsMutation.mutate();
              }}
            >
              {saving ? (
                <>
                  <Loader2 className="mr-1.5 size-4 animate-spin" aria-hidden /> Adding…
                </>
              ) : (
                <>
                  <Check className="mr-1.5 size-4" aria-hidden />
                  {type === "QUIZ"
                    ? "Add selected questions"
                    : type === "CODING"
                      ? "Add selected problems"
                      : "Add selected assignments"}
                </>
              )}
            </Button>
          </div>
        </Panel>
      ) : busy ? (
        <Panel title="Review" description="">
          <div className="space-y-3">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
          </div>
        </Panel>
      ) : (
        <Panel title="Review" description="">
          <div className="flex flex-col items-center gap-2 py-10 text-center">
            <Sparkles className="size-8 text-muted-foreground" aria-hidden />
            <p className="text-sm text-muted-foreground">
              Generated questions will appear here for you to review.
            </p>
          </div>
        </Panel>
      )}
    </>
  );
}

/** Editable MCQ cards. Editing in place keeps the teacher in one screen. */
function QuizDrafts({
  questions,
  selected,
  onToggle,
  onRemove,
  onChange,
}: {
  questions: AiQuizQuestionDraft[];
  selected: Set<number>;
  onToggle: (i: number) => void;
  onRemove: (i: number) => void;
  onChange: (qs: AiQuizQuestionDraft[]) => void;
}) {
  function update(i: number, patch: Partial<AiQuizQuestionDraft>) {
    onChange(questions.map((q, idx) => (idx === i ? { ...q, ...patch } : q)));
  }

  return (
    <ol className="flex flex-col gap-4">
      {questions.map((q, i) => (
        <li key={i} className={`rounded-md border p-3 ${selected.has(i) ? "" : "opacity-55"}`}>
          <div className="mb-2 flex items-start justify-between gap-3">
            <Textarea
              rows={2}
              value={q.question}
              onChange={(e) => update(i, { question: e.target.value })}
              className="font-medium"
            />
            <div className="flex shrink-0 gap-1">
              <Button
                size="sm"
                variant={selected.has(i) ? "default" : "outline"}
                onClick={() => onToggle(i)}
              >
                {selected.has(i) ? "Selected" : "Select"}
              </Button>
              <Button size="sm" variant="ghost" onClick={() => onRemove(i)} aria-label="Delete">
                <Trash2 className="size-4" aria-hidden />
              </Button>
            </div>
          </div>

          <div className="flex flex-col gap-2">
            {q.options.map((opt, oi) => (
              <label
                key={oi}
                className={`flex cursor-pointer items-center gap-3 rounded-md border p-2 text-sm ${
                  q.correctOption === oi ? "border-primary bg-primary/5" : ""
                }`}
              >
                <input
                  type="radio"
                  name={`ai-q-${i}`}
                  checked={q.correctOption === oi}
                  onChange={() => update(i, { correctOption: oi })}
                  className="size-4 shrink-0"
                />
                <Input
                  value={opt}
                  onChange={(e) =>
                    update(i, {
                      options: q.options.map((o, idx) => (idx === oi ? e.target.value : o)),
                    })
                  }
                  className="border-0 shadow-none focus-visible:ring-0"
                />
              </label>
            ))}
          </div>

          <div className="mt-2 flex flex-wrap items-center gap-3">
            <Badge variant="secondary">{q.difficulty}</Badge>
            <div className="flex items-center gap-1.5">
              <Label htmlFor={`ai-marks-${i}`} className="text-xs">
                Marks
              </Label>
              <Input
                id={`ai-marks-${i}`}
                type="number"
                min={1}
                value={q.marks}
                onChange={(e) => update(i, { marks: Math.max(1, Number(e.target.value) || 1) })}
                className="h-8 w-20"
              />
            </div>
          </div>

          <Textarea
            rows={2}
            value={q.explanation}
            onChange={(e) => update(i, { explanation: e.target.value })}
            className="mt-2 text-sm"
            placeholder="Explanation"
          />
        </li>
      ))}
    </ol>
  );
}

/** Coding problem cards, with the sample/hidden split shown explicitly. */
function CodingDrafts({
  problems,
  selected,
  onToggle,
  onRemove,
}: {
  problems: AiCodingProblemDraft[];
  selected: Set<number>;
  onToggle: (i: number) => void;
  onRemove: (i: number) => void;
}) {
  return (
    <ol className="flex flex-col gap-4">
      {problems.map((p, i) => (
        <li key={i} className={`rounded-md border p-3 ${selected.has(i) ? "" : "opacity-55"}`}>
          <div className="mb-2 flex items-start justify-between gap-3">
            <div>
              <p className="font-medium">{p.title}</p>
              <div className="mt-1 flex gap-2">
                <Badge variant="secondary">{p.difficulty}</Badge>
                <Badge variant="outline">{p.language}</Badge>
                <Badge variant="outline">{p.testCases.length} test cases</Badge>
              </div>
            </div>
            <div className="flex shrink-0 gap-1">
              <Button
                size="sm"
                variant={selected.has(i) ? "default" : "outline"}
                onClick={() => onToggle(i)}
              >
                {selected.has(i) ? "Selected" : "Select"}
              </Button>
              <Button size="sm" variant="ghost" onClick={() => onRemove(i)} aria-label="Delete">
                <Trash2 className="size-4" aria-hidden />
              </Button>
            </div>
          </div>

          <p className="whitespace-pre-wrap text-sm text-muted-foreground">{p.description}</p>

          {p.constraints && p.constraints.length > 0 ? (
            <ul className="mt-2 list-inside list-disc text-sm text-muted-foreground">
              {p.constraints.map((c, ci) => (
                <li key={ci}>{c}</li>
              ))}
            </ul>
          ) : null}

          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            <div>
              <p className="text-xs font-medium uppercase text-muted-foreground">Sample input</p>
              <pre className="mt-1 overflow-x-auto rounded bg-muted p-2 text-xs">
                {p.sampleInput}
              </pre>
            </div>
            <div>
              <p className="text-xs font-medium uppercase text-muted-foreground">Sample output</p>
              <pre className="mt-1 overflow-x-auto rounded bg-muted p-2 text-xs">
                {p.sampleOutput}
              </pre>
            </div>
          </div>

          <div className="mt-3">
            <p className="text-xs font-medium uppercase text-muted-foreground">
              Test cases ({p.testCases.filter((t) => t.sample).length} sample,{" "}
              {p.testCases.filter((t) => !t.sample).length} hidden)
            </p>
            <div className="mt-1 flex flex-col gap-1">
              {p.testCases.map((tc, ti) => (
                <div key={ti} className="flex items-center gap-2 text-xs">
                  <Badge variant={tc.sample ? "secondary" : "outline"}>
                    {tc.sample ? "Sample" : "Hidden"}
                  </Badge>
                  <code className="truncate text-muted-foreground">
                    {(tc.input ?? "").replace(/\n/g, " ⏎ ")} → {tc.expectedOutput}
                  </code>
                </div>
              ))}
            </div>
          </div>
        </li>
      ))}
    </ol>
  );
}

/** Editable assignment briefs. Added as DRAFT through the existing assignment API. */
function AssignmentDrafts({
  assignments,
  selected,
  onToggle,
  onRemove,
  onChange,
}: {
  assignments: AiAssignmentDraft[];
  selected: Set<number>;
  onToggle: (i: number) => void;
  onRemove: (i: number) => void;
  onChange: (next: AiAssignmentDraft[]) => void;
}) {
  function update(i: number, patch: Partial<AiAssignmentDraft>) {
    onChange(assignments.map((a, idx) => (idx === i ? { ...a, ...patch } : a)));
  }

  return (
    <ol className="flex flex-col gap-4">
      {assignments.map((a, i) => (
        <li key={i} className={`rounded-md border p-3 ${selected.has(i) ? "" : "opacity-55"}`}>
          <div className="mb-2 flex items-start justify-between gap-3">
            <Input
              value={a.title}
              onChange={(e) => update(i, { title: e.target.value })}
              className="font-medium"
            />
            <div className="flex shrink-0 gap-1">
              <Button
                size="sm"
                variant={selected.has(i) ? "default" : "outline"}
                onClick={() => onToggle(i)}
              >
                {selected.has(i) ? "Selected" : "Select"}
              </Button>
              <Button size="sm" variant="ghost" onClick={() => onRemove(i)} aria-label="Delete">
                <Trash2 className="size-4" aria-hidden />
              </Button>
            </div>
          </div>

          <Textarea
            rows={4}
            value={a.description}
            onChange={(e) => update(i, { description: e.target.value })}
            className="text-sm"
          />

          <div className="mt-2 flex flex-wrap items-center gap-3">
            <Badge variant="secondary">{a.difficulty}</Badge>
            <div className="flex items-center gap-1.5">
              <Label htmlFor={`ai-points-${i}`} className="text-xs">
                Marks
              </Label>
              <Input
                id={`ai-points-${i}`}
                type="number"
                min={1}
                value={a.points}
                onChange={(e) => update(i, { points: Math.max(1, Number(e.target.value) || 1) })}
                className="h-8 w-24"
              />
            </div>
            {a.suggestedDueInDays ? (
              <span className="text-xs text-muted-foreground">
                Suggested due in {a.suggestedDueInDays} days
              </span>
            ) : null}
          </div>
        </li>
      ))}
    </ol>
  );
}
