import { createFileRoute, Link } from "@tanstack/react-router";
import { BrainCircuit, MessagesSquare, Route as RouteIcon, Terminal } from "lucide-react";

import { SectionHeading } from "@/components/common/SectionHeading";
import { Button } from "@/components/ui/button";
import { PublicLayout } from "@/layouts/PublicLayout";

export const Route = createFileRoute("/ai-learning")({
  head: () => ({
    meta: [
      { title: "AI Learning — AI Tutor, Coding Coach & Personalised Paths | Learntrix" },
      {
        name: "description",
        content:
          "Learn faster with the Learntrix AI Tutor, AI Coding Coach and course assistant — context-aware guidance built into every lesson.",
      },
      { property: "og:title", content: "AI Learning at Learntrix" },
      {
        property: "og:description",
        content: "Context-aware AI tutoring, coding coaching and personalised learning paths.",
      },
    ],
  }),
  component: AiLearningPage,
});

const assistants = [
  {
    icon: BrainCircuit,
    title: "General AI Tutor",
    body: "Ask anything across the curriculum and get structured, source-aware explanations.",
  },
  {
    icon: MessagesSquare,
    title: "Course AI Tutor",
    body: "Scoped to the course you are studying, including transcripts and resources.",
  },
  {
    icon: Terminal,
    title: "AI Coding Coach",
    body: "Diagnoses failed submissions, suggests approaches and reviews complexity.",
  },
  {
    icon: RouteIcon,
    title: "Interview Coach",
    body: "Runs mock interviews and gives calibrated feedback on your answers.",
  },
];

function AiLearningPage() {
  return (
    <PublicLayout>
      <section className="border-b border-border bg-surface py-20">
        <div className="container-page max-w-3xl">
          <p className="eyebrow">AI learning</p>
          <h1 className="mt-4 text-display text-4xl sm:text-5xl">
            An always-available tutor that understands your syllabus.
          </h1>
          <p className="mt-6 text-lg leading-relaxed text-muted-foreground">
            Learntrix AI is grounded in your course material, submissions and progress — so guidance
            is specific to where you are, not generic advice.
          </p>
          <Button size="lg" className="mt-9" asChild>
            <Link to="/register">Try the AI tutor</Link>
          </Button>
        </div>
      </section>

      <section className="container-page py-20">
        <SectionHeading eyebrow="Assistants" title="Four modes, one learning context" />
        <div className="mt-10 grid gap-6 sm:grid-cols-2">
          {assistants.map((item) => (
            <div key={item.title} className="surface-panel p-8">
              <item.icon className="size-5 text-primary" aria-hidden />
              <h2 className="mt-4 text-2xl">{item.title}</h2>
              <p className="mt-2 text-muted-foreground">{item.body}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="container-page pb-20">
        <div className="surface-panel bg-ink p-10 text-ink-foreground">
          <h2 className="text-3xl">Built provider-agnostic and privacy-first</h2>
          <p className="mt-3 max-w-2xl text-ink-foreground/70">
            All AI requests are brokered by the Learntrix backend. No model provider keys are ever
            exposed to the browser, and conversation history stays inside your account.
          </p>
        </div>
      </section>
    </PublicLayout>
  );
}
