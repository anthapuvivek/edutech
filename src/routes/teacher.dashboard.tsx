import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { Activity, BookOpen, Code2, ListChecks, TrendingUp, UserCheck, Users, Video } from "lucide-react";

import { PageHeader, StatCard, StatCardSkeleton } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { teacherService } from "@/services/teacher.service";

export const Route = createFileRoute("/teacher/dashboard")({
  head: () => ({
    meta: [
      { title: "Trainer Dashboard — Learntrix" },
      { name: "description", content: "Trainer view of student engagement, course completion, quiz performance and upcoming classes." },
      { property: "og:title", content: "Trainer Dashboard — Learntrix" },
      { property: "og:description", content: "Monitor your batches, students and class schedule." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: TeacherDashboard,
});

const moduleCompletion = [
  { label: "Module 1 · Foundations", value: 85 },
  { label: "Module 2 · Core Concepts", value: 72 },
  { label: "Module 3 · Advanced Patterns", value: 48 },
  { label: "Module 4 · Capstone Project", value: 21 },
];

function TeacherDashboard() {
  const stats = useQuery({ queryKey: ["teacher", "stats"], queryFn: () => teacherService.stats() });

  return (
    <>
      <PageHeader
        title="Trainer Dashboard"
        description="Only your assigned courses, batches and students are shown."
        action={
          <Button asChild>
            <Link to="/teacher/students">View students</Link>
          </Button>
        }
      />

      <section className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.isPending ? (
          Array.from({ length: 8 }).map((_, i) => <StatCardSkeleton key={i} />)
        ) : stats.isError || !stats.data ? (
          <p className="text-sm text-destructive">Unable to load your analytics. Please try again.</p>
        ) : (
          <>
            <StatCard label="Total Students" value={stats.data.totalStudents} icon={Users} />
            <StatCard label="Active Students" value={stats.data.activeStudents} icon={UserCheck} />
            <StatCard label="Courses" value={stats.data.courses} icon={BookOpen} />
            <StatCard label="Avg Completion" value={`${stats.data.averageCompletion}%`} icon={TrendingUp} />
            <StatCard label="Avg Quiz Score" value={`${stats.data.averageQuizScore}%`} icon={ListChecks} />
            <StatCard label="Problems Solved" value={stats.data.problemsSolved.toLocaleString()} icon={Code2} />
            <StatCard label="Engagement" value={`${stats.data.engagement}%`} icon={Activity} />
            <StatCard label="Upcoming Classes" value={stats.data.upcomingClasses} icon={Video} />
          </>
        )}
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="surface-panel p-6">
          <h2 className="text-xl">Module completion</h2>
          <p className="mt-1 text-sm text-muted-foreground">Across your active batches.</p>
          <div className="mt-5 space-y-4">
            {moduleCompletion.map((m) => (
              <div key={m.label}>
                <div className="flex items-center justify-between text-sm">
                  <span>{m.label}</span>
                  <span className="text-muted-foreground">{m.value}%</span>
                </div>
                <Progress value={m.value} className="mt-2" />
              </div>
            ))}
          </div>
        </div>

        <div className="surface-panel p-6">
          <h2 className="text-xl">Weekly student activity</h2>
          <p className="mt-1 text-sm text-muted-foreground">Sessions logged per day this week.</p>
          <div className="mt-6 flex h-40 items-end gap-3">
            {[62, 78, 55, 91, 84, 40, 28].map((v, i) => (
              <div key={i} className="flex flex-1 flex-col items-center gap-2">
                <div className="w-full rounded-t-sm bg-primary/80" style={{ height: `${v}%` }} />
                <span className="text-xs text-muted-foreground">{["M", "T", "W", "T", "F", "S", "S"][i]}</span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
