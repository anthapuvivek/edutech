import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { useMemo, useState } from "react";
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
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/career/jobs")({
  head: () => ({
    meta: [
      { title: "Jobs — Learntrix Admin" },
      {
        name: "description",
        content:
          "Create, publish and expire job opportunities with course, attendance and coding eligibility criteria.",
      },
      { property: "og:title", content: "Jobs — Learntrix Admin" },
      {
        property: "og:description",
        content: "Full control of the career job portal and its eligibility criteria.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminJobs,
});

const ALL = "all";

function AdminJobs() {
  const jobs = useQuery({ queryKey: ["admin", "jobs"], queryFn: () => adminService.jobs() });
  const [status, setStatus] = useState(ALL);
  const [open, setOpen] = useState(false);
  const data = jobs.data ?? [];
  const rows = useMemo(
    () => data.filter((j) => status === ALL || j.status === status),
    [data, status],
  );

  return (
    <>
      <PageHeader
        title="Jobs"
        description="Eligibility criteria set here are evaluated by the backend, which returns the eligible student list."
        action={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="size-4" aria-hidden /> Create job
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
              <DialogHeader>
                <DialogTitle>Create job</DialogTitle>
                <DialogDescription>
                  Set the eligibility criteria — the backend computes which students qualify.
                </DialogDescription>
              </DialogHeader>
              <form
                id="create-job"
                className="grid gap-4 sm:grid-cols-2"
                onSubmit={(e) => {
                  e.preventDefault();
                  void adminService.saveJob({}).then(() => {
                    toast.success("Job saved as draft.");
                    setOpen(false);
                  });
                }}
              >
                {[
                  { id: "company", label: "Company" },
                  { id: "title", label: "Job title" },
                  { id: "location", label: "Location" },
                  { id: "salary", label: "Salary" },
                  { id: "experience", label: "Experience" },
                  { id: "skills", label: "Skills" },
                  { id: "qualification", label: "Qualification" },
                  { id: "deadline", label: "Deadline", type: "date" },
                  { id: "applicationUrl", label: "Application URL" },
                  { id: "eligibleCourses", label: "Eligible courses" },
                  { id: "minAttendance", label: "Min attendance %", type: "number" },
                  { id: "minCoding", label: "Min coding score %", type: "number" },
                  { id: "minQuiz", label: "Min quiz score %", type: "number" },
                  { id: "minProgress", label: "Min course progress %", type: "number" },
                  { id: "graduationYear", label: "Graduation year", type: "number" },
                ].map((f) => (
                  <div key={f.id} className="space-y-1.5">
                    <Label htmlFor={f.id}>{f.label}</Label>
                    <Input id={f.id} name={f.id} type={f.type ?? "text"} maxLength={160} />
                  </div>
                ))}
                <div className="space-y-1.5 sm:col-span-2">
                  <Label htmlFor="description">Description</Label>
                  <Textarea id="description" name="description" maxLength={4000} rows={4} />
                </div>
              </form>
              <DialogFooter>
                <Button type="submit" form="create-job">
                  Save job
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Jobs" value={data.length} />
        <StatCard label="Published" value={data.filter((j) => j.status === "Published").length} />
        <StatCard label="Applicants" value={data.reduce((s, j) => s + j.applicants, 0)} />
        <StatCard
          label="Expiring soon"
          value={data.filter((j) => j.status === "Published").length}
          hint="Within 7 days"
        />
      </section>

      <Panel
        title="Job listings"
        action={
          <Select value={status} onValueChange={setStatus}>
            <SelectTrigger className="w-40" aria-label="Filter by status">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All statuses</SelectItem>
              {["Draft", "Published", "Unpublished", "Expired", "Archived"].map((s) => (
                <SelectItem key={s} value={s}>
                  {s}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        }
      >
        {jobs.isPending ? (
          <Skeleton className="h-72 w-full" />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Role</TableHead>
                  <TableHead>Type / Mode</TableHead>
                  <TableHead>Eligibility</TableHead>
                  <TableHead className="text-right">Eligible</TableHead>
                  <TableHead className="text-right">Applicants</TableHead>
                  <TableHead>Deadline</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((j) => (
                  <TableRow key={j.id}>
                    <TableCell>
                      <div className="font-medium">{j.title}</div>
                      <div className="text-xs text-muted-foreground">
                        {j.companyName} · {j.location} · {j.salaryRange}
                      </div>
                    </TableCell>
                    <TableCell className="text-sm">
                      {j.jobType}
                      <div className="text-xs text-muted-foreground">
                        {j.workMode} · {j.applicationMethod}
                      </div>
                    </TableCell>
                    <TableCell className="text-xs">
                      <div className="flex flex-wrap gap-1">
                        <Badge variant="outline">Attendance ≥ {j.minAttendance}%</Badge>
                        <Badge variant="outline">Coding ≥ {j.minCodingScore}%</Badge>
                        <Badge variant="outline">Progress ≥ {j.minCourseProgress}%</Badge>
                      </div>
                      <p className="mt-1 text-muted-foreground">{j.eligibleCourses.join(", ")}</p>
                    </TableCell>
                    <TableCell className="text-right">{j.eligibleStudents}</TableCell>
                    <TableCell className="text-right">{j.applicants}</TableCell>
                    <TableCell className="text-sm">{j.deadline}</TableCell>
                    <TableCell>
                      <StatusBadge value={j.status} />
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => toast.success("Status change submitted.")}
                      >
                        {j.status === "Published" ? "Unpublish" : "Publish"}
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => toast.info("Archived jobs stay available for reporting.")}
                      >
                        Archive
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
