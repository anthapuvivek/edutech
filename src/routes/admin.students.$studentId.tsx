import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { ArrowLeft, ShieldAlert } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { MetricBar, Panel, StatusBadge, humanize } from "@/components/portal/AdminBits";
import { PageHeader } from "@/components/portal/StatCard";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { formatPrice } from "@/lib/format";
import { adminService } from "@/services/admin.service";
import type { StudentAccountStatus } from "@/types/admin";

export const Route = createFileRoute("/admin/students/$studentId")({
  head: () => ({
    meta: [
      { title: "Student 360 — Learntrix Admin" },
      {
        name: "description",
        content:
          "Complete student journey: profile, enrollment, attendance, learning, career eligibility, applications and payments.",
      },
      { property: "og:title", content: "Student 360 — Learntrix Admin" },
      { property: "og:description", content: "One page for the full student lifecycle." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: StudentDetail,
});

const statusActions: StudentAccountStatus[] = ["active", "inactive", "suspended", "archived"];

function StudentDetail() {
  const { studentId } = Route.useParams();
  const student = useQuery({
    queryKey: ["admin", "student", studentId],
    queryFn: () => adminService.student(studentId),
  });
  const [reason, setReason] = useState("");

  if (student.isPending) return <Skeleton className="h-[70vh] w-full" />;
  if (student.isError || !student.data)
    return <p className="text-sm text-destructive">Unable to load this student.</p>;

  const s = student.data;

  async function applyStatus(next: StudentAccountStatus) {
    try {
      await adminService.setStudentStatus(studentId, next, reason);
      toast.success(`Status change submitted — the backend records it in the audit log.`);
      setReason("");
    } catch {
      toast.error("Could not update the status.");
    }
  }

  return (
    <>
      <Button variant="ghost" size="sm" className="mb-3" asChild>
        <Link to="/admin/students">
          <ArrowLeft className="size-4" aria-hidden /> All students
        </Link>
      </Button>

      <PageHeader
        title={s.name}
        description={`${s.studentId} · ${s.courseTitle} · ${s.batchName} · Trainer ${s.trainerName}`}
        action={
          <div className="flex flex-wrap gap-2">
            {statusActions.map((action) => (
              <AlertDialog key={action}>
                <AlertDialogTrigger asChild>
                  <Button
                    size="sm"
                    variant={
                      action === "suspended" || action === "archived" ? "destructive" : "outline"
                    }
                  >
                    {humanize(action === "active" ? "activate" : action)}
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Set status to {humanize(action)}?</AlertDialogTitle>
                    <AlertDialogDescription>
                      This affects portal access and career eligibility. The action and reason are
                      written to the audit log.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <div className="space-y-1.5">
                    <Label htmlFor={`reason-${action}`}>Reason</Label>
                    <Textarea
                      id={`reason-${action}`}
                      value={reason}
                      maxLength={300}
                      onChange={(e) => setReason(e.target.value)}
                      placeholder="Documented reason for this change"
                    />
                  </div>
                  <AlertDialogFooter>
                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                    <AlertDialogAction onClick={() => void applyStatus(action)}>
                      Confirm
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            ))}
          </div>
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Panel title="Learning">
          <StatusBadge value={s.status} />
          <p className="mt-2 text-2xl font-semibold">{s.progressPercent}%</p>
          <p className="text-xs text-muted-foreground">Course progress</p>
        </Panel>
        <Panel title="Attendance">
          <StatusBadge value={s.attendancePercent >= 80 ? "active" : "pending"} />
          <p className="mt-2 text-2xl font-semibold">{s.attendancePercent}%</p>
          <p className="text-xs text-muted-foreground">Across {s.batchName}</p>
        </Panel>
        <Panel title="Career">
          <StatusBadge value={s.careerStatus} />
          <p className="mt-2 text-2xl font-semibold">#{s.rank}</p>
          <p className="text-xs text-muted-foreground">{s.points.toLocaleString()} points</p>
        </Panel>
        <Panel title="Placement">
          <StatusBadge value={s.placementStatus} />
          <p className="mt-2 text-2xl font-semibold">{s.applications.length}</p>
          <p className="text-xs text-muted-foreground">Active applications</p>
        </Panel>
      </div>

      <Tabs defaultValue="overview">
        <TabsList className="mb-4 flex h-auto flex-wrap justify-start gap-1">
          {[
            "overview",
            "enrollment",
            "attendance",
            "performance",
            "career",
            "documents",
            "payments",
            "activity",
          ].map((t) => (
            <TabsTrigger key={t} value={t} className="capitalize">
              {t}
            </TabsTrigger>
          ))}
        </TabsList>

        <TabsContent value="overview" className="grid gap-4 lg:grid-cols-2">
          <Panel title="Personal information">
            <dl className="grid grid-cols-2 gap-3 text-sm">
              {[
                ["Email", s.email],
                ["Phone", s.phone],
                ["Date of birth", s.dateOfBirth],
                ["Gender", s.gender],
                ["Location", s.location],
                ["College", s.college],
                ["Qualification", s.qualification],
                ["Graduation year", String(s.graduationYear)],
              ].map(([label, value]) => (
                <div key={label}>
                  <dt className="text-xs uppercase tracking-wide text-muted-foreground">{label}</dt>
                  <dd className="break-words">{value}</dd>
                </div>
              ))}
            </dl>
            <p className="mt-4 text-xs text-muted-foreground">
              Contact details are private. Trainers only see students assigned to them; students
              only see their own record.
            </p>
          </Panel>

          <Panel title="Profile & links">
            <div className="flex flex-wrap gap-1.5">
              {s.skills.map((skill) => (
                <Badge key={skill} variant="outline">
                  {skill}
                </Badge>
              ))}
            </div>
            <ul className="mt-4 space-y-1 text-sm">
              {Object.entries(s.links).map(([key, url]) =>
                url ? (
                  <li key={key}>
                    <span className="capitalize text-muted-foreground">{key}: </span>
                    <a
                      href={url}
                      target="_blank"
                      rel="noreferrer noopener"
                      className="underline underline-offset-4"
                    >
                      {url}
                    </a>
                  </li>
                ) : null,
              )}
            </ul>
            <div className="mt-4 space-y-3">
              <MetricBar label="Profile completion" value={s.profileCompletion} />
              <MetricBar label="Assignment completion" value={s.assignmentCompletion} />
            </div>
          </Panel>

          <Panel
            title="Student journey"
            className="lg:col-span-2"
            description="Registration through verified placement"
          >
            <ol className="space-y-3">
              {s.timeline.map((step) => (
                <li key={step.id} className="flex items-start gap-3">
                  <span
                    className={`mt-1 size-2.5 shrink-0 rounded-full ${
                      step.state === "done"
                        ? "bg-accent"
                        : step.state === "current"
                          ? "bg-primary"
                          : "bg-muted"
                    }`}
                  />
                  <div>
                    <p className="text-sm font-medium">{step.stage}</p>
                    <p className="text-xs text-muted-foreground">
                      {step.detail} · {step.occurredAt}
                    </p>
                  </div>
                </li>
              ))}
            </ol>
          </Panel>
        </TabsContent>

        <TabsContent value="enrollment">
          <Panel
            title="Enrollments"
            description="Course, batch, trainer, dates, payment and career eligibility are all admin-controlled."
            action={
              <Button
                size="sm"
                onClick={() =>
                  toast.info("Enrollment creation posts to the backend enrollment service.")
                }
              >
                Create enrollment
              </Button>
            }
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Course</TableHead>
                  <TableHead>Batch / Trainer</TableHead>
                  <TableHead>Dates</TableHead>
                  <TableHead>Course status</TableHead>
                  <TableHead>Payment</TableHead>
                  <TableHead>Career eligible</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {s.enrollments.map((e) => (
                  <TableRow key={e.id}>
                    <TableCell>{e.courseTitle}</TableCell>
                    <TableCell className="text-sm">
                      {e.batchName}
                      <div className="text-xs text-muted-foreground">{e.trainerName}</div>
                    </TableCell>
                    <TableCell className="text-sm">
                      {e.startDate} → {e.endDate}
                    </TableCell>
                    <TableCell>
                      <StatusBadge value={e.courseStatus} />
                    </TableCell>
                    <TableCell>
                      <StatusBadge value={e.paymentStatus} />
                    </TableCell>
                    <TableCell>
                      <StatusBadge value={e.careerEligible ? "eligible" : "not_eligible"} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Panel>
        </TabsContent>

        <TabsContent value="attendance">
          <Panel
            title="Attendance history"
            description="Trainer-marked, admin-correctable. Every correction is audited."
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Class</TableHead>
                  <TableHead>Batch</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Correct</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {s.attendance.map((a) => (
                  <TableRow key={a.date}>
                    <TableCell>{a.date}</TableCell>
                    <TableCell>{a.classTitle}</TableCell>
                    <TableCell>{a.batchName}</TableCell>
                    <TableCell>
                      <StatusBadge value={a.status} />
                    </TableCell>
                    <TableCell className="text-right">
                      <Select
                        defaultValue={a.status}
                        onValueChange={(value) =>
                          void adminService
                            .correctAttendance({ studentId, date: a.date, status: value })
                            .then(() => toast.success("Attendance correction submitted."))
                        }
                      >
                        <SelectTrigger
                          className="ml-auto w-36"
                          aria-label={`Correct attendance for ${a.date}`}
                        >
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {["present", "absent", "late", "excused"].map((v) => (
                            <SelectItem key={v} value={v} className="capitalize">
                              {v}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Panel>
        </TabsContent>

        <TabsContent value="performance" className="grid gap-4 lg:grid-cols-2">
          <Panel title="Performance scorecard">
            <div className="space-y-3">
              <MetricBar label="Course progress" value={s.progressPercent} />
              <MetricBar label="Attendance" value={s.attendancePercent} />
              <MetricBar label="Quiz average" value={s.quizScore} />
              <MetricBar label="Coding score" value={s.codingScore} />
              <MetricBar label="Assignments" value={s.assignmentCompletion} />
            </div>
          </Panel>
          <Panel title="Assessments">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Type</TableHead>
                  <TableHead>Title</TableHead>
                  <TableHead className="text-right">Score</TableHead>
                  <TableHead>Submitted</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {s.assessments.map((a) => (
                  <TableRow key={a.id}>
                    <TableCell className="capitalize">{a.kind}</TableCell>
                    <TableCell>{a.title}</TableCell>
                    <TableCell className="text-right">{a.score}%</TableCell>
                    <TableCell>{a.submittedAt}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Panel>
        </TabsContent>

        <TabsContent value="career" className="grid gap-4 lg:grid-cols-2">
          <Panel
            title="Career eligibility"
            description="Evaluated by the backend against the configured rule sets."
          >
            <StatusBadge value={s.careerStatus} />
            {s.attendancePercent < 80 ? (
              <div className="mt-3 rounded-lg border border-destructive/30 bg-destructive/5 p-3 text-sm">
                <p className="flex items-center gap-2 font-medium">
                  <ShieldAlert className="size-4 text-destructive" aria-hidden /> Attendance below
                  required threshold
                </p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Current attendance: {s.attendancePercent}% · Required: 80%. Recommended action:
                  attend upcoming classes to restore eligibility.
                </p>
              </div>
            ) : null}
            <div className="mt-4 space-y-2">
              <Label htmlFor="override">Manual override reason</Label>
              <Textarea
                id="override"
                maxLength={300}
                placeholder="e.g. medical leave documented — place under review"
              />
              <div className="flex flex-wrap gap-2">
                {["eligible", "conditional", "under_review", "not_eligible"].map((v) => (
                  <Button
                    key={v}
                    size="sm"
                    variant="outline"
                    onClick={() =>
                      void adminService
                        .overrideEligibility(studentId, { status: v })
                        .then(() =>
                          toast.success("Override submitted and recorded in the audit log."),
                        )
                    }
                  >
                    {humanize(v)}
                  </Button>
                ))}
              </div>
            </div>
          </Panel>
          <Panel title="Applications & referrals">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Company</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Applied</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {s.applications.map((a) => (
                  <TableRow key={a.id}>
                    <TableCell>{a.company}</TableCell>
                    <TableCell>{a.role}</TableCell>
                    <TableCell>{a.appliedAt}</TableCell>
                    <TableCell>
                      <StatusBadge value={a.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Panel>
        </TabsContent>

        <TabsContent value="documents">
          <Panel
            title="Documents"
            description="Review, approve, replace or remove student uploads."
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Type</TableHead>
                  <TableHead>File</TableHead>
                  <TableHead>Uploaded</TableHead>
                  <TableHead>Review</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {s.documents.map((d) => (
                  <TableRow key={d.id}>
                    <TableCell>{humanize(d.kind)}</TableCell>
                    <TableCell>{d.name}</TableCell>
                    <TableCell>{d.uploadedAt}</TableCell>
                    <TableCell>
                      <StatusBadge value={d.reviewStatus} />
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => toast.success("Approval submitted.")}
                      >
                        Approve
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => toast.info("Removal requires backend confirmation.")}
                      >
                        Remove
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <div className="mt-4 grid gap-2 sm:max-w-sm">
              <Label htmlFor="replace">Upload replacement document</Label>
              <Input id="replace" type="file" />
            </div>
          </Panel>
        </TabsContent>

        <TabsContent value="payments" className="grid gap-4 lg:grid-cols-2">
          <Panel
            title="Payments"
            description="Verification is performed by the backend payment service."
          >
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Description</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Date</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {s.payments.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell>{p.description}</TableCell>
                    <TableCell className="text-right">{formatPrice(p.amount)}</TableCell>
                    <TableCell>
                      <StatusBadge value={p.status} />
                    </TableCell>
                    <TableCell>{p.paidAt}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Panel>
          <Panel title="Certificates">
            <ul className="space-y-2 text-sm">
              {s.certificates.map((c) => (
                <li
                  key={c.id}
                  className="flex items-center justify-between rounded-md border border-border px-3 py-2"
                >
                  <span>{c.title}</span>
                  <span className="text-xs text-muted-foreground">{c.issuedAt}</span>
                </li>
              ))}
            </ul>
          </Panel>
        </TabsContent>

        <TabsContent value="activity">
          <Panel title="Activity history">
            <ul className="divide-y divide-border text-sm">
              {s.activity.map((a) => (
                <li key={a.id} className="flex items-center justify-between py-2.5">
                  <span>{a.label}</span>
                  <span className="text-xs text-muted-foreground">{a.occurredAt}</span>
                </li>
              ))}
            </ul>
          </Panel>
        </TabsContent>
      </Tabs>
    </>
  );
}
