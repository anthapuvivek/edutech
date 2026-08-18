import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { CalendarClock, Video } from "lucide-react";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { studentService } from "@/services/student.service";
import type { PlatformEvent } from "@/types/lms";

export const Route = createFileRoute("/student/live-classes")({
  head: () => ({
    meta: [
      { title: "Live Classes — Learntrix" },
      { name: "description", content: "Today's, upcoming and past live classes with your Learntrix trainers." },
      { property: "og:title", content: "Live Classes — Learntrix" },
      { property: "og:description", content: "Join your scheduled live sessions and review past classes." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: LiveClasses,
});

function LiveClasses() {
  const classes = useQuery({ queryKey: ["student", "live-classes"], queryFn: () => studentService.liveClasses() });
  const items = classes.data ?? [];
  const today = items.filter((e) => e.status === "Live Now");
  const upcoming = items.filter((e) => e.status === "Upcoming");
  const past = items.filter((e) => e.status === "Completed" || e.status === "Cancelled");

  return (
    <>
      <PageHeader title="Live Classes" description="Meeting links are released by the backend based on your enrollment." />

      {classes.isPending ? (
        <div className="space-y-3">
          <Skeleton className="h-32 w-full" />
          <Skeleton className="h-32 w-full" />
        </div>
      ) : classes.isError ? (
        <p className="text-sm text-destructive">Unable to load your classes. Please try again.</p>
      ) : (
        <Tabs defaultValue="today">
          <TabsList>
            <TabsTrigger value="today">Today</TabsTrigger>
            <TabsTrigger value="upcoming">Upcoming</TabsTrigger>
            <TabsTrigger value="past">Past</TabsTrigger>
          </TabsList>
          <TabsContent value="today" className="mt-5">
            <ClassList items={today} emptyText="No classes scheduled for today." />
          </TabsContent>
          <TabsContent value="upcoming" className="mt-5">
            <ClassList items={upcoming} emptyText="No upcoming classes." />
          </TabsContent>
          <TabsContent value="past" className="mt-5">
            <ClassList items={past} emptyText="No past classes yet." />
          </TabsContent>
        </Tabs>
      )}
    </>
  );
}

function ClassList({ items, emptyText }: { items: PlatformEvent[]; emptyText: string }) {
  if (items.length === 0) return <EmptyState icon={CalendarClock} title={emptyText} />;
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      {items.map((e) => (
        <article key={e.id} className="surface-panel flex flex-col gap-3 p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="text-base">{e.title}</h2>
              <p className="text-sm text-muted-foreground">
                {e.courseTitle} · {e.trainerName}
              </p>
            </div>
            <Badge variant={e.status === "Live Now" ? "default" : "outline"}>{e.status}</Badge>
          </div>
          <p className="text-sm text-muted-foreground">
            {new Date(`${e.date}T${e.startTime}`).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" })} ·{" "}
            {e.platform} · {e.type}
          </p>
          <div className="flex gap-2">
            <Button size="sm" disabled={e.status !== "Live Now"}>
              <Video className="size-4" aria-hidden />
              {e.status === "Live Now" ? "Join live class" : "Join class"}
            </Button>
            <Button size="sm" variant="outline">
              View details
            </Button>
          </div>
        </article>
      ))}
    </div>
  );
}
