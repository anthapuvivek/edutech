import { useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { reportOnboardingOutcome, reportResendOutcome } from "@/lib/onboarding-toast";

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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { adminService } from "@/services/admin.service";
import { useAuth } from "@/hooks/useAuth";
import type { AdminTrainer } from "@/types/admin";

export const Route = createFileRoute("/admin/teachers")({
  head: () => ({
    meta: [
      { title: "Trainers — Learntrix Admin" },
      {
        name: "description",
        content:
          "Approve trainers, assign courses, batches and students, and review teaching performance.",
      },
      { property: "og:title", content: "Trainers — Learntrix Admin" },
      {
        property: "og:description",
        content:
          "Approve trainers, assign courses, batches and students, and review teaching performance.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminTeachers,
});

function AdminTeachers() {
  const { user } = useAuth();
  const isAdmin = user?.role === "admin" || user?.role === "super_admin";
  const queryClient = useQueryClient();
  const trainers = useQuery({
    queryKey: ["admin", "trainers"],
    queryFn: () => adminService.trainers(),
  });
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [teacherToDelete, setTeacherToDelete] = useState<AdminTrainer | null>(null);
  const [deleting, setDeleting] = useState(false);
  const data = trainers.data ?? [];
  const pending = data.filter((t) => t.approvalStatus === "pending");

  async function handleCreate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const getString = (key: string) => (form.get(key) as string | null)?.trim() || undefined;
    const expYearsStr = form.get("experienceYears") as string | null;
    const payload: Record<string, unknown> = {
      fullName: getString("fullName"),
      email: (form.get("email") as string | null)?.trim().toLowerCase(),
      phone: getString("phone"),
      headline: getString("headline"),
      department: getString("department"),
      qualification: getString("qualification"),
      experienceYears: expYearsStr && expYearsStr.trim() ? Number(expYearsStr) : undefined,
      skills: getString("skills"),
      bio: getString("bio"),
    };

    setSaving(true);
    try {
      const res = await adminService.createTeacher(payload);
      reportOnboardingOutcome(res, "Trainer");
      setOpen(false);
      await queryClient.invalidateQueries({ queryKey: ["admin", "trainers"] });
      void trainers.refetch();
    } catch (err: unknown) {
      const message =
        err instanceof Error && err.message
          ? err.message
          : "Could not create trainer account. Please check the form and try again.";
      toast.error(message);
    } finally {
      setSaving(false);
    }
  }

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
                <DialogDescription>
                  The backend validates uniqueness, issues the Employee ID, and sends the activation email.
                </DialogDescription>
              </DialogHeader>
              <form
                id="add-trainer"
                className="grid gap-4 sm:grid-cols-2"
                onSubmit={handleCreate}
              >
                {[
                  { name: "fullName", label: "Full name", required: true },
                  { name: "email", label: "Email", type: "email", required: true },
                  { name: "phone", label: "Phone" },
                  { name: "headline", label: "Headline (e.g. Lead Java Trainer)" },
                  { name: "department", label: "Department" },
                  { name: "qualification", label: "Qualification" },
                  { name: "experienceYears", label: "Experience (years)", type: "number" },
                  { name: "skills", label: "Skills (comma separated)" },
                ].map((field) => (
                  <div key={field.name} className="space-y-1.5">
                    <Label htmlFor={field.name}>{field.label}</Label>
                    <Input
                      id={field.name}
                      name={field.name}
                      type={field.type ?? "text"}
                      required={field.required ?? false}
                      maxLength={120}
                    />
                  </div>
                ))}
                <div className="space-y-1.5 sm:col-span-2">
                  <Label htmlFor="bio">Bio</Label>
                  <Textarea id="bio" name="bio" maxLength={600} />
                </div>
              </form>
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" form="add-trainer" disabled={saving}>
                  {saving ? "Saving…" : "Send invitation"}
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
        <StatCard
          label="Classes this month"
          value={data.reduce((s, t) => s + t.classesThisMonth, 0)}
        />
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
                        {(t.skills ?? []).map((s) => (
                          <Badge key={s} variant="outline" className="text-[10px]">
                            {s}
                          </Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell className="text-sm">{(t.courses ?? []).join(", ")}</TableCell>
                    <TableCell className="text-right">{t.batches}</TableCell>
                    <TableCell className="text-right">{t.students}</TableCell>
                    <TableCell className="text-right">{t.classesThisMonth}</TableCell>
                    <TableCell className="text-right">{t.rating}</TableCell>
                    <TableCell>
                      <StatusBadge value={t.approvalStatus} />
                    </TableCell>
                    <TableCell className="space-x-1 text-right">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() =>
                          void adminService
                            .resendTeacherWelcomeEmail(t.id)
                            .then((res) => reportResendOutcome(res, t.email))
                            .catch(() => toast.error("Could not resend email."))
                        }
                      >
                        Resend Invite
                      </Button>
                      {t.approvalStatus === "pending" ? (
                        <Button
                          size="sm"
                          onClick={() =>
                            void adminService
                              .approveTrainer(t.id, true)
                              .then(() => {
                                toast.success("Trainer approved.");
                                void trainers.refetch();
                              })
                          }
                        >
                          Approve
                        </Button>
                      ) : null}
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => toast.info("Assignment editor opens the batch planner.")}
                      >
                        Assign
                      </Button>
                      {isAdmin ? (
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                          onClick={() => setTeacherToDelete(t)}
                          title="Remove trainer"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      ) : null}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Panel>

      {/* Teacher Deletion / Deactivation Confirmation Dialog */}
      <Dialog open={!!teacherToDelete} onOpenChange={(open) => !open && setTeacherToDelete(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Remove Teacher</DialogTitle>
            <DialogDescription>
              Are you sure you want to remove this teacher?
            </DialogDescription>
          </DialogHeader>
          <div className="py-2 text-sm text-muted-foreground">
            <p>
              This will deactivate <strong>{teacherToDelete?.name}</strong> ({teacherToDelete?.email}), set their profile status to deactivated, and revoke their access to teaching tools and courses.
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setTeacherToDelete(null)} disabled={deleting}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={deleting}
              onClick={async () => {
                if (!teacherToDelete) return;
                setDeleting(true);
                try {
                  const res = await adminService.deleteTeacher(teacherToDelete.id);
                  toast.success(res?.message || "Teacher account deactivated successfully.");
                  setTeacherToDelete(null);
                  void queryClient.invalidateQueries({ queryKey: ["admin", "trainers"] });
                } catch (err: any) {
                  if (err?.status === 403 || err?.response?.status === 403) {
                    toast.error("You are not authorized to perform this action.");
                  } else {
                    toast.error(err?.message || "Failed to remove teacher.");
                  }
                } finally {
                  setDeleting(false);
                }
              }}
            >
              {deleting ? "Removing..." : "Remove Teacher"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
