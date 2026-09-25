import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { ExternalLink } from "lucide-react";
import { toast } from "sonner";
import { EmptyState } from "@/components/common/EmptyState";
import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { codingAssignmentService } from "@/services/coding.service";
import { platformLabel } from "@/types/coding";

export const Route = createFileRoute("/student/coding-practice/")({
  component: StudentCodingPractice,
});

function StudentCodingPractice() {
  const qc = useQueryClient();
  const query = useQuery({
    queryKey: ["student", "coding-assignments"],
    queryFn: codingAssignmentService.studentList,
  });
  const refresh = () => void qc.invalidateQueries({ queryKey: ["student", "coding-assignments"] });
  const opened = useMutation({
    mutationFn: codingAssignmentService.markOpened,
    onSuccess: refresh,
    onError: (e: Error) => toast.error(e.message),
  });
  const complete = useMutation({
    mutationFn: codingAssignmentService.complete,
    onSuccess: () => {
      toast.success("Marked as completed.");
      refresh();
    },
    onError: (e: Error) => toast.error(e.message),
  });
  const problems = query.data ?? [];
  const solve = (id: string, url: string) => {
    opened.mutate(id);
    window.open(url, "_blank", "noopener,noreferrer");
  };
  return (
    <>
      <PageHeader
        title="Coding Practice"
        description="Open assigned problems on their original platform and track your progress here."
      />
      <section className="mb-6 grid gap-4 sm:grid-cols-3">
        <StatCard label="Available" value={problems.length} />
        <StatCard
          label="Completed"
          value={problems.filter((p) => p.progressStatus === "COMPLETED").length}
        />
        <StatCard
          label="In Progress"
          value={
            problems.filter(
              (p) => p.progressStatus === "OPENED" || p.progressStatus === "IN_PROGRESS",
            ).length
          }
        />
      </section>
      {problems.length === 0 ? (
        <Panel title="Problems">
          <EmptyState
            title="No problems yet"
            description="Published problems for your batch will appear here."
          />
        </Panel>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {problems.map((p) => (
            <article key={p.id} className="surface-panel flex flex-col p-5">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg font-semibold">{p.title}</h2>
                  <p className="text-sm text-muted-foreground">{platformLabel(p.platform)}</p>
                </div>
                <Badge variant={p.difficulty === "HARD" ? "destructive" : "secondary"}>
                  {p.difficulty}
                </Badge>
              </div>
              <div className="mt-3 flex flex-wrap gap-2">
                <Badge variant="outline">{p.topics || "General"}</Badge>
                <StatusBadge value={p.progressStatus ?? "NOT_STARTED"} />
              </div>
              {p.description && (
                <p className="mt-3 text-sm text-muted-foreground">{p.description}</p>
              )}
              <p className="mt-3 text-sm">
                <span className="text-muted-foreground">Deadline: </span>
                {p.deadline
                  ? new Date(`${p.deadline}T00:00:00`).toLocaleDateString(undefined, {
                      day: "numeric",
                      month: "short",
                      year: "numeric",
                    })
                  : "No deadline"}
              </p>
              <div className="mt-auto flex flex-wrap gap-2 pt-5">
                <Button onClick={() => solve(p.id, p.problemUrl)} disabled={opened.isPending}>
                  <ExternalLink className="size-4" /> Solve Problem
                </Button>
                <Button
                  variant="outline"
                  disabled={p.progressStatus === "COMPLETED" || complete.isPending}
                  onClick={() => complete.mutate(p.id)}
                >
                  {p.progressStatus === "COMPLETED" ? "Completed" : "Mark as Completed"}
                </Button>
              </div>
            </article>
          ))}
        </div>
      )}
    </>
  );
}
