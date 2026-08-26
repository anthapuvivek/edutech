import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { ArrowLeft, Calendar, CheckCircle2, Clock, PlayCircle, Video } from "lucide-react";

import { PageHeader } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { courseService } from "@/services/course.service";
import { recordingService } from "@/services/recording.service";
import type { RecordingProgress } from "@/types/recording";

export const Route = createFileRoute("/student/courses/$slug/recordings")({
  head: ({ params }) => {
    const readable = params.slug.replace(/-/g, " ");
    return {
      meta: [{ title: `Recorded Classes — ${readable}` }, { name: "robots", content: "noindex" }],
    };
  },
  component: CourseRecordingsPage,
});

function CourseRecordingsPage() {
  const { slug } = Route.useParams();

  // Load course details by slug
  const courseQuery = useQuery({
    queryKey: ["course", slug],
    queryFn: () => courseService.bySlug(slug),
  });

  const course = courseQuery.data;

  // Load recordings for this course
  const recordingsQuery = useQuery({
    queryKey: ["student", "recordings", course?.id],
    queryFn: () => recordingService.getPublishedRecordingsForCourse(course!.id),
    enabled: !!course,
  });

  // Load student progress map
  const progressQuery = useQuery({
    queryKey: ["student", "recordings", "progress", course?.id],
    queryFn: async () => {
      const items = recordingsQuery.data ?? [];
      const progressMap: Record<string, RecordingProgress> = {};
      for (const item of items) {
        try {
          const prog = await recordingService.getProgress(item.id);
          progressMap[item.id] = prog;
        } catch {
          // ignore
        }
      }
      return progressMap;
    },
    enabled: (recordingsQuery.data ?? []).length > 0,
  });

  const recordings = recordingsQuery.data ?? [];
  const progressMap = progressQuery.data ?? {};

  const formatDuration = (seconds: number) => {
    if (!seconds) return "--";
    const mins = Math.floor(seconds / 60);
    const hrs = Math.floor(mins / 60);
    const remMins = mins % 60;
    return hrs > 0 ? `${hrs}h ${remMins}m` : `${remMins}m`;
  };

  if (courseQuery.isLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <Loader2 className="size-8 animate-spin text-accent" />
      </div>
    );
  }

  if (courseQuery.isError || !course) {
    return (
      <div className="surface-panel p-8 text-center text-destructive">
        Course details could not be loaded. Please verify the URL.
      </div>
    );
  }

  return (
    <>
      <div className="mb-4">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/student/courses">
            <ArrowLeft className="mr-2 size-4" /> Back to My Courses
          </Link>
        </Button>
      </div>

      <PageHeader
        title={`${course.title} — Recorded Classes`}
        description="Review recording sessions corresponding to your course lessons."
      />

      {recordingsQuery.isLoading ? (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-64 w-full animate-pulse rounded-md bg-muted/30" />
          ))}
        </div>
      ) : recordingsQuery.isError ? (
        <div className="surface-panel p-8 text-center text-destructive">
          Failed to load class recordings for this course.
        </div>
      ) : recordings.length === 0 ? (
        <div className="surface-panel py-16 text-center">
          <Video className="mx-auto size-12 text-muted-foreground/60" />
          <h3 className="mt-4 text-lg font-medium">No recorded classes published</h3>
          <p className="mt-2 text-sm text-muted-foreground max-w-md mx-auto">
            Once your instructor uploads recordings for this course, they will appear here.
          </p>
        </div>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {recordings.map((r) => {
            const prog = progressMap[r.id];
            const pct =
              prog && prog.durationSeconds > 0
                ? Math.min(Math.round((prog.watchedSeconds / prog.durationSeconds) * 100), 100)
                : 0;
            const completed = prog ? prog.completed : false;

            return (
              <article key={r.id} className="surface-panel flex flex-col overflow-hidden">
                <div className="relative aspect-video bg-ink">
                  <img
                    src={
                      r.thumbnailUrl ||
                      "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=800&q=70"
                    }
                    alt=""
                    loading="lazy"
                    className="w-full h-full object-cover opacity-85"
                  />
                  {completed && (
                    <div className="absolute top-2 right-2 bg-success/90 text-success-foreground p-1 rounded-full shadow">
                      <CheckCircle2 className="size-4" />
                    </div>
                  )}
                </div>

                <div className="flex flex-1 flex-col p-5">
                  <div className="space-y-1">
                    <h2 className="text-base font-bold text-foreground leading-snug line-clamp-2">
                      <Link
                        to="/student/recordings/$id"
                        params={{ id: r.id }}
                        className="hover:underline"
                      >
                        {r.title}
                      </Link>
                    </h2>
                    <p className="text-xs text-muted-foreground">
                      {r.moduleTitle} · {r.lessonTitle}
                    </p>
                  </div>

                  <div className="mt-4 grid grid-cols-2 gap-2 text-xs text-muted-foreground border-y py-2.5">
                    <span className="flex items-center gap-1">
                      <Calendar className="size-3.5" />
                      {new Date(r.classDate).toLocaleDateString("en-IN", { dateStyle: "short" })}
                    </span>
                    <span className="flex items-center gap-1 justify-end">
                      <Clock className="size-3.5" />
                      {formatDuration(r.durationSeconds)}
                    </span>
                  </div>

                  {pct > 0 && (
                    <div className="mt-4 space-y-1.5">
                      <div className="flex justify-between text-2xs text-muted-foreground">
                        <span>Progress</span>
                        <span>{pct}%</span>
                      </div>
                      <Progress value={pct} className="h-1.5" />
                    </div>
                  )}

                  <div className="mt-5 flex gap-2">
                    <Button asChild className="w-full" variant={completed ? "outline" : "default"}>
                      <Link to="/student/recordings/$id" params={{ id: r.id }} className="gap-2">
                        <PlayCircle className="size-4" />
                        {completed ? "Watch Again" : pct > 0 ? "Continue Watching" : "Watch Class"}
                      </Link>
                    </Button>
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

// Simple fallback spinner icon
function Loader2(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg
      {...props}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={`animate-spin ${props.className || ""}`}
    >
      <path d="M21 12a9 9 0 1 1-6.219-8.56" />
    </svg>
  );
}
