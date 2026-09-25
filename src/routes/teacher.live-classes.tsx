import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import {
  AlertTriangle,
  Calendar,
  CalendarClock,
  Check,
  CheckCircle2,
  Clock,
  Copy,
  Edit3,
  ExternalLink,
  Eye,
  EyeOff,
  Globe,
  Lock,
  MoreVertical,
  PlayCircle,
  Plus,
  Radio,
  Search,
  Sparkles,
  Trash2,
  Users,
  Video,
  VideoOff,
  XCircle,
} from "lucide-react";
import { useMemo, useState } from "react";
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
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
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
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { courseService } from "@/services/course.service";
import { teacherService } from "@/services/teacher.service";
import type {
  CreateLiveClassPayload,
  EventStatus,
  EventType,
  MeetingPlatform,
  PlatformEvent,
  UpdateLiveClassPayload,
} from "@/types/lms";

export const Route = createFileRoute("/teacher/live-classes")({
  head: () => ({
    meta: [
      { title: "Live Classes — Teacher Portal | Learntrix" },
      {
        name: "description",
        content: "Schedule, manage, edit, publish, and host live classes for your enrolled batches.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: TeacherLiveClassesPage,
});

const EVENT_TYPES: EventType[] = [
  "Regular Class",
  "Demo Class",
  "Webinar",
  "Workshop",
  "Placement Program",
  "Orientation",
  "Other",
];

const PLATFORMS: MeetingPlatform[] = ["Google Meet", "Zoom", "Microsoft Teams", "Other"];

interface ClassFormData {
  title: string;
  courseTitle: string;
  batchId: string;
  customCourse: string;
  date: string;
  startTime: string;
  endTime: string;
  platform: MeetingPlatform;
  meetingUrl: string;
  passcode: string;
  type: EventType;
  description: string;
  visibility: "public" | "restricted";
  published: boolean;
  status: EventStatus;
}

const defaultFormData: ClassFormData = {
  title: "",
  // No default course: the list is loaded from the database, and pre-selecting a title that
  // may not exist would silently mislabel the class.
  courseTitle: "",
  batchId: "",
  customCourse: "",
  date: new Date().toISOString().substring(0, 10),
  startTime: "10:00",
  endTime: "11:30",
  platform: "Google Meet",
  meetingUrl: "https://meet.google.com/new",
  passcode: "",
  type: "Regular Class",
  description: "",
  visibility: "restricted",
  published: true,
  status: "Upcoming",
};

function calculateDuration(startTime: string, endTime: string): string {
  if (!startTime || !endTime) return "60 mins";
  try {
    const [sh, sm] = startTime.split(":").map(Number);
    const [eh, em] = endTime.split(":").map(Number);
    if (sh === undefined || sm === undefined || eh === undefined || em === undefined)
      return "60 mins";
    let diff = eh * 60 + em - (sh * 60 + sm);
    if (diff <= 0) diff += 24 * 60;
    const hours = Math.floor(diff / 60);
    const mins = diff % 60;
    if (hours > 0 && mins > 0) return `${hours}h ${mins}m`;
    if (hours > 0) return `${hours} hr${hours > 1 ? "s" : ""}`;
    return `${mins} mins`;
  } catch {
    return "60 mins";
  }
}

function TeacherLiveClassesPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<string>("all");
  const [selectedCourseFilter, setSelectedCourseFilter] = useState<string>("ALL");
  const [searchQuery, setSearchQuery] = useState<string>("");

  // Modal states
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingClass, setEditingClass] = useState<PlatformEvent | null>(null);
  const [cancellingClass, setCancellingClass] = useState<PlatformEvent | null>(null);
  const [deletingClass, setDeletingClass] = useState<PlatformEvent | null>(null);

  // Form State
  const [formData, setFormData] = useState<ClassFormData>(defaultFormData);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Fetch Live Classes
  const { data: classes = [], isLoading, isError } = useQuery({
    queryKey: ["teacher", "live-classes"],
    queryFn: () => teacherService.liveClasses(),
  });

  // Fetch Courses for Dropdown
  const { data: coursesData } = useQuery({
    queryKey: ["courses", "list"],
    queryFn: () => courseService.list({ pageSize: 50 }),
  });

  // The teacher's own batches. GET /teacher/batches resolves them from the JWT, so a
  // teacher can only ever schedule against a batch they actually run.
  const { data: myBatches = [] } = useQuery({
    queryKey: ["teacher", "batches"],
    queryFn: () => teacherService.batches(),
  });

  // Built only from what the database returns - the course catalogue, the teacher's own
  // batches, and courses already used by their classes. Nothing is hard-coded, so adding a
  // course or a teacher anywhere in the system needs no change here.
  const availableCourses = useMemo(() => {
    const fromApi = coursesData?.items.map((c) => c.title) ?? [];
    const fromBatches = (myBatches as any[]).map((b) => b.courseTitle);
    const fromClasses = classes.map((c) => c.courseTitle);
    return Array.from(new Set([...fromApi, ...fromBatches, ...fromClasses])).filter(Boolean);
  }, [coursesData, myBatches, classes]);

  // Mutations
  const createMutation = useMutation({
    mutationFn: (payload: CreateLiveClassPayload) => teacherService.createLiveClass(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "live-classes"] });
      toast.success("Online class scheduled successfully", {
        description: "Enrolled students can view the session schedule in their portal.",
      });
      setIsCreateOpen(false);
      setFormData(defaultFormData);
    },
    onError: () => {
      toast.error("Failed to create class. Please try again.");
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateLiveClassPayload }) =>
      teacherService.updateLiveClass(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "live-classes"] });
      toast.success("Class updated successfully");
      setEditingClass(null);
    },
    onError: () => {
      toast.error("Failed to update class.");
    },
  });

  const cancelMutation = useMutation({
    mutationFn: (id: string) => teacherService.cancelLiveClass(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "live-classes"] });
      toast.success("Class has been cancelled", {
        description: "Status changed to Cancelled. Students will be notified.",
      });
      setCancellingClass(null);
    },
    onError: () => {
      toast.error("Failed to cancel class.");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => teacherService.deleteLiveClass(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "live-classes"] });
      toast.success("Class deleted permanently");
      setDeletingClass(null);
    },
    onError: () => {
      toast.error("Failed to delete class.");
    },
  });

  const togglePublishMutation = useMutation({
    mutationFn: ({ id, published }: { id: string; published: boolean }) =>
      teacherService.togglePublish(id, published),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "live-classes"] });
      toast.success(
        variables.published ? "Class published to students" : "Class moved to draft state",
      );
    },
    onError: () => {
      toast.error("Failed to update publication status.");
    },
  });

  // Filtered classes
  const filteredClasses = useMemo(() => {
    return classes.filter((item) => {
      // Tab filter
      if (activeTab === "live" && item.status !== "Live Now") return false;
      if (activeTab === "upcoming" && item.status !== "Upcoming") return false;
      if (activeTab === "past" && item.status !== "Completed" && item.status !== "Cancelled")
        return false;

      // Course filter
      if (selectedCourseFilter !== "ALL" && item.courseTitle !== selectedCourseFilter) return false;

      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesTitle = item.title.toLowerCase().includes(q);
        const matchesCourse = item.courseTitle.toLowerCase().includes(q);
        const matchesTrainer = item.trainerName.toLowerCase().includes(q);
        const matchesDesc = (item.description || "").toLowerCase().includes(q);
        const matchesPlatform = item.platform.toLowerCase().includes(q);
        if (!matchesTitle && !matchesCourse && !matchesTrainer && !matchesDesc && !matchesPlatform) {
          return false;
        }
      }

      return true;
    });
  }, [classes, activeTab, selectedCourseFilter, searchQuery]);

  // Statistics
  const stats = useMemo(() => {
    const total = classes.length;
    const live = classes.filter((c) => c.status === "Live Now").length;
    const upcoming = classes.filter((c) => c.status === "Upcoming").length;
    const completed = classes.filter((c) => c.status === "Completed").length;
    const cancelled = classes.filter((c) => c.status === "Cancelled").length;
    return { total, live, upcoming, completed, cancelled };
  }, [classes]);

  // Copy link helper
  const handleCopyLink = (meetingUrl?: string, id?: string) => {
    if (!meetingUrl) {
      toast.error("No meeting link set for this class");
      return;
    }
    void navigator.clipboard.writeText(meetingUrl);
    if (id) {
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
    }
    toast.success("Meeting link copied to clipboard");
  };

  // Open create modal
  const openCreateDialog = () => {
    setFormData({
      ...defaultFormData,
      date: new Date().toISOString().substring(0, 10),
    });
    setIsCreateOpen(true);
  };

  // Open edit modal
  const openEditDialog = (item: PlatformEvent) => {
    setEditingClass(item);
    const isCustom = !availableCourses.includes(item.courseTitle);
    setFormData({
      title: item.title,
      courseTitle: isCustom ? "Custom" : item.courseTitle,
      batchId: (item as { batchId?: string }).batchId ?? "",
      customCourse: isCustom ? item.courseTitle : "",
      date: item.date,
      startTime: item.startTime,
      endTime: item.endTime,
      platform: item.platform,
      meetingUrl: item.meetingUrl || "",
      passcode: "",
      type: item.type,
      description: item.description || "",
      visibility: item.visibility,
      published: item.published,
      status: item.status,
    });
  };

  // Handle Form Submit
  const handleSaveClass = (e: React.FormEvent) => {
    e.preventDefault();
    const finalCourseTitle =
      formData.courseTitle === "Custom"
        ? formData.customCourse.trim() || "General Subject"
        : formData.courseTitle;

    if (!formData.title.trim()) {
      toast.error("Please enter a class title");
      return;
    }

    if (editingClass) {
      updateMutation.mutate({
        id: editingClass.id,
        payload: {
          title: formData.title.trim(),
          courseTitle: finalCourseTitle,
          // The backend reads `classDate`; sending only `date` meant an edited date was
          // silently discarded. `date` stays for the mock layer, which still reads it.
          classDate: formData.date,
          date: formData.date,
          startTime: formData.startTime,
          endTime: formData.endTime,
          platform: formData.platform,
          meetingUrl: formData.meetingUrl.trim() || undefined,
          type: formData.type,
          description: formData.description.trim() || undefined,
          visibility: formData.visibility,
          published: formData.published,
          status: formData.status,
        },
      });
    } else {
      // The batch decides who can see this class, and it also supplies the course. The
      // backend reads `classDate`, not `date` - sending `date` silently fell back to
      // "tomorrow" and left course_id/batch_id null, which no student query can match.
      const batch = (myBatches as any[]).find((b) => b.id === formData.batchId);
      if (!batch) {
        toast.error("Select a batch for this class.", {
          description: "Students see a class through their batch, so one has to be chosen.",
        });
        return;
      }

      createMutation.mutate({
        title: formData.title.trim(),
        courseId: batch.courseId,
        batchId: batch.id,
        courseTitle: finalCourseTitle,
        classDate: formData.date,
        date: formData.date,
        startTime: formData.startTime,
        endTime: formData.endTime,
        platform: formData.platform,
        meetingUrl: formData.meetingUrl.trim() || undefined,
        type: formData.type,
        description: formData.description.trim() || undefined,
        visibility: formData.visibility,
        published: formData.published,
        status: formData.status,
      });
    }
  };

  return (
    <>
      <PageHeader
        title="Live Online Classes"
        description="Schedule live lectures, set meeting links, add student instructions, and manage your online class calendar."
        action={
          <Button onClick={openCreateDialog} className="shadow-sm">
            <Plus className="mr-1.5 size-4" />
            Schedule Online Class
          </Button>
        }
      />

      {/* Overview Stat Cards */}
      <section className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="surface-panel flex items-center justify-between p-5">
          <div>
            <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
              Total Scheduled
            </p>
            <p className="mt-1 text-2xl font-bold">{stats.total}</p>
            <p className="mt-1 text-xs text-muted-foreground">All time sessions</p>
          </div>
          <div className="flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <Calendar className="size-5" />
          </div>
        </div>

        <div className="surface-panel flex items-center justify-between p-5">
          <div>
            <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
              Live Now
            </p>
            <div className="mt-1 flex items-center gap-2">
              <p className="text-2xl font-bold text-emerald-500">{stats.live}</p>
              {stats.live > 0 && (
                <span className="relative flex size-2.5">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
                  <span className="relative inline-flex size-2.5 rounded-full bg-emerald-500" />
                </span>
              )}
            </div>
            <p className="mt-1 text-xs text-muted-foreground">Ongoing sessions</p>
          </div>
          <div className="flex size-11 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-500">
            <Radio className="size-5" />
          </div>
        </div>

        <div className="surface-panel flex items-center justify-between p-5">
          <div>
            <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
              Upcoming Classes
            </p>
            <p className="mt-1 text-2xl font-bold text-sky-500">{stats.upcoming}</p>
            <p className="mt-1 text-xs text-muted-foreground">Scheduled ahead</p>
          </div>
          <div className="flex size-11 items-center justify-center rounded-xl bg-sky-500/10 text-sky-500">
            <Clock className="size-5" />
          </div>
        </div>

        <div className="surface-panel flex items-center justify-between p-5">
          <div>
            <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">
              Completed / Cancelled
            </p>
            <p className="mt-1 text-2xl font-bold">
              {stats.completed}{" "}
              <span className="text-sm font-normal text-muted-foreground">
                ({stats.cancelled} cancelled)
              </span>
            </p>
            <p className="mt-1 text-xs text-muted-foreground">Past sessions record</p>
          </div>
          <div className="flex size-11 items-center justify-center rounded-xl bg-muted text-muted-foreground">
            <CheckCircle2 className="size-5" />
          </div>
        </div>
      </section>

      {/* Tabs & Controls */}
      <section className="mb-6 space-y-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full sm:w-auto">
            <TabsList className="grid w-full grid-cols-4 sm:w-auto">
              <TabsTrigger value="all">
                All <span className="ml-1.5 text-xs text-muted-foreground">({classes.length})</span>
              </TabsTrigger>
              <TabsTrigger value="live" className="gap-1.5">
                Live Now
                {stats.live > 0 && (
                  <Badge variant="destructive" className="h-4 px-1 text-[10px] leading-none">
                    {stats.live}
                  </Badge>
                )}
              </TabsTrigger>
              <TabsTrigger value="upcoming">
                Upcoming{" "}
                <span className="ml-1.5 text-xs text-muted-foreground">({stats.upcoming})</span>
              </TabsTrigger>
              <TabsTrigger value="past">
                Past{" "}
                <span className="ml-1.5 text-xs text-muted-foreground">
                  ({stats.completed + stats.cancelled})
                </span>
              </TabsTrigger>
            </TabsList>
          </Tabs>

          <div className="flex flex-col gap-2.5 sm:flex-row sm:items-center">
            {/* Search */}
            <div className="relative min-w-[220px]">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search class or topic..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 text-sm"
              />
            </div>

            {/* Course Filter */}
            <Select value={selectedCourseFilter} onValueChange={setSelectedCourseFilter}>
              <SelectTrigger className="w-full sm:w-[200px]">
                <SelectValue placeholder="All Courses / Subjects" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Subjects</SelectItem>
                {availableCourses.map((c) => (
                  <SelectItem key={c} value={c}>
                    {c}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      </section>

      {/* Class List Content */}
      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="surface-panel p-5">
              <Skeleton className="h-5 w-1/3 mb-3" />
              <Skeleton className="h-7 w-3/4 mb-2" />
              <Skeleton className="h-4 w-1/2 mb-4" />
              <Skeleton className="h-10 w-full" />
            </div>
          ))}
        </div>
      ) : isError ? (
        <div className="surface-panel p-8 text-center">
          <p className="text-destructive font-medium">Failed to load classes</p>
          <p className="mt-1 text-sm text-muted-foreground">Please try refreshing the page.</p>
        </div>
      ) : filteredClasses.length === 0 ? (
        <EmptyState
          icon={CalendarClock}
          title={searchQuery || selectedCourseFilter !== "ALL" ? "No matching classes found" : "No live classes scheduled"}
          description={
            searchQuery || selectedCourseFilter !== "ALL"
              ? "Try adjusting your filters or search query."
              : "Schedule your first live online class to start teaching and engaging with your students."
          }
          action={
            <Button onClick={openCreateDialog} variant="outline">
              <Plus className="mr-1.5 size-4" />
              Schedule Online Class
            </Button>
          }
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-2">
          {filteredClasses.map((item) => {
            const isLive = item.status === "Live Now";
            const isCancelled = item.status === "Cancelled";
            const isCompleted = item.status === "Completed";
            const isUpcoming = item.status === "Upcoming";
            const duration = calculateDuration(item.startTime, item.endTime);

            return (
              <article
                key={item.id}
                className={`surface-panel relative flex flex-col justify-between p-5 transition-all hover:border-primary/40 ${
                  isLive
                    ? "border-emerald-500/40 bg-gradient-to-br from-emerald-500/5 via-transparent to-transparent shadow-sm"
                    : isCancelled
                      ? "opacity-75"
                      : ""
                }`}
              >
                <div>
                  {/* Top badges & actions */}
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant="outline" className="text-xs font-normal">
                        {item.courseTitle}
                      </Badge>
                      <Badge variant="secondary" className="text-xs">
                        {item.type}
                      </Badge>
                      {item.published ? (
                        <span className="inline-flex items-center gap-1 rounded-md bg-emerald-500/10 px-2 py-0.5 text-[11px] font-medium text-emerald-600 dark:text-emerald-400">
                          <Eye className="size-3" /> Published
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-md bg-amber-500/10 px-2 py-0.5 text-[11px] font-medium text-amber-600 dark:text-amber-400">
                          <EyeOff className="size-3" /> Draft
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-1.5">
                      {isLive && (
                        <Badge className="bg-emerald-500 hover:bg-emerald-600 text-white animate-pulse">
                          ● LIVE NOW
                        </Badge>
                      )}
                      {isUpcoming && (
                        <Badge variant="outline" className="border-sky-500/50 text-sky-500">
                          Upcoming
                        </Badge>
                      )}
                      {isCompleted && <Badge variant="secondary">Completed</Badge>}
                      {isCancelled && (
                        <Badge variant="destructive" className="bg-destructive/80">
                          Cancelled
                        </Badge>
                      )}

                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="size-8">
                            <MoreVertical className="size-4" />
                            <span className="sr-only">Actions</span>
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-48">
                          <DropdownMenuItem onClick={() => openEditDialog(item)}>
                            <Edit3 className="mr-2 size-4" />
                            Edit Class
                          </DropdownMenuItem>
                          {item.meetingUrl && (
                            <DropdownMenuItem
                              onClick={() => handleCopyLink(item.meetingUrl, item.id)}
                            >
                              <Copy className="mr-2 size-4" />
                              Copy Meeting Link
                            </DropdownMenuItem>
                          )}
                          <DropdownMenuItem
                            onClick={() =>
                              togglePublishMutation.mutate({
                                id: item.id,
                                published: !item.published,
                              })
                            }
                          >
                            {item.published ? (
                              <>
                                <EyeOff className="mr-2 size-4" />
                                Unpublish (Draft)
                              </>
                            ) : (
                              <>
                                <Eye className="mr-2 size-4" />
                                Publish Class
                              </>
                            )}
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          {!isCancelled && (
                            <DropdownMenuItem
                              onClick={() => setCancellingClass(item)}
                              className="text-amber-600 dark:text-amber-400"
                            >
                              <XCircle className="mr-2 size-4" />
                              Cancel Class
                            </DropdownMenuItem>
                          )}
                          <DropdownMenuItem
                            onClick={() => setDeletingClass(item)}
                            className="text-destructive"
                          >
                            <Trash2 className="mr-2 size-4" />
                            Delete Permanently
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>

                  {/* Title & Description */}
                  <h3 className="mt-3 text-lg font-semibold tracking-tight text-foreground">
                    {item.title}
                  </h3>

                  {item.description && (
                    <p className="mt-1.5 text-sm text-muted-foreground line-clamp-2">
                      {item.description}
                    </p>
                  )}

                  {/* Date, Time & Platform Grid */}
                  <div className="mt-4 grid grid-cols-2 gap-2.5 rounded-lg bg-muted/40 p-3 text-xs sm:grid-cols-3">
                    <div className="flex items-center gap-1.5 text-muted-foreground">
                      <Calendar className="size-3.5 text-primary" />
                      <span>
                        {new Date(item.date).toLocaleDateString("en-IN", {
                          day: "numeric",
                          month: "short",
                          year: "numeric",
                        })}
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5 text-muted-foreground">
                      <Clock className="size-3.5 text-primary" />
                      <span>
                        {item.startTime} - {item.endTime} ({duration})
                      </span>
                    </div>

                    <div className="col-span-2 flex items-center gap-1.5 text-muted-foreground sm:col-span-1">
                      <Video className="size-3.5 text-primary" />
                      <span className="font-medium text-foreground">{item.platform}</span>
                    </div>
                  </div>

                  {/* Link Details snippet */}
                  {item.meetingUrl && (
                    <div className="mt-3 flex items-center justify-between rounded-md border border-border/60 bg-background/50 px-3 py-1.5 text-xs">
                      <div className="flex items-center gap-2 truncate text-muted-foreground">
                        <span className="font-mono truncate">{item.meetingUrl}</span>
                      </div>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="h-6 px-2 text-xs"
                        onClick={() => handleCopyLink(item.meetingUrl, item.id)}
                      >
                        {copiedId === item.id ? (
                          <>
                            <Check className="mr-1 size-3 text-emerald-500" />
                            Copied
                          </>
                        ) : (
                          <>
                            <Copy className="mr-1 size-3" />
                            Copy
                          </>
                        )}
                      </Button>
                    </div>
                  )}
                </div>

                {/* Footer Actions */}
                <div className="mt-5 flex items-center justify-between gap-2 border-t border-border/50 pt-3">
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    {item.visibility === "restricted" ? (
                      <span className="inline-flex items-center gap-1">
                        <Lock className="size-3" /> Enrolled Only
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1">
                        <Globe className="size-3" /> Public
                      </span>
                    )}
                    <span>•</span>
                    <span>{item.trainerName}</span>
                  </div>

                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => openEditDialog(item)}
                      disabled={isCancelled}
                    >
                      <Edit3 className="mr-1.5 size-3.5" />
                      Edit
                    </Button>

                    {item.meetingUrl && !isCancelled && (
                      <Button
                        asChild
                        size="sm"
                        variant={isLive ? "default" : "secondary"}
                        className={isLive ? "bg-emerald-600 hover:bg-emerald-700 text-white font-medium" : ""}
                      >
                        <a href={item.meetingUrl} target="_blank" rel="noopener noreferrer">
                          <ExternalLink className="mr-1.5 size-3.5" />
                          {isLive ? "Join Live Class" : "Start Meeting"}
                        </a>
                      </Button>
                    )}
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      {/* CREATE & EDIT CLASS DIALOG */}
      <Dialog
        open={isCreateOpen || !!editingClass}
        onOpenChange={(open) => {
          if (!open) {
            setIsCreateOpen(false);
            setEditingClass(null);
          }
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle className="text-xl">
              {editingClass ? "Edit Online Class" : "Schedule New Online Class"}
            </DialogTitle>
            <DialogDescription>
              {editingClass
                ? "Update your class schedule, subject, meeting details, or student instructions."
                : "Enter class details, select your subject, configure meeting link, and publish to your students."}
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSaveClass} className="space-y-4 py-2">
            {/* Title */}
            <div className="space-y-1.5">
              <Label htmlFor="class-title" className="text-sm font-medium">
                Class Title <span className="text-destructive">*</span>
              </Label>
              <Input
                id="class-title"
                required
                placeholder="e.g. Masterclass: LLM Fine-Tuning & Quantization Techniques"
                value={formData.title}
                onChange={(e) => setFormData((p) => ({ ...p, title: e.target.value }))}
              />
            </div>

            {/* Course / Subject & Event Type */}
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label className="text-sm font-medium">
                  Course / Subject <span className="text-destructive">*</span>
                </Label>
                <Select
                  value={formData.courseTitle}
                  onValueChange={(val) => setFormData((p) => ({ ...p, courseTitle: val }))}
                >
                  <SelectTrigger id="class-course">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {availableCourses.map((c) => (
                      <SelectItem key={c} value={c}>
                        {c}
                      </SelectItem>
                    ))}
                    <SelectItem value="Custom">+ Custom Subject Name</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {/* A class must target a batch. The student portal only returns classes whose
                  batch contains the student, or (when there is no batch) whose course they
                  are enrolled in. A class saved with neither is invisible to everyone. */}
              <div className="space-y-1.5">
                <Label htmlFor="class-batch" className="text-sm font-medium">
                  Batch <span className="text-destructive">*</span>
                </Label>
                <Select
                  value={formData.batchId}
                  onValueChange={(val) => setFormData((p) => ({ ...p, batchId: val }))}
                >
                  <SelectTrigger id="class-batch">
                    <SelectValue placeholder="Select the batch attending this class" />
                  </SelectTrigger>
                  <SelectContent>
                    {myBatches.length === 0 ? (
                      <SelectItem value="none" disabled>
                        No batches assigned to you yet
                      </SelectItem>
                    ) : (
                      myBatches.map((b: any) => (
                        <SelectItem key={b.id} value={b.id}>
                          {b.name} · {b.courseTitle} ({b.studentCount} students)
                        </SelectItem>
                      ))
                    )}
                  </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">
                  Only students in this batch will see the class.
                </p>
              </div>

              <div className="space-y-1.5">
                <Label className="text-sm font-medium">Event Type</Label>
                <Select
                  value={formData.type}
                  onValueChange={(val) => setFormData((p) => ({ ...p, type: val as EventType }))}
                >
                  <SelectTrigger id="class-type">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {EVENT_TYPES.map((t) => (
                      <SelectItem key={t} value={t}>
                        {t}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Custom Subject Field if selected */}
            {formData.courseTitle === "Custom" && (
              <div className="space-y-1.5">
                <Label htmlFor="custom-subject" className="text-sm font-medium">
                  Custom Subject / Topic Name
                </Label>
                <Input
                  id="custom-subject"
                  required
                  placeholder="Enter course or topic name"
                  value={formData.customCourse}
                  onChange={(e) => setFormData((p) => ({ ...p, customCourse: e.target.value }))}
                />
              </div>
            )}

            {/* Date, Start Time, End Time */}
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label htmlFor="class-date" className="text-sm font-medium">
                  Date <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="class-date"
                  type="date"
                  required
                  value={formData.date}
                  onChange={(e) => setFormData((p) => ({ ...p, date: e.target.value }))}
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="class-start" className="text-sm font-medium">
                  Start Time <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="class-start"
                  type="time"
                  required
                  value={formData.startTime}
                  onChange={(e) => setFormData((p) => ({ ...p, startTime: e.target.value }))}
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="class-end" className="text-sm font-medium">
                  End Time <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="class-end"
                  type="time"
                  required
                  value={formData.endTime}
                  onChange={(e) => setFormData((p) => ({ ...p, endTime: e.target.value }))}
                />
              </div>
            </div>

            {/* Platform & Meeting URL */}
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label className="text-sm font-medium">Meeting Platform</Label>
                <Select
                  value={formData.platform}
                  onValueChange={(val) =>
                    setFormData((p) => ({ ...p, platform: val as MeetingPlatform }))
                  }
                >
                  <SelectTrigger id="class-platform">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {PLATFORMS.map((p) => (
                      <SelectItem key={p} value={p}>
                        {p}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="class-url" className="text-sm font-medium">
                  Meeting Link / URL
                </Label>
                <Input
                  id="class-url"
                  type="url"
                  placeholder="https://meet.google.com/xyz-abc-def"
                  value={formData.meetingUrl}
                  onChange={(e) => setFormData((p) => ({ ...p, meetingUrl: e.target.value }))}
                />
              </div>
            </div>

            {/* Description & Student Instructions */}
            <div className="space-y-1.5">
              <Label htmlFor="class-desc" className="text-sm font-medium">
                Description & Student Instructions
              </Label>
              <Textarea
                id="class-desc"
                rows={3}
                placeholder="Share the session agenda, prerequisites, codebase link, or required software installations for your students..."
                value={formData.description}
                onChange={(e) => setFormData((p) => ({ ...p, description: e.target.value }))}
              />
            </div>

            {/* Visibility & Status (for edit) */}
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label className="text-sm font-medium">Access Visibility</Label>
                <Select
                  value={formData.visibility}
                  onValueChange={(val: "public" | "restricted") =>
                    setFormData((p) => ({ ...p, visibility: val }))
                  }
                >
                  <SelectTrigger id="class-visibility">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="restricted">
                      Restricted (Enrolled batch students only)
                    </SelectItem>
                    <SelectItem value="public">Public (Open for demo & webinars)</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {editingClass && (
                <div className="space-y-1.5">
                  <Label className="text-sm font-medium">Session Status</Label>
                  <Select
                    value={formData.status}
                    onValueChange={(val: EventStatus) =>
                      setFormData((p) => ({ ...p, status: val }))
                    }
                  >
                    <SelectTrigger id="class-status">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Upcoming">Upcoming</SelectItem>
                      <SelectItem value="Live Now">Live Now</SelectItem>
                      <SelectItem value="Completed">Completed</SelectItem>
                      <SelectItem value="Cancelled">Cancelled</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              )}
            </div>

            {/* Publish immediately toggle */}
            <div className="flex items-center justify-between rounded-lg border border-border/80 p-3 bg-muted/20">
              <div className="space-y-0.5">
                <Label htmlFor="publish-toggle" className="text-sm font-medium cursor-pointer">
                  Publish Class Immediately
                </Label>
                <p className="text-xs text-muted-foreground">
                  Published classes are visible to students and added to their live class schedule.
                </p>
              </div>
              <Switch
                id="publish-toggle"
                checked={formData.published}
                onCheckedChange={(checked) => setFormData((p) => ({ ...p, published: checked }))}
              />
            </div>

            <DialogFooter className="pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setIsCreateOpen(false);
                  setEditingClass(null);
                }}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                {editingClass
                  ? updateMutation.isPending
                    ? "Saving Changes..."
                    : "Save Changes"
                  : createMutation.isPending
                    ? "Scheduling..."
                    : "Schedule & Save"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* CANCEL CLASS CONFIRMATION MODAL */}
      <Dialog
        open={!!cancellingClass}
        onOpenChange={(open) => {
          if (!open) setCancellingClass(null);
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-500/10 text-amber-500 mb-2">
              <AlertTriangle className="size-5" />
            </div>
            <DialogTitle>Cancel Online Class?</DialogTitle>
            <DialogDescription>
              Are you sure you want to cancel{" "}
              <strong className="text-foreground">"{cancellingClass?.title}"</strong>?
              <br />
              <br />
              This class will be marked as <strong>Cancelled</strong>. Enrolled students will see
              the cancellation notice on their schedule.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-3 gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              onClick={() => setCancellingClass(null)}
              disabled={cancelMutation.isPending}
            >
              Keep Class
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={cancelMutation.isPending}
              onClick={() => cancellingClass && cancelMutation.mutate(cancellingClass.id)}
            >
              {cancelMutation.isPending ? "Cancelling..." : "Yes, Cancel Class"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* DELETE PERMANENTLY CONFIRMATION MODAL */}
      <Dialog
        open={!!deletingClass}
        onOpenChange={(open) => {
          if (!open) setDeletingClass(null);
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <div className="flex size-10 items-center justify-center rounded-full bg-destructive/10 text-destructive mb-2">
              <Trash2 className="size-5" />
            </div>
            <DialogTitle>Delete Class Permanently?</DialogTitle>
            <DialogDescription>
              This will permanently delete{" "}
              <strong className="text-foreground">"{deletingClass?.title}"</strong> and remove it
              from all records. This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-3 gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              onClick={() => setDeletingClass(null)}
              disabled={deleteMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={deleteMutation.isPending}
              onClick={() => deletingClass && deleteMutation.mutate(deletingClass.id)}
            >
              {deleteMutation.isPending ? "Deleting..." : "Delete Permanently"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
