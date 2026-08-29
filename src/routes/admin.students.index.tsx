import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { Download, Plus, Search } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader } from "@/components/portal/StatCard";
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

export const Route = createFileRoute("/admin/students/")({
  head: () => ({
    meta: [
      { title: "Students — Learntrix Admin" },
      {
        name: "description",
        content:
          "Search, onboard and manage every student across registration, enrollment, attendance and placement.",
      },
      { property: "og:title", content: "Students — Learntrix Admin" },
      {
        property: "og:description",
        content: "Complete student lifecycle management for the Learntrix platform.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminStudents,
});

const ALL = "all";

function AdminStudents() {
  const queryClient = useQueryClient();
  const students = useQuery({
    queryKey: ["admin", "students"],
    queryFn: () => adminService.students(),
  });
  const coursesQuery = useQuery({
    queryKey: ["admin", "courses"],
    queryFn: () => adminService.courses(),
  });
  const trainersQuery = useQuery({
    queryKey: ["admin", "trainers"],
    queryFn: () => adminService.trainers(),
  });
  const batchesQuery = useQuery({
    queryKey: ["admin", "batches"],
    queryFn: () => adminService.batches(),
  });

  const [query, setQuery] = useState("");
  const [status, setStatus] = useState(ALL);
  const [career, setCareer] = useState(ALL);
  const [payment, setPayment] = useState(ALL);
  const [attendance, setAttendance] = useState(ALL);
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  const [selectedCourse, setSelectedCourse] = useState<string>("none");
  const [selectedTrainer, setSelectedTrainer] = useState<string>("none");
  const [selectedBatch, setSelectedBatch] = useState<string>("none");

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (students.data ?? []).filter((s) => {
      const matchesQuery =
        !q ||
        [s.name, s.email, s.phone, s.studentId, s.courseTitle, s.batchName, s.trainerName]
          .join(" ")
          .toLowerCase()
          .includes(q);
      const matchesAttendance =
        attendance === ALL ||
        (attendance === "below80" && s.attendancePercent < 80) ||
        (attendance === "above80" && s.attendancePercent >= 80);
      return (
        matchesQuery &&
        matchesAttendance &&
        (status === ALL || s.status === status) &&
        (career === ALL || s.careerStatus === career) &&
        (payment === ALL || s.paymentStatus === payment)
      );
    });
  }, [students.data, query, status, career, payment, attendance]);

  const lowAttendance = (students.data ?? []).filter((s) => s.attendancePercent < 80).length;

  async function handleCreate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const getString = (key: string) => (form.get(key) as string | null)?.trim() || undefined;
    const gradYearStr = form.get("graduationYear") as string | null;
    const payload: Record<string, unknown> = {
      fullName: getString("fullName"),
      email: (form.get("email") as string | null)?.trim().toLowerCase(),
      phone: getString("phone"),
      dateOfBirth: getString("dateOfBirth"),
      location: getString("location"),
      education: getString("education"),
      college: getString("college"),
      graduationYear: gradYearStr && gradYearStr.trim() ? Number(gradYearStr) : undefined,
      qualification: getString("qualification"),
      skills: getString("skills"),
      github: getString("github"),
      linkedin: getString("linkedin"),
      leetcode: getString("leetcode"),
      hackerrank: getString("hackerrank"),
      gender: getString("gender") || "unspecified",
      accountStatus: getString("accountStatus") || "pending",
      courseId: selectedCourse && selectedCourse !== "none" ? selectedCourse : undefined,
      teacherId: selectedTrainer && selectedTrainer !== "none" ? selectedTrainer : undefined,
      batchId: selectedBatch && selectedBatch !== "none" ? selectedBatch : undefined,
    };

    setSaving(true);
    try {
      const res = await adminService.createStudent(payload);
      if (res?.emailStatus === "FAILED") {
        toast.warning(
          res.message ||
            (res?.identifier
              ? `Student ${res.identifier} created, but activation email could not be sent.`
              : "Student created, but activation email could not be sent."),
        );
      } else {
        toast.success(
          res?.message ||
            (res?.identifier
              ? `Student ${res.identifier} onboarded! Welcome activation email sent.`
              : "Student onboarded! Welcome activation email sent."),
        );
      }
      setOpen(false);
      setSelectedCourse("none");
      setSelectedTrainer("none");
      setSelectedBatch("none");
      await queryClient.invalidateQueries({ queryKey: ["admin", "students"] });
      void students.refetch();
    } catch (err: unknown) {
      const message =
        err instanceof Error && err.message
          ? err.message
          : "Could not create the student. Please check the form and try again.";
      toast.error(message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <PageHeader
        title="Students"
        description="Registration → enrollment → batch → attendance → learning → career eligibility → placement."
        action={
          <div className="flex flex-wrap gap-2">
            <Button
              variant="outline"
              onClick={() => toast.info("Export is queued on the backend and emailed when ready.")}
            >
              <Download className="size-4" aria-hidden /> Export
            </Button>
            <Dialog open={open} onOpenChange={setOpen}>
              <DialogTrigger asChild>
                <Button>
                  <Plus className="size-4" aria-hidden /> Add student
                </Button>
              </DialogTrigger>
              <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
                <DialogHeader>
                  <DialogTitle>Onboard a student</DialogTitle>
                  <DialogDescription>
                    The backend validates uniqueness, issues the student ID and sends the activation
                    email.
                  </DialogDescription>
                </DialogHeader>
                <form
                  id="add-student"
                  onSubmit={handleCreate}
                  className="grid gap-4 sm:grid-cols-2"
                >
                  {[
                    { name: "fullName", label: "Full name", required: true },
                    { name: "email", label: "Email", type: "email", required: true },
                    { name: "phone", label: "Phone", required: true },
                    { name: "dateOfBirth", label: "Date of birth", type: "date" },
                    { name: "location", label: "Location" },
                    { name: "education", label: "Education" },
                    { name: "college", label: "College" },
                    { name: "graduationYear", label: "Graduation year", type: "number" },
                    { name: "qualification", label: "Qualification" },
                    { name: "skills", label: "Skills (comma separated)" },
                    { name: "github", label: "GitHub" },
                    { name: "linkedin", label: "LinkedIn" },
                    { name: "leetcode", label: "LeetCode" },
                    { name: "hackerrank", label: "HackerRank" },
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
                  <div className="space-y-1.5">
                    <Label htmlFor="gender">Gender</Label>
                    <Select name="gender" defaultValue="unspecified">
                      <SelectTrigger id="gender">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {["female", "male", "other", "unspecified"].map((g) => (
                          <SelectItem key={g} value={g} className="capitalize">
                            {g}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="accountStatus">Account status</Label>
                    <Select name="accountStatus" defaultValue="pending">
                      <SelectTrigger id="accountStatus">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {["pending", "active", "suspended", "inactive"].map((s) => (
                          <SelectItem key={s} value={s} className="capitalize">
                            {s}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1.5 sm:col-span-2 pt-2 border-t">
                    <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      Optional Teacher & Course Allocation
                    </p>
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="alloc-course">Initial Course</Label>
                    <Select value={selectedCourse} onValueChange={setSelectedCourse}>
                      <SelectTrigger id="alloc-course">
                        <SelectValue placeholder="Select Course (Optional)" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="none">None (Allocate Later)</SelectItem>
                        {(coursesQuery.data ?? []).map((c: any) => (
                          <SelectItem key={c.id} value={c.id}>
                            {c.title}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="alloc-trainer">Assigned Teacher</Label>
                    <Select value={selectedTrainer} onValueChange={setSelectedTrainer}>
                      <SelectTrigger id="alloc-trainer">
                        <SelectValue placeholder="Select Teacher (Optional)" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="none">None</SelectItem>
                        {(trainersQuery.data ?? []).map((t: any) => (
                          <SelectItem key={t.id} value={t.id}>
                            {t.name} ({t.email})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1.5 sm:col-span-2">
                    <Label htmlFor="alloc-batch">Batch</Label>
                    <Select value={selectedBatch} onValueChange={setSelectedBatch}>
                      <SelectTrigger id="alloc-batch">
                        <SelectValue placeholder="Select Batch (Optional)" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="none">None</SelectItem>
                        {(batchesQuery.data ?? []).map((b: any) => (
                          <SelectItem key={b.id} value={b.id}>
                            {b.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1.5 sm:col-span-2">
                    <Label htmlFor="profilePhoto">Profile photo</Label>
                    <Input id="profilePhoto" name="profilePhoto" type="file" accept="image/*" />
                  </div>
                </form>
                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                    Cancel
                  </Button>
                  <Button type="submit" form="add-student" disabled={saving}>
                    {saving ? "Saving…" : "Create student"}
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>
        }
      />

      {lowAttendance > 0 ? (
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-4">
          <p className="text-sm">
            <span className="font-medium">⚠ {lowAttendance} students</span> have attendance below
            the configured 80% threshold.
          </p>
          <div className="flex gap-2">
            <Button size="sm" variant="outline" onClick={() => setAttendance("below80")}>
              View students
            </Button>
            <Button size="sm" variant="outline" asChild>
              <Link to="/admin/notifications">Send notification</Link>
            </Button>
          </div>
        </div>
      ) : null}

      <Panel
        title="Student directory"
        description={`${rows.length} of ${students.data?.length ?? 0} students`}
      >
        <div className="mb-4 grid gap-2 md:grid-cols-2 xl:grid-cols-5">
          <div className="relative xl:col-span-1">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
              aria-hidden
            />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Name, email, phone, ID, batch…"
              aria-label="Search students"
              className="pl-9"
              maxLength={80}
            />
          </div>
          {[
            {
              value: status,
              set: setStatus,
              label: "Status",
              options: ["pending", "active", "suspended", "inactive", "archived"],
            },
            {
              value: career,
              set: setCareer,
              label: "Career",
              options: ["eligible", "conditional", "not_eligible", "under_review"],
            },
            {
              value: payment,
              set: setPayment,
              label: "Payment",
              options: ["paid", "partial", "pending", "refunded"],
            },
            {
              value: attendance,
              set: setAttendance,
              label: "Attendance",
              options: ["below80", "above80"],
            },
          ].map((filter) => (
            <Select key={filter.label} value={filter.value} onValueChange={filter.set}>
              <SelectTrigger aria-label={filter.label}>
                <SelectValue placeholder={filter.label} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>All {filter.label.toLowerCase()}</SelectItem>
                {filter.options.map((o) => (
                  <SelectItem key={o} value={o} className="capitalize">
                    {o
                      .replace(/_/g, " ")
                      .replace("below80", "Below 80%")
                      .replace("above80", "80% and above")}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          ))}
        </div>

        {students.isPending ? (
          <Skeleton className="h-72 w-full" />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Student</TableHead>
                  <TableHead>Course / Batch</TableHead>
                  <TableHead>Trainer</TableHead>
                  <TableHead className="text-right">Attendance</TableHead>
                  <TableHead className="text-right">Progress</TableHead>
                  <TableHead>Career</TableHead>
                  <TableHead>Payment</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((s) => (
                  <TableRow key={s.id}>
                    <TableCell>
                      <div className="font-medium">{s.name}</div>
                      <div className="text-xs text-muted-foreground">{s.studentId}</div>
                    </TableCell>
                    <TableCell className="text-sm">
                      <div>{s.courseTitle}</div>
                      <div className="text-xs text-muted-foreground">{s.batchName}</div>
                    </TableCell>
                    <TableCell className="text-sm">{s.trainerName}</TableCell>
                    <TableCell
                      className={`text-right text-sm font-medium ${s.attendancePercent < 80 ? "text-destructive" : ""}`}
                    >
                      {s.attendancePercent}%
                    </TableCell>
                    <TableCell className="text-right text-sm">{s.progressPercent}%</TableCell>
                    <TableCell>
                      <StatusBadge value={s.careerStatus} />
                    </TableCell>
                    <TableCell>
                      <StatusBadge value={s.paymentStatus} />
                    </TableCell>
                    <TableCell>
                      <StatusBadge value={s.status} />
                    </TableCell>
                    <TableCell className="space-x-1 text-right">
                      {s.status === "pending" ? (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() =>
                            void adminService
                              .resendStudentWelcomeEmail(s.id)
                              .then((res) => {
                                if (res?.emailStatus === "FAILED") {
                                  toast.warning(res.message || "Failed to send activation email.");
                                } else {
                                  toast.success(res?.message || `Activation email resent to ${s.email}`);
                                }
                              })
                              .catch(() => toast.error("Could not resend email."))
                          }
                        >
                          Resend Invite
                        </Button>
                      ) : null}
                      <Button size="sm" variant="outline" asChild>
                        <Link to="/admin/students/$studentId" params={{ studentId: s.id }}>
                          View
                        </Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {rows.length === 0 ? (
              <p className="py-8 text-center text-sm text-muted-foreground">
                No students match these filters.
              </p>
            ) : null}
          </div>
        )}
      </Panel>
    </>
  );
}
