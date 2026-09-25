import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { toast } from "sonner";
import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { codingAssignmentService } from "@/services/coding.service";
import { adminService } from "@/services/admin.service";
import { teacherService } from "@/services/teacher.service";
import { useAuth } from "@/hooks/useAuth";
import {
  CODING_DIFFICULTIES,
  CODING_PLATFORMS,
  platformLabel,
  type ExternalCodingProblemPayload,
} from "@/types/coding";

export const Route = createFileRoute("/teacher/coding-problems")({
  component: TeacherCodingProblems,
});

const empty: ExternalCodingProblemPayload = {
  courseId: "",
  batchId: "",
  platform: "LEETCODE",
  problemUrl: "",
  title: "",
  difficulty: "EASY",
  topics: "",
  description: "",
  active: false,
  deadline: "",
};

export function TeacherCodingProblems() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const [form, setForm] = useState(empty);
  const [editing, setEditing] = useState<string | null>(null);
  const [progressId, setProgressId] = useState<string | null>(null);
  const problemsQ = useQuery({
    queryKey: ["teacher", "coding-assignments"],
    queryFn: codingAssignmentService.teacherList,
  });
  const isAdmin = user?.role === "admin" || user?.role === "super_admin";
  const batchesQ = useQuery({
    queryKey: [isAdmin ? "admin" : "teacher", "batches", "coding"],
    queryFn: async () => {
      if (!isAdmin) return teacherService.batches();
      return (await adminService.batches())
        .filter((b) => b.courseId)
        .map((b) => ({
          id: b.id,
          name: b.name,
          courseId: b.courseId!,
          courseTitle: b.courseTitle,
          teacherId: b.teacherId ?? "",
          teacherName: b.teacherName ?? b.trainerName,
          capacity: b.capacity,
          status: b.status,
          startDate: b.startDate,
          endDate: b.endDate,
          studentCount: b.enrolled,
        }));
    },
  });
  const progressQ = useQuery({
    queryKey: ["teacher", "coding-progress", progressId],
    queryFn: () => codingAssignmentService.progress(progressId!),
    enabled: !!progressId,
  });
  const refresh = () => void qc.invalidateQueries({ queryKey: ["teacher", "coding-assignments"] });
  const save = useMutation({
    mutationFn: () =>
      editing
        ? codingAssignmentService.update(editing, form)
        : codingAssignmentService.create(form),
    onSuccess: () => {
      toast.success(editing ? "Problem updated." : "Problem created.");
      setEditing(null);
      setForm(empty);
      refresh();
    },
    onError: (e: Error) => toast.error(e.message || "Could not save problem."),
  });
  const toggle = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      codingAssignmentService.setActive(id, active),
    onSuccess: refresh,
    onError: (e: Error) => toast.error(e.message),
  });
  const archive = useMutation({
    mutationFn: codingAssignmentService.remove,
    onSuccess: () => {
      toast.success("Problem archived.");
      refresh();
    },
  });
  const problems = problemsQ.data ?? [];
  const batches = batchesQ.data ?? [];
  const set = (key: keyof ExternalCodingProblemPayload, value: string | boolean) =>
    setForm((f) => ({ ...f, [key]: value }));
  const selectBatch = (id: string) => {
    const b = batches.find((x) => x.id === id);
    setForm((f) => ({ ...f, batchId: id, courseId: b?.courseId ?? "" }));
  };

  return (
    <>
      <PageHeader
        title="Coding Practice"
        description="Assign external coding problems to the batches you manage."
      />
      <section className="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard label="Problems" value={problems.length} />
        <StatCard
          label="Published"
          value={problems.filter((p) => p.status === "PUBLISHED").length}
        />
        <StatCard
          label="Completed marks"
          value={problems.reduce((n, p) => n + (p.completedCount ?? 0), 0)}
        />
      </section>
      <Panel
        title={editing ? "Edit Problem" : "Add Problem"}
        description="Students open the link on the selected platform; LearntriX does not execute code."
      >
        <div className="grid gap-4 md:grid-cols-2">
          <Field label="Title">
            <Input value={form.title} onChange={(e) => set("title", e.target.value)} />
          </Field>
          <Field label="Batch">
            <select
              className="h-10 w-full rounded-md border bg-background px-3"
              value={form.batchId}
              onChange={(e) => selectBatch(e.target.value)}
            >
              <option value="">Select batch</option>
              {batches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} — {b.courseTitle}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Platform">
            <select
              className="h-10 w-full rounded-md border bg-background px-3"
              value={form.platform}
              onChange={(e) => set("platform", e.target.value)}
            >
              {CODING_PLATFORMS.map((p) => (
                <option key={p.value} value={p.value}>
                  {p.label}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Problem URL">
            <Input
              type="url"
              placeholder="https://..."
              value={form.problemUrl}
              onChange={(e) => set("problemUrl", e.target.value)}
            />
          </Field>
          <Field label="Difficulty">
            <select
              className="h-10 w-full rounded-md border bg-background px-3"
              value={form.difficulty}
              onChange={(e) => set("difficulty", e.target.value)}
            >
              {CODING_DIFFICULTIES.map((d) => (
                <option key={d}>{d}</option>
              ))}
            </select>
          </Field>
          <Field label="Topic">
            <Input value={form.topics} onChange={(e) => set("topics", e.target.value)} />
          </Field>
          <Field label="Deadline">
            <Input
              type="date"
              value={form.deadline}
              onChange={(e) => set("deadline", e.target.value)}
            />
          </Field>
          <label className="flex items-end gap-2 pb-2 text-sm">
            <input
              type="checkbox"
              checked={form.active}
              onChange={(e) => set("active", e.target.checked)}
            />{" "}
            Publish immediately
          </label>
          <div className="md:col-span-2">
            <Field label="Description (optional)">
              <Textarea
                value={form.description}
                onChange={(e) => set("description", e.target.value)}
              />
            </Field>
          </div>
        </div>
        <div className="mt-4 flex gap-2">
          <Button
            disabled={
              save.isPending || !form.title.trim() || !form.problemUrl.trim() || !form.batchId
            }
            onClick={() => save.mutate()}
          >
            {save.isPending ? "Saving…" : editing ? "Update Problem" : "Add Problem"}
          </Button>
          {editing && (
            <Button
              variant="outline"
              onClick={() => {
                setEditing(null);
                setForm(empty);
              }}
            >
              Cancel
            </Button>
          )}
        </div>
      </Panel>
      <Panel
        className="mt-6"
        title="Problems"
        description="Only problems in batches you are authorized to manage are listed."
      >
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Title</TableHead>
                <TableHead>Platform</TableHead>
                <TableHead>Difficulty</TableHead>
                <TableHead>Topic</TableHead>
                <TableHead>Batch</TableHead>
                <TableHead>Deadline</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {problems.map((p) => (
                <TableRow key={p.id}>
                  <TableCell className="font-medium">{p.title}</TableCell>
                  <TableCell>{platformLabel(p.platform)}</TableCell>
                  <TableCell>{p.difficulty}</TableCell>
                  <TableCell>{p.topics || "—"}</TableCell>
                  <TableCell>{p.batchName}</TableCell>
                  <TableCell>
                    {p.deadline ? new Date(`${p.deadline}T00:00:00`).toLocaleDateString() : "—"}
                  </TableCell>
                  <TableCell>
                    <StatusBadge value={p.status ?? "DRAFT"} />
                  </TableCell>
                  <TableCell className="space-x-1">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => {
                        setEditing(p.id);
                        setForm({
                          courseId: p.courseId,
                          batchId: p.batchId!,
                          platform: p.platform,
                          problemUrl: p.problemUrl,
                          title: p.title,
                          difficulty: p.difficulty ?? "EASY",
                          topics: p.topics ?? "",
                          description: p.description ?? "",
                          active: p.active,
                          deadline: p.deadline ?? "",
                        });
                      }}
                    >
                      Edit
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => toggle.mutate({ id: p.id, active: !p.active })}
                    >
                      {p.active ? "Unpublish" : "Publish"}
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => setProgressId(p.id)}>
                      Progress
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => archive.mutate(p.id)}>
                      Archive
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </Panel>
      {progressId && (
        <Panel
          className="mt-6"
          title={`Progress — ${progressQ.data?.problemTitle ?? "Loading…"}`}
          action={
            <Button variant="ghost" onClick={() => setProgressId(null)}>
              Close
            </Button>
          }
        >
          <div className="mb-4 grid gap-3 sm:grid-cols-4">
            <StatCard label="Total Students" value={progressQ.data?.totalStudents ?? 0} />
            <StatCard label="Completed" value={progressQ.data?.completed ?? 0} />
            <StatCard label="In Progress" value={progressQ.data?.inProgress ?? 0} />
            <StatCard label="Not Started" value={progressQ.data?.notStarted ?? 0} />
          </div>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Student</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Opened At</TableHead>
                <TableHead>Completed At</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {progressQ.data?.students.map((s) => (
                <TableRow key={s.studentId}>
                  <TableCell>
                    {s.studentName}
                    <span className="block text-xs text-muted-foreground">{s.studentEmail}</span>
                  </TableCell>
                  <TableCell>
                    <StatusBadge value={s.status} />
                  </TableCell>
                  <TableCell>{s.openedAt ? new Date(s.openedAt).toLocaleString() : "—"}</TableCell>
                  <TableCell>
                    {s.completedAt ? new Date(s.completedAt).toLocaleString() : "—"}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Panel>
      )}
    </>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="space-y-1.5">
      <Label>{label}</Label>
      {children}
    </div>
  );
}
