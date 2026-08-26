import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import { Calendar, CheckCircle2, Clock, PlayCircle, Search, Video } from "lucide-react";
import { useState } from "react";

import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";
import { recordingService } from "@/services/recording.service";
import type { RecordingProgress } from "@/types/recording";

export const Route = createFileRoute("/student/recordings")({
  head: () => ({
    meta: [
      { title: "Recorded Classes — Learntrix Student" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: StudentRecordingsPage,
});

function StudentRecordingsPage() {
  const [search, setSearch] = useState("");

  const recordingsQuery = useQuery({
    queryKey: ["student", "recordings"],
    queryFn: () => recordingService.getStudentRecordings(),
  });

  const progressQuery = useQuery({
    queryKey: ["student", "recordings", "progress"],
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

  const filtered = recordings.filter(
    (r) =>
      r.title.toLowerCase().includes(search.toLowerCase()) ||
      r.courseTitle.toLowerCase().includes(search.toLowerCase()),
  );

  const formatDuration = (seconds: number) => {
    if (!seconds) return "--";
    const mins = Math.floor(seconds / 60);
    const hrs = Math.floor(mins / 60);
    const remMins = mins % 60;
    return hrs > 0 ? `${hrs}h ${remMins}m` : `${mins}m`;
  };

  return (
    <>
      <PageHeader
        title="Recorded Classes"
        description="Access class recordings and watch lessons at your own pace. Your progress is saved automatically."
      />

      <div className="surface-panel mb-6 p-4">
        <div className="relative max-w-sm">
          <Search className="absolute top-2.5 left-3 size-4 text-muted-foreground" />
          <Input
            placeholder="Search by topic, lesson or course..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>
      </div>

      {recordingsQuery.isLoading ? (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-64 w-full animate-pulse rounded-md bg-muted/30" />
          ))}
        </div>
      ) : recordingsQuery.isError ? (
        <div className="surface-panel p-8 text-center text-destructive">
          Failed to load class recordings. Please verify login or enrollment.
        </div>
      ) : filtered.length === 0 ? (
        <div className="surface-panel py-16 text-center">
          <Video className="mx-auto size-12 text-muted-foreground/60" />
          <h3 className="mt-4 text-lg font-medium">No class recordings available</h3>
          <p className="mt-2 text-sm text-muted-foreground max-w-md mx-auto">
            {search
              ? "No classes matched your query."
              : "Once your instructor publishes an online class recording, it will appear here."}
          </p>
        </div>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((r) => {
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
                    <span className="text-xs font-semibold text-accent tracking-wider uppercase">
                      {r.courseTitle}
                    </span>
                    <h2 className="text-base font-bold text-foreground leading-snug line-clamp-2">
                      <Link
                        to="/student/recordings/$id"
                        params={{ id: r.id }}
                        className="hover:underline"
                      >
                        {r.title}
                      </Link>
                    </h2>
                    <p className="text-xs text-muted-foreground">Instructor: {r.teacherName}</p>
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
