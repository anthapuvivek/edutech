import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  BookOpen,
  CheckCircle2,
  ExternalLink,
  Eye,
  EyeOff,
  Filter,
  Layers,
  MoreVertical,
  Pencil,
  Plus,
  Search,
  Sparkles,
  Trash2,
  Users,
} from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
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
  DropdownMenuLabel,
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { formatPrice } from "@/lib/format";
import { ApiError } from "@/services/api-client";
import { adminService } from "@/services/admin.service";
import type { Course } from "@/types";

export const Route = createFileRoute("/admin/courses")({
  head: () => ({
    meta: [
      { title: "Courses — Learntrix Admin" },
      {
        name: "description",
        content: "Manage course catalogue, create programs, and publish courses to the public storefront.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminCoursesPage,
});

const CATEGORIES = [
  "Full Stack Development",
  "Cloud & DevOps",
  "Data Science & AI",
  "Mobile Development",
  "Cybersecurity",
  "Software Engineering",
  "UI/UX Design",
];

const DEFAULT_THUMBNAILS = [
  { label: "Full Stack / Coding", url: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=800&q=70" },
  { label: "Cloud & DevOps", url: "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=70" },
  { label: "Data Science & AI", url: "https://images.unsplash.com/photo-1555949963-aa79dcee981c?auto=format&fit=crop&w=800&q=70" },
  { label: "System Architecture", url: "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=800&q=70" },
];

function AdminCoursesPage() {
  const queryClient = useQueryClient();

  const coursesQuery = useQuery({
    queryKey: ["admin", "courses"],
    queryFn: () => adminService.courses(),
  });

  const trainersQuery = useQuery({
    queryKey: ["admin", "trainers"],
    queryFn: () => adminService.trainers(),
  });

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [categoryFilter, setCategoryFilter] = useState("ALL");

  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [saving, setSaving] = useState(false);

  // Form states for creation
  const [createCategory, setCreateCategory] = useState(CATEGORIES[0]!);
  const [createLevel, setCreateLevel] = useState("Intermediate");
  const [createStatus, setCreateStatus] = useState("DRAFT");
  const [createInstructorId, setCreateInstructorId] = useState<string>("none");

  // Form states for editing
  const [editCategory, setEditCategory] = useState(CATEGORIES[0]!);
  const [editLevel, setEditLevel] = useState("Intermediate");
  const [editStatus, setEditStatus] = useState("PUBLISHED");
  const [editInstructorId, setEditInstructorId] = useState<string>("none");

  // Mutations
  const publishMutation = useMutation({
    mutationFn: (id: string) => adminService.publishCourse(id),
    onSuccess: (updated) => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "courses"] });
      void queryClient.invalidateQueries({ queryKey: ["courses"] });
      void queryClient.invalidateQueries({ queryKey: ["courses", "featured"] });
      toast.success(`Published "${updated.title}" successfully. It is now live on the storefront!`);
    },
    onError: (err) => {
      toast.error(err instanceof Error ? err.message : "Failed to publish course");
    },
  });

  const unpublishMutation = useMutation({
    mutationFn: (id: string) => adminService.unpublishCourse(id),
    onSuccess: (updated) => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "courses"] });
      void queryClient.invalidateQueries({ queryKey: ["courses"] });
      void queryClient.invalidateQueries({ queryKey: ["courses", "featured"] });
      toast.success(`Unpublished "${updated.title}". It is now hidden in DRAFT status.`);
    },
    onError: (err) => {
      toast.error(err instanceof Error ? err.message : "Failed to unpublish course");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => adminService.deleteCourse(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["admin", "courses"] });
      void queryClient.invalidateQueries({ queryKey: ["courses"] });
      void queryClient.invalidateQueries({ queryKey: ["courses", "featured"] });
      toast.success("Course deleted/archived successfully.");
    },
    onError: (err) => {
      toast.error(err instanceof Error ? err.message : "Failed to delete course");
    },
  });

  const courses = coursesQuery.data ?? [];
  const trainers = trainersQuery.data ?? [];

  // Filtered courses
  const filteredCourses = courses.filter((c) => {
    const q = search.trim().toLowerCase();
    const matchesSearch =
      !q ||
      c.title.toLowerCase().includes(q) ||
      (c.slug && c.slug.toLowerCase().includes(q)) ||
      (c.category && c.category.toLowerCase().includes(q)) ||
      (c.subtitle && c.subtitle.toLowerCase().includes(q));

    const matchesStatus =
      statusFilter === "ALL" || (c.status && c.status.toUpperCase() === statusFilter.toUpperCase());

    const matchesCategory =
      categoryFilter === "ALL" || (c.category && c.category.toLowerCase() === categoryFilter.toLowerCase());

    return matchesSearch && matchesStatus && matchesCategory;
  });

  // Calculate statistics
  const totalCourses = courses.length;
  const publishedCourses = courses.filter((c) => (c.status ?? "PUBLISHED").toUpperCase() === "PUBLISHED").length;
  const draftCourses = courses.filter((c) => (c.status ?? "").toUpperCase() === "DRAFT").length;
  const totalStudents = courses.reduce((acc, c) => acc + (c.studentCount || 0), 0);

  // Handle Create Course
  async function handleCreateCourse(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const getString = (k: string) => (form.get(k) as string | null)?.trim() || undefined;

    const title = getString("title");
    if (!title) {
      toast.error("Course title is required");
      return;
    }

    const payload: Record<string, unknown> = {
      title,
      slug: getString("slug"),
      subtitle: getString("subtitle"),
      category: createCategory,
      thumbnailUrl: getString("thumbnailUrl") || DEFAULT_THUMBNAILS[0]!.url,
      level: createLevel,
      language: getString("language") || "English",
      durationHours: form.get("durationHours") ? Number(form.get("durationHours")) : 36,
      lessonCount: form.get("lessonCount") ? Number(form.get("lessonCount")) : 12,
      price: form.get("price") ? Number(form.get("price")) : 0,
      originalPrice: form.get("originalPrice") ? Number(form.get("originalPrice")) : undefined,
      currency: getString("currency") || "INR",
      status: createStatus,
      instructorId: createInstructorId !== "none" ? createInstructorId : undefined,
    };

    setSaving(true);
    try {
      const created = await adminService.createCourse(payload);
      void queryClient.invalidateQueries({ queryKey: ["admin", "courses"] });
      void queryClient.invalidateQueries({ queryKey: ["courses"] });
      void queryClient.invalidateQueries({ queryKey: ["courses", "featured"] });
      toast.success(`Course "${created.title}" created successfully as ${created.status || "DRAFT"}!`);
      setCreateDialogOpen(false);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        toast.error(err.message || "Failed to create course");
      } else {
        toast.error(err instanceof Error ? err.message : "Failed to create course");
      }
    } finally {
      setSaving(false);
    }
  }

  // Handle Edit Course
  async function handleUpdateCourse(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!selectedCourse) return;

    const form = new FormData(e.currentTarget);
    const getString = (k: string) => (form.get(k) as string | null)?.trim() || undefined;

    const title = getString("title");
    if (!title) {
      toast.error("Course title is required");
      return;
    }

    const payload: Record<string, unknown> = {
      title,
      slug: getString("slug"),
      subtitle: getString("subtitle"),
      category: editCategory,
      thumbnailUrl: getString("thumbnailUrl"),
      level: editLevel,
      language: getString("language"),
      durationHours: form.get("durationHours") ? Number(form.get("durationHours")) : undefined,
      lessonCount: form.get("lessonCount") ? Number(form.get("lessonCount")) : undefined,
      price: form.get("price") !== null ? Number(form.get("price")) : undefined,
      originalPrice: form.get("originalPrice") ? Number(form.get("originalPrice")) : undefined,
      currency: getString("currency"),
      status: editStatus,
      instructorId: editInstructorId !== "none" ? editInstructorId : undefined,
    };

    setSaving(true);
    try {
      const updated = await adminService.updateCourse(selectedCourse.id, payload);
      void queryClient.invalidateQueries({ queryKey: ["admin", "courses"] });
      void queryClient.invalidateQueries({ queryKey: ["courses"] });
      void queryClient.invalidateQueries({ queryKey: ["courses", "featured"] });
      toast.success(`Course "${updated.title}" updated successfully!`);
      setEditDialogOpen(false);
      setSelectedCourse(null);
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        toast.error(err.message || "Failed to update course");
      } else {
        toast.error(err instanceof Error ? err.message : "Failed to update course");
      }
    } finally {
      setSaving(false);
    }
  }

  const openEditModal = (c: Course) => {
    setSelectedCourse(c);
    setEditCategory(c.category || CATEGORIES[0]!);
    setEditLevel(c.level || "Intermediate");
    setEditStatus(c.status ? c.status.toUpperCase() : "PUBLISHED");
    setEditInstructorId(c.instructor?.id ? String(c.instructor.id) : "none");
    setEditDialogOpen(true);
  };

  return (
    <>
      <PageHeader
        title="Courses & Programs"
        description="Manage curriculum, create courses, update pricing, and publish programs to the public catalogue."
        action={
          <Button onClick={() => setCreateDialogOpen(true)} className="gap-2 shadow-sm">
            <Plus className="size-4" /> Create Course
          </Button>
        }
      />

      {/* KPI Stats */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" aria-label="Course stats">
        <StatCard
          label="Total Courses"
          value={coursesQuery.isPending ? "..." : totalCourses}
          hint="All registered programs"
          icon={BookOpen}
        />
        <StatCard
          label="Published Courses"
          value={coursesQuery.isPending ? "..." : publishedCourses}
          hint="Visible on storefront"
          icon={CheckCircle2}
        />
        <StatCard
          label="Draft Courses"
          value={coursesQuery.isPending ? "..." : draftCourses}
          hint="Hidden from public catalogue"
          icon={Layers}
        />
        <StatCard
          label="Total Enrollments"
          value={coursesQuery.isPending ? "..." : totalStudents.toLocaleString()}
          hint="Active learners across catalogue"
          icon={Users}
        />
      </section>

      {/* Filter and Search Bar */}
      <Panel title="Search & Filter Catalogue">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by title, slug, category..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <div className="flex items-center gap-1.5 text-xs text-muted-foreground font-medium">
              <Filter className="size-3.5" />
              <span>Filters:</span>
            </div>

            {/* Status Filter */}
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-[140px] h-9 text-xs">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Statuses</SelectItem>
                <SelectItem value="PUBLISHED">Published</SelectItem>
                <SelectItem value="DRAFT">Draft</SelectItem>
                <SelectItem value="ARCHIVED">Archived</SelectItem>
              </SelectContent>
            </Select>

            {/* Category Filter */}
            <Select value={categoryFilter} onValueChange={setCategoryFilter}>
              <SelectTrigger className="w-[180px] h-9 text-xs">
                <SelectValue placeholder="Category" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Categories</SelectItem>
                {CATEGORIES.map((cat) => (
                  <SelectItem key={cat} value={cat}>
                    {cat}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      </Panel>

      {/* Main Course Table */}
      <Panel title={`Courses (${filteredCourses.length})`}>
        {coursesQuery.isPending ? (
          <div className="p-6 space-y-3">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
            <Skeleton className="h-14 w-full" />
          </div>
        ) : filteredCourses.length === 0 ? (
          <div className="py-16 text-center space-y-3">
            <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
              <BookOpen className="size-6" />
            </div>
            <h3 className="text-base font-semibold">No courses match your criteria</h3>
            <p className="text-sm text-muted-foreground max-w-sm mx-auto">
              {search || statusFilter !== "ALL" || categoryFilter !== "ALL"
                ? "Try adjusting your search query or status filter."
                : "Get started by creating your first course program."}
            </p>
            <Button onClick={() => setCreateDialogOpen(true)} className="gap-2 mt-2">
              <Plus className="size-4" /> Create Course
            </Button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Course</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Level / Duration</TableHead>
                  <TableHead>Price</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Students</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredCourses.map((c) => {
                  const isPublished = (c.status ?? "PUBLISHED").toUpperCase() === "PUBLISHED";
                  return (
                    <TableRow key={c.id} className="group">
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <img
                            src={c.thumbnailUrl || DEFAULT_THUMBNAILS[0]!.url}
                            alt={c.title}
                            className="size-12 rounded object-cover border bg-muted flex-shrink-0"
                          />
                          <div className="min-w-0">
                            <p className="font-medium text-foreground truncate max-w-xs">{c.title}</p>
                            <p className="text-xs text-muted-foreground font-mono truncate max-w-xs">
                              /{c.slug}
                            </p>
                          </div>
                        </div>
                      </TableCell>

                      <TableCell>
                        <Badge variant="secondary" className="font-normal text-xs">
                          {c.category}
                        </Badge>
                      </TableCell>

                      <TableCell>
                        <div className="text-xs space-y-0.5">
                          <span className="font-medium text-foreground">{c.level}</span>
                          <p className="text-muted-foreground">
                            {c.durationHours} hrs • {c.lessonCount} lessons
                          </p>
                        </div>
                      </TableCell>

                      <TableCell>
                        <div className="text-xs">
                          <p className="font-semibold text-foreground">
                            {formatPrice(c.price, c.currency)}
                          </p>
                          {c.originalPrice ? (
                            <p className="text-muted-foreground line-through">
                              {formatPrice(c.originalPrice, c.currency)}
                            </p>
                          ) : null}
                        </div>
                      </TableCell>

                      <TableCell>
                        <StatusBadge value={isPublished ? "published" : (c.status ?? "draft")} />
                      </TableCell>

                      <TableCell>
                        <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                          <Users className="size-3.5" />
                          <span className="font-medium text-foreground">
                            {(c.studentCount || 0).toLocaleString()}
                          </span>
                        </div>
                      </TableCell>

                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          {isPublished ? (
                            <Button
                              size="sm"
                              variant="outline"
                              className="h-8 text-xs gap-1.5"
                              disabled={unpublishMutation.isPending}
                              onClick={() => unpublishMutation.mutate(c.id)}
                            >
                              <EyeOff className="size-3.5 text-muted-foreground" />
                              <span>Unpublish</span>
                            </Button>
                          ) : (
                            <Button
                              size="sm"
                              variant="default"
                              className="h-8 text-xs gap-1.5 bg-success text-success-foreground hover:bg-success/90"
                              disabled={publishMutation.isPending}
                              onClick={() => publishMutation.mutate(c.id)}
                            >
                              <Eye className="size-3.5" />
                              <span>Publish</span>
                            </Button>
                          )}

                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon" className="size-8">
                                <MoreVertical className="size-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="w-44">
                              <DropdownMenuLabel>Actions</DropdownMenuLabel>
                              <DropdownMenuItem onClick={() => openEditModal(c)}>
                                <Pencil className="size-4 mr-2" /> Edit Course
                              </DropdownMenuItem>

                              {isPublished && (
                                <DropdownMenuItem asChild>
                                  <Link to="/courses/$slug" params={{ slug: c.slug }} target="_blank">
                                    <ExternalLink className="size-4 mr-2" /> View Live Page
                                  </Link>
                                </DropdownMenuItem>
                              )}

                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                className="text-destructive focus:text-destructive"
                                onClick={() => {
                                  if (confirm(`Are you sure you want to delete or archive "${c.title}"?`)) {
                                    deleteMutation.mutate(c.id);
                                  }
                                }}
                              >
                                <Trash2 className="size-4 mr-2" /> Delete / Archive
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        )}
      </Panel>

      {/* CREATE COURSE DIALOG */}
      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <div className="flex items-center gap-2 text-primary text-sm font-medium">
              <Sparkles className="size-4" />
              <span>Course Creation</span>
            </div>
            <DialogTitle className="text-xl">Create New Course Program</DialogTitle>
            <DialogDescription>
              Enter course details. You can save as DRAFT or directly set as PUBLISHED to launch it live.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleCreateCourse} className="space-y-4 pt-2">
            <div className="space-y-1.5">
              <Label htmlFor="create-title">Course Title *</Label>
              <Input
                id="create-title"
                name="title"
                placeholder="e.g. Java Full Stack Development Masterclass"
                required
              />
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="create-slug">Custom Slug (Optional)</Label>
                <Input
                  id="create-slug"
                  name="slug"
                  placeholder="e.g. java-full-stack-development"
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="create-category">Category *</Label>
                <Select value={createCategory} onValueChange={setCreateCategory}>
                  <SelectTrigger id="create-category">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {CATEGORIES.map((cat) => (
                      <SelectItem key={cat} value={cat}>
                        {cat}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="create-subtitle">Subtitle / Short Description</Label>
              <Textarea
                id="create-subtitle"
                name="subtitle"
                rows={2}
                placeholder="Brief summary of what students will master in this course..."
              />
            </div>

            <div className="grid gap-3 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label htmlFor="create-level">Level</Label>
                <Select value={createLevel} onValueChange={setCreateLevel}>
                  <SelectTrigger id="create-level">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Beginner">Beginner</SelectItem>
                    <SelectItem value="Intermediate">Intermediate</SelectItem>
                    <SelectItem value="Advanced">Advanced</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="create-duration">Duration (Hours)</Label>
                <Input
                  id="create-duration"
                  name="durationHours"
                  type="number"
                  defaultValue={48}
                  min={1}
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="create-lessons">Total Lessons</Label>
                <Input
                  id="create-lessons"
                  name="lessonCount"
                  type="number"
                  defaultValue={16}
                  min={1}
                />
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label htmlFor="create-price">Price (₹ INR)</Label>
                <Input
                  id="create-price"
                  name="price"
                  type="number"
                  defaultValue={14999}
                  min={0}
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="create-originalPrice">Original Price (₹ INR)</Label>
                <Input
                  id="create-originalPrice"
                  name="originalPrice"
                  type="number"
                  defaultValue={24999}
                  min={0}
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="create-status">Initial Status</Label>
                <Select value={createStatus} onValueChange={setCreateStatus}>
                  <SelectTrigger id="create-status">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="DRAFT">DRAFT (Hidden)</SelectItem>
                    <SelectItem value="PUBLISHED">PUBLISHED (Live)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="create-instructor">Assigned Instructor (Optional)</Label>
              <Select value={createInstructorId} onValueChange={setCreateInstructorId}>
                <SelectTrigger id="create-instructor">
                  <SelectValue placeholder="Select trainer" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">No Instructor assigned</SelectItem>
                  {trainers.map((t) => (
                    <SelectItem key={t.id} value={t.id}>
                      {t.name} ({t.email})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="create-thumb">Thumbnail Image URL</Label>
              <Input
                id="create-thumb"
                name="thumbnailUrl"
                defaultValue={DEFAULT_THUMBNAILS[0]!.url}
                placeholder="https://..."
              />
            </div>

            <DialogFooter className="pt-3">
              <Button type="button" variant="outline" onClick={() => setCreateDialogOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={saving}>
                {saving ? "Creating Course..." : "Create Course"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* EDIT COURSE DIALOG */}
      {selectedCourse && (
        <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle className="text-xl">Edit Course Program</DialogTitle>
              <DialogDescription>
                Update details for <span className="font-semibold text-foreground">{selectedCourse.title}</span>.
              </DialogDescription>
            </DialogHeader>

            <form onSubmit={handleUpdateCourse} className="space-y-4 pt-2">
              <div className="space-y-1.5">
                <Label htmlFor="edit-title">Course Title *</Label>
                <Input
                  id="edit-title"
                  name="title"
                  defaultValue={selectedCourse.title}
                  required
                />
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                <div className="space-y-1.5">
                  <Label htmlFor="edit-slug">Slug</Label>
                  <Input
                    id="edit-slug"
                    name="slug"
                    defaultValue={selectedCourse.slug}
                  />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="edit-category">Category *</Label>
                  <Select value={editCategory} onValueChange={setEditCategory}>
                    <SelectTrigger id="edit-category">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {CATEGORIES.map((cat) => (
                        <SelectItem key={cat} value={cat}>
                          {cat}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="edit-subtitle">Subtitle / Short Description</Label>
                <Textarea
                  id="edit-subtitle"
                  name="subtitle"
                  rows={2}
                  defaultValue={selectedCourse.subtitle || ""}
                />
              </div>

              <div className="grid gap-3 sm:grid-cols-3">
                <div className="space-y-1.5">
                  <Label htmlFor="edit-level">Level</Label>
                  <Select value={editLevel} onValueChange={setEditLevel}>
                    <SelectTrigger id="edit-level">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Beginner">Beginner</SelectItem>
                      <SelectItem value="Intermediate">Intermediate</SelectItem>
                      <SelectItem value="Advanced">Advanced</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="edit-duration">Duration (Hours)</Label>
                  <Input
                    id="edit-duration"
                    name="durationHours"
                    type="number"
                    defaultValue={selectedCourse.durationHours || 36}
                    min={1}
                  />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="edit-lessons">Total Lessons</Label>
                  <Input
                    id="edit-lessons"
                    name="lessonCount"
                    type="number"
                    defaultValue={selectedCourse.lessonCount || 12}
                    min={1}
                  />
                </div>
              </div>

              <div className="grid gap-3 sm:grid-cols-3">
                <div className="space-y-1.5">
                  <Label htmlFor="edit-price">Price (₹ INR)</Label>
                  <Input
                    id="edit-price"
                    name="price"
                    type="number"
                    defaultValue={selectedCourse.price || 0}
                    min={0}
                  />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="edit-originalPrice">Original Price (₹ INR)</Label>
                  <Input
                    id="edit-originalPrice"
                    name="originalPrice"
                    type="number"
                    defaultValue={selectedCourse.originalPrice || 0}
                    min={0}
                  />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="edit-status">Status</Label>
                  <Select value={editStatus} onValueChange={setEditStatus}>
                    <SelectTrigger id="edit-status">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="DRAFT">DRAFT (Hidden)</SelectItem>
                      <SelectItem value="PUBLISHED">PUBLISHED (Live)</SelectItem>
                      <SelectItem value="ARCHIVED">ARCHIVED</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="edit-instructor">Assigned Instructor (Optional)</Label>
                <Select value={editInstructorId} onValueChange={setEditInstructorId}>
                  <SelectTrigger id="edit-instructor">
                    <SelectValue placeholder="Select trainer" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">No Instructor assigned</SelectItem>
                    {trainers.map((t) => (
                      <SelectItem key={t.id} value={t.id}>
                        {t.name} ({t.email})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="edit-thumb">Thumbnail Image URL</Label>
                <Input
                  id="edit-thumb"
                  name="thumbnailUrl"
                  defaultValue={selectedCourse.thumbnailUrl || ""}
                />
              </div>

              <DialogFooter className="pt-3">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setEditDialogOpen(false);
                    setSelectedCourse(null);
                  }}
                >
                  Cancel
                </Button>
                <Button type="submit" disabled={saving}>
                  {saving ? "Saving..." : "Save Changes"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      )}
    </>
  );
}
