import { createFileRoute, Link } from "@tanstack/react-router";

import { SectionHeading } from "@/components/common/SectionHeading";
import { Button } from "@/components/ui/button";
import { PublicLayout } from "@/layouts/PublicLayout";

export const Route = createFileRoute("/about")({
  head: () => ({
    meta: [
      { title: "About Learntrix — Our Mission in Technology Education" },
      {
        name: "description",
        content:
          "Learntrix builds rigorous, industry-aligned technology programs led by practising engineers, with career outcomes designed into every curriculum.",
      },
      { property: "og:title", content: "About Learntrix" },
      {
        property: "og:description",
        content:
          "Rigorous technology education led by practising engineers, built for career outcomes.",
      },
    ],
  }),
  component: AboutPage,
});

const values = [
  {
    title: "Academic rigour",
    body: "Curricula reviewed by senior practitioners and updated every quarter.",
  },
  {
    title: "Practice over theory",
    body: "Every concept is reinforced with graded projects and coding problems.",
  },
  {
    title: "Transparency",
    body: "Public outcome reporting and honest guidance on what a program can deliver.",
  },
  { title: "Longevity", body: "We teach durable engineering judgement, not framework trivia." },
];

function AboutPage() {
  return (
    <PublicLayout>
      <section className="border-b border-border bg-surface py-20">
        <div className="container-page max-w-3xl">
          <p className="eyebrow">About us</p>
          <h1 className="mt-4 text-display text-4xl sm:text-5xl">
            We build the education technology teams wish their new hires already had.
          </h1>
          <p className="mt-6 text-lg leading-relaxed text-muted-foreground">
            Learntrix was founded by engineers and educators who were frustrated by the gap between
            course completion and job readiness. We design programs the way strong engineering teams
            onboard people: structured fundamentals, deliberate practice, honest review, real work.
          </p>
        </div>
      </section>

      <section className="container-page py-20">
        <SectionHeading eyebrow="What we stand for" title="Principles that shape every program" />
        <div className="mt-10 grid gap-6 sm:grid-cols-2">
          {values.map((value) => (
            <div key={value.title} className="surface-panel p-8">
              <h2 className="text-2xl">{value.title}</h2>
              <p className="mt-3 text-muted-foreground">{value.body}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="container-page pb-20">
        <div className="surface-panel flex flex-col items-start gap-5 p-10 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-2xl">Considering a program for yourself or your team?</h2>
            <p className="mt-2 text-muted-foreground">
              Our advisors will map a track to your goals.
            </p>
          </div>
          <Button asChild size="lg">
            <Link to="/contact">Contact us</Link>
          </Button>
        </div>
      </section>
    </PublicLayout>
  );
}
