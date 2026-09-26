import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { announcementService } from "@/services/announcement.service";
import { priorityTone } from "@/types/announcement";
import type { Announcement } from "@/types/announcement";

export const Route = createFileRoute("/student/announcements")({
  head: () => ({
    meta: [
      { title: "Announcements — Learntrix Student" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: StudentAnnouncementsPage,
});

function StudentAnnouncementsPage() {
  const query = useQuery({
    queryKey: ["student", "announcements"],
    queryFn: () => announcementService.studentAnnouncements(),
  });

  const announcements = query.data ?? [];
  const unread = announcements.filter((a) => !a.read).length;

  if (query.isLoading) {
    return (
      <>
        <PageHeader title="Announcements" description="" />
        <div className="space-y-3">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
        </div>
      </>
    );
  }

  if (query.isError) {
    return (
      <>
        <PageHeader title="Announcements" description="" />
        <Panel title="Could not load announcements" description="">
          <p className="py-6 text-sm text-muted-foreground">
            Something went wrong. Refresh the page to try again.
          </p>
        </Panel>
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="Announcements"
        description={
          unread > 0
            ? `${unread} unread`
            : "Everything from your trainers for your course and batch."
        }
      />

      <Panel title="Recent" description="">
        {announcements.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">
            No announcements yet. Anything your trainer posts will appear here.
          </p>
        ) : (
          <ol className="flex flex-col gap-3">
            {announcements.map((a) => (
              <AnnouncementCard key={a.id} announcement={a} />
            ))}
          </ol>
        )}
      </Panel>
    </>
  );
}

/** One announcement. Opening it marks it read for this student only. */
function AnnouncementCard({ announcement }: { announcement: Announcement }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);

  const markRead = useMutation({
    mutationFn: () => announcementService.markRead(announcement.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["student", "announcements"] });
      void queryClient.invalidateQueries({ queryKey: ["student", "announcements", "unread"] });
    },
  });

  function toggle() {
    const next = !open;
    setOpen(next);
    // Only on open, and only once — the backend call is idempotent anyway.
    if (next && !announcement.read) markRead.mutate();
  }

  return (
    <li className={`rounded-md border p-3 ${announcement.read ? "" : "border-primary/40 bg-primary/5"}`}>
      <button type="button" onClick={toggle} className="flex w-full items-start justify-between gap-3 text-left">
        <div>
          <p className="font-medium">
            {announcement.read ? null : (
              <span className="mr-2 inline-block size-2 rounded-full bg-primary align-middle" aria-label="Unread" />
            )}
            {announcement.title}
          </p>
          <p className="mt-0.5 text-sm text-muted-foreground">
            {announcement.teacherName}
            {announcement.courseTitle ? ` · ${announcement.courseTitle}` : ""}
            {announcement.batchName ? ` · ${announcement.batchName}` : ""}
            {announcement.createdAt
              ? ` · ${new Date(announcement.createdAt).toLocaleDateString()}`
              : ""}
          </p>
        </div>
        <Badge variant={priorityTone(announcement.priority)} className="shrink-0">
          {announcement.priority ?? "NORMAL"}
        </Badge>
      </button>

      {open ? (
        <p className="mt-3 whitespace-pre-wrap border-t pt-3 text-sm">{announcement.content}</p>
      ) : null}
    </li>
  );
}
