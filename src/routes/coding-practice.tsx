import { createFileRoute, Link } from "@tanstack/react-router";
import { Activity, Code2, Flame, Trophy } from "lucide-react";

import { SectionHeading } from "@/components/common/SectionHeading";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PublicLayout } from "@/layouts/PublicLayout";

export const Route = createFileRoute("/coding-practice")({
  head: () => ({
    meta: [
      { title: "Coding Practice — Problems, Contests & Leaderboards | Learntrix" },
      {
        name: "description",
        content:
          "Practise 3,000+ curated coding problems in Python, Java, C++, JavaScript and C#, with contests, leaderboards and real-time execution.",
      },
      { property: "og:title", content: "Learntrix Coding Practice" },
      {
        property: "og:description",
        content: "Curated problems, contests and leaderboards with multi-language support.",
      },
    ],
  }),
  component: CodingPracticePage,
});

const problems = [
  { title: "Two Sum Variants", difficulty: "Easy", topic: "Arrays", acceptance: "72%" },
  { title: "LRU Cache Design", difficulty: "Medium", topic: "Design", acceptance: "48%" },
  { title: "Word Ladder", difficulty: "Hard", topic: "Graphs", acceptance: "31%" },
  { title: "Kth Largest in Stream", difficulty: "Medium", topic: "Heaps", acceptance: "55%" },
  {
    title: "Median of Two Sorted Arrays",
    difficulty: "Hard",
    topic: "Binary Search",
    acceptance: "27%",
  },
];

const difficultyVariant: Record<string, "success" | "warning" | "destructive"> = {
  Easy: "success",
  Medium: "warning",
  Hard: "destructive",
};

function CodingPracticePage() {
  return (
    <PublicLayout>
      <section className="bg-ink py-20 text-ink-foreground">
        <div className="container-page max-w-3xl">
          <p className="eyebrow text-accent">Coding practice</p>
          <h1 className="mt-4 text-display text-4xl sm:text-5xl">
            Deliberate practice, measured like an engineering scorecard.
          </h1>
          <p className="mt-6 text-lg leading-relaxed text-ink-foreground/70">
            Solve curated problems mapped to your course modules, track accuracy and streaks, and
            compete in weekly contests. Code executes securely on the Learntrix backend runner.
          </p>
          <div className="mt-9 flex flex-wrap gap-3">
            <Button size="lg" variant="accent" asChild>
              <Link to="/register">Start practising</Link>
            </Button>
            <Button size="lg" variant="onDark" asChild>
              <Link to="/courses">See related courses</Link>
            </Button>
          </div>
        </div>
      </section>

      <section className="container-page grid gap-6 py-16 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { icon: Code2, label: "Curated problems", value: "3,000+" },
          { icon: Trophy, label: "Weekly contests", value: "52 / year" },
          { icon: Flame, label: "Longest streak record", value: "418 days" },
          { icon: Activity, label: "Submissions processed", value: "1M+" },
        ].map((item) => (
          <div key={item.label} className="surface-panel p-6">
            <item.icon className="size-5 text-primary" aria-hidden />
            <p className="mt-4 text-display text-3xl">{item.value}</p>
            <p className="text-sm text-muted-foreground">{item.label}</p>
          </div>
        ))}
      </section>

      <section className="container-page pb-20">
        <SectionHeading eyebrow="Problem set preview" title="A taste of the practice library" />
        <div className="surface-panel mt-8 divide-y divide-border">
          {problems.map((problem) => (
            <div
              key={problem.title}
              className="flex flex-wrap items-center justify-between gap-3 p-5"
            >
              <div>
                <p className="font-medium">{problem.title}</p>
                <p className="text-sm text-muted-foreground">{problem.topic}</p>
              </div>
              <div className="flex items-center gap-4">
                <span className="text-sm text-muted-foreground">{problem.acceptance} accepted</span>
                <Badge variant={difficultyVariant[problem.difficulty]}>{problem.difficulty}</Badge>
              </div>
            </div>
          ))}
        </div>
        <p className="mt-4 text-sm text-muted-foreground">
          Languages supported: Python, Java, C++, JavaScript and C#.
        </p>
      </section>
    </PublicLayout>
  );
}
