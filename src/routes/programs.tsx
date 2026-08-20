import { createFileRoute, Link } from "@tanstack/react-router";
import { ArrowRight, Clock, Users } from "lucide-react";

import { SectionHeading } from "@/components/common/SectionHeading";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PublicLayout } from "@/layouts/PublicLayout";

export const Route = createFileRoute("/programs")({
  head: () => ({
    meta: [
      { title: "Career Programs — Learntrix Technology Tracks" },
      {
        name: "description",
        content:
          "Multi-month Learntrix career tracks in full stack engineering, data science, AI and cloud, with mentorship, projects and placement guidance.",
      },
      { property: "og:title", content: "Career Programs at Learntrix" },
      {
        property: "og:description",
        content:
          "Multi-month career tracks with mentorship, graded projects and placement guidance.",
      },
    ],
  }),
  component: ProgramsPage,
});

const programs = [
  {
    name: "Full Stack Engineering",
    months: 9,
    cohort: "Mar 2026",
    summary:
      "React, TypeScript, FastAPI, PostgreSQL and cloud deployment with four portfolio builds.",
    outcomes: ["4 graded projects", "Weekly mentor reviews", "DSA interview sprint"],
  },
  {
    name: "Data Science & Analytics",
    months: 8,
    cohort: "Apr 2026",
    summary: "Statistics, SQL, Python and machine learning applied to production-grade datasets.",
    outcomes: ["Capstone with real data", "Model deployment lab", "Case-study interviews"],
  },
  {
    name: "Applied AI Engineering",
    months: 7,
    cohort: "May 2026",
    summary: "LLM applications, retrieval systems, evaluation and responsible deployment.",
    outcomes: ["RAG capstone", "Evaluation harness lab", "AI systems design"],
  },
  {
    name: "Cloud & DevOps",
    months: 8,
    cohort: "Mar 2026",
    summary: "AWS architecture, Kubernetes, Terraform and observability for reliable platforms.",
    outcomes: ["Certification prep", "Production incident drills", "Platform capstone"],
  },
];

function ProgramsPage() {
  return (
    <PublicLayout>
      <section className="border-b border-border bg-surface py-20">
        <div className="container-page max-w-3xl">
          <p className="eyebrow">Career tracks</p>
          <h1 className="mt-4 text-display text-4xl sm:text-5xl">
            Long-form programs designed around hiring outcomes.
          </h1>
          <p className="mt-6 text-lg leading-relaxed text-muted-foreground">
            Programs combine live mentorship, graded projects, coding practice and interview
            preparation across a fixed cohort schedule.
          </p>
        </div>
      </section>

      <section className="container-page py-20">
        <SectionHeading eyebrow="Open cohorts" title="Choose a track" />
        <div className="mt-10 grid gap-6 lg:grid-cols-2">
          {programs.map((program) => (
            <article key={program.name} className="surface-panel flex h-full flex-col p-8">
              <div className="flex items-start justify-between gap-4">
                <h2 className="text-2xl">{program.name}</h2>
                <Badge variant="secondary">Cohort {program.cohort}</Badge>
              </div>
              <p className="mt-3 text-muted-foreground">{program.summary}</p>
              <ul className="mt-6 space-y-2 text-sm">
                {program.outcomes.map((outcome) => (
                  <li key={outcome} className="flex items-center gap-2">
                    <span className="size-1.5 rounded-full bg-accent" aria-hidden />
                    {outcome}
                  </li>
                ))}
              </ul>
              <div className="mt-6 flex items-center gap-5 border-t border-border pt-5 text-sm text-muted-foreground">
                <span className="inline-flex items-center gap-1.5">
                  <Clock className="size-4" aria-hidden /> {program.months} months
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <Users className="size-4" aria-hidden /> Limited seats
                </span>
              </div>
              <Button className="mt-6 self-start" asChild>
                <Link to="/contact">
                  Request curriculum <ArrowRight className="size-4" />
                </Link>
              </Button>
            </article>
          ))}
        </div>
      </section>
    </PublicLayout>
  );
}
