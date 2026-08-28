import { useQuery } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import {
  Bell,
  Bookmark,
  Briefcase,
  CalendarClock,
  CheckCircle2,
  Circle,
  Clock3,
  Handshake,
  Target,
  TrendingUp,
} from "lucide-react";

import { PageHeader, StatCard, StatCardSkeleton } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { careerService } from "@/services/career.service";

export const Route = createFileRoute("/student/career/")({
  head: () => ({
    meta: [
      { title: "Career Dashboard — Learntrix" },
      {
        name: "description",
        content:
          "Track career readiness, recommended jobs, applications, interviews and referral opportunities.",
      },
      { property: "og:title", content: "Career Dashboard — Learntrix" },
      {
        property: "og:description",
        content: "Your placement readiness cockpit inside the Learntrix student portal.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: CareerDashboardPage,
});

function CareerDashboardPage() {
  const dash = useQuery({
    queryKey: ["career", "dashboard"],
    queryFn: () => careerService.dashboard(),
  });
  const data = dash.data;

  return (
    <>
      <PageHeader
        title="Career dashboard"
        description="Readiness scores are computed by the backend from your learning, coding and interview activity."
        action={
          data ? (
            <Badge variant="outline" className="h-fit">
              Status · {data.placementStatus}
            </Badge>
          ) : undefined
        }
      />

      {dash.isLoading || !data ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <StatCardSkeleton key={i} />
          ))}
        </div>
      ) : (
        <div className="space-y-6">
          <section className="surface-panel p-6">
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">
                  Career readiness
                </p>
                <p className="text-display text-4xl leading-none">{data.readiness.overall}%</p>
              </div>
              <p className="text-xs text-muted-foreground">
                Updated {new Date(data.readiness.updatedAt).toLocaleDateString()}
              </p>
            </div>
            <Progress value={data.readiness.overall} className="mt-4" />
            <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {(data.readiness?.components ?? []).map((c) => (
                <div key={c.label}>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">{c.label}</span>
                    <span className="font-medium">{c.score}</span>
                  </div>
                  <Progress value={c.score} className="mt-1.5 h-1.5" />
                </div>
              ))}
            </div>
          </section>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Applications" value={(data.applications ?? []).length} icon={Briefcase} />
            <StatCard
              label="Interviews"
              value={(data.upcomingInterviews ?? []).length}
              icon={CalendarClock}
            />
            <StatCard label="Saved jobs" value={(data.savedJobs ?? []).length} icon={Bookmark} />
            <StatCard
              label="Referral options"
              value={(data.referralOpportunities ?? []).length}
              icon={Handshake}
            />
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <section className="surface-panel p-6 lg:col-span-2">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-display text-xl">Recommended jobs</h2>
                <Button asChild variant="ghost" size="sm">
                  <Link to="/student/career/jobs">View all</Link>
                </Button>
              </div>
              <ul className="divide-y divide-border">
                {(data.recommendedJobs ?? []).map((job) => (
                  <li
                    key={job.id}
                    className="flex flex-wrap items-center justify-between gap-3 py-3"
                  >
                    <div>
                      <p className="font-medium">
                        {job.title} — {job.companyName}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {job.location} · {job.workMode} · {job.experience}
                      </p>
                    </div>
                    <Badge variant="success">Match {job.matchPercent}%</Badge>
                  </li>
                ))}
              </ul>
              <p className="mt-4 text-xs text-muted-foreground">
                Match scores are guidance only and are not a guarantee of shortlisting or
                employment.
              </p>
            </section>

            <section className="surface-panel p-6">
              <h2 className="text-display mb-4 text-xl">Placement readiness</h2>
              <ul className="space-y-2.5">
                {(data.checklist ?? []).map((item) => (
                  <li key={item.label} className="flex items-start gap-2 text-sm">
                    {item.status === "complete" ? (
                      <CheckCircle2 className="mt-0.5 size-4 text-success" aria-hidden />
                    ) : item.status === "in_progress" ? (
                      <Clock3 className="mt-0.5 size-4 text-muted-foreground" aria-hidden />
                    ) : (
                      <Circle className="mt-0.5 size-4 text-muted-foreground" aria-hidden />
                    )}
                    <span>
                      {item.label}
                      {item.detail ? (
                        <span className="block text-xs text-muted-foreground">{item.detail}</span>
                      ) : null}
                    </span>
                  </li>
                ))}
              </ul>
            </section>

            <section className="surface-panel p-6">
              <h2 className="text-display mb-4 text-xl">Upcoming interviews</h2>
              {(data.upcomingInterviews ?? []).length ? (
                <ul className="space-y-3 text-sm">
                  {(data.upcomingInterviews ?? []).map((a) => (
                    <li key={a.id}>
                      <p className="font-medium">
                        {a.companyName} — {a.role}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {a.interviewAt
                          ? new Date(a.interviewAt).toLocaleString()
                          : "Date to be confirmed"}
                      </p>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-muted-foreground">No interviews scheduled yet.</p>
              )}
            </section>

            <section className="surface-panel p-6">
              <h2 className="text-display mb-4 text-xl">Internships</h2>
              <ul className="space-y-3 text-sm">
                {(data.recommendedInternships ?? []).map((job) => (
                  <li key={job.id} className="flex items-center justify-between gap-3">
                    <span>
                      {job.title}
                      <span className="block text-xs text-muted-foreground">{job.companyName}</span>
                    </span>
                    <Badge variant="outline">{job.matchPercent}%</Badge>
                  </li>
                ))}
              </ul>
            </section>

            <section className="surface-panel p-6">
              <h2 className="text-display mb-4 flex items-center gap-2 text-xl">
                <Target className="size-4" aria-hidden /> Skills to improve
              </h2>
              <div className="flex flex-wrap gap-2">
                {(data.skillsToImprove ?? []).map((s) => (
                  <Badge key={s} variant="warning">
                    {s}
                  </Badge>
                ))}
              </div>
              <Button asChild variant="outline" size="sm" className="mt-4">
                <Link to="/student/courses">Find a course</Link>
              </Button>
            </section>

            <section className="surface-panel p-6 lg:col-span-2">
              <h2 className="text-display mb-4 flex items-center gap-2 text-xl">
                <Handshake className="size-4" aria-hidden /> Referral opportunities
              </h2>
              <ul className="divide-y divide-border">
                {(data.referralOpportunities ?? []).map((r) => (
                  <li
                    key={r.id}
                    className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm"
                  >
                    <div>
                      <p className="font-medium">
                        {r.role} — {r.companyName}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {r.referralType} · {r.location} · {r.seatsAvailable} available
                      </p>
                    </div>
                    <Badge variant={r.verified ? "success" : "outline"}>
                      {r.verified ? "Admin verified" : "Pending verification"}
                    </Badge>
                  </li>
                ))}
              </ul>
            </section>

            <section className="surface-panel p-6">
              <h2 className="text-display mb-4 flex items-center gap-2 text-xl">
                <Bell className="size-4" aria-hidden /> Career alerts
              </h2>
              <ul className="space-y-3 text-sm">
                {(data.notifications ?? []).map((n) => (
                  <li key={n.id}>
                    <p>{n.title}</p>
                    <p className="text-xs text-muted-foreground">
                      {new Date(n.createdAt).toLocaleString()}
                    </p>
                  </li>
                ))}
              </ul>
            </section>
          </div>

          <p className="flex items-center gap-2 text-xs text-muted-foreground">
            <TrendingUp className="size-3.5" aria-hidden />
            Placement outcomes are only shown once verified by the placement team.
          </p>
        </div>
      )}

      {dash.isLoading ? <Skeleton className="mt-6 h-40 w-full" /> : null}
    </>
  );
}
