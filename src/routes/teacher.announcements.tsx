import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus, Trash2 } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader } from "@/components/portal/StatCard";
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
import { Textarea } from "@/components/ui/textarea";
import { announcementService } from "@/services/announcement.service";
import { teacherService } from "@/services/teacher.service";
import { priorityTone } from "@/types/announcement";

export const Route = createFileRoute("/teacher/announcements")({
  head: () => ({
    meta: [
      { title: "Announcements — Learntrix Trainer" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: TeacherAnnouncementsPage,
});

const PRIORITIES = ["LOW", "NORMAL", "HIGH", "URGENT"] as const;

function TeacherAnnouncementsPage() {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [courseId, setCourseId] = useState("");
  const [batchId, setBatchId] = useState("");
  const [priority, setPriority] = useState<string>("NORMAL");

  const listQuery = useQuery({
    queryKey: ["teacher", "announcements"],
    queryFn: () => announcementService.teacherAnnouncements(),
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
  const batches = useMemo(
    () => (batchesQuery.data ?? []).filter((b) => !courseId || b.courseId === courseId),
    [batchesQuery.data, courseId],
  );

  const removeMutation = useMutation({
    mutationFn: (id: string) => announcementService.remove(id),
    onSuccess: () => {
      toast.success("Announcement deleted");
      void queryClient.invalidateQueries({ queryKey: ["teacher", "announcements"] });
    },
    onError: (err: unknown) =>
      toast.error(err instanceof Error ? err.message : "Could not delete the announcement."),
  });

  async function handleCreate(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    if (!courseId) {
      toast.error("Select a course first.");
      return;
    }
    setSaving(true);
    try {
      await announcementService.create({
        courseId,
        batchId: batchId || undefined,
        title: String(form.get("title") ?? ""),
        content: String(form.get("content") ?? ""),
        priority: priority as "LOW" | "NORMAL" | "HIGH" | "URGENT",
      });
      toast.success("Announcement posted");
      setOpen(false);
      setCourseId("");
      setBatchId("");
      setPriority("NORMAL");
      void queryClient.invalidateQueries({ queryKey: ["teacher", "announcements"] });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Could not post the announcement.");
    } finally {
      setSaving(false);
    }
  }

  const announcements = listQuery.data ?? [];

  return (
    <>
      <PageHeader
        title="Announcements"
        description="Post to a whole course or a single batch. Students see only what applies to them."
        action={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="mr-1.5 size-4" aria-hidden /> New announcement
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl">
              <DialogHeader>
                <DialogTitle>Post announcement</DialogTitle>
                <DialogDescription>
                  Leave the batch empty to reach everyone on the course.
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
                  <Label htmlFor="an-title">Title</Label>
                  <Input id="an-title" name="title" required />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="an-content">Message</Label>
                  <Textarea id="an-content" name="content" rows={5} required />
                </div>

                <div className="space-y-1.5">
                  <Label>Priority</Label>
                  <Select value={priority} onValueChange={setPriority}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {PRIORITIES.map((p) => (
                        <SelectItem key={p} value={p}>
                          {p.charAt(0) + p.slice(1).toLowerCase()}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setOpen(false)} disabled={saving}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={saving}>
                    {saving ? "Posting…" : "Post"}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        }
      />

      <Panel title="Posted announcements" description="">
        {listQuery.isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        ) : listQuery.isError ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Failed to load announcements. Refresh to try again.
          </p>
        ) : announcements.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            Nothing posted yet. Use “New announcement” to tell your students something.
          </p>
        ) : (
          <ol className="flex flex-col gap-3">
            {announcements.map((a) => (
              <li key={a.id} className="rounded-md border p-3">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-medium">{a.title}</p>
                    <p className="mt-0.5 text-sm text-muted-foreground">
                      {a.courseTitle}
                      {a.batchName ? ` · ${a.batchName}` : " · whole course"}
                      {a.createdAt ? ` · ${new Date(a.createdAt).toLocaleDateString()}` : ""}
                      {a.readCount != null ? ` · read by ${a.readCount}` : ""}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <Badge variant={priorityTone(a.priority)}>{a.priority ?? "NORMAL"}</Badge>
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label="Delete announcement"
                      disabled={removeMutation.isPending}
                      onClick={() => {
                        // Destructive and irreversible, so confirm before calling.
                        if (window.confirm(`Delete “${a.title}”? This cannot be undone.`)) {
                          removeMutation.mutate(a.id);
                        }
                      }}
                    >
                      <Trash2 className="size-4" aria-hidden />
                    </Button>
                  </div>
                </div>
                <p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">{a.content}</p>
              </li>
            ))}
          </ol>
        )}
      </Panel>
    </>
  );
}
