import { createFileRoute } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

export const Route = createFileRoute("/admin/career/preparation")({
  head: () => ({
    meta: [
      { title: "Interview Preparation — Learntrix Admin" },
      {
        name: "description",
        content:
          "Manage company-wise interview questions, preparation roadmaps, mock interview slots and assessment patterns.",
      },
      { property: "og:title", content: "Interview Preparation — Learntrix Admin" },
      { property: "og:description", content: "Company preparation content management." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminPreparation,
});

const questions = [
  {
    id: "q1",
    company: "Microsoft",
    round: "DSA Round",
    topic: "Graphs",
    difficulty: "Hard",
    asked: 24,
  },
  {
    id: "q2",
    company: "Wipro",
    round: "Aptitude",
    topic: "Time & Work",
    difficulty: "Easy",
    asked: 61,
  },
  {
    id: "q3",
    company: "Amazon",
    round: "System Design",
    topic: "Rate limiter",
    difficulty: "Hard",
    asked: 12,
  },
  { id: "q4", company: "TCS", round: "HR", topic: "Behavioural", difficulty: "Easy", asked: 88 },
];

const roadmaps = [
  { id: "r1", company: "Microsoft", weeks: 8, modules: 14, published: true },
  { id: "r2", company: "Amazon", weeks: 10, modules: 18, published: true },
  { id: "r3", company: "Infosys", weeks: 6, modules: 9, published: false },
];

function AdminPreparation() {
  return (
    <>
      <PageHeader
        title="Interview Preparation"
        description="Company-wise preparation content surfaced to eligible students in the career portal."
        action={
          <Button onClick={() => toast.info("Content is authored in the preparation editor.")}>
            <Plus className="size-4" aria-hidden /> Add content
          </Button>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Questions" value={questions.length} />
        <StatCard label="Roadmaps" value={roadmaps.length} />
        <StatCard label="Mock interviews (month)" value={64} />
        <StatCard label="Companies covered" value={new Set(questions.map((q) => q.company)).size} />
      </section>

      <Tabs defaultValue="questions">
        <TabsList className="mb-4">
          <TabsTrigger value="questions">Question bank</TabsTrigger>
          <TabsTrigger value="roadmaps">Roadmaps</TabsTrigger>
        </TabsList>

        <TabsContent value="questions">
          <Panel title="Question bank">
            <ul className="divide-y divide-border">
              {questions.map((q) => (
                <li key={q.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                  <div>
                    <p className="text-sm font-medium">
                      {q.company} · {q.round}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {q.topic} · asked {q.asked} times
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={q.difficulty === "Hard" ? "warning" : "outline"}>
                      {q.difficulty}
                    </Badge>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => toast.info("Opening question editor.")}
                    >
                      Edit
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          </Panel>
        </TabsContent>

        <TabsContent value="roadmaps">
          <div className="grid gap-4 md:grid-cols-3">
            {roadmaps.map((r) => (
              <Panel
                key={r.id}
                title={`${r.company} roadmap`}
                description={`${r.weeks} weeks · ${r.modules} modules`}
              >
                <div className="flex items-center justify-between">
                  <Badge variant={r.published ? "success" : "outline"}>
                    {r.published ? "Published" : "Draft"}
                  </Badge>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => toast.success("Roadmap status updated.")}
                  >
                    {r.published ? "Unpublish" : "Publish"}
                  </Button>
                </div>
              </Panel>
            ))}
          </div>
        </TabsContent>
      </Tabs>
    </>
  );
}
