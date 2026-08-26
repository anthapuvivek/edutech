import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import {
  AlertCircle,
  CheckCircle2,
  ChevronRight,
  FileVideo,
  Loader2,
  Sparkles,
  Upload,
} from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";

import { env } from "@/lib/env";
import { PageHeader } from "@/components/portal/StatCard";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { Textarea } from "@/components/ui/textarea";
import { courseService } from "@/services/course.service";
import { recordingService } from "@/services/recording.service";
import type { Recording } from "@/types/recording";

export const Route = createFileRoute("/teacher/recordings/new")({
  head: () => ({
    meta: [{ title: "Add Recording — Teacher Portal" }, { name: "robots", content: "noindex" }],
  }),
  component: CreateRecordedClassPage,
});

type Step = "details" | "upload" | "processing" | "ready";

function CreateRecordedClassPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [step, setStep] = useState<Step>("details");

  // Form fields
  const [courseId, setCourseId] = useState("");
  const [moduleId, setModuleId] = useState("");
  const [lessonId, setLessonId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [classDate, setClassDate] = useState(new Date().toISOString().substring(0, 16));

  // Upload fields
  const [videoFile, setVideoFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [isUploading, setIsUploading] = useState(false);

  // Created recording details
  const [createdRecording, setCreatedRecording] = useState<Recording | null>(null);

  // Fetch courses list
  const coursesQuery = useQuery({
    queryKey: ["courses", "list"],
    queryFn: () => courseService.list({ pageSize: 50 }),
  });

  // Mock sub-data for modules & lessons when in mock mode
  const courses = coursesQuery.data?.items ?? [];
  const selectedCourse = courses.find((c) => c.id === courseId);

  // Dynamic modules/lessons based on selection
  const mockModules = selectedCourse
    ? selectedCourse.skills.map((s, idx) => ({
        id: `mod-${idx + 1}`,
        title: `Module ${idx + 1} · ${s}`,
      }))
    : [];

  const mockLessons = moduleId
    ? [
        { id: `les-1`, title: "Introduction to concepts" },
        { id: `les-2`, title: "Advanced coding session" },
        { id: `les-3`, title: "Review & recap" },
      ]
    : [];

  const createRecordingMutation = useMutation({
    mutationFn: () =>
      recordingService.createRecording({
        courseId,
        moduleId,
        lessonId,
        title,
        description,
        classDate: new Date(classDate).toISOString(),
      }),
    onSuccess: (data) => {
      setCreatedRecording(data);
      setStep("upload");
      toast.success("Draft recording created. Ready for video upload.");
    },
    onError: () => {
      toast.error("Failed to create recording details.");
    },
  });

  const uploadVideoMutation = useMutation({
    mutationFn: async () => {
      if (!createdRecording || !videoFile) return;
      setIsUploading(true);
      setUploadProgress(0);

      // Generate upload signature
      const uploadInfo = await recordingService.createUploadUrl(
        createdRecording.id,
        videoFile.name,
      );

      // Perform file upload with progress tracking
      await recordingService.uploadLocalFile(uploadInfo.uploadUrl, videoFile, (pct) => {
        setUploadProgress(pct);
      });

      // Complete upload
      const finalized = await recordingService.completeUpload(createdRecording.id);
      setCreatedRecording(finalized);
      setStep("processing");
      setIsUploading(false);
      toast.success("Video uploaded. Transcoding has started.");
    },
    onError: (e) => {
      setIsUploading(false);
      toast.error("Video upload failed: " + e.message);
    },
  });

  // Poll processing status
  useEffect(() => {
    if (step !== "processing" || !createdRecording) return;

    const intervalId = setInterval(async () => {
      try {
        const updated = await recordingService.getRecording(createdRecording.id);
        if (updated.status === "READY") {
          setCreatedRecording(updated);
          setStep("ready");
          clearInterval(intervalId);
          toast.success("Video processing complete. Ready to publish.");
        } else if (updated.status === "FAILED") {
          clearInterval(intervalId);
          toast.error("Video processing failed. Please retry.");
        }
      } catch {
        // Suppress polling network logs
      }
    }, 4000);

    return () => clearInterval(intervalId);
  }, [step, createdRecording]);

  const publishMutation = useMutation({
    mutationFn: () => {
      if (!createdRecording) return Promise.resolve({} as Recording);
      return recordingService.publishRecording(createdRecording.id);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "recordings"] });
      toast.success("Class published! Enrolled students are notified.");
      void navigate({ to: "/teacher/recordings" });
    },
    onError: () => {
      toast.error("Failed to publish recording.");
    },
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      const validTypes = ["video/mp4", "video/quicktime", "video/webm"];
      if (!validTypes.includes(file.type)) {
        toast.error("Unsupported file type. Please upload MP4, MOV, or WebM.");
        return;
      }
      if (file.size > 2147483648) {
        toast.error("File is too large. Maximum size is 2GB.");
        return;
      }
      setVideoFile(file);
    }
  };

  return (
    <>
      <PageHeader
        title="Add Recorded Class"
        description="Fill class details, upload your video recording, and publish it to the student curriculum."
      />

      {/* Process Wizard */}
      <div className="surface-panel mb-8 p-5">
        <div className="flex flex-col justify-around gap-y-4 md:flex-row md:items-center">
          <div className="flex items-center gap-2">
            <span
              className={`grid size-7 place-items-center rounded-full text-xs font-semibold ${step === "details" ? "bg-accent text-accent-foreground" : "bg-success text-success-foreground"}`}
            >
              {step === "details" ? "1" : "✓"}
            </span>
            <span
              className={`text-sm font-medium ${step === "details" ? "text-foreground" : "text-muted-foreground"}`}
            >
              Class Details
            </span>
          </div>
          <ChevronRight className="hidden md:block size-4 text-muted-foreground" />

          <div className="flex items-center gap-2">
            <span
              className={`grid size-7 place-items-center rounded-full text-xs font-semibold ${step === "upload" ? "bg-accent text-accent-foreground" : step === "details" ? "bg-muted text-muted-foreground" : "bg-success text-success-foreground"}`}
            >
              {step === "upload" ? "2" : step === "details" ? "2" : "✓"}
            </span>
            <span
              className={`text-sm font-medium ${step === "upload" ? "text-foreground" : "text-muted-foreground"}`}
            >
              Upload Video
            </span>
          </div>
          <ChevronRight className="hidden md:block size-4 text-muted-foreground" />

          <div className="flex items-center gap-2">
            <span
              className={`grid size-7 place-items-center rounded-full text-xs font-semibold ${step === "processing" ? "bg-accent text-accent-foreground" : step === "ready" ? "bg-success text-success-foreground" : "bg-muted text-muted-foreground"}`}
            >
              {step === "processing" ? "3" : step === "ready" ? "✓" : "3"}
            </span>
            <span
              className={`text-sm font-medium ${step === "processing" ? "text-foreground" : "text-muted-foreground"}`}
            >
              Transcoding
            </span>
          </div>
          <ChevronRight className="hidden md:block size-4 text-muted-foreground" />

          <div className="flex items-center gap-2">
            <span
              className={`grid size-7 place-items-center rounded-full text-xs font-semibold ${step === "ready" ? "bg-accent text-accent-foreground animate-pulse" : "bg-muted text-muted-foreground"}`}
            >
              4
            </span>
            <span
              className={`text-sm font-medium ${step === "ready" ? "text-foreground" : "text-muted-foreground"}`}
            >
              Publish
            </span>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-3xl">
        {step === "details" && (
          <Card>
            <CardHeader>
              <CardTitle>Class Recording Details</CardTitle>
              <CardDescription>
                Map this recording to a specific course curriculum module and lesson.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="course">Course</Label>
                  <select
                    id="course"
                    value={courseId}
                    onChange={(e) => {
                      setCourseId(e.target.value);
                      setModuleId("");
                      setLessonId("");
                    }}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    <option value="">Select a Course</option>
                    {courses.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.title}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="classDate">Class Date & Time</Label>
                  <Input
                    id="classDate"
                    type="datetime-local"
                    value={classDate}
                    onChange={(e) => setClassDate(e.target.value)}
                  />
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="module">Curriculum Module</Label>
                  <select
                    id="module"
                    value={moduleId}
                    onChange={(e) => {
                      setModuleId(e.target.value);
                      setLessonId("");
                    }}
                    disabled={!courseId}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
                  >
                    <option value="">Select Module</option>
                    {/* If using real DB, we would load. For mock/dev, populate list */}
                    {env.useMocks ? (
                      mockModules.map((m) => (
                        <option key={m.id} value={m.id}>
                          {m.title}
                        </option>
                      ))
                    ) : (
                      // We will support simple fallback in rendering
                      <>
                        <option value="00000000-0000-0000-0000-000000000301">
                          Introduction & Setup
                        </option>
                        <option value="00000000-0000-0000-0000-000000000302">
                          JPA & Databases
                        </option>
                      </>
                    )}
                  </select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="lesson">Lesson Reference</Label>
                  <select
                    id="lesson"
                    value={lessonId}
                    onChange={(e) => setLessonId(e.target.value)}
                    disabled={!moduleId}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
                  >
                    <option value="">Select Lesson</option>
                    {env.useMocks ? (
                      mockLessons.map((l) => (
                        <option key={l.id} value={l.id}>
                          {l.title}
                        </option>
                      ))
                    ) : (
                      <>
                        <option value="00000000-0000-0000-0000-000000000401">
                          Introduction to Java
                        </option>
                        <option value="00000000-0000-0000-0000-000000000402">
                          Spring Boot Core Concepts
                        </option>
                        <option value="00000000-0000-0000-0000-000000000403">
                          PostgreSQL Integration
                        </option>
                      </>
                    )}
                  </select>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="title">Class Title</Label>
                <Input
                  id="title"
                  placeholder="e.g. Java OOP - Interface and Abstract Classes"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Description & Resources</Label>
                <Textarea
                  id="description"
                  placeholder="Provide an overview of concepts discussed, links to Github labs, slides or homework assignments..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={4}
                />
              </div>

              <div className="flex justify-end gap-2 border-t pt-4">
                <Button variant="outline" asChild>
                  <Link to="/teacher/recordings">Cancel</Link>
                </Button>
                <Button
                  onClick={() => createRecordingMutation.mutate()}
                  disabled={
                    !courseId ||
                    !moduleId ||
                    !lessonId ||
                    !title.trim() ||
                    createRecordingMutation.isPending
                  }
                >
                  {createRecordingMutation.isPending ? (
                    <Loader2 className="mr-2 size-4 animate-spin" />
                  ) : null}
                  Save & Continue
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {step === "upload" && (
          <Card>
            <CardHeader>
              <CardTitle>Upload Class Video</CardTitle>
              <CardDescription>
                Upload the class recording file. Supported formats: MP4, MOV, WebM (Max 2GB).
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {!isUploading ? (
                <div className="flex flex-col items-center justify-center border-2 border-dashed border-muted-foreground/30 rounded-lg p-10 bg-muted/10 hover:bg-muted/20 transition cursor-pointer relative">
                  <input
                    type="file"
                    accept="video/mp4,video/quicktime,video/webm"
                    className="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
                    onChange={handleFileChange}
                  />
                  <Upload className="size-10 text-muted-foreground mb-4" />
                  <p className="text-sm font-medium text-foreground text-center">
                    Drag and drop your video file here, or click to browse
                  </p>
                  <p className="text-xs text-muted-foreground mt-2">
                    MP4 / H.264 is recommended for fastest encoding.
                  </p>

                  {videoFile && (
                    <div className="mt-6 flex items-center gap-3 bg-panel border rounded-md p-3 max-w-md w-full relative z-10">
                      <FileVideo className="size-8 text-accent shrink-0" />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium truncate text-foreground">
                          {videoFile.name}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {(videoFile.size / 1048576).toFixed(1)} MB
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="flex justify-between text-sm">
                    <span className="font-medium text-foreground flex items-center gap-2">
                      <Loader2 className="size-4 animate-spin text-accent" /> Uploading video
                      file...
                    </span>
                    <span className="tabular-nums text-muted-foreground font-semibold">
                      {uploadProgress}%
                    </span>
                  </div>
                  <Progress value={uploadProgress} className="h-2.5" />
                  <p className="text-xs text-muted-foreground text-center">
                    Please keep this window open while the file is transferring to secure storage.
                  </p>
                </div>
              )}

              <div className="flex justify-end gap-2 border-t pt-4">
                <Button variant="outline" onClick={() => setStep("details")} disabled={isUploading}>
                  Back
                </Button>
                <Button
                  onClick={() => uploadVideoMutation.mutate()}
                  disabled={!videoFile || isUploading}
                >
                  Start Upload
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {step === "processing" && (
          <Card className="text-center py-10">
            <CardHeader className="items-center">
              <div className="size-14 rounded-full bg-accent/20 flex items-center justify-center animate-pulse">
                <Sparkles className="size-6 text-accent" />
              </div>
              <CardTitle className="mt-4">Processing Recording</CardTitle>
              <CardDescription className="max-w-md">
                We are generating the adaptive HLS streams (.m3u8 manifests) and compressing
                thumbnails. This usually takes around 10–15 seconds in development.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex justify-center items-center gap-3 text-sm text-accent font-medium">
                <Loader2 className="size-4 animate-spin" /> Transcoding in progress...
              </div>
              <Progress value={45} className="h-2 max-w-xs mx-auto animate-pulse" />

              <Alert className="max-w-md mx-auto text-left">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>Did you know?</AlertTitle>
                <AlertDescription>
                  You don't have to wait here! You can return to the recordings list and check
                  status later. We will process it in the background.
                </AlertDescription>
              </Alert>

              <div className="border-t pt-6 flex justify-center">
                <Button variant="outline" asChild>
                  <Link to="/teacher/recordings">Go to Dashboard</Link>
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {step === "ready" && (
          <Card className="text-center py-10 border-success/30">
            <CardHeader className="items-center">
              <CheckCircle2 className="size-14 text-success" />
              <CardTitle className="mt-4 text-success">Processing Completed!</CardTitle>
              <CardDescription className="max-w-md">
                Your video HLS manifest and thumbnails are fully prepared. The class is ready to be
                published to enrolled students.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="surface-panel p-4 max-w-md mx-auto text-left space-y-2 text-sm border bg-muted/10">
                <p>
                  <strong>Title:</strong> {title}
                </p>
                <p>
                  <strong>Course:</strong> {selectedCourse?.title || "Spring Boot"}
                </p>
                <p>
                  <strong>Length:</strong> 1 hour 12 minutes (transcoded)
                </p>
              </div>

              <div className="flex justify-center gap-3 border-t pt-6">
                <Button variant="outline" asChild>
                  <Link to="/teacher/recordings">Keep Draft</Link>
                </Button>
                <Button
                  onClick={() => publishMutation.mutate()}
                  disabled={publishMutation.isPending}
                >
                  {publishMutation.isPending ? (
                    <Loader2 className="mr-2 size-4 animate-spin" />
                  ) : null}
                  Publish Class
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </>
  );
}
