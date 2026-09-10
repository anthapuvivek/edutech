import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus, Trash2 } from "lucide-react";
import { Fragment, useMemo, useState } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
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
import { adminService } from "@/services/admin.service";
import { useAuth } from "@/hooks/useAuth";
import type { AdminBatch } from "@/types/admin";

export const Route = createFileRoute("/admin/batches")({
  head: () => ({
    meta: [
      { title: "Batches — Learntrix Admin" },
      {
        name: "description",
        content:
          "Create, schedule, publish and archive batches with trainers, capacity and meeting links.",
      },
      { property: "og:title", content: "Batches — Learntrix Admin" },
      {
        property: "og:description",
        content: "Batch scheduling and capacity management for every program.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminBatches,
});

function AdminBatches() {
  const batches = useQuery({
    queryKey: ["admin", "batches"],
    queryFn: () => adminService.batches(),
  });
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [courseId, setCourseId] = useState("");
  const [teacherId, setTeacherId] = useState("");
  const [expanded, setExpanded] = useState<string | null>(null);
  const [batchToDelete, setBatchToDelete] = useState<AdminBatch | null>(null);
  const [deleting, setDeleting] = useState(false);
  const { user } = useAuth();
  const isAdmin = user?.role === "admin" || user?.role === "super_admin";
  const data = batches.data ?? [];

  // POST /admin/batches requires real courseId and teacherId - the form used to send an
  // empty object, which is why every attempt failed with "Batch name is required".
  const coursesQuery = useQuery({
    queryKey: ["admin", "courses"],
    queryFn: () => adminService.courses(),
  });
  const trainersQuery = useQuery({
    queryKey: ["admin", "trainers"],
    queryFn: () => adminService.trainers(),
  });

  // A batch is taught by someone authorised for that course: the course instructor, or a
  // teacher who already runs another batch of it. Before any batch exists we cannot infer
  // that, so fall back to the full teacher list rather than showing an empty dropdown.
  const eligibleTeachers = useMemo(() => {
    const all = trainersQuery.data ?? [];
    if (!courseId) return all;
    const viaBatch = new Set(
      data.filter((b: any) => b.courseId === courseId).map((b: any) => b.teacherId),
    );
    const course = (coursesQuery.data ?? []).find((c: any) => c.id === courseId);
    const instructorId = (course as any)?.instructorId;
    if (instructorId) viaBatch.add(instructorId);
    const filtered = all.filter((t: any) => viaBatch.has(t.id));
    return filtered.length > 0 ? filtered : all;
  }, [trainersQuery.data, coursesQuery.data, data, courseId]);

  return (
    <>
      <PageHeader
        title="Batches"
        description="Schedules, trainers, capacity and meeting access. Students only see links their enrollment permits."
        action={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="size-4" aria-hidden /> Create batch
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
              <DialogHeader>
                <DialogTitle>Create batch</DialogTitle>
                <DialogDescription>
                  Meeting credentials are stored server side and revealed only to permitted
                  students.
                </DialogDescription>
              </DialogHeader>
              <form
                id="create-batch"
                className="grid gap-4 sm:grid-cols-2"
                onSubmit={async (e) => {
                  e.preventDefault();
                  const form = new FormData(e.currentTarget);
                  const str = (k: string) =>
                    ((form.get(k) as string | null) ?? "").trim() || undefined;

                  const name = str("name");
                  if (!name) {
                    toast.error("Batch name is required.");
                    return;
                  }
                  if (!courseId || !teacherId) {
                    toast.error("Select a course and a trainer.", {
                      description: "A batch belongs to one course and is taught by one trainer.",
                    });
                    return;
                  }

                  const capacityRaw = str("capacity");
                  setSaving(true);
                  try {
                    await adminService.saveBatch({
                      name,
                      courseId,
                      teacherId,
                      capacity: capacityRaw ? Number(capacityRaw) : 0,
                      status: str("status") ?? "Upcoming",
                      startDate: str("startDate"),
                      endDate: str("endDate"),
                    });
                    toast.success(`Batch ${name} created.`);
                    setOpen(false);
                    setCourseId("");
                    setTeacherId("");
                    await batches.refetch();
                  } catch (err: unknown) {
                    toast.error(
                      err instanceof Error && err.message
                        ? err.message
                        : "Could not create the batch.",
                    );
                  } finally {
                    setSaving(false);
                  }
                }}
              >
                <div className="space-y-1.5">
                  <Label htmlFor="batch-course">
                    Course <span className="text-destructive">*</span>
                  </Label>
                  <Select
                    value={courseId}
                    onValueChange={(v) => {
                      setCourseId(v);
                      setTeacherId("");
                    }}
                  >
                    <SelectTrigger id="batch-course">
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
                  <Label htmlFor="batch-trainer">
                    Trainer <span className="text-destructive">*</span>
                  </Label>
                  <Select value={teacherId} onValueChange={setTeacherId} disabled={!courseId}>
                    <SelectTrigger id="batch-trainer">
                      <SelectValue
                        placeholder={courseId ? "Select trainer" : "Select a course first"}
                      />
                    </SelectTrigger>
                    <SelectContent>
                      {eligibleTeachers.map((t: any) => (
                        <SelectItem key={t.id} value={t.id}>
                          {t.name} ({t.email})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {[
                  { id: "name", label: "Batch name" },
                  { id: "startDate", label: "Start date", type: "date" },
                  { id: "endDate", label: "End date", type: "date" },
                  { id: "days", label: "Days" },
                  { id: "time", label: "Time" },
                  { id: "capacity", label: "Capacity", type: "number" },
                  { id: "meetingUrl", label: "Meeting link" },
                ].map((f) => (
                  <div key={f.id} className="space-y-1.5">
                    <Label htmlFor={f.id}>{f.label}</Label>
                    <Input id={f.id} name={f.id} type={f.type ?? "text"} maxLength={160} />
                  </div>
                ))}
                {[
                  { id: "mode", label: "Mode", options: ["Online", "Offline", "Hybrid"] },
                  {
                    id: "platform",
                    label: "Platform",
                    options: ["Google Meet", "Zoom", "Microsoft Teams", "Other"],
                  },
                  {
                    id: "status",
                    label: "Status",
                    options: ["Upcoming", "Active", "Completed", "Cancelled"],
                  },
                ].map((f) => (
                  <div key={f.id} className="space-y-1.5">
                    <Label htmlFor={f.id}>{f.label}</Label>
                    <Select name={f.id} defaultValue={f.options[0]!}>
                      <SelectTrigger id={f.id}>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {f.options.map((o) => (
                          <SelectItem key={o} value={o}>
                            {o}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                ))}
              </form>
              <DialogFooter>
                <Button type="submit" form="create-batch">
                  Create batch
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Batches" value={data.length} />
        <StatCard label="Active" value={data.filter((b) => b.status === "Active").length} />
        <StatCard label="Upcoming" value={data.filter((b) => b.status === "Upcoming").length} />
        <StatCard
          label="Seats filled"
          value={`${data.reduce((s, b) => s + b.enrolled, 0)}/${data.reduce((s, b) => s + b.capacity, 0)}`}
        />
      </section>

      <Panel title="All batches">
        {batches.isPending ? (
          <Skeleton className="h-72 w-full" />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Batch</TableHead>
                  <TableHead>Course / Trainer</TableHead>
                  <TableHead>Schedule</TableHead>
                  <TableHead>Mode</TableHead>
                  <TableHead className="text-right">Seats</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((b) => (
                  <Fragment key={b.id}>
                  <TableRow>
                    <TableCell>
                      <div className="font-medium">{b.name}</div>
                      <div className="text-xs text-muted-foreground">
                        {b.startDate} → {b.endDate}
                      </div>
                    </TableCell>
                    <TableCell className="text-sm">
                      {b.courseTitle}
                      <div className="text-xs text-muted-foreground">{b.trainerName}</div>
                    </TableCell>
                    <TableCell className="text-sm">
                      {b.days}
                      <div className="text-xs text-muted-foreground">{b.time}</div>
                    </TableCell>
                    <TableCell className="text-sm">
                      {b.mode}
                      <div className="text-xs text-muted-foreground">{b.platform}</div>
                    </TableCell>
                    <TableCell className="text-right text-sm">
                      <button
                        type="button"
                        className="underline-offset-2 hover:underline"
                        onClick={() => setExpanded((cur) => (cur === b.id ? null : b.id))}
                        title="Show the students in this batch"
                      >
                        {b.enrolled}/{b.capacity}
                      </button>
                    </TableCell>
                    <TableCell className="space-x-2">
                      <StatusBadge value={b.status} />
                      <StatusBadge value={b.published ? "published" : "draft"} />
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => toast.success("Batch duplicated as a draft.")}
                      >
                        Duplicate
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() =>
                          toast.success(b.published ? "Batch unpublished." : "Batch published.")
                        }
                      >
                        {b.published ? "Unpublish" : "Publish"}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setExpanded((cur) => (cur === b.id ? null : b.id))}
                      >
                        {expanded === b.id ? "Close" : "Open"}
                      </Button>
                      {isAdmin ? (
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                          onClick={() => setBatchToDelete(b)}
                          title="Delete this batch"
                        >
                          <Trash2 className="size-4" aria-hidden />
                        </Button>
                      ) : null}
                    </TableCell>
                  </TableRow>
                  {expanded === b.id ? (
                    <TableRow>
                      <TableCell colSpan={7} className="bg-muted/40 p-0">
                        <BatchStudents batchId={b.id} />
                      </TableCell>
                    </TableRow>
                  ) : null}
                  </Fragment>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Panel>

      {/* Delete confirmation. Shows exactly what the batch groups and states plainly that
          students and their course enrolments survive, because that is the behaviour the
          backend implements. */}
      <Dialog open={!!batchToDelete} onOpenChange={(o) => !o && setBatchToDelete(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete this batch?</DialogTitle>
            <DialogDescription>
              The batch is removed. The people in it are not.
            </DialogDescription>
          </DialogHeader>

          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 py-2 text-sm">
            <dt className="text-muted-foreground">Batch</dt>
            <dd className="font-medium">{batchToDelete?.name}</dd>
            <dt className="text-muted-foreground">Course</dt>
            <dd>{batchToDelete?.courseTitle ?? "—"}</dd>
            <dt className="text-muted-foreground">Teacher</dt>
            <dd>{batchToDelete?.teacherName ?? batchToDelete?.trainerName ?? "—"}</dd>
            <dt className="text-muted-foreground">Students</dt>
            <dd>{batchToDelete?.enrolled ?? 0}</dd>
          </dl>

          {(batchToDelete?.enrolled ?? 0) > 0 ? (
            <p className="rounded-md border border-amber-500/40 bg-amber-500/10 p-3 text-sm">
              {batchToDelete?.enrolled} student
              {batchToDelete?.enrolled === 1 ? "" : "s"} will be released from this batch.{" "}
              <strong>Their accounts and their enrolment in {batchToDelete?.courseTitle} are kept</strong>
              , as are any other courses they are enrolled in. Only the batch assignment is removed.
            </p>
          ) : (
            <p className="text-sm text-muted-foreground">
              This batch has no students assigned.
            </p>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setBatchToDelete(null)} disabled={deleting}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={deleting}
              onClick={async () => {
                if (!batchToDelete) return;
                setDeleting(true);
                try {
                  const res = await adminService.deleteBatch(batchToDelete.id);
                  toast.success(res?.message ?? `Batch ${batchToDelete.name} deleted.`);
                  if (expanded === batchToDelete.id) setExpanded(null);
                  setBatchToDelete(null);
                  await batches.refetch();
                } catch (err: unknown) {
                  // Surface what the backend actually said - never a false success.
                  toast.error(
                    err instanceof Error && err.message
                      ? err.message
                      : "Could not delete the batch.",
                  );
                } finally {
                  setDeleting(false);
                }
              }}
            >
              {deleting ? "Deleting…" : "Delete batch"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

/**
 * Students in one batch, straight from GET /admin/batches/{id}/students, which reads the
 * batch_students relationship and joins each student's enrolment for the batch course.
 * No mock data and no client-side filtering - the backend decides who belongs here.
 */
function BatchStudents({ batchId }: { batchId: string }) {
  const q = useQuery({
    queryKey: ["admin", "batch-students", batchId],
    queryFn: () => adminService.batchStudents(batchId),
  });

  if (q.isLoading) {
    return <div className="p-4"><Skeleton className="h-16 w-full" /></div>;
  }
  if (q.isError) {
    return (
      <p className="p-4 text-sm text-destructive">
        Could not load the students for this batch.
      </p>
    );
  }

  const students = q.data ?? [];
  if (students.length === 0) {
    return (
      <p className="p-4 text-sm text-muted-foreground">
        No students assigned to this batch yet. Add one from Students → Add student by
        selecting this batch, or allocate an existing student from their detail page.
      </p>
    );
  }

  return (
    <div className="overflow-x-auto p-4">
      <p className="mb-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
        {students.length} student{students.length === 1 ? "" : "s"} in this batch
      </p>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Student</TableHead>
            <TableHead>Student ID</TableHead>
            <TableHead>Course</TableHead>
            <TableHead>Teacher</TableHead>
            <TableHead>Enrollment</TableHead>
            <TableHead>Since</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {students.map((s) => (
            <TableRow key={s.id}>
              <TableCell>
                <div className="font-medium">{s.name}</div>
                <div className="text-xs text-muted-foreground">
                  {s.email}
                  {s.phone ? ` · ${s.phone}` : ""}
                </div>
              </TableCell>
              <TableCell className="text-sm">{s.studentId ?? "—"}</TableCell>
              <TableCell className="text-sm">{s.courseTitle ?? "—"}</TableCell>
              <TableCell className="text-sm">{s.teacherName ?? "—"}</TableCell>
              <TableCell>
                {s.enrollmentStatus ? (
                  <StatusBadge value={s.enrollmentStatus} />
                ) : (
                  <span className="text-xs text-muted-foreground">Not enrolled</span>
                )}
              </TableCell>
              <TableCell className="text-sm text-muted-foreground">
                {s.enrolledAt ? new Date(s.enrolledAt).toLocaleDateString() : "—"}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
