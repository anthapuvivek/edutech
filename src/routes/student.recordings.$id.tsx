import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, createFileRoute } from "@tanstack/react-router";
import {
  ArrowLeft,
  Calendar,
  CheckCircle2,
  Clock,
  ExternalLink,
  Loader2,
  Lock,
  MessageSquare,
  Play,
  RotateCcw,
  Volume2,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { toast } from "sonner";

import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { recordingService } from "@/services/recording.service";

export const Route = createFileRoute("/student/recordings/$id")({
  head: () => ({
    meta: [{ title: "Watch Class — Learntrix Player" }, { name: "robots", content: "noindex" }],
  }),
  component: StudentRecordingPlayerPage,
});

function StudentRecordingPlayerPage() {
  const { id } = Route.useParams();
  const queryClient = useQueryClient();
  const videoRef = useRef<HTMLVideoElement>(null);
  const lastSavedTimeRef = useRef<number>(0);
  const [playbackSpeed, setPlaybackSpeed] = useState<number>(1);
  const [isCompletedState, setIsCompletedState] = useState(false);

  // Load class recording details
  const recordingQuery = useQuery({
    queryKey: ["student", "recording", id],
    queryFn: () => recordingService.getRecording(id),
  });

  // Load progress details
  const progressQuery = useQuery({
    queryKey: ["student", "recording", "progress", id],
    queryFn: () => recordingService.getProgress(id),
  });

  // Progress update mutation
  const progressMutation = useMutation({
    mutationFn: (args: { watchedSeconds: number; durationSeconds: number }) =>
      recordingService.updateProgress(id, args.watchedSeconds, args.durationSeconds),
    onSuccess: (data) => {
      if (data.completed && !isCompletedState) {
        setIsCompletedState(true);
        toast.success("✓ Class completed! Progress saved.");
      }
    },
  });

  const recording = recordingQuery.data;
  const progress = progressQuery.data;

  // Initialize playback time on metadata load
  const handleLoadedMetadata = () => {
    if (progress && progress.watchedSeconds > 0 && videoRef.current) {
      videoRef.current.currentTime = progress.watchedSeconds;
      lastSavedTimeRef.current = progress.watchedSeconds;
      toast.info(`Resumed playback from ${formatTime(progress.watchedSeconds)}.`);
    }
    if (progress?.completed) {
      setIsCompletedState(true);
    }
  };

  // Periodic watch progress tracking
  const handleTimeUpdate = () => {
    const video = videoRef.current;
    if (!video) return;

    const currentTime = video.currentTime;
    const duration = video.duration || recording?.durationSeconds || 3600;

    // Check difference and save every 12 seconds
    const diff = Math.abs(currentTime - lastSavedTimeRef.current);
    if (diff >= 12) {
      lastSavedTimeRef.current = currentTime;
      progressMutation.mutate({
        watchedSeconds: Math.round(currentTime),
        durationSeconds: Math.round(duration),
      });
    }
  };

  // Speed controls
  const handleSpeedChange = (speed: number) => {
    setPlaybackSpeed(speed);
    if (videoRef.current) {
      videoRef.current.playbackRate = speed;
    }
  };

  // Helper time formatter
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const hrs = Math.floor(mins / 60);
    const remMins = mins % 60;
    const secs = Math.floor(seconds % 60);
    const pad = (n: number) => String(n).padStart(2, "0");

    return hrs > 0 ? `${hrs}:${pad(remMins)}:${pad(secs)}` : `${mins}:${pad(secs)}`;
  };

  // Clean-up refs on unmount
  useEffect(() => {
    return () => {
      // Final progress save on navigate away
      if (videoRef.current) {
        const currentTime = videoRef.current.currentTime;
        const duration = videoRef.current.duration || 3600;
        if (currentTime > 0) {
          recordingService.updateProgress(id, Math.round(currentTime), Math.round(duration));
        }
      }
    };
  }, [id]);

  if (recordingQuery.isLoading || progressQuery.isLoading) {
    return (
      <div className="flex h-96 items-center justify-center">
        <Loader2 className="size-8 animate-spin text-accent" />
      </div>
    );
  }

  if (recordingQuery.isError || !recording) {
    return (
      <div className="surface-panel p-10 text-center space-y-4">
        <Lock className="size-12 text-destructive mx-auto" />
        <h2 className="text-xl font-medium text-foreground">Access Restricted</h2>
        <p className="text-muted-foreground">
          You must be enrolled in the course, and the recording must be published, to watch this
          class.
        </p>
        <Button asChild variant="outline">
          <Link to="/student/recordings">Back to Recorded Classes</Link>
        </Button>
      </div>
    );
  }

  return (
    <>
      <div className="mb-4">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/student/recordings">
            <ArrowLeft className="mr-2 size-4" /> Back to list
          </Link>
        </Button>
      </div>

      <div className="space-y-6">
        {/* Main Video Player Wrapper */}
        <section className="bg-ink overflow-hidden border rounded-lg shadow-2xl relative">
          {recording.videoUrl ? (
            <video
              ref={videoRef}
              src={recording.videoUrl}
              controls
              onLoadedMetadata={handleLoadedMetadata}
              onTimeUpdate={handleTimeUpdate}
              className="aspect-video w-full object-contain"
            />
          ) : (
            <div className="aspect-video w-full flex flex-col items-center justify-center text-center p-8 text-ink-foreground/60">
              <Loader2 className="size-8 animate-spin mb-4" />
              <h3 className="text-base font-semibold">Preparing Video...</h3>
              <p className="text-xs text-ink-foreground/50 mt-1">
                Transcoding and preparing the adaptive HLS streams.
              </p>
            </div>
          )}

          {/* Quick Player controls overlay */}
          <div className="absolute top-4 right-4 flex items-center gap-2 bg-ink/75 px-3 py-1.5 rounded-full border border-border/20 text-xs text-ink-foreground">
            <span className="flex items-center gap-1">Playback Speed:</span>
            {[1, 1.25, 1.5, 2].map((s) => (
              <button
                key={s}
                onClick={() => handleSpeedChange(s)}
                className={`px-1.5 py-0.5 rounded font-medium hover:bg-white/20 transition ${
                  playbackSpeed === s ? "bg-accent text-accent-foreground" : ""
                }`}
              >
                {s}x
              </button>
            ))}
          </div>
        </section>

        {/* Title, Details and Completion status */}
        <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4 border-b pb-6">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2 text-xs font-semibold text-accent uppercase">
              <span>{recording.courseTitle}</span>
              <span>·</span>
              <span>{recording.moduleTitle}</span>
            </div>
            <h1 className="text-2xl font-bold text-foreground leading-snug">{recording.title}</h1>
            <p className="text-sm text-muted-foreground flex flex-wrap items-center gap-x-4 gap-y-1">
              <span>
                Instructor: <strong>{recording.teacherName}</strong>
              </span>
              <span className="flex items-center gap-1">
                <Calendar className="size-3.5" />{" "}
                {new Date(recording.classDate).toLocaleDateString("en-IN", { dateStyle: "medium" })}
              </span>
              <span className="flex items-center gap-1">
                <Clock className="size-3.5" /> Duration:{" "}
                {formatTime(recording.durationSeconds || 3600)}
              </span>
            </p>
          </div>

          <div className="shrink-0 flex items-center gap-2">
            {isCompletedState ? (
              <Badge variant="success" className="gap-1 py-1.5 px-3 text-sm">
                <CheckCircle2 className="size-4" /> Completed
              </Badge>
            ) : (
              <Badge variant="outline" className="gap-1 py-1.5 px-3 text-sm animate-pulse">
                In Progress
              </Badge>
            )}
          </div>
        </div>

        {/* Navigation Tabs */}
        <Tabs defaultValue="resources" className="w-full">
          <TabsList className="grid w-full max-w-md grid-cols-3">
            <TabsTrigger value="resources">Resources</TabsTrigger>
            <TabsTrigger value="notes">Notes</TabsTrigger>
            <TabsTrigger value="discussion">Discussion</TabsTrigger>
          </TabsList>

          <TabsContent value="resources" className="surface-panel p-6 mt-4 space-y-4">
            <h3 className="font-bold text-foreground text-lg">Class Summary & Reference Links</h3>
            <p className="text-sm text-muted-foreground whitespace-pre-line leading-relaxed">
              {recording.description ||
                "No description or resource links provided for this recording."}
            </p>
            <div className="border-t pt-4 space-y-2">
              <h4 className="text-sm font-semibold text-foreground">Useful Resources</h4>
              <ul className="space-y-1.5 text-sm">
                <li className="flex items-center gap-2 text-accent">
                  <ExternalLink className="size-4" />
                  <a
                    href="https://github.com/learntrix"
                    target="_blank"
                    rel="noreferrer"
                    className="hover:underline"
                  >
                    Learntrix Github Repository
                  </a>
                </li>
                <li className="flex items-center gap-2 text-accent">
                  <ExternalLink className="size-4" />
                  <a
                    href="https://spring.io/projects/spring-boot"
                    target="_blank"
                    rel="noreferrer"
                    className="hover:underline"
                  >
                    Spring Boot Reference Documentation
                  </a>
                </li>
              </ul>
            </div>
          </TabsContent>

          <TabsContent value="notes" className="surface-panel p-6 mt-4 text-center space-y-3">
            <Clock className="size-10 text-muted-foreground/60 mx-auto" />
            <h3 className="font-bold text-foreground">Personal Study Notes</h3>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              Write down timestamps, key questions, and code syntax snippets. Notes are saved
              locally in your portal profile.
            </p>
            <Button size="sm" variant="outline">
              Create Note
            </Button>
          </TabsContent>

          <TabsContent value="discussion" className="surface-panel p-6 mt-4 text-center space-y-3">
            <MessageSquare className="size-10 text-muted-foreground/60 mx-auto" />
            <h3 className="font-bold text-foreground">Class Discussion Q&A</h3>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              Ask your peers and instructor questions about concepts taught in this class.
            </p>
            <Button size="sm" variant="outline" className="gap-1">
              <MessageSquare className="size-4" /> Ask a Question
            </Button>
          </TabsContent>
        </Tabs>
      </div>
    </>
  );
}
