/**
 * Shared domain types. These mirror the eventual FastAPI response contracts,
 * so swapping mock services for real HTTP calls requires no UI changes.
 */

export type UserRole =
  | "student"
  | "teacher"
  | "admin"
  | "super_admin"
  | "mentor"
  | "placement_officer"
  | "support_agent"
  | "counsellor"
  | "content_manager"
  | "reviewer"
  | "support"
  | "corporate";

export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  avatarUrl?: string | undefined;
  createdAt: string;
  lastActiveAt?: string | undefined;
  status: "active" | "suspended" | "pending";
}

export type CourseLevel = "Beginner" | "Intermediate" | "Advanced";

export interface Instructor {
  id: string;
  name: string;
  title: string;
  avatarUrl?: string | undefined;
  bio?: string | undefined;
  rating?: number | undefined;
  students?: number | undefined;
}

export interface Course {
  id: string;
  slug: string;
  title: string;
  subtitle: string;
  category: string;
  skills: string[];
  level: CourseLevel;
  language: string;
  durationHours: number;
  lessonCount: number;
  rating: number;
  ratingCount: number;
  studentCount: number;
  price: number;
  originalPrice?: number | undefined;
  currency: "INR" | "USD";
  thumbnailUrl: string;
  status?: "DRAFT" | "PUBLISHED" | "ARCHIVED" | string | undefined;
  // Nullable to match the API: courses.instructor_id is ON DELETE SET NULL,
  // so a course can legitimately have no instructor.
  instructor: Instructor | null;
  badges?: Array<"Bestseller" | "New" | "Career Track"> | undefined;
  updatedAt: string;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  courseCount: number;
  description: string;
}

export interface Testimonial {
  id: string;
  name: string;
  role: string;
  company: string;
  quote: string;
  avatarUrl?: string | undefined;
  rating: number;
}

export interface PlatformStat {
  id: string;
  label: string;
  value: string;
}

export interface EnquiryPayload {
  name: string;
  email: string;
  phone: string;
  courseId: string;
  experienceLevel: "Fresher" | "0-2 years" | "2-5 years" | "5+ years";
  learningMode: "Live Online" | "Self Paced" | "Hybrid" | "Corporate";
  message?: string | undefined;
}

export interface Paginated<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
}

export interface CourseQuery {
  search?: string | undefined;
  category?: string | undefined;
  level?: CourseLevel | "All" | undefined;
  maxPrice?: number | undefined;
  minRating?: number | undefined;
  sort?: "popular" | "rating" | "newest" | "price_low" | "price_high" | undefined;
  page?: number | undefined;
  pageSize?: number | undefined;
}
