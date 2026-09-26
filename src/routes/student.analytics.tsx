import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Info } from "lucide-react";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { progressService } from "@/services/progress.service";
import { pct } from "@/types/progress";

export const Route = createFileRoute("/student/analytics")({
  head: () => ({
    meta: [
      { title: "Analytics — Learntrix Student" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: StudentAnalyticsPage,
});

function StudentAnalyticsPage() {
  const query = useQuery({
    queryKey: ["student", "analytics"],
    queryFn: () => progressService.myAnalytics(),
  });

  if (query.isLoading) {
    return (
      <>
        <PageHeader title="Analytics" description="" />
        <Skeleton className="h-40 w-full" />
      </>
    );
  }

  if (query.isError || !query.data) {
    return (
      <>
        <PageHeader title="Analytics" description="" />
        <Panel title="Could not load your analytics" description="">
          <p className="py-6 text-sm text-muted-foreground">
            Something went wrong. Refresh the page to try again.
          </p>
        </Panel>
      </>
    );
  }

  const a = query.data;
  const nothingYet =
    !a.quizzesAttempted && !a.assignmentsSubmitted && !a.codingSolved && !a.recordingsWatched;

  return (
    <>
      <PageHeader
        title="Analytics"
        description="Your own performance, computed from your stored activity."
      />

      {nothingYet ? (
        <Panel title="Nothing to analyse yet" description="">
          <p className="py-8 text-center text-sm text-muted-foreground">
            Once you attempt a quiz, submit an assignment or complete a coding problem, your
            analytics will appear here. Nothing is estimated before then.
          </p>
        </Panel>
      ) : (
        <>
          <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Overall progress" value={pct(a.averageProgress)} />
            <StatCard label="Quiz average" value={pct(a.averageQuizScore)} />
            <StatCard label="Assignment average" value={pct(a.averageAssignmentScore)} />
            <StatCard label="Coding solved" value={String(a.codingSolved ?? 0)} />
          </section>

          <Panel title="Participation" description="How much of the set work you have engaged with.">
            <ul className="flex flex-col gap-4">
              <li>
                <div className="mb-1.5 flex items-center justify-between text-sm">
                  <span className="font-medium">Quizzes attempted</span>
                  <span className="text-muted-foreground">{pct(a.quizParticipationRate)}</span>
                </div>
                <Progress value={a.quizParticipationRate ?? 0} />
              </li>
              <li>
                <div className="mb-1.5 flex items-center justify-between text-sm">
                  <span className="font-medium">Assignments submitted</span>
                  <span className="text-muted-foreground">{pct(a.assignmentSubmissionRate)}</span>
                </div>
                <Progress value={a.assignmentSubmissionRate ?? 0} />
              </li>
            </ul>
          </Panel>

          <Panel title="Activity totals" description="">
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <StatCard label="Quizzes attempted" value={String(a.quizzesAttempted ?? 0)} />
              <StatCard label="Assignments submitted" value={String(a.assignmentsSubmitted ?? 0)} />
              <StatCard label="Coding solved" value={String(a.codingSolved ?? 0)} />
              <StatCard label="Recordings watched" value={String(a.recordingsWatched ?? 0)} />
            </div>
          </Panel>
        </>
      )}

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
