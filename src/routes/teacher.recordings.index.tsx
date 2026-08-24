import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import {
  Calendar,
  Clock,
  Eye,
  FileEdit,
  FolderOpen,
  Globe,
  MoreVertical,
  Plus,
  Search,
  Trash2,
  Video,
} from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { recordingService } from "@/services/recording.service";
import type { Recording, RecordingStatus } from "@/types/recording";

export const Route = createFileRoute("/teacher/recordings/")({
  head: () => ({
    meta: [
      { title: "Recorded Classes — Teacher Portal" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: TeacherRecordingsPage,
});

function TeacherRecordingsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");

  const { data, isLoading, isError } = useQuery({
    queryKey: ["teacher", "recordings"],
    queryFn: () => recordingService.getTeacherRecordings(),
  });

  const publishMutation = useMutation({
    mutationFn: (id: string) => recordingService.publishRecording(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "recordings"] });
      toast.success("Recording published successfully. Students will be notified.");
    },
    onError: () => {
      toast.error("Failed to publish recording.");
    },
  });

  const unpublishMutation = useMutation({
    mutationFn: (id: string) => recordingService.unpublishRecording(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "recordings"] });
      toast.success("Recording unpublished.");
    },
    onError: () => {
      toast.error("Failed to unpublish recording.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => recordingService.deleteRecording(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "recordings"] });
      toast.success("Recording deleted successfully.");
    },
    onError: () => {
      toast.error("Failed to delete recording.");
    },
  });

  const recordings = data?.items ?? [];

  const filtered = recordings.filter((r) => {
    const matchesSearch =
      r.title.toLowerCase().includes(search.toLowerCase()) ||
      r.courseTitle.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === "ALL" || r.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const formatDuration = (seconds: number) => {
    if (!seconds) return "--";
    const mins = Math.floor(seconds / 60);
    const hrs = Math.floor(mins / 60);
    const remMins = mins % 60;
    return hrs > 0 ? `${hrs}h ${remMins}m` : `${mins}m`;
  };

  const getStatusBadge = (status: RecordingStatus) => {
    const config: Record<RecordingStatus, { label: string; variant: "default" | "secondary" | "destructive" | "outline" | "success" | "warning" | "info" }> = {
      DRAFT: { label: "Draft", variant: "secondary" },
      UPLOADING: { label: "Uploading", variant: "outline" },
      PROCESSING: { label: "Processing", variant: "info" },
      READY: { label: "Ready", variant: "warning" },
      PUBLISHED: { label: "Published", variant: "success" },
      UNPUBLISHED: { label: "Unpublished", variant: "outline" },
      FAILED: { label: "Failed", variant: "destructive" },
    };

    const c = config[status] || { label: status, variant: "outline" };
    return <Badge variant={c.variant as any}>{c.label}</Badge>;
  };

  return (
    <>
      <PageHeader
        title="Recorded Classes"
        description="Manage online class recordings, upload videos, and publish them to student curriculum modules."
        action={
          <Button asChild>
            <Link to="/teacher/recordings/new">
              <Plus className="mr-2 size-4" /> Add Recording
            </Link>
          </Button>
        }
      />

      <div className="surface-panel mb-6 p-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative max-w-sm flex-1">
            <Search className="absolute top-2.5 left-3 size-4 text-muted-foreground" />
            <Input
              placeholder="Search by title or course..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant={statusFilter === "ALL" ? "default" : "outline"}
              size="sm"
              onClick={() => setStatusFilter("ALL")}
            >
              All
            </Button>
            <Button
              variant={statusFilter === "PUBLISHED" ? "default" : "outline"}
              size="sm"
              onClick={() => setStatusFilter("PUBLISHED")}
            >
              Published
            </Button>
            <Button
              variant={statusFilter === "READY" ? "default" : "outline"}
              size="sm"
              onClick={() => setStatusFilter("READY")}
            >
              Ready
            </Button>
            <Button
              variant={statusFilter === "PROCESSING" ? "default" : "outline"}
              size="sm"
              onClick={() => setStatusFilter("PROCESSING")}
            >
              Processing
            </Button>
            <Button
              variant={statusFilter === "DRAFT" ? "default" : "outline"}
              size="sm"
              onClick={() => setStatusFilter("DRAFT")}
            >
              Drafts
            </Button>
          </div>
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-16 w-full animate-pulse rounded-md bg-muted/30" />
          ))}
        </div>
      ) : isError ? (
        <div className="surface-panel p-8 text-center text-destructive">
          Error loading class recordings. Please verify backend connection.
        </div>
      ) : filtered.length === 0 ? (
        <div className="surface-panel py-16 text-center">
          <Video className="mx-auto size-12 text-muted-foreground/60" />
          <h3 className="mt-4 text-lg font-medium">No recordings found</h3>
          <p className="mt-2 text-sm text-muted-foreground max-w-md mx-auto">
            {search || statusFilter !== "ALL"
              ? "Try adjusting your search query or filters."
              : "Start by adding your first online class recording to map with a course module."}
          </p>
          <Button asChild className="mt-6" variant="outline">
            <Link to="/teacher/recordings/new">Create Recording</Link>
          </Button>
        </div>
      ) : (
        <div className="surface-panel overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Class Recording Details</TableHead>
                <TableHead>Course / Module</TableHead>
                <TableHead>Class Date</TableHead>
                <TableHead>Duration</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((r) => (
                <TableRow key={r.id}>
                  <TableCell>
                    <div className="font-medium text-foreground">
                      <Link
                        to="/teacher/recordings/$id"
                        params={{ id: r.id }}
                        className="hover:underline hover:text-accent"
                      >
                        {r.title}
                      </Link>
                    </div>
                    {r.description && (
                      <p className="mt-1 max-w-sm truncate text-xs text-muted-foreground">
                        {r.description}
                      </p>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="text-sm font-medium text-foreground flex items-center gap-1">
                        <FolderOpen className="size-3.5" /> {r.courseTitle}
                      </span>
                      <span className="text-xs text-muted-foreground mt-0.5">
                        {r.moduleTitle} · {r.lessonTitle}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1.5 text-sm">
                      <Calendar className="size-4 text-muted-foreground" />
                      {new Date(r.classDate).toLocaleDateString("en-IN", {
                        dateStyle: "medium",
                      })}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-1.5 text-sm tabular-nums">
                      <Clock className="size-4 text-muted-foreground" />
                      {formatDuration(r.durationSeconds)}
                    </div>
                  </TableCell>
                  <TableCell>{getStatusBadge(r.status)}</TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreVertical className="size-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-48">
                        <DropdownMenuItem asChild>
                          <Link to="/teacher/recordings/$id" params={{ id: r.id }} className="flex w-full items-center">
                            <Eye className="mr-2 size-4" /> Details & Stats
                          </Link>
                        </DropdownMenuItem>
                        
                        {(r.status === "READY" || r.status === "UNPUBLISHED") && (
                          <DropdownMenuItem
                            onClick={() => publishMutation.mutate(r.id)}
                            className="text-success focus:text-success"
                          >
                            <Globe className="mr-2 size-4" /> Publish Class
                          </DropdownMenuItem>
                        )}
                        
                        {r.status === "PUBLISHED" && (
                          <DropdownMenuItem
                            onClick={() => unpublishMutation.mutate(r.id)}
                            className="text-amber-500 focus:text-amber-500"
                          >
                            <Globe className="mr-2 size-4" /> Unpublish
                          </DropdownMenuItem>
                        )}

                        <DropdownMenuItem className="text-destructive focus:text-destructive" onClick={() => {
                          if (confirm("Are you sure you want to delete this class recording? This cannot be undone.")) {
                            deleteMutation.mutate(r.id);
                          }
                        }}>
                          <Trash2 className="mr-2 size-4" /> Delete Recording
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
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
