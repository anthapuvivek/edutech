import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { CalendarClock } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
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
import { assignmentService } from "@/services/assignment.service";
import { submissionTone } from "@/types/assignment";
import type { Assignment } from "@/types/assignment";

export const Route = createFileRoute("/student/assignments/")({
  head: () => ({
    meta: [
      { title: "Assignments — Learntrix Student" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: StudentAssignmentsPage,
});

function StudentAssignmentsPage() {
  const [filter, setFilter] = useState("ALL");

  const query = useQuery({
    queryKey: ["student", "assignments"],
    queryFn: () => assignmentService.studentAssignments(),
  });

  const assignments = query.data ?? [];
  const visible = useMemo(() => {
    if (filter === "PENDING") return assignments.filter((a) => !a.submitted);
    if (filter === "SUBMITTED") return assignments.filter((a) => a.submitted && a.grade == null);
    if (filter === "GRADED") return assignments.filter((a) => a.grade != null);
    return assignments;
  }, [assignments, filter]);

  const pending = assignments.filter((a) => !a.submitted).length;
  const graded = assignments.filter((a) => a.grade != null).length;

  if (query.isLoading) {
    return (
      <>
        <PageHeader title="Assignments" description="" />
        <div className="space-y-3">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      </>
    );
  }

  if (query.isError) {
    return (
      <>
        <PageHeader title="Assignments" description="" />
        <Panel title="Could not load assignments" description="">
          <p className="py-6 text-sm text-muted-foreground">
            Something went wrong loading your assignments. Refresh the page to try again.
          </p>
        </Panel>
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="Assignments"
        description="Work set for your course and batch. Submit a link to your work before the due date."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard label="Assignments" value={String(assignments.length)} />
        <StatCard label="Not submitted" value={String(pending)} />
        <StatCard label="Graded" value={String(graded)} />
      </section>

      <Panel
        title="Your assignments"
        description=""
        action={
          <Select value={filter} onValueChange={setFilter}>
            <SelectTrigger className="w-44">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All</SelectItem>
              <SelectItem value="PENDING">Not submitted</SelectItem>
              <SelectItem value="SUBMITTED">Awaiting grade</SelectItem>
              <SelectItem value="GRADED">Graded</SelectItem>
            </SelectContent>
          </Select>
        }
      >
        {visible.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            {assignments.length === 0
              ? "No assignments available yet. Your trainer will post work here."
              : "Nothing matches this filter."}
          </p>
        ) : (
          <ol className="flex flex-col gap-4">
            {visible.map((a) => (
              <AssignmentCard key={a.id} assignment={a} />
            ))}
          </ol>
        )}
      </Panel>
    </>
  );
}

/** One assignment, with its submission form inline. */
function AssignmentCard({ assignment }: { assignment: Assignment }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [url, setUrl] = useState(assignment.submitted ? "" : "");
  const [notes, setNotes] = useState("");

  const overdue =
    !assignment.submitted &&
    assignment.dueDate != null &&
    new Date(assignment.dueDate).getTime() < Date.now();

  const submitMutation = useMutation({
    mutationFn: () =>
      assignmentService.submitAssignment(assignment.id, {
        submissionUrl: url.trim(),
        notes: notes.trim() || undefined,
      }),
    onSuccess: () => {
      toast.success("Submitted. Your trainer can see it now.");
      setOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["student", "assignments"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof Error ? err.message : "Could not submit. Please try again."),
  });

  function handleSubmit() {
    if (!url.trim()) {
      toast.error("Add a link to your work first.");
      return;
    }
    submitMutation.mutate();
  }

  return (
    <li className="rounded-md border p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-medium">{assignment.title}</p>
          <p className="mt-0.5 text-sm text-muted-foreground">
            {assignment.courseTitle}
            {assignment.batchName ? ` · ${assignment.batchName}` : ""}
            {assignment.teacherName ? ` · ${assignment.teacherName}` : ""}
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <Badge variant={submissionTone(assignment.submissionStatus)}>
            {assignment.submissionStatus ?? "Not Submitted"}
          </Badge>
          <Badge variant="outline">{assignment.points ?? 100} marks</Badge>
        </div>
      </div>

      {assignment.description ? (
        <p className="mt-3 whitespace-pre-wrap text-sm text-muted-foreground">
          {assignment.description}
        </p>
      ) : null}

      <div className="mt-3 flex flex-wrap items-center gap-4 text-sm">
        <span className={`flex items-center gap-1.5 ${overdue ? "text-destructive" : "text-muted-foreground"}`}>
          <CalendarClock className="size-4" aria-hidden />
          {assignment.dueDate
            ? `Due ${new Date(assignment.dueDate).toLocaleString()}`
            : "No due date"}
          {overdue ? " — overdue" : ""}
        </span>
        {assignment.grade != null ? (
          <span className="font-medium">
            Grade: {assignment.grade}/{assignment.points ?? 100}
          </span>
        ) : null}
      </div>

      {assignment.feedback ? (
        <div className="mt-3 rounded-md border-l-2 border-primary bg-muted/40 p-3">
          <p className="text-xs font-medium uppercase text-muted-foreground">Trainer feedback</p>
          <p className="mt-1 text-sm">{assignment.feedback}</p>
        </div>
      ) : null}

      <div className="mt-4 flex items-center gap-2 border-t pt-3">
        <Button size="sm" variant={assignment.submitted ? "outline" : "default"} onClick={() => setOpen((v) => !v)}>
          {open ? "Cancel" : assignment.submitted ? "Resubmit" : "Submit work"}
        </Button>
        {assignment.submitted ? (
          <span className="text-xs text-muted-foreground">
            Resubmitting replaces your previous submission.
          </span>
        ) : null}
      </div>

      {open ? (
        <div className="mt-3 space-y-3">
          <div className="space-y-1.5">
            <Label htmlFor={`url-${assignment.id}`}>Link to your work</Label>
            <Input
              id={`url-${assignment.id}`}
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://github.com/you/your-work"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor={`notes-${assignment.id}`}>Notes (optional)</Label>
            <Textarea
              id={`notes-${assignment.id}`}
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
          <Button size="sm" onClick={handleSubmit} disabled={submitMutation.isPending}>
            {submitMutation.isPending ? "Submitting…" : "Submit"}
          </Button>
        </div>
      ) : null}
    </li>
  );
}
