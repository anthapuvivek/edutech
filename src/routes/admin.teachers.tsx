import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

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
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/teachers")({
  head: () => ({
    meta: [
      { title: "Trainers — Learntrix Admin" },
      { name: "description", content: "Approve trainers, assign courses, batches and students, and review teaching performance." },
      { property: "og:title", content: "Trainers — Learntrix Admin" },
      { property: "og:description", content: "Trainer onboarding, approvals and performance in one place." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminTeachers,
});

function AdminTeachers() {
  const trainers = useQuery({ queryKey: ["admin", "trainers"], queryFn: () => adminService.trainers() });
  const [open, setOpen] = useState(false);
  const data = trainers.data ?? [];
  const pending = data.filter((t) => t.approvalStatus === "pending");

  return (
    <>
      <PageHeader
        title="Trainers"
        description="Trainer profiles, approvals, assignments and class delivery performance."
        action={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="size-4" aria-hidden /> Add trainer
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-xl">
              <DialogHeader>
                <DialogTitle>Add a trainer</DialogTitle>
                <DialogDescription>Trainers can complete their bio, CV, certifications and links after activation.</DialogDescription>
              </DialogHeader>
              <form
                id="add-trainer"
                className="grid gap-4 sm:grid-cols-2"
                onSubmit={(e) => {
                  e.preventDefault();
                  toast.success("Trainer invitation submitted.");
                  setOpen(false);
                }}
              >
                {["Full name", "Email", "Phone", "Headline", "Experience (years)", "Skills"].map((label) => (
                  <div key={label} className="space-y-1.5">
                    <Label htmlFor={label}>{label}</Label>
                    <Input id={label} name={label} maxLength={120} />
                  </div>
                ))}
                <div className="space-y-1.5 sm:col-span-2">
                  <Label htmlFor="bio">Bio</Label>
                  <Textarea id="bio" name="bio" maxLength={600} />
                </div>
                <div className="space-y-1.5 sm:col-span-2">
                  <Label htmlFor="cv">Photo / CV / certifications</Label>
                  <Input id="cv" name="cv" type="file" multiple />
                </div>
              </form>
              <DialogFooter>
                <Button type="submit" form="add-trainer">
                  Send invitation
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Trainers" value={data.length} />
        <StatCard label="Pending approval" value={pending.length} />
        <StatCard label="Students taught" value={data.reduce((s, t) => s + t.students, 0)} />
        <StatCard label="Classes this month" value={data.reduce((s, t) => s + t.classesThisMonth, 0)} />
      </section>

      <Panel title="Trainer directory">
        {trainers.isPending ? (
          <Skeleton className="h-72 w-full" />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Trainer</TableHead>
                  <TableHead>Courses</TableHead>
                  <TableHead className="text-right">Batches</TableHead>
                  <TableHead className="text-right">Students</TableHead>
                  <TableHead className="text-right">Classes</TableHead>
                  <TableHead className="text-right">Rating</TableHead>
                  <TableHead>Approval</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((t) => (
                  <TableRow key={t.id}>
                    <TableCell>
                      <div className="font-medium">{t.name}</div>
                      <div className="text-xs text-muted-foreground">
                        {t.headline} · {t.experienceYears} yrs
                      </div>
                      <div className="mt-1 flex flex-wrap gap-1">
                        {t.skills.map((s) => (
                          <Badge key={s} variant="outline" className="text-[10px]">
                            {s}
                          </Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell className="text-sm">{t.courses.join(", ")}</TableCell>
                    <TableCell className="text-right">{t.batches}</TableCell>
                    <TableCell className="text-right">{t.students}</TableCell>
                    <TableCell className="text-right">{t.classesThisMonth}</TableCell>
                    <TableCell className="text-right">{t.rating}</TableCell>
                    <TableCell>
                      <StatusBadge value={t.approvalStatus} />
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      {t.approvalStatus === "pending" ? (
                        <Button
                          size="sm"
                          onClick={() => void adminService.approveTrainer(t.id, true).then(() => toast.success("Trainer approved."))}
                        >
                          Approve
                        </Button>
                      ) : null}
                      <Button size="sm" variant="outline" onClick={() => toast.info("Assignment editor opens the batch planner.")}>
                        Assign
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
