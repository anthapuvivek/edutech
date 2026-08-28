import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import {
  AlertTriangle,
  Award,
  BookOpen,
  Briefcase,
  CalendarCheck,
  CalendarDays,
  ClipboardList,
  GraduationCap,
  Info,
  MessageSquare,
  ShieldCheck,
  Users,
  Video,
  Wallet,
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard, StatCardSkeleton } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { formatCompact, formatPrice } from "@/lib/format";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/dashboard")({
  head: () => ({
    meta: [
      { title: "Admin Control Center — Learntrix" },
      {
        name: "description",
        content:
          "Platform-wide command center for students, trainers, batches, attendance, career eligibility, jobs and revenue.",
      },
      { property: "og:title", content: "Admin Control Center — Learntrix" },
      {
        property: "og:description",
        content: "Run the entire Learntrix learning and placement operation from one console.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminDashboard,
});

const chartAxis = {
  stroke: "currentColor",
  fontSize: 11,
  tickLine: false,
  axisLine: false,
} as const;

function AdminDashboard() {
  const overview = useQuery({
    queryKey: ["admin", "overview"],
    queryFn: () => adminService.overview(),
  });
  const setup = useQuery({
    queryKey: ["admin", "setup"],
    queryFn: () => adminService.setupSteps(),
  });

  const setupComplete = setup.data
    ? Math.round((setup.data.filter((s) => s.complete).length / setup.data.length) * 100)
    : 0;

  return (
    <>
      <PageHeader
        title="Admin Control Center"
        description="Every number below is computed and authorized by the backend. This console submits admin intent — it never decides eligibility on its own."
        action={
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" asChild>
              <Link to="/admin/attendance">Attendance</Link>
            </Button>
            <Button asChild>
              <Link to="/admin/students">Manage students</Link>
            </Button>
          </div>
        }
      />

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {overview.isPending ? (
          Array.from({ length: 12 }).map((_, i) => <StatCardSkeleton key={i} />)
        ) : overview.isError || !overview.data ? (
          <p className="text-sm text-destructive">
            Unable to load platform metrics. Please try again.
          </p>
        ) : (
          (() => {
            const m = overview.data.metrics;
            return (
              <>
                <StatCard
                  label="Total Students"
                  value={formatCompact(m.students)}
                  hint={`${formatCompact(m.activeStudents)} active · ${formatCompact(m.inactiveStudents)} inactive`}
                  icon={Users}
                />
                <StatCard label="Teachers" value={m.teachers} icon={GraduationCap} />
                <StatCard label="Active Courses" value={m.activeCourses} icon={BookOpen} />
                <StatCard label="Active Batches" value={m.activeBatches} icon={CalendarDays} />
                <StatCard
                  label="Enrollments"
                  value={formatCompact(m.enrollments)}
                  icon={ClipboardList}
                />
                <StatCard label="Today's Classes" value={m.todaysClasses} icon={Video} />
                <StatCard
                  label="Today's Attendance"
                  value={`${m.todaysAttendance}%`}
                  icon={CalendarCheck}
                />
                <StatCard
                  label="Career Eligible"
                  value={formatCompact(m.careerEligible)}
                  hint={`${formatCompact(m.careerIneligible)} not eligible`}
                  icon={ShieldCheck}
                />
                <StatCard label="Active Jobs" value={m.activeJobs} icon={Briefcase} />
                <StatCard
                  label="Applications"
                  value={formatCompact(m.applications)}
                  hint={`${m.interviews} interviews`}
                  icon={ClipboardList}
                />
                <StatCard
                  label="Offers / Placements"
                  value={`${m.offers} / ${m.placements}`}
                  icon={Award}
                />
                <StatCard
                  label="Revenue (MTD)"
                  value={formatPrice(m.revenueThisMonth)}
                  hint={`${m.newEnquiries} new enquiries`}
                  icon={Wallet}
                />
              </>
            );
          })()
        )}
      </section>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <Panel title="Student & enrollment growth" description="Last six months">
          {overview.data ? (
            <ResponsiveContainer width="100%" height={230}>
              <AreaChart data={overview.data.studentGrowth} className="text-muted-foreground">
                <CartesianGrid strokeDasharray="3 3" className="stroke-border" vertical={false} />
                <XAxis dataKey="month" {...chartAxis} />
                <YAxis {...chartAxis} width={44} />
                <Tooltip contentStyle={{ borderRadius: 12, fontSize: 12 }} />
                <Area
                  type="monotone"
                  dataKey="students"
                  stroke="var(--color-primary)"
                  fill="var(--color-primary)"
                  fillOpacity={0.12}
                  strokeWidth={2}
                />
                <Area
                  type="monotone"
                  dataKey="enrollments"
                  stroke="var(--color-accent)"
                  fill="var(--color-accent)"
                  fillOpacity={0.1}
                  strokeWidth={2}
                />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <Skeleton className="h-[230px] w-full" />
          )}
        </Panel>

        <Panel title="Attendance & course completion" description="Rolling six weeks">
          {overview.data ? (
            <ResponsiveContainer width="100%" height={230}>
              <LineChart data={overview.data.attendanceTrend} className="text-muted-foreground">
                <CartesianGrid strokeDasharray="3 3" className="stroke-border" vertical={false} />
                <XAxis dataKey="week" {...chartAxis} />
                <YAxis {...chartAxis} width={44} domain={[40, 100]} />
                <Tooltip contentStyle={{ borderRadius: 12, fontSize: 12 }} />
                <Line
                  type="monotone"
                  dataKey="attendance"
                  stroke="var(--color-primary)"
                  strokeWidth={2}
                  dot={false}
                />
                <Line
                  type="monotone"
                  dataKey="completion"
                  stroke="var(--color-accent)"
                  strokeWidth={2}
                  dot={false}
                />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <Skeleton className="h-[230px] w-full" />
          )}
        </Panel>

        <Panel title="Quiz vs coding performance" description="By program track">
          {overview.data ? (
            <ResponsiveContainer width="100%" height={230}>
              <BarChart data={overview.data.performance} className="text-muted-foreground">
                <CartesianGrid strokeDasharray="3 3" className="stroke-border" vertical={false} />
                <XAxis dataKey="label" {...chartAxis} />
                <YAxis {...chartAxis} width={44} />
                <Tooltip contentStyle={{ borderRadius: 12, fontSize: 12 }} />
                <Bar dataKey="quiz" fill="var(--color-primary)" radius={[4, 4, 0, 0]} />
                <Bar dataKey="coding" fill="var(--color-accent)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <Skeleton className="h-[230px] w-full" />
          )}
        </Panel>

        <Panel title="Placement funnel" description="Career readiness through verified placement">
          {overview.data ? (
            <div className="space-y-3">
              {(overview.data.placementFunnel ?? []).map((stage) => {
                const max = overview.data.placementFunnel?.[0]?.value || 1;
                return (
                  <div key={stage.stage}>
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-muted-foreground">{stage.stage}</span>
                      <span className="font-medium">{formatCompact(stage.value)}</span>
                    </div>
                    <Progress value={(stage.value / max) * 100} className="mt-1.5 h-2" />
                  </div>
                );
              })}
            </div>
          ) : (
            <Skeleton className="h-[230px] w-full" />
          )}
        </Panel>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <Panel title="Today's operations" className="lg:col-span-2">
          <ul className="divide-y divide-border">
            {(overview.data?.todaysOperations ?? []).map((op) => (
              <li key={op.id} className="flex items-center justify-between gap-4 py-3">
                <div>
                  <p className="text-sm font-medium">{op.label}</p>
                  <p className="text-xs text-muted-foreground">{op.detail}</p>
                </div>
                <Badge variant="outline">{op.count}</Badge>
              </li>
            ))}
          </ul>
        </Panel>

        <Panel title="Alerts" description="Needs admin attention">
          <ul className="space-y-3">
            {(overview.data?.alerts ?? []).map((alert) => (
              <li key={alert.id} className="rounded-lg border border-border p-3">
                <div className="flex items-start gap-2">
                  {alert.severity === "warning" ? (
                    <AlertTriangle
                      className="mt-0.5 size-4 shrink-0 text-destructive"
                      aria-hidden
                    />
                  ) : (
                    <Info className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden />
                  )}
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium">
                      {alert.count} · {alert.title}
                    </p>
                    {alert.to ? (
                      <Button variant="link" size="sm" className="h-auto p-0 text-xs" asChild>
                        <Link to={alert.to}>{alert.actionLabel}</Link>
                      </Button>
                    ) : null}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </Panel>
      </div>

      <Panel
        title="Platform setup"
        description={`${setupComplete}% complete — finish configuration to unlock the full control center.`}
        className="mt-6"
        action={
          <Button variant="outline" size="sm" asChild>
            <Link to="/admin/settings">Open settings</Link>
          </Button>
        }
      >
        <Progress value={setupComplete} className="mb-4 h-2" />
        <ol className="grid gap-2 sm:grid-cols-2">
          {(setup.data ?? []).map((step, i) => (
            <li
              key={step.id}
              className="flex items-start gap-3 rounded-lg border border-border p-3"
            >
              <span className="mt-0.5 grid size-6 shrink-0 place-items-center rounded-full bg-muted text-xs font-medium">
                {i + 1}
              </span>
              <div>
                <div className="text-sm font-medium">
                  {step.label}{" "}
                  {step.complete ? (
                    <Badge variant="success" className="ml-1 align-middle">
                      Done
                    </Badge>
                  ) : (
                    <Badge variant="warning" className="ml-1 align-middle">
                      Pending
                    </Badge>
                  )}
                </div>
                <p className="text-xs text-muted-foreground">{step.description}</p>
              </div>
            </li>
          ))}
        </ol>
      </Panel>

      <p className="mt-6 flex items-center gap-2 text-xs text-muted-foreground">
        <MessageSquare className="size-3.5" aria-hidden />
        Notifications, WhatsApp campaigns and alert automation are configured in Communication and
        dispatched by the backend.
      </p>
    </>
  );
}
