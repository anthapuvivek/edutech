import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Area, AreaChart, Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Skeleton } from "@/components/ui/skeleton";
import { formatPrice } from "@/lib/format";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/analytics")({
  head: () => ({
    meta: [
      { title: "Analytics — Learntrix Admin" },
      { name: "description", content: "Enterprise reporting on enrolments, attendance, learning performance, placements and revenue." },
      { property: "og:title", content: "Analytics — Learntrix Admin" },
      { property: "og:description", content: "Platform-wide reporting for the Learntrix admin team." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminAnalytics,
});

function AdminAnalytics() {
  const overview = useQuery({ queryKey: ["admin", "overview"], queryFn: () => adminService.overview() });

  if (overview.isPending || !overview.data) return <Skeleton className="h-[70vh] w-full" />;
  const { metrics, studentGrowth, attendanceTrend, performance, placementFunnel } = overview.data;

  return (
    <>
      <PageHeader title="Analytics" description="All figures are computed server-side; charts are read-only reporting views." />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Enrollments" value={metrics.enrollments.toLocaleString()} />
        <StatCard label="Placements" value={metrics.placements} hint={`${metrics.offers} offers`} />
        <StatCard label="Revenue (MTD)" value={formatPrice(metrics.revenueThisMonth)} />
        <StatCard label="New enquiries" value={metrics.newEnquiries} />
      </section>

      <div className="grid gap-4 xl:grid-cols-2">
        <Panel title="Student & enrollment growth">
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart data={studentGrowth}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
              <XAxis dataKey="month" fontSize={12} />
              <YAxis fontSize={12} />
              <Tooltip />
              <Area type="monotone" dataKey="students" stroke="var(--color-primary)" fill="var(--color-primary)" fillOpacity={0.15} />
              <Area type="monotone" dataKey="enrollments" stroke="var(--color-accent)" fill="var(--color-accent)" fillOpacity={0.15} />
            </AreaChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="Attendance & completion trend">
          <ResponsiveContainer width="100%" height={260}>
            <LineChart data={attendanceTrend}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
              <XAxis dataKey="week" fontSize={12} />
              <YAxis fontSize={12} />
              <Tooltip />
              <Line type="monotone" dataKey="attendance" stroke="var(--color-primary)" strokeWidth={2} dot={false} />
              <Line type="monotone" dataKey="completion" stroke="var(--color-accent)" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="Performance by course">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={performance}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
              <XAxis dataKey="label" fontSize={12} />
              <YAxis fontSize={12} />
              <Tooltip />
              <Bar dataKey="quiz" fill="var(--color-primary)" radius={4} />
              <Bar dataKey="coding" fill="var(--color-accent)" radius={4} />
            </BarChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="Placement funnel">
          <ul className="space-y-3">
            {placementFunnel.map((stage) => {
              const max = Math.max(...placementFunnel.map((s) => s.value)) || 1;
              return (
                <li key={stage.stage}>
                  <div className="mb-1 flex items-center justify-between text-sm">
                    <span>{stage.stage}</span>
                    <span className="text-muted-foreground">{stage.value.toLocaleString()}</span>
                  </div>
                  <div className="h-2 rounded-full bg-muted">
                    <div className="h-2 rounded-full bg-primary" style={{ width: `${(stage.value / max) * 100}%` }} />
                  </div>
                </li>
              );
            })}
          </ul>
        </Panel>
      </div>
    </>
  );
}
