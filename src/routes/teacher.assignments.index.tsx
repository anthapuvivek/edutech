import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
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
import { assignmentService } from "@/services/assignment.service";
import { teacherService } from "@/services/teacher.service";
import { submissionTone } from "@/types/assignment";

export const Route = createFileRoute("/teacher/assignments/")({
  head: () => ({
    meta: [
      { title: "Assignments — Learntrix Trainer" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: TeacherAssignmentsPage,
});

function TeacherAssignmentsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState("ALL");

  const [courseId, setCourseId] = useState("");
  const [batchId, setBatchId] = useState("");

  const assignmentsQuery = useQuery({
    queryKey: ["teacher", "assignments"],
    queryFn: () => assignmentService.teacherAssignments(),
  });
  const coursesQuery = useQuery({
    queryKey: ["teacher", "courses"],
    queryFn: () => teacherService.courses(),
  });
  const batchesQuery = useQuery({
    queryKey: ["teacher", "batches"],
    queryFn: () => teacherService.batches(),
  });

  const courses = coursesQuery.data ?? [];
  // A batch belongs to one course; offering the rest only earns a BATCH_COURSE_MISMATCH.
  const batches = useMemo(
    () => (batchesQuery.data ?? []).filter((b) => !courseId || b.courseId === courseId),
    [batchesQuery.data, courseId],
  );

  const assignments = assignmentsQuery.data ?? [];
  const visible = useMemo(
    () => (statusFilter === "ALL" ? assignments : assignments.filter((a) => a.status === statusFilter)),
    [assignments, statusFilter],
  );

  const published = assignments.filter((a) => a.status === "PUBLISHED").length;
  const awaitingGrading = assignments.reduce(
    (n, a) => n + Math.max(0, (a.submittedCount ?? 0) - (a.gradedCount ?? 0)),
    0,
  );

  const publishMutation = useMutation({
    mutationFn: ({ id, publish }: { id: string; publish: boolean }) =>
      publish ? assignmentService.publishAssignment(id) : assignmentService.unpublishAssignment(id),
    onSuccess: (_d, v) => {
      toast.success(v.publish ? "Assignment published" : "Assignment unpublished");
      void queryClient.invalidateQueries({ queryKey: ["teacher", "assignments"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof Error ? err.message : "Could not change the status."),
  });

  async function handleCreate(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const due = String(form.get("dueDate") ?? "");
    if (!courseId) {
      toast.error("Select a course first.");
      return;
    }
    setSaving(true);
    try {
      await assignmentService.createAssignment({
        courseId,
        batchId: batchId || undefined,
        title: String(form.get("title") ?? ""),
        description: String(form.get("description") ?? ""),
        // datetime-local has no timezone; the backend stores an Instant.
        dueDate: due ? new Date(due).toISOString() : undefined,
        points: Number(form.get("points") ?? 100),
        status: "DRAFT",
      });
      toast.success("Assignment saved as a draft");
      setOpen(false);
      setCourseId("");
      setBatchId("");
      void queryClient.invalidateQueries({ queryKey: ["teacher", "assignments"] });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Could not create the assignment.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <PageHeader
        title="Assignments"
        description="Set work for a course or a single batch. Assignments stay as drafts until you publish them."
        action={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="mr-1.5 size-4" aria-hidden /> New assignment
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl">
              <DialogHeader>
                <DialogTitle>Create assignment</DialogTitle>
                <DialogDescription>
                  Saved as a draft. Students see nothing until you publish.
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleCreate} className="space-y-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label>Course</Label>
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
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="asg-title">Title</Label>
                  <Input id="asg-title" name="title" required placeholder="e.g. Week 1 — Collections" />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="asg-desc">Instructions</Label>
                  <Textarea id="asg-desc" name="description" rows={4} />
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="asg-due">Due date</Label>
                    <Input id="asg-due" name="dueDate" type="datetime-local" />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="asg-points">Maximum marks</Label>
                    <Input id="asg-points" name="points" type="number" min={1} defaultValue={100} />
                  </div>
                </div>

                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setOpen(false)} disabled={saving}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={saving}>
                    {saving ? "Saving…" : "Save draft"}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Assignments" value={String(assignments.length)} />
        <StatCard label="Published" value={String(published)} />
        <StatCard label="Drafts" value={String(assignments.length - published)} />
        <StatCard label="Awaiting grading" value={String(awaitingGrading)} />
      </section>

      <Panel
        title="All assignments"
        description="Counts are of students, not submissions — resubmitting does not count twice."
        action={
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All statuses</SelectItem>
              <SelectItem value="PUBLISHED">Published</SelectItem>
              <SelectItem value="DRAFT">Draft</SelectItem>
            </SelectContent>
          </Select>
        }
      >
        {assignmentsQuery.isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </div>
        ) : assignmentsQuery.isError ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Failed to load assignments. Refresh to try again.
          </p>
        ) : visible.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            {assignments.length === 0
              ? "No assignments created yet. Use “New assignment” to set your first one."
              : "No assignments match this filter."}
          </p>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Title</TableHead>
                  <TableHead>Course</TableHead>
                  <TableHead>Scope</TableHead>
                  <TableHead>Due</TableHead>
                  <TableHead className="text-right">Marks</TableHead>
                  <TableHead className="text-right">Submitted</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {visible.map((a) => (
                  <TableRow key={a.id}>
                    <TableCell className="font-medium">{a.title}</TableCell>
                    <TableCell className="max-w-48 truncate">{a.courseTitle}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {a.batchName ?? "Whole course"}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {a.dueDate ? new Date(a.dueDate).toLocaleDateString() : "—"}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{a.points ?? 100}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      {a.submittedCount ?? 0}/{a.totalStudents ?? 0}
                      {a.lateCount ? (
                        <span className="ml-1 text-xs text-destructive">({a.lateCount} late)</span>
                      ) : null}
                    </TableCell>
                    <TableCell>
                      <Badge variant={a.status === "PUBLISHED" ? "default" : "outline"}>
                        {a.status === "PUBLISHED" ? "Published" : "Draft"}
                      </Badge>
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={publishMutation.isPending}
                        onClick={() =>
                          publishMutation.mutate({ id: a.id, publish: a.status !== "PUBLISHED" })
                        }
                      >
                        {a.status === "PUBLISHED" ? "Unpublish" : "Publish"}
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setExpanded((c) => (c === a.id ? null : a.id))}
                      >
                        Submissions
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Panel>

      {expanded ? <AssignmentSubmissions assignmentId={expanded} /> : null}
    </>
  );
}

/** Submission list with inline grading for one assignment. */
function AssignmentSubmissions({ assignmentId }: { assignmentId: string }) {
  const queryClient = useQueryClient();
  const [grades, setGrades] = useState<Record<string, string>>({});
  const [feedback, setFeedback] = useState<Record<string, string>>({});

  const assignmentQuery = useQuery({
    queryKey: ["teacher", "assignment", assignmentId],
    queryFn: () => assignmentService.teacherAssignment(assignmentId),
  });
  const submissionsQuery = useQuery({
    queryKey: ["teacher", "assignment-submissions", assignmentId],
    queryFn: () => assignmentService.submissions(assignmentId),
  });

  const maxMarks = assignmentQuery.data?.points ?? 100;

  const gradeMutation = useMutation({
    mutationFn: ({ submissionId, grade, fb }: { submissionId: string; grade: number; fb: string }) =>
      assignmentService.gradeSubmission(assignmentId, submissionId, { grade, feedback: fb }),
    onSuccess: () => {
      toast.success("Grade saved");
      void queryClient.invalidateQueries({ queryKey: ["teacher", "assignment-submissions", assignmentId] });
      void queryClient.invalidateQueries({ queryKey: ["teacher", "assignments"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof Error ? err.message : "Could not save the grade."),
  });

  function save(submissionId: string) {
    const raw = grades[submissionId];
    const value = Number(raw);
    // Mirror the server rule so the teacher gets the message before the round trip.
    if (raw === undefined || raw === "" || Number.isNaN(value)) {
      toast.error("Enter a grade first.");
      return;
    }
    if (value < 0 || value > maxMarks) {
      toast.error(`Grade must be between 0 and ${maxMarks}.`);
      return;
    }
    gradeMutation.mutate({ submissionId, grade: value, fb: feedback[submissionId] ?? "" });
  }

  if (submissionsQuery.isLoading) {
    return (
      <Panel title="Submissions" description="">
        <Skeleton className="h-24 w-full" />
      </Panel>
    );
  }

  const submissions = submissionsQuery.data ?? [];

  return (
    <Panel
      title={`Submissions — ${assignmentQuery.data?.title ?? "Assignment"}`}
      description={`Out of ${maxMarks} marks.`}
    >
      {submissionsQuery.isError ? (
        <p className="py-6 text-sm text-muted-foreground">Could not load submissions.</p>
      ) : submissions.length === 0 ? (
        <p className="py-6 text-sm text-muted-foreground">No submissions yet.</p>
      ) : (
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Student</TableHead>
                <TableHead>Submitted</TableHead>
                <TableHead>Work</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="w-64">Grade &amp; feedback</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {submissions.map((s) => (
                <TableRow key={s.id}>
                  <TableCell className="font-medium">
                    {s.studentName}
                    {s.studentEmail ? (
                      <span className="block text-xs text-muted-foreground">{s.studentEmail}</span>
                    ) : null}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {s.submittedAt ? new Date(s.submittedAt).toLocaleString() : "—"}
                  </TableCell>
                  <TableCell>
                    {s.submissionUrl ? (
                      <a
                        href={s.submissionUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-sm underline underline-offset-2"
                      >
                        Open
                      </a>
                    ) : (
                      <span className="text-sm text-muted-foreground">—</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <Badge variant={submissionTone(s.status)}>{s.status}</Badge>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col gap-2">
                      <Input
                        type="number"
                        min={0}
                        max={maxMarks}
                        placeholder={`0 – ${maxMarks}`}
                        defaultValue={s.grade ?? ""}
                        onChange={(e) =>
                          setGrades((p) => ({ ...p, [s.id]: e.target.value }))
                        }
                        className="h-8"
                      />
                      <Input
                        placeholder="Feedback"
                        defaultValue={s.feedback ?? ""}
                        onChange={(e) =>
                          setFeedback((p) => ({ ...p, [s.id]: e.target.value }))
                        }
                        className="h-8"
                      />
                      <Button size="sm" onClick={() => save(s.id)} disabled={gradeMutation.isPending}>
                        Save grade
                      </Button>
                    </div>
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
