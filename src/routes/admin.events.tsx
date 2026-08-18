import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { toast } from "sonner";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/events")({
  head: () => ({
    meta: [
      { title: "Live Classes & Events — Learntrix Admin" },
      { name: "description", content: "Create and manage demo classes, webinars, workshops and regular classes across the platform." },
      { property: "og:title", content: "Live Classes & Events — Learntrix Admin" },
      { property: "og:description", content: "Schedule, publish and track platform events and registrations." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminEvents,
});

const eventTypes = ["Regular Class", "Demo Class", "Webinar", "Workshop", "Placement Program", "Orientation", "Other"];
const platforms = ["Google Meet", "Zoom", "Microsoft Teams", "Other"];

function AdminEvents() {
  const [typeFilter, setTypeFilter] = useState("All");
  const [open, setOpen] = useState(false);
  const events = useQuery({ queryKey: ["admin", "events"], queryFn: () => adminService.events() });
  const rows = (events.data ?? []).filter((e) => typeFilter === "All" || e.type === typeFilter);

  return (
    <>
      <PageHeader
        title="Live Classes & Events"
        description="Published public events appear on the public website. Restricted meeting links stay private until the backend authorizes access."
        action={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>Create event</Button>
            </DialogTrigger>
            <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-lg">
              <DialogHeader>
                <DialogTitle>Create event</DialogTitle>
                <DialogDescription>
                  This form is wired to the event service. Persistence arrives with the backend.
                </DialogDescription>
              </DialogHeader>
              <form
                className="space-y-4"
                onSubmit={(e) => {
                  e.preventDefault();
                  setOpen(false);
                  toast.success("Event drafted", { description: "It will be persisted once the backend is connected." });
                }}
              >
                <div className="space-y-2">
                  <Label htmlFor="ev-title">Event title</Label>
                  <Input id="ev-title" required placeholder="DATA SCIENCE with Gen AI" />
                </div>
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="ev-course">Course</Label>
                    <Input id="ev-course" placeholder="Generative AI & LLM Systems" />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="ev-trainer">Trainer</Label>
                    <Input id="ev-trainer" placeholder="Team of Experts" />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="ev-desc">Description</Label>
                  <Textarea id="ev-desc" rows={3} />
                </div>
                <div className="grid gap-4 sm:grid-cols-3">
                  <div className="space-y-2">
                    <Label htmlFor="ev-date">Date</Label>
                    <Input id="ev-date" type="date" required />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="ev-start">Start</Label>
                    <Input id="ev-start" type="time" required />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="ev-end">End</Label>
                    <Input id="ev-end" type="time" required />
                  </div>
                </div>
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label>Event type</Label>
                    <Select defaultValue="Demo Class">
                      <SelectTrigger><SelectValue /></SelectTrigger>
                      <SelectContent>
                        {eventTypes.map((t) => (
                          <SelectItem key={t} value={t}>{t}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label>Platform</Label>
                    <Select defaultValue="Google Meet">
                      <SelectTrigger><SelectValue /></SelectTrigger>
                      <SelectContent>
                        {platforms.map((p) => (
                          <SelectItem key={p} value={p}>{p}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <div className="grid gap-4 sm:grid-cols-3">
                  <div className="space-y-2 sm:col-span-2">
                    <Label htmlFor="ev-url">Meeting URL</Label>
                    <Input id="ev-url" type="url" placeholder="https://" />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="ev-pass">Passcode</Label>
                    <Input id="ev-pass" />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>Visibility</Label>
                  <Select defaultValue="public">
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="public">Public — link visible after registration</SelectItem>
                      <SelectItem value="restricted">Restricted — enrolled students only</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <DialogFooter>
                  <Button type="submit">Save event</Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        }
      />

      <div className="mb-5 max-w-56">
        <Select value={typeFilter} onValueChange={setTypeFilter}>
          <SelectTrigger aria-label="Filter by event type"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="All">All event types</SelectItem>
            {eventTypes.map((t) => (
              <SelectItem key={t} value={t}>{t}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {events.isPending ? (
        <Skeleton className="h-72 w-full" />
      ) : events.isError ? (
        <p className="text-sm text-destructive">Unable to load events. Please try again.</p>
      ) : rows.length === 0 ? (
        <EmptyState title="No events found" description="Create an event or clear the filter." />
      ) : (
        <div className="surface-panel overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Event</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Course</TableHead>
                <TableHead>Trainer</TableHead>
                <TableHead>Date</TableHead>
                <TableHead>Time</TableHead>
                <TableHead>Platform</TableHead>
                <TableHead className="text-right">Registrations</TableHead>
                <TableHead>Status</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((e) => (
                <TableRow key={e.id}>
                  <TableCell className="font-medium">{e.title}</TableCell>
                  <TableCell>{e.type}</TableCell>
                  <TableCell className="max-w-44 truncate">{e.courseTitle}</TableCell>
                  <TableCell>{e.trainerName}</TableCell>
                  <TableCell>{new Date(e.date).toLocaleDateString("en-IN", { dateStyle: "medium" })}</TableCell>
                  <TableCell>{e.startTime}</TableCell>
                  <TableCell>{e.platform}</TableCell>
                  <TableCell className="text-right">{e.registrations}</TableCell>
                  <TableCell>
                    <div className="flex flex-col gap-1">
                      <Badge variant={e.status === "Live Now" ? "default" : "outline"}>{e.status}</Badge>
                      <span className="text-xs text-muted-foreground">
                        {e.published ? "Published" : "Draft"} · {e.visibility}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() =>
                        toast.info(e.published ? "Unpublish queued" : "Publish queued", {
                          description: "The backend will apply this change.",
                        })
                      }
                    >
                      {e.published ? "Unpublish" : "Publish"}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </>
  );
}
