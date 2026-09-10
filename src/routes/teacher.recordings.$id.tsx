import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import {
  ArrowLeft,
  Calendar,
  Clock,
  Eye,
  FileVideo,
  Globe,
  Loader2,
  Play,
  Share2,
  Trash2,
  Users,
} from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { formatCurriculumPath } from "@/lib/format";
import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { recordingService } from "@/services/recording.service";
import type { RecordingStatus } from "@/types/recording";

export const Route = createFileRoute("/teacher/recordings/$id")({
  head: () => ({
    meta: [{ title: "Recording Details — Teacher Portal" }, { name: "robots", content: "noindex" }],
  }),
  component: RecordingDetailsPage,
});

function RecordingDetailsPage() {
  const { id } = Route.useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isPlayingPreview, setIsPlayingPreview] = useState(false);

  const recordingQuery = useQuery({
    queryKey: ["teacher", "recording", id],
    queryFn: () => recordingService.getRecording(id),
  });

  const statsQuery = useQuery({
    queryKey: ["teacher", "recording", "stats", id],
    queryFn: () => recordingService.getStatistics(id),
  });

  const publishMutation = useMutation({
    mutationFn: () => recordingService.publishRecording(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "recording", id] });
      toast.success("Recording published successfully.");
    },
    onError: () => {
      toast.error("Failed to publish recording.");
    },
  });

  const unpublishMutation = useMutation({
    mutationFn: () => recordingService.unpublishRecording(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "recording", id] });
      toast.success("Recording unpublished.");
    },
    onError: () => {
      toast.error("Failed to unpublish recording.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => recordingService.deleteRecording(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "recordings"] });
      toast.success("Recording deleted successfully.");
      void navigate({ to: "/teacher/recordings" });
    },
    onError: () => {
      toast.error("Failed to delete recording.");
    },
  });

  if (recordingQuery.isLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <Loader2 className="size-8 animate-spin text-accent" />
      </div>
    );
  }

  if (recordingQuery.isError || !recordingQuery.data) {
    return (
      <div className="surface-panel p-10 text-center space-y-4">
        <AlertCircle className="size-12 text-destructive mx-auto" />
        <h2 className="text-xl font-medium text-foreground">Class recording not found</h2>
        <p className="text-muted-foreground">
          The recording may have been deleted or you do not have permission to view it.
        </p>
        <Button asChild variant="outline">
          <Link to="/teacher/recordings">Back to recordings</Link>
        </Button>
      </div>
    );
  }

  const recording = recordingQuery.data;
  const stats = statsQuery.data;

  const formatDuration = (seconds: number) => {
    if (!seconds) return "--";
    const mins = Math.floor(seconds / 60);
    const hrs = Math.floor(mins / 60);
    const remMins = mins % 60;
    return hrs > 0 ? `${hrs}h ${remMins}m` : `${mins}m`;
  };

  const getStatusBadge = (status: RecordingStatus) => {
    const config: Record<
      RecordingStatus,
      {
        label: string;
        variant:
          "default" | "secondary" | "destructive" | "outline" | "success" | "warning" | "info";
      }
    > = {
      DRAFT: { label: "Draft", variant: "secondary" },
      UPLOADING: { label: "Uploading", variant: "outline" },
      PROCESSING: { label: "Processing", variant: "info" },
      READY: { label: "Ready", variant: "warning" },
      PUBLISHED: { label: "Published", variant: "success" },
      UNPUBLISHED: { label: "Unpublished", variant: "outline" },
      FAILED: { label: "Failed", variant: "destructive" },
    };

    const c = config[status] || { label: status, variant: "outline" as const };
    return <Badge variant={c.variant}>{c.label}</Badge>;
  };

  return (
    <>
      <div className="mb-4">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/teacher/recordings">
            <ArrowLeft className="mr-2 size-4" /> Back to Recorded Classes
          </Link>
        </Button>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Left 2 Cols: Details & Video Preview */}
        <div className="lg:col-span-2 space-y-6">
          <section className="surface-panel p-6">
            <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
              <div>
                <h1 className="text-2xl font-bold text-foreground">{recording.title}</h1>
                <p className="text-sm text-muted-foreground mt-1">
                  Mapped to <strong>{recording.courseTitle}</strong> ·{" "}
                  {formatCurriculumPath(recording.moduleTitle, recording.lessonTitle)}
                </p>
              </div>
              <div className="shrink-0">{getStatusBadge(recording.status)}</div>
            </div>

            <div className="mt-6 grid gap-4 sm:grid-cols-3 border-y py-4 text-sm text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <Calendar className="size-4" />
                Date:{" "}
                {new Date(recording.classDate).toLocaleDateString("en-IN", { dateStyle: "medium" })}
              </span>
              <span className="flex items-center gap-1.5">
                <Clock className="size-4" />
                Duration: {formatDuration(recording.durationSeconds)}
              </span>
              <span className="flex items-center gap-1.5">
                <FileVideo className="size-4" />
                Format: {recording.videoFormat ? recording.videoFormat.toUpperCase() : "N/A"}
              </span>
            </div>

            <div className="mt-6 space-y-3">
              <h3 className="font-semibold text-foreground">Class Description & Resources</h3>
              <p className="text-sm text-muted-foreground whitespace-pre-line leading-relaxed">
                {recording.description || "No description provided."}
              </p>
            </div>
          </section>

          {/* Video Preview */}
          <section className="surface-panel overflow-hidden p-0 bg-ink">
            {recording.videoUrl ? (
              !isPlayingPreview ? (
                <div className="relative aspect-video w-full flex flex-col items-center justify-center text-ink-foreground p-8">
                  <img
                    src={
                      recording.thumbnailUrl ||
                      "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=800&q=70"
                    }
                    alt=""
                    className="absolute inset-0 w-full h-full object-cover opacity-30"
                  />
                  <div className="relative z-10 text-center">
                    <Button
                      size="lg"
                      className="size-16 rounded-full p-0 flex items-center justify-center bg-accent text-accent-foreground hover:scale-105 transition duration-300 shadow-xl"
                      onClick={() => setIsPlayingPreview(true)}
                    >
                      <Play className="size-8 fill-accent-foreground ml-1" />
                    </Button>
                    <p className="mt-4 text-sm font-medium">Click to Play Preview</p>
                  </div>
                </div>
              ) : (
                <video
                  src={recording.videoUrl}
                  controls
                  autoPlay
                  className="aspect-video w-full object-contain"
                />
              )
            ) : (
              <div className="aspect-video w-full flex flex-col items-center justify-center text-center p-8 text-ink-foreground/60">
                <FileVideo className="size-12 text-ink-foreground/30 mb-4" />
                <h3 className="text-base font-semibold">No Video File</h3>
                <p className="text-xs text-ink-foreground/50 mt-1 max-w-xs">
                  This class recording draft does not contain an uploaded video file.
                </p>
              </div>
            )}
          </section>
        </div>

        {/* Right 1 Col: Statistics & Publish Controls */}
        <div className="space-y-6">
          <section className="surface-panel p-6 space-y-4">
            <h2 className="text-lg font-bold text-foreground">Actions</h2>

            {recording.status === "PUBLISHED" ? (
              <Button
                variant="outline"
                className="w-full text-amber-500 border-amber-500/30 hover:bg-amber-500/10"
                onClick={() => unpublishMutation.mutate()}
                disabled={unpublishMutation.isPending}
              >
                {unpublishMutation.isPending ? (
                  <Loader2 className="mr-2 size-4 animate-spin" />
                ) : null}
                Unpublish Class
              </Button>
            ) : recording.status === "READY" || recording.status === "UNPUBLISHED" ? (
              <Button
                className="w-full bg-success text-success-foreground hover:bg-success/90"
                onClick={() => publishMutation.mutate()}
                disabled={publishMutation.isPending}
              >
                {publishMutation.isPending ? (
                  <Loader2 className="mr-2 size-4 animate-spin" />
                ) : null}
                <Globe className="mr-2 size-4" /> Publish Class
              </Button>
            ) : null}

            {recording.status === "DRAFT" && (
              <Button asChild className="w-full" variant="outline">
                <Link to="/teacher/recordings/new" search={{ resumeId: recording.id }}>
                  Resume Setup / Upload
                </Link>
              </Button>
            )}

            <Button
              variant="outline"
              className="w-full text-destructive border-destructive/30 hover:bg-destructive/10"
              onClick={() => {
                if (
                  confirm("Are you sure you want to delete this recording? This cannot be undone.")
                ) {
                  deleteMutation.mutate();
                }
              }}
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? <Loader2 className="mr-2 size-4 animate-spin" /> : null}
              <Trash2 className="mr-2 size-4" /> Delete Recording
            </Button>
          </section>

          {/* Student Engagement Statistics Card */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Users className="size-4 text-accent" /> Student Engagement
              </CardTitle>
              <CardDescription>Metrics from enrolled student activity</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {statsQuery.isLoading ? (
                <div className="flex h-36 items-center justify-center">
                  <Loader2 className="size-4 animate-spin text-muted-foreground" />
                </div>
              ) : statsQuery.isError || !stats ? (
                <p className="text-sm text-muted-foreground text-center">
                  Analytics not available for this course.
                </p>
              ) : (
                <>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="surface-panel p-3 border text-center">
                      <span className="block text-2xl font-bold text-foreground">
                        {stats.totalStudents}
                      </span>
                      <span className="text-xs text-muted-foreground">Enrolled Students</span>
                    </div>
                    <div className="surface-panel p-3 border text-center">
                      <span className="block text-2xl font-bold text-foreground">
                        {stats.studentsStarted}
                      </span>
                      <span className="text-xs text-muted-foreground">Started Watching</span>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground font-medium">Completion Rate</span>
                      <span className="font-semibold text-foreground">{stats.completionRate}%</span>
                    </div>
                    <Progress value={stats.completionRate} className="h-2" />
                    <p className="text-xs text-muted-foreground">
                      {stats.studentsCompleted} of {stats.studentsStarted} active viewers reached
                      90% completion.
                    </p>
                  </div>

                  <div className="space-y-4 border-t pt-4">
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Average Progress</span>
                      <span className="font-semibold text-foreground">
                        {stats.averageWatchPercentage}%
                      </span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Average Watch Duration</span>
                      <span className="font-semibold text-foreground">
                        {formatDuration(stats.averageWatchDuration)}
                      </span>
                    </div>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </>
  );
}

// Simple fallback icon
function AlertCircle(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg
      {...props}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="12" cy="12" r="10" />
      <line x1="12" x2="12" y1="8" y2="12" />
      <line x1="12" x2="12.01" y1="16" y2="16" />
    </svg>
  );
}
