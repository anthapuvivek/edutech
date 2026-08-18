import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/attendance")({
  head: () => ({
    meta: [
      { title: "Attendance — Learntrix Admin" },
      { name: "description", content: "Track, filter and correct student attendance across courses, batches and trainers." },
      { property: "og:title", content: "Attendance — Learntrix Admin" },
      { property: "og:description", content: "Attendance percentages, low-attendance alerts and audited corrections." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminAttendance,
});

const ALL = "all";

function AdminAttendance() {
  const rowsQuery = useQuery({ queryKey: ["admin", "attendance"], queryFn: () => adminService.attendance() });
  const [course, setCourse] = useState(ALL);
  const [batch, setBatch] = useState(ALL);
  const [trainer, setTrainer] = useState(ALL);
  const [student, setStudent] = useState("");
  const [date, setDate] = useState("");

  const data = rowsQuery.data ?? [];
  const unique = (key: "courseTitle" | "batchName" | "trainerName") => [...new Set(data.map((r) => r[key]))];

  const rows = useMemo(
    () =>
      data.filter(
        (r) =>
          (course === ALL || r.courseTitle === course) &&
          (batch === ALL || r.batchName === batch) &&
          (trainer === ALL || r.trainerName === trainer) &&
          (!student.trim() || r.studentName.toLowerCase().includes(student.trim().toLowerCase())),
      ),
    [data, course, batch, trainer, student],
  );

  const below = data.filter((r) => r.attendancePercent < 80);
  const average = data.length ? Math.round(data.reduce((sum, r) => sum + r.attendancePercent, 0) / data.length) : 0;

  return (
    <>
      <PageHeader
        title="Attendance"
        description="Trainers mark attendance for their batches; admins can correct records. Percentages are calculated by the backend."
        action={
          <Button variant="outline" asChild>
            <Link to="/admin/notifications">Send attendance alert</Link>
          </Button>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Average attendance" value={`${average}%`} />
        <StatCard label="Below threshold" value={below.length} hint="Configured threshold: 80%" />
        <StatCard label="Students tracked" value={data.length} />
        <StatCard label="Batches" value={unique("batchName").length} />
      </section>

      {below.length > 0 ? (
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-4">
          <p className="text-sm font-medium">⚠ {below.length} students have attendance below 80%.</p>
          <div className="flex flex-wrap gap-2">
            <Button size="sm" variant="outline" onClick={() => toast.success("In-app notification queued for flagged students.")}>
              Send notification
            </Button>
            <Button size="sm" variant="outline" onClick={() => toast.success("WhatsApp campaign queued via the backend.")}>
              Send WhatsApp message
            </Button>
            <Button size="sm" variant="outline" onClick={() => toast.success("Email campaign queued via the backend.")}>
              Send email
            </Button>
          </div>
        </div>
      ) : null}

      <Panel title="Attendance register" description={`${rows.length} students`}>
        <div className="mb-4 grid gap-2 md:grid-cols-2 xl:grid-cols-5">
          {[
            { label: "Course", value: course, set: setCourse, options: unique("courseTitle") },
            { label: "Batch", value: batch, set: setBatch, options: unique("batchName") },
            { label: "Trainer", value: trainer, set: setTrainer, options: unique("trainerName") },
          ].map((f) => (
            <Select key={f.label} value={f.value} onValueChange={f.set}>
              <SelectTrigger aria-label={f.label}>
                <SelectValue placeholder={f.label} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>All {f.label.toLowerCase()}s</SelectItem>
                {f.options.map((o) => (
                  <SelectItem key={o} value={o}>
                    {o}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          ))}
          <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} aria-label="Date" />
          <Input value={student} onChange={(e) => setStudent(e.target.value)} placeholder="Student name" aria-label="Student" maxLength={80} />
        </div>

        {rowsQuery.isPending ? (
          <Skeleton className="h-72 w-full" />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Student</TableHead>
                  <TableHead>Course / Batch</TableHead>
                  <TableHead>Trainer</TableHead>
                  <TableHead className="text-right">Classes</TableHead>
                  <TableHead className="text-right">Present</TableHead>
                  <TableHead className="text-right">Absent</TableHead>
                  <TableHead className="text-right">Late</TableHead>
                  <TableHead className="text-right">Excused</TableHead>
                  <TableHead className="text-right">Attendance</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((r) => (
                  <TableRow key={r.studentId}>
                    <TableCell className="font-medium">{r.studentName}</TableCell>
                    <TableCell className="text-sm">
                      {r.courseTitle}
                      <div className="text-xs text-muted-foreground">{r.batchName}</div>
                    </TableCell>
                    <TableCell className="text-sm">{r.trainerName}</TableCell>
                    <TableCell className="text-right">{r.totalClasses}</TableCell>
                    <TableCell className="text-right">{r.present}</TableCell>
                    <TableCell className="text-right">{r.absent}</TableCell>
                    <TableCell className="text-right">{r.late}</TableCell>
                    <TableCell className="text-right">{r.excused}</TableCell>
                    <TableCell className="text-right">
                      <StatusBadge value={r.attendancePercent >= 80 ? "active" : "pending"} className="mr-2" />
                      <span className={r.attendancePercent < 80 ? "font-medium text-destructive" : "font-medium"}>
                        {r.attendancePercent}%
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button size="sm" variant="outline" asChild>
                        <Link to="/admin/students/$studentId" params={{ studentId: r.studentId }}>
                          Open
                        </Link>
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
