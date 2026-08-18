import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import {
  BookOpen,
  CheckCircle2,
  Clock,
  Code2,
  Flame,
  ListChecks,
  Sparkles,
  Star,
  Trophy,
} from "lucide-react";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader, StatCard, StatCardSkeleton } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { studentService } from "@/services/student.service";

export const Route = createFileRoute("/student/dashboard")({
  head: () => ({
    meta: [
      { title: "Student Dashboard — Learntrix" },
      { name: "description", content: "Track your courses, learning hours, coding practice, points and rank in the Learntrix student portal." },
      { property: "og:title", content: "Student Dashboard — Learntrix" },
      { property: "og:description", content: "Your learning progress, points and rank at a glance." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: StudentDashboard,
});

function StudentDashboard() {
  const profile = useQuery({ queryKey: ["student", "profile"], queryFn: () => studentService.profile() });
  const stats = useQuery({ queryKey: ["student", "stats"], queryFn: () => studentService.stats() });
  const enrollments = useQuery({ queryKey: ["student", "enrollments"], queryFn: () => studentService.enrollments() });
  const activity = useQuery({ queryKey: ["student", "activity"], queryFn: () => studentService.activity() });

  const continueLearning = (enrollments.data ?? []).filter(
    (e) => e.status === "active" && e.progressPercent < 100,
  );

  return (
    <>
      <PageHeader title="Dashboard" description="Your learning, practice and performance in one place." />

      <section className="surface-panel mb-6 flex flex-col gap-6 p-6 lg:flex-row lg:items-center lg:justify-between">
        {profile.isPending ? (
          <Skeleton className="h-20 w-full" />
        ) : profile.isError || !profile.data ? (
          <p className="text-sm text-destructive">Unable to load your profile. Please try again.</p>
        ) : (
          <>
            <div className="flex items-center gap-4">
              <span className="grid size-14 place-items-center rounded-full bg-ink text-lg text-ink-foreground">
                {profile.data.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}
              </span>
              <div>
                <h2 className="text-xl">{profile.data.name}</h2>
                <p className="text-sm text-muted-foreground">Student ID · {profile.data.studentId}</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <Badge variant="secondary">Level: {profile.data.level}</Badge>
                  <Badge variant="outline">Rank #{profile.data.rank}</Badge>
                  <Badge variant="outline" className="gap-1">
                    <Flame className="size-3" aria-hidden /> {profile.data.streakDays} day streak
                  </Badge>
                </div>
              </div>
            </div>
            <div className="w-full max-w-sm">
              <div className="flex items-baseline justify-between text-sm">
                <span className="font-medium">{profile.data.points.toLocaleString()} points</span>
                <span className="text-muted-foreground">{profile.data.nextLevelThreshold.toLocaleString()}</span>
              </div>
              <Progress value={(profile.data.points / profile.data.nextLevelThreshold) * 100} className="mt-2" />
              <p className="mt-2 text-xs text-muted-foreground">
                {profile.data.pointsToNextLevel.toLocaleString()} points to {profile.data.nextLevel}. Points and rank are
                calculated by the platform backend.
              </p>
            </div>
          </>
        )}
      </section>

      <section className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.isPending
          ? Array.from({ length: 8 }).map((_, i) => <StatCardSkeleton key={i} />)
          : stats.isError || !stats.data
            ? <p className="text-sm text-destructive">Unable to load your progress. Please try again.</p>
            : (
              <>
                <StatCard label="Total Courses" value={stats.data.totalCourses} icon={BookOpen} />
                <StatCard label="Active Courses" value={stats.data.activeCourses} icon={Sparkles} />
                <StatCard label="Completed" value={stats.data.completedCourses} icon={CheckCircle2} />
                <StatCard label="Learning Hours" value={stats.data.learningHours} icon={Clock} />
                <StatCard label="Problems Solved" value={stats.data.problemsSolved} icon={Code2} />
                <StatCard label="Quiz Average" value={`${stats.data.quizAverage}%`} icon={ListChecks} />
                <StatCard label="Current Points" value={stats.data.points.toLocaleString()} icon={Star} />
                <StatCard label="Current Rank" value={`#${stats.data.rank}`} icon={Trophy} />
              </>
            )}
      </section>

      <section className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <h2 className="mb-4 text-xl">Continue learning</h2>
          {enrollments.isPending ? (
            <div className="space-y-3">
              <Skeleton className="h-28 w-full" />
              <Skeleton className="h-28 w-full" />
            </div>
          ) : enrollments.isError ? (
            <p className="text-sm text-destructive">Unable to load your courses. Please try again.</p>
          ) : continueLearning.length === 0 ? (
            <EmptyState
              icon={BookOpen}
              title="No courses in progress"
              description="No courses enrolled yet. Browse the catalogue to start learning."
              action={
                <Button asChild>
                  <Link to="/courses">Browse courses</Link>
                </Button>
              }
            />
          ) : (
            <div className="space-y-4">
              {continueLearning.map((e) => (
                <article key={e.id} className="surface-panel flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
                  <img
                    src={e.thumbnailUrl}
                    alt=""
                    loading="lazy"
                    className="h-24 w-full rounded-md object-cover sm:w-40"
                  />
                  <div className="min-w-0 flex-1">
                    <h3 className="truncate text-base">{e.courseTitle}</h3>
                    <p className="text-sm text-muted-foreground">{e.instructorName}</p>
                    <Progress value={e.progressPercent} className="mt-3" />
                    <p className="mt-2 text-xs text-muted-foreground">
                      {e.progressPercent}% · {e.lessonsCompleted}/{e.lessonsTotal} lessons · Next: {e.nextLessonTitle}
                    </p>
                  </div>
                  <Button asChild className="shrink-0">
                    <Link to="/student/courses">Continue learning</Link>
                  </Button>
                </article>
              ))}
            </div>
          )}
        </div>

        <div>
          <h2 className="mb-4 text-xl">Recent activity</h2>
          <div className="surface-panel divide-y divide-border">
            {activity.isPending ? (
              <div className="space-y-3 p-5">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-4/5" />
                <Skeleton className="h-4 w-3/5" />
              </div>
            ) : (activity.data ?? []).length === 0 ? (
              <p className="p-5 text-sm text-muted-foreground">No activity yet.</p>
            ) : (
              (activity.data ?? []).map((a) => (
                <div key={a.id} className="flex items-start justify-between gap-3 p-4">
                  <div>
                    <p className="text-sm">{a.title}</p>
                    <p className="text-xs text-muted-foreground">
                      {new Date(a.occurredAt).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" })}
                    </p>
                  </div>
                  <Badge variant="outline" className="shrink-0">+{a.points}</Badge>
                </div>
              ))
            )}
          </div>
        </div>
      </section>
    </>
  );
}
