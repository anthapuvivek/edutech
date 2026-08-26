import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { BookOpen } from "lucide-react";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { studentService } from "@/services/student.service";
import type { EnrollmentStatus } from "@/types/lms";

export const Route = createFileRoute("/student/courses")({
  head: () => ({
    meta: [
      { title: "My Courses — Learntrix" },
      {
        name: "description",
        content: "All the Learntrix courses you are enrolled in, with progress and access status.",
      },
      { property: "og:title", content: "My Courses — Learntrix" },
      { property: "og:description", content: "Your enrolled Learntrix courses and progress." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: MyCourses,
});

const statusLabel: Record<EnrollmentStatus, string> = {
  not_enrolled: "Not enrolled",
  payment_pending: "Payment pending",
  enrolled: "Enrolled",
  active: "Active",
  completed: "Completed",
  expired: "Expired",
};

function MyCourses() {
  const enrollments = useQuery({
    queryKey: ["student", "enrollments"],
    queryFn: () => studentService.enrollments(),
  });

  return (
    <>
      <PageHeader
        title="My Courses"
        description="Access is granted by the platform backend after successful enrollment or payment."
        action={
          <Button variant="outline" asChild>
            <Link to="/courses">Browse catalogue</Link>
          </Button>
        }
      />

      {enrollments.isPending ? (
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-64 w-full" />
          ))}
        </div>
      ) : enrollments.isError ? (
        <p className="text-sm text-destructive">Unable to load your courses. Please try again.</p>
      ) : (enrollments.data ?? []).length === 0 ? (
        <EmptyState
          icon={BookOpen}
          title="No courses enrolled yet"
          description="Once you enroll in a course it will appear here with full learning access."
          action={
            <Button asChild>
              <Link to="/courses">Explore courses</Link>
            </Button>
          }
        />
      ) : (
        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
          {enrollments.data.map((e) => {
            const locked =
              e.status === "payment_pending" ||
              e.status === "expired" ||
              e.status === "not_enrolled";
            return (
              <article key={e.id} className="surface-panel flex flex-col overflow-hidden">
                <img
                  src={e.thumbnailUrl}
                  alt=""
                  loading="lazy"
                  className="aspect-16/9 w-full object-cover"
                />
                <div className="flex flex-1 flex-col p-5">
                  <div className="flex items-start justify-between gap-3">
                    <h2 className="text-base leading-snug">{e.courseTitle}</h2>
                    <Badge
                      variant={
                        e.status === "completed" ? "secondary" : locked ? "outline" : "default"
                      }
                    >
                      {statusLabel[e.status]}
                    </Badge>
                  </div>
                  <p className="mt-1 text-sm text-muted-foreground">{e.instructorName}</p>
                  <Progress value={e.progressPercent} className="mt-4" />
                  <p className="mt-2 text-xs text-muted-foreground">
                    {e.progressPercent}% · {e.lessonsCompleted}/{e.lessonsTotal} lessons completed
                  </p>
                  <div className="mt-5 flex gap-2">
                    {locked ? (
                      <>
                        <Button variant="outline" className="flex-1" asChild>
                          <Link to="/courses/$slug" params={{ slug: e.courseSlug }}>
                            View course
                          </Link>
                        </Button>
                        <Button className="flex-1" disabled>
                          Access locked
                        </Button>
                      </>
                    ) : (
                      <div className="flex flex-col gap-2 w-full">
                        <Button className="w-full" asChild>
                          <Link
                            to="/student/courses/$slug/recordings"
                            params={{ slug: e.courseSlug }}
                          >
                            {e.status === "completed" ? "Review Recordings" : "Recorded Classes"}
                          </Link>
                        </Button>
                        <Button variant="outline" className="w-full" asChild>
                          <Link to="/courses/$slug" params={{ slug: e.courseSlug }}>
                            Course Landing Page
                          </Link>
                        </Button>
                      </div>
                    )}
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </>
  );
}
