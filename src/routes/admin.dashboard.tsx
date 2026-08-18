import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { BookOpen, CalendarDays, GraduationCap, MessageSquare, Users, Video, Wallet } from "lucide-react";

import { PageHeader, StatCard, StatCardSkeleton } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/format";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/dashboard")({
  head: () => ({
    meta: [
      { title: "Admin Console — Learntrix" },
      { name: "description", content: "Platform-wide view of students, trainers, courses, batches, events and revenue." },
      { property: "og:title", content: "Admin Console — Learntrix" },
      { property: "og:description", content: "Manage the Learntrix learning platform end to end." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminDashboard,
});

function AdminDashboard() {
  const stats = useQuery({ queryKey: ["admin", "stats"], queryFn: () => adminService.stats() });

  return (
    <>
      <PageHeader
        title="Admin Console"
        description="Platform-wide operations. All actions are authorized by the backend."
        action={
          <Button asChild>
            <Link to="/admin/events">Manage events</Link>
          </Button>
        }
      />

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.isPending ? (
          Array.from({ length: 8 }).map((_, i) => <StatCardSkeleton key={i} />)
        ) : stats.isError || !stats.data ? (
          <p className="text-sm text-destructive">Unable to load platform metrics. Please try again.</p>
        ) : (
          <>
            <StatCard label="Students" value={stats.data.students.toLocaleString()} icon={Users} />
            <StatCard label="Trainers" value={stats.data.teachers} icon={GraduationCap} />
            <StatCard label="Courses" value={stats.data.courses} icon={BookOpen} />
            <StatCard label="Batches" value={stats.data.batches} hint={`${stats.data.activeBatches} active`} icon={CalendarDays} />
            <StatCard label="Live Events" value={stats.data.liveEvents} icon={Video} />
            <StatCard label="Open Enquiries" value={stats.data.openEnquiries} icon={MessageSquare} />
            <StatCard label="Revenue (MTD)" value={formatPrice(stats.data.revenueThisMonth)} icon={Wallet} />
            <StatCard label="Active Batches" value={stats.data.activeBatches} icon={CalendarDays} />
          </>
        )}
      </section>
    </>
  );
}
