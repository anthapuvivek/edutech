import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { AlertTriangle, Info } from "lucide-react";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { progressService } from "@/services/progress.service";
import { pct } from "@/types/progress";

export const Route = createFileRoute("/teacher/analytics")({
  head: () => ({
    meta: [
      { title: "Analytics — Learntrix Trainer" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: TeacherAnalyticsPage,
});

/** Fixed order so the chart reads best-to-worst rather than by object key order. */
const BUCKETS = ["90-100", "75-89", "60-74", "below-60"] as const;

function TeacherAnalyticsPage() {
  const query = useQuery({
    queryKey: ["teacher", "analytics"],
    queryFn: () => progressService.teacherAnalytics(),
  });

  if (query.isLoading) {
    return (
      <>
        <PageHeader title="Analytics" description="" />
        <Skeleton className="h-48 w-full" />
      </>
    );
  }

  if (query.isError || !query.data) {
    return (
      <>
        <PageHeader title="Analytics" description="" />
        <Panel title="Could not load analytics" description="">
          <p className="py-6 text-sm text-muted-foreground">
            Something went wrong. Refresh the page to try again.
          </p>
        </Panel>
      </>
    );
  }

  const a = query.data;
  const dist = a.performanceDistribution ?? {};
  const distTotal = BUCKETS.reduce((n, k) => n + (dist[k] ?? 0), 0);
  const atRisk = a.atRiskStudents ?? [];

  return (
    <>
      <PageHeader
        title="Analytics"
        description="Across the batches you run. Computed from stored records only."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Students" value={String(a.studentCount ?? 0)} />
        <StatCard label="Average progress" value={pct(a.averageProgress)} />
        <StatCard label="Average quiz score" value={pct(a.averageQuizScore)} />
        <StatCard label="Average assignment score" value={pct(a.averageAssignmentScore)} />
      </section>

      <Panel title="Engagement" description="Proportion of set work that has actually been done.">
        <ul className="flex flex-col gap-4">
          <li>
            <div className="mb-1.5 flex items-center justify-between text-sm">
              <span className="font-medium">Assignment submission rate</span>
              <span className="text-muted-foreground">{pct(a.assignmentSubmissionRate)}</span>
            </div>
            <Progress value={a.assignmentSubmissionRate ?? 0} />
          </li>
          <li>
            <div className="mb-1.5 flex items-center justify-between text-sm">
              <span className="font-medium">Quiz participation</span>
              <span className="text-muted-foreground">{pct(a.quizParticipationRate)}</span>
            </div>
            <Progress value={a.quizParticipationRate ?? 0} />
          </li>
        </ul>
      </Panel>

      <Panel title="Performance distribution" description="Students grouped by overall progress.">
        {distTotal === 0 ? (
          <p className="py-6 text-sm text-muted-foreground">
            No students with recorded activity yet.
          </p>
        ) : (
          <ul className="flex flex-col gap-3">
            {BUCKETS.map((b) => {
              const count = dist[b] ?? 0;
              const share = distTotal === 0 ? 0 : Math.round((count / distTotal) * 100);
              return (
                <li key={b}>
                  <div className="mb-1.5 flex items-center justify-between text-sm">
                    <span className="font-medium">{b === "below-60" ? "Below 60%" : `${b}%`}</span>
                    <span className="text-muted-foreground">
                      {count} student{count === 1 ? "" : "s"}
                    </span>
                  </div>
                  <Progress value={share} />
                </li>
              );
            })}
          </ul>
        )}
      </Panel>

      <Panel
        title="Students needing attention"
        description="Flagged by stated thresholds only — every reason is listed so you can disagree with it."
      >
        {atRisk.length === 0 ? (
          <p className="py-6 text-sm text-muted-foreground">
            No students currently meet any of the attention thresholds.
          </p>
        ) : (
          <ol className="flex flex-col gap-3">
            {atRisk.map((s) => (
              <li key={s.studentId} className="rounded-md border p-3">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="flex items-center gap-2 font-medium">
                      <AlertTriangle className="size-4 text-destructive" aria-hidden />
                      {s.studentName}
                    </p>
                    <ul className="mt-2 list-inside list-disc text-sm text-muted-foreground">
                      {s.reasons.map((r) => (
                        <li key={r}>{r}</li>
                      ))}
                    </ul>
                  </div>
                  <div className="flex shrink-0 gap-2">
                    <Badge variant="outline">{s.overallProgress ?? 0}% progress</Badge>
                    {s.averageQuizScore != null ? (
                      <Badge variant="outline">{s.averageQuizScore}% quiz</Badge>
                    ) : null}
                  </div>
                </div>
              </li>
            ))}
          </ol>
        )}
      </Panel>

      {a.unavailableMetrics && a.unavailableMetrics.length > 0 ? (
        <Panel title="Not measured yet" description="">
          <ul className="flex flex-col gap-2">
            {a.unavailableMetrics.map((m) => (
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
