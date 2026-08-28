import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";

import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { cn } from "@/lib/utils";
import { studentService } from "@/services/student.service";

export const Route = createFileRoute("/student/leaderboard")({
  head: () => ({
    meta: [
      { title: "Leaderboard — Learntrix" },
      {
        name: "description",
        content:
          "See how your Learntrix points, coding practice and quiz scores rank against your peers.",
      },
      { property: "og:title", content: "Leaderboard — Learntrix" },
      { property: "og:description", content: "Global, course, batch and weekly student rankings." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: LeaderboardPage,
});

function LeaderboardPage() {
  const board = useQuery({
    queryKey: ["student", "leaderboard"],
    queryFn: () => studentService.leaderboard(),
  });
  const me = (board.data ?? []).find((e) => e.isCurrentUser);
  const above = me ? (board.data ?? []).find((e) => e.rank === me.rank - 1) : undefined;

  return (
    <>
      <PageHeader
        title="Leaderboard"
        description="Ranks are computed by the backend ranking engine."
      />

      <Tabs defaultValue="global" className="mb-6">
        <TabsList>
          <TabsTrigger value="global">Global</TabsTrigger>
          <TabsTrigger value="course">Course</TabsTrigger>
          <TabsTrigger value="batch">Batch</TabsTrigger>
          <TabsTrigger value="weekly">Weekly</TabsTrigger>
          <TabsTrigger value="monthly">Monthly</TabsTrigger>
        </TabsList>
      </Tabs>

      {me ? (
        <div className="surface-panel mb-6 flex flex-wrap items-center gap-x-10 gap-y-3 p-5">
          <div>
            <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">Your rank</p>
            <p className="text-display text-2xl">#{me.rank}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">Your points</p>
            <p className="text-display text-2xl">{me.points.toLocaleString()}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">
              To next rank
            </p>
            <p className="text-display text-2xl">
              {above ? (above.points - me.points + 1).toLocaleString() : "—"}
            </p>
          </div>
        </div>
      ) : null}

      {board.isPending ? (
        <Skeleton className="h-72 w-full" />
      ) : board.isError ? (
        <p className="text-sm text-destructive">
          Unable to load the leaderboard. Please try again.
        </p>
      ) : (
        <div className="surface-panel overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Rank</TableHead>
                <TableHead>Student</TableHead>
                <TableHead className="text-right">Points</TableHead>
                <TableHead className="text-right">Solved</TableHead>
                <TableHead className="text-right">Quiz</TableHead>
                <TableHead className="text-right">Attendance</TableHead>
                <TableHead className="text-right">Streak</TableHead>
                <TableHead>Level</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(board.data ?? []).map((row) => (
                <TableRow key={row.studentId} className={cn(row.isCurrentUser && "bg-accent/10")}>
                  <TableCell className="font-medium">#{row.rank}</TableCell>
                  <TableCell>
                    {row.name}
                    {row.isCurrentUser ? (
                      <Badge variant="outline" className="ml-2">
                        You
                      </Badge>
                    ) : null}
                  </TableCell>
                  <TableCell className="text-right">{row.points.toLocaleString()}</TableCell>
                  <TableCell className="text-right">{row.problemsSolved}</TableCell>
                  <TableCell className="text-right">{row.quizScore}%</TableCell>
                  <TableCell className="text-right">{row.attendance}%</TableCell>
                  <TableCell className="text-right">{row.streak}d</TableCell>
                  <TableCell>{row.level}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </>
  );
}
