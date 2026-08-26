/**
 * Learning-platform domain types. These mirror the future FastAPI/PostgreSQL
 * contracts so services can swap mock data for HTTP without UI changes.
 */
import type { User, UserRole } from "@/types";

export type Role = Extract<
  UserRole,
  | "student"
  | "teacher"
  | "admin"
  | "super_admin"
  | "mentor"
  | "placement_officer"
  | "support_agent"
  | "counsellor"
>;

export interface AuthSession {
  user: AuthUser;
  accessToken: string;
  expiresAt: string;
}

export interface AuthUser extends User {
  role: Role;
  studentId?: string | undefined;
}

export interface LoginPayload {
  email: string;
  password: string;
  remember?: boolean | undefined;
}

export interface RegisterPayload {
  name: string;
  email: string;
  password: string;
}

export type EnrollmentStatus =
  "not_enrolled" | "payment_pending" | "enrolled" | "active" | "completed" | "expired";

export interface Enrollment {
  id: string;
  courseId: string;
  courseSlug: string;
  courseTitle: string;
  instructorName: string;
  thumbnailUrl: string;
  status: EnrollmentStatus;
  progressPercent: number;
  lessonsCompleted: number;
  lessonsTotal: number;
  lastLessonTitle?: string | undefined;
  nextLessonTitle?: string | undefined;
  enrolledAt: string;
  expiresAt?: string | undefined;
}

export interface StudentProfile {
  id: string;
  studentId: string;
  name: string;
  avatarUrl?: string | undefined;
  level: "Beginner" | "Learner" | "Intermediate" | "Advanced" | "Expert" | "Elite";
  nextLevel?: string | undefined;
  points: number;
  pointsToNextLevel: number;
  nextLevelThreshold: number;
  rank: number;
  streakDays: number;
}

export interface StudentStats {
  totalCourses: number;
  activeCourses: number;
  completedCourses: number;
  learningHours: number;
  problemsSolved: number;
  quizAverage: number;
  points: number;
  rank: number;
}

export type ActivityKind = "lesson" | "coding" | "quiz" | "class" | "assignment" | "achievement";

export interface ActivityItem {
  id: string;
  kind: ActivityKind;
  title: string;
  points: number;
  occurredAt: string;
}

export type EventType =
  | "Regular Class"
  | "Demo Class"
  | "Webinar"
  | "Workshop"
  | "Placement Program"
  | "Orientation"
  | "Other";

export type MeetingPlatform = "Google Meet" | "Zoom" | "Microsoft Teams" | "Other";
export type EventStatus = "Upcoming" | "Live Now" | "Completed" | "Cancelled";

export interface PlatformEvent {
  id: string;
  title: string;
  courseId?: string | undefined;
  courseTitle: string;
  batchId?: string | undefined;
  batchName?: string | undefined;
  trainerName: string;
  description?: string | undefined;
  date: string;
  startTime: string;
  endTime: string;
  platform: MeetingPlatform;
  meetingUrl?: string | undefined;
  type: EventType;
  visibility: "public" | "restricted";
  published: boolean;
  status: EventStatus;
  registrations: number;
}

export interface CreateLiveClassPayload {
  title: string;
  courseId?: string | undefined;
  courseTitle?: string | undefined;
  batchId?: string | undefined;
  trainerName?: string | undefined;
  description?: string | undefined;
  date: string;
  startTime: string;
  endTime: string;
  platform: MeetingPlatform;
  meetingUrl?: string | undefined;
  type: EventType;
  visibility?: "public" | "restricted" | undefined;
  published?: boolean | undefined;
  status?: EventStatus | undefined;
}

export interface UpdateLiveClassPayload extends Partial<CreateLiveClassPayload> {}

export interface TeacherStats {
  totalStudents: number;
  activeStudents: number;
  courses: number;
  averageCompletion: number;
  averageQuizScore: number;
  problemsSolved: number;
  engagement: number;
  upcomingClasses: number;
}

export interface TeacherStudentRow {
  id: string;
  name: string;
  email: string;
  courseTitle: string;
  progressPercent: number;
  lessonsCompleted: number;
  quizScore: number;
  assignments: string;
  problemsSolved: number;
  points: number;
  rank: number;
  lastActive: string;
  status: "active" | "at_risk" | "inactive";
}

export interface AdminStats {
  students: number;
  teachers: number;
  courses: number;
  batches: number;
  liveEvents: number;
  openEnquiries: number;
  revenueThisMonth: number;
  activeBatches: number;
}

export interface LeaderboardEntry {
  rank: number;
  studentId: string;
  name: string;
  points: number;
  problemsSolved: number;
  quizScore: number;
  attendance: number;
  streak: number;
  level: string;
  isCurrentUser?: boolean | undefined;
}
