import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "@tanstack/react-router";
import {
  AlertCircle,
  BookOpen,
  CheckCircle2,
  Clock,
  Loader2,
  LogIn,
  Sparkles,
  UserCheck,
} from "lucide-react";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuth } from "@/hooks/useAuth";
import { formatPrice } from "@/lib/format";
import { ApiError } from "@/services/api-client";
import { courseService } from "@/services/course.service";
import type { Course } from "@/types";
import type { Enrollment } from "@/types/lms";

interface EnrollmentDialogProps {
  course: Course;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  isAlreadyEnrolled?: boolean;
}

export function EnrollmentDialog({
  course,
  open,
  onOpenChange,
  isAlreadyEnrolled = false,
}: EnrollmentDialogProps) {
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [experienceLevel, setExperienceLevel] = useState("Fresher");
  const [learningGoal, setLearningGoal] = useState("Career Transition");
  const [phone, setPhone] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [apiErrorMsg, setApiErrorMsg] = useState<string | null>(null);
  const [isConflictAlreadyEnrolled, setIsConflictAlreadyEnrolled] = useState(false);
  const [enrolledData, setEnrolledData] = useState<Enrollment | null>(null);

  const mutation = useMutation({
    mutationFn: async () => {
      return await courseService.enroll(course.id);
    },
    onSuccess: (data) => {
      setEnrolledData(data);
      setApiErrorMsg(null);
      setIsConflictAlreadyEnrolled(false);
      // Invalidate student queries so UI immediately reflects the new enrollment
      void queryClient.invalidateQueries({ queryKey: ["student", "enrollments"] });
      void queryClient.invalidateQueries({ queryKey: ["student", "stats"] });
      void queryClient.invalidateQueries({ queryKey: ["student", "activity"] });
      toast.success("Successfully enrolled in " + course.title);
    },
    onError: (err) => {
      if (err instanceof ApiError) {
        if (err.status === 409 || err.code === "ENROLLMENT_ALREADY_EXISTS") {
          setIsConflictAlreadyEnrolled(true);
          void queryClient.invalidateQueries({ queryKey: ["student", "enrollments"] });
          setApiErrorMsg(err.message || "You are already enrolled in this course.");
          return;
        }
        if (err.status === 401) {
          setApiErrorMsg("Your session has expired. Please log in again.");
          return;
        }
        if (err.status === 403) {
          setApiErrorMsg("Only student accounts can enroll in courses. Please switch to a student account.");
          return;
        }
        if (err.fieldErrors && Object.keys(err.fieldErrors).length > 0) {
          setFieldErrors(err.fieldErrors);
        }
        setApiErrorMsg(err.message || "Enrollment failed. Please try again.");
      } else {
        setApiErrorMsg(err instanceof Error ? err.message : "An unexpected error occurred.");
      }
    },
  });

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setFieldErrors({});
    setApiErrorMsg(null);

    // Form validation
    const errors: Record<string, string> = {};
    if (phone.trim() && !/^\+?[0-9\s\-()]{7,20}$/.test(phone.trim())) {
      errors["phone"] = "Please enter a valid phone number.";
    }


    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    mutation.mutate();
  };

  const handleClose = () => {
    onOpenChange(false);
    // Reset transient states on dialog close
    setTimeout(() => {
      setApiErrorMsg(null);
      setIsConflictAlreadyEnrolled(false);
      setFieldErrors({});
    }, 200);
  };

  // 1. Unauthenticated state
  if (!isAuthenticated || !user) {
    return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-accent/10 text-accent mb-2">
              <LogIn className="size-6" />
            </div>
            <DialogTitle className="text-center text-xl">Sign in to enroll</DialogTitle>
            <DialogDescription className="text-center text-muted-foreground">
              You need an active student account to enroll in{" "}
              <span className="font-semibold text-foreground">{course.title}</span>.
            </DialogDescription>
          </DialogHeader>
          <div className="mt-4 flex flex-col gap-3">
            <Button
              size="lg"
              className="w-full"
              onClick={() => {
                onOpenChange(false);
                navigate({ to: "/login", search: { redirect: `/courses/${course.slug}` } as any });
              }}
            >
              Sign In to Continue
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="w-full"
              onClick={() => {
                onOpenChange(false);
                navigate({ to: "/register" });
              }}
            >
              Create New Account
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    );
  }

  // 2. Already enrolled state (either pre-existing or returned from 409 Conflict)
  if (isAlreadyEnrolled || isConflictAlreadyEnrolled) {
    return (
      <Dialog open={open} onOpenChange={handleClose}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary mb-2">
              <UserCheck className="size-6" />
            </div>
            <DialogTitle className="text-center text-xl">Already Enrolled</DialogTitle>
            <DialogDescription className="text-center text-muted-foreground">
              You are currently enrolled in{" "}
              <span className="font-semibold text-foreground">{course.title}</span>.
            </DialogDescription>
          </DialogHeader>
          <div className="rounded-lg border bg-muted/40 p-4 text-sm text-muted-foreground space-y-2">
            <div className="flex justify-between">
              <span>Status:</span>
              <Badge variant="success">Active</Badge>
            </div>
            <div className="flex justify-between">
              <span>Enrolled Account:</span>
              <span className="font-medium text-foreground">{user.email}</span>
            </div>
          </div>
          <DialogFooter className="mt-4 sm:justify-center gap-2">
            <Button asChild size="lg" className="w-full sm:w-auto">
              <Link to="/student/dashboard" onClick={handleClose}>
                Go to Student Dashboard
              </Link>
            </Button>
            <Button variant="outline" size="lg" onClick={handleClose}>
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  // 3. Success state
  if (mutation.isSuccess && enrolledData) {
    return (
      <Dialog open={open} onOpenChange={handleClose}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-success/15 text-success mb-2">
              <CheckCircle2 className="size-8" />
            </div>
            <DialogTitle className="text-center text-2xl font-bold text-success">
              Enrollment Confirmed!
            </DialogTitle>
            <DialogDescription className="text-center text-muted-foreground">
              Congratulations! You are now enrolled in{" "}
              <span className="font-semibold text-foreground">{course.title}</span>.
            </DialogDescription>
          </DialogHeader>

          <div className="rounded-lg border bg-muted/30 p-4 space-y-2.5 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Enrollment ID:</span>
              <span className="font-mono text-xs text-foreground font-semibold">
                {enrolledData.id}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Student Name:</span>
              <span className="font-medium text-foreground">{user.name}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Course Status:</span>
              <Badge variant="success">{enrolledData.status.toUpperCase()}</Badge>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Total Lessons:</span>
              <span className="font-medium text-foreground">{course.lessonCount} lessons</span>
            </div>
          </div>

          <DialogFooter className="mt-4 flex-col sm:flex-row gap-2">
            <Button asChild size="lg" className="w-full">
              <Link to="/student/dashboard" onClick={handleClose}>
                Go to Student Dashboard
              </Link>
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  // 4. Enrollment Form Modal
  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-2 text-primary text-sm font-medium">
            <Sparkles className="size-4" />
            <span>Course Enrollment</span>
          </div>
          <DialogTitle className="text-2xl">{course.title}</DialogTitle>
          <DialogDescription>
            Confirm your details below to complete your enrollment.
          </DialogDescription>
        </DialogHeader>

        {apiErrorMsg && (
          <Alert variant="destructive" className="my-2">
            <AlertCircle className="size-4" />
            <AlertTitle>Enrollment Error</AlertTitle>
            <AlertDescription>{apiErrorMsg}</AlertDescription>
          </Alert>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Summary Banner */}
          <div className="rounded-lg bg-muted/40 p-3.5 border text-sm flex items-center justify-between">
            <div className="space-y-0.5">
              <div className="flex items-center gap-2 text-muted-foreground text-xs">
                <Clock className="size-3.5" />
                <span>{course.durationHours} hours</span>
                <span>•</span>
                <BookOpen className="size-3.5" />
                <span>{course.lessonCount} lessons</span>
                <span>•</span>
                <span>{course.level}</span>
              </div>
              <p className="font-semibold text-foreground">{formatPrice(course.price, course.currency)}</p>
            </div>
            <Badge variant="secondary">Lifetime Access</Badge>
          </div>

          {/* Student details */}
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="enrol-name">Student name</Label>
              <Input id="enrol-name" value={user.name} readOnly disabled className="bg-muted" />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="enrol-email">Email address</Label>
              <Input id="enrol-email" value={user.email} readOnly disabled className="bg-muted" />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="enrol-phone">Phone number (optional)</Label>
              <Input
                id="enrol-phone"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+91 98765 43210"
              />
              {fieldErrors["phone"] && (
                <p className="text-xs text-destructive">{fieldErrors["phone"]}</p>
              )}

            </div>

            <div className="space-y-1.5">
              <Label htmlFor="enrol-exp">Experience level</Label>
              <Select value={experienceLevel} onValueChange={setExperienceLevel}>
                <SelectTrigger id="enrol-exp">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Fresher">Fresher / Beginner</SelectItem>
                  <SelectItem value="Intermediate">1-3 Years Experience</SelectItem>
                  <SelectItem value="Advanced">3+ Years Experience</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="enrol-goal">Primary learning goal</Label>
            <Select value={learningGoal} onValueChange={setLearningGoal}>
              <SelectTrigger id="enrol-goal">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="Career Transition">Career Transition / Job Search</SelectItem>
                <SelectItem value="Skill Upskilling">Skill Upskilling for Current Role</SelectItem>
                <SelectItem value="Academic">Academic / College Preparation</SelectItem>
                <SelectItem value="Personal Interest">Personal Project / Interest</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <DialogFooter className="pt-2 sm:justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={mutation.isPending}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              size="lg"
              disabled={mutation.isPending}
              className="gap-2"
            >
              {mutation.isPending ? (
                <>
                  <Loader2 className="size-4 animate-spin" />
                  <span>Enrolling...</span>
                </>
              ) : (
                <span>Confirm & Enrol Now</span>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
