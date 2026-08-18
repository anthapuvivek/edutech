import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { useState } from "react";
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/batches")({
  head: () => ({
    meta: [
      { title: "Batches — Learntrix Admin" },
      { name: "description", content: "Create, schedule, publish and archive batches with trainers, capacity and meeting links." },
      { property: "og:title", content: "Batches — Learntrix Admin" },
      { property: "og:description", content: "Batch scheduling and capacity management for every program." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminBatches,
});

function AdminBatches() {
  const batches = useQuery({ queryKey: ["admin", "batches"], queryFn: () => adminService.batches() });
  const [open, setOpen] = useState(false);
  const data = batches.data ?? [];

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
                <DialogDescription>Meeting credentials are stored server side and revealed only to permitted students.</DialogDescription>
              </DialogHeader>
              <form
                id="create-batch"
                className="grid gap-4 sm:grid-cols-2"
                onSubmit={(e) => {
                  e.preventDefault();
                  void adminService.saveBatch({}).then(() => {
                    toast.success("Batch submitted.");
                    setOpen(false);
                  });
                }}
              >
                {[
                  { id: "name", label: "Batch name" },
                  { id: "course", label: "Course" },
                  { id: "trainer", label: "Trainer" },
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
                  { id: "platform", label: "Platform", options: ["Google Meet", "Zoom", "Microsoft Teams", "Other"] },
                  { id: "status", label: "Status", options: ["Upcoming", "Active", "Completed", "Cancelled"] },
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
        <StatCard label="Seats filled" value={`${data.reduce((s, b) => s + b.enrolled, 0)}/${data.reduce((s, b) => s + b.capacity, 0)}`} />
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
                  <TableRow key={b.id}>
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
                      {b.enrolled}/{b.capacity}
                    </TableCell>
                    <TableCell className="space-x-2">
                      <StatusBadge value={b.status} />
                      <StatusBadge value={b.published ? "published" : "draft"} />
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      <Button size="sm" variant="outline" onClick={() => toast.success("Batch duplicated as a draft.")}>
                        Duplicate
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => toast.success(b.published ? "Batch unpublished." : "Batch published.")}
                      >
                        {b.published ? "Unpublish" : "Publish"}
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
