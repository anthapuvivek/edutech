import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { ExternalLink } from "lucide-react";

import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { careerService } from "@/services/career.service";

export const Route = createFileRoute("/student/career/profile")({
  head: () => ({
    meta: [
      { title: "Career Profile — Learntrix" },
      {
        name: "description",
        content: "Manage the career profile that powers job matching, resumes and referrals.",
      },
      { property: "og:title", content: "Career Profile — Learntrix" },
      {
        property: "og:description",
        content: "Skills, preferences and coding profiles used for placement matching.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: CareerProfilePage,
});

function CareerProfilePage() {
  const profile = useQuery({
    queryKey: ["career", "profile"],
    queryFn: () => careerService.profile(),
  });
  const skills = useQuery({
    queryKey: ["career", "skill-gap"],
    queryFn: () => careerService.skillGap(),
  });

  if (profile.isLoading || !profile.data) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-9 w-56" />
        <Skeleton className="h-56 w-full" />
      </div>
    );
  }

  const p = profile.data;
  const links = Object.entries(p.links).filter(([, value]) => Boolean(value));

  return (
    <>
      <PageHeader
        title="Career profile"
        description="Recruiters and matching use only the details you publish here."
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="surface-panel space-y-5 p-6 lg:col-span-2">
          <div>
            <h2 className="text-display text-2xl">{p.name}</h2>
            <p className="text-sm text-muted-foreground">{p.headline}</p>
            <p className="mt-1 text-xs text-muted-foreground">
              {p.studentId} · {p.education} · {p.courseTitle}
            </p>
          </div>

          <div>
            <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">Skills</p>
            <div className="mt-2 flex flex-wrap gap-2">
              {p.skills.map((s) => (
                <Badge key={s} variant="outline">
                  {s}
                </Badge>
              ))}
            </div>
          </div>

          <dl className="grid gap-4 sm:grid-cols-2">
            <Field label="Preferred roles" value={p.preferredRoles.join(", ")} />
            <Field label="Preferred locations" value={p.preferredLocations.join(", ")} />
            <Field label="Work mode" value={p.workMode} />
            <Field label="Experience" value={p.experienceLevel} />
            <Field label="Expected salary" value={p.expectedSalary ?? "Not specified"} />
            <Field label="Availability" value={p.availability} />
          </dl>

          <div>
            <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">
              Coding & professional profiles
            </p>
            {links.length ? (
              <ul className="mt-2 space-y-1.5 text-sm">
                {links.map(([key, value]) => (
                  <li key={key}>
                    <a
                      className="inline-flex items-center gap-1.5 text-primary underline-offset-4 hover:underline"
                      href={value as string}
                      target="_blank"
                      rel="noreferrer"
                    >
                      {key} <ExternalLink className="size-3.5" aria-hidden />
                    </a>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-2 text-sm text-muted-foreground">No profiles linked yet.</p>
            )}
          </div>
        </section>

        <aside className="space-y-6">
          <section className="surface-panel p-6">
            <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">
              Profile completion
            </p>
            <p className="text-display text-3xl leading-none">{p.completionPercent}%</p>
            <Progress value={p.completionPercent} className="mt-3" />
            <ul className="mt-4 space-y-1.5 text-sm text-muted-foreground">
              {p.missingItems.map((item) => (
                <li key={item}>• {item}</li>
              ))}
            </ul>
          </section>

          <section className="surface-panel p-6">
            <h2 className="text-display mb-3 text-xl">Skill gap</h2>
            {skills.data ? (
              <div className="space-y-4 text-sm">
                <p className="text-muted-foreground">Target · {skills.data.targetRole}</p>
                <div className="flex flex-wrap gap-2">
                  {skills.data.strong.map((s) => (
                    <Badge key={s} variant="success">
                      {s}
                    </Badge>
                  ))}
                </div>
                <div className="space-y-2">
                  {skills.data.needsImprovement.map((s) => (
                    <div key={s.skill}>
                      <div className="flex justify-between text-xs">
                        <span>{s.skill}</span>
                        <span className="text-muted-foreground">{s.level}%</span>
                      </div>
                      <Progress value={s.level} className="mt-1 h-1.5" />
                    </div>
                  ))}
                </div>
                <div className="flex flex-wrap gap-2">
                  {skills.data.missing.map((s) => (
                    <Badge key={s} variant="warning">
                      Missing · {s}
                    </Badge>
                  ))}
                </div>
              </div>
            ) : (
              <Skeleton className="h-24 w-full" />
            )}
          </section>
        </aside>
      </div>
    </>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-[0.14em] text-muted-foreground">{label}</dt>
      <dd className="mt-1 text-sm">{value}</dd>
    </div>
  );
}
