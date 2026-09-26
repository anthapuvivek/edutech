import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Info } from "lucide-react";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { progressService } from "@/services/progress.service";
import { pct } from "@/types/progress";

export const Route = createFileRoute("/student/progress")({
  head: () => ({
    meta: [
      { title: "Progress — Learntrix Student" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: StudentProgressPage,
});

function StudentProgressPage() {
  const query = useQuery({
    queryKey: ["student", "progress"],
    queryFn: () => progressService.myProgress(),
  });

  if (query.isLoading) {
    return (
      <>
        <PageHeader title="Progress" description="" />
        <div className="space-y-3">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-40 w-full" />
        </div>
      </>
    );
  }

  if (query.isError || !query.data) {
    return (
      <>
        <PageHeader title="Progress" description="" />
        <Panel title="Could not load your progress" description="">
          <p className="py-6 text-sm text-muted-foreground">
            Something went wrong. Refresh the page to try again.
          </p>
        </Panel>
      </>
    );
  }

  const p = query.data;
  const areas = p.areas ?? [];

  return (
    <>
      <PageHeader
        title="Progress"
        description="Calculated from what you have actually completed — quizzes, assignments, coding practice and recorded classes."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Overall progress" value={`${p.overallProgress}%`} />
        <StatCard label="Quiz average" value={pct(p.averageQuizScore)} />
        <StatCard label="Assignment average" value={pct(p.averageAssignmentScore)} />
        <StatCard
          label="Coding solved"
          value={`${p.codingSolved ?? 0}/${p.codingTotal ?? 0}`}
        />
      </section>

      <Panel
        title="By area"
        description="An area with nothing in it is not counted towards your overall progress."
      >
        <ul className="flex flex-col gap-4">
          {areas.map((a) => (
            <li key={a.area}>
              <div className="mb-1.5 flex items-center justify-between gap-3 text-sm">
                <span className="font-medium">{a.area}</span>
                <span className="text-muted-foreground">
                  {a.applicable ? (
                    <>
                      {a.completed ?? 0}/{a.total ?? 0} · {pct(a.percent)}
                      {a.averageScore != null ? ` · avg ${a.averageScore}%` : ""}
                    </>
                  ) : (
                    "Nothing set yet"
                  )}
                </span>
              </div>
              {/* Zero-width bar for a non-applicable area, so it never reads as a failure. */}
              <Progress value={a.applicable ? (a.percent ?? 0) : 0} />
            </li>
          ))}
        </ul>
      </Panel>

      <Panel title="Activity" description="">
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard
            label="Quizzes attempted"
            value={`${p.quizzesAttempted ?? 0}/${p.quizzesTotal ?? 0}`}
          />
          <StatCard
            label="Assignments submitted"
            value={`${p.assignmentsSubmitted ?? 0}/${p.assignmentsTotal ?? 0}`}
          />
          <StatCard
            label="Recordings watched"
            value={`${p.recordingsWatched ?? 0}/${p.recordingsTotal ?? 0}`}
          />
          <StatCard
            label="Last activity"
            value={p.lastActivityAt ? new Date(p.lastActivityAt).toLocaleDateString() : "—"}
          />
        </div>
      </Panel>

      {p.unavailableMetrics && p.unavailableMetrics.length > 0 ? (
        <Panel title="Not measured yet" description="">
          <ul className="flex flex-col gap-2">
            {p.unavailableMetrics.map((m) => (
              <li key={m} className="flex items-start gap-2 text-sm text-muted-foreground">
                <Info className="mt-0.5 size-4 shrink-0" aria-hidden />
                {m}
              </li>
            ))}
          </ul>
        </Panel>
      ) : null}
    </>
  );
}
