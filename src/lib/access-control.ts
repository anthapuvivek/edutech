/**
 * Central route → permission registry.
 *
 * This is the single source of truth the UI uses to decide which portal pages a
 * role may open and which navigation items it may see. It intentionally mirrors
 * the permission list the backend will enforce: the frontend hides and blocks,
 * the API is still the authority (see src/lib/authz.ts for the service-layer
 * guard applied to every privileged call).
 */
import { can } from "@/services/permissions.service";
import type { Permission, PlatformRole } from "@/types/ops";

/** Any-of semantics: a role may open the path if it holds at least one listed permission. */
export const routePermissions: Record<string, Permission[]> = {
  // Student portal — own learning and career data only.
  "/student/dashboard": ["learning.own"],
  "/student/courses": ["learning.own"],
  "/student/live-classes": ["learning.own"],
  "/student/recordings": ["learning.own"],
  "/student/leaderboard": ["learning.own"],
  // Assessment surfaces. The backend decides which quizzes and problems this
  // student actually sees (enrolment + batch + PUBLISHED); this only decides
  // whether the page may be opened at all.
  "/student/quizzes": ["learning.own"],
  "/student/coding-practice": ["learning.own"],
  "/student/career": ["career.own"],
  "/student/career/jobs": ["career.own"],
  "/student/career/applications": ["career.own"],
  "/student/career/profile": ["career.own"],
  "/student/career/onboarding": ["career.own"],

  // Teaching / mentoring views. Mentors reuse the existing student-progress
  // screens read-only; they never get the teaching management surface.
  "/teacher/dashboard": ["teaching.manage"],
  "/teacher/batches": ["teaching.manage"],
  "/teacher/live-classes": ["teaching.manage"],
  "/teacher/recordings": ["teaching.manage"],
  "/teacher/students": ["teaching.manage", "mentor.students"],
  // Authoring surfaces. CourseAccessService still re-checks course and batch
  // ownership on every call, so this gate is about the page, not the data.
  "/teacher/quizzes": ["teaching.manage"],
  "/teacher/coding-problems": ["teaching.manage"],
  // AI Question Assistant. Teacher-only: the backend rejects a student with 403
  // regardless of this entry, which only governs whether the page may be opened.
  "/teacher/ai-assistant": ["teaching.manage"],

  // Platform administration.
  "/admin/dashboard": ["platform.manage"],
  "/admin/students": ["platform.manage"],
  "/admin/teachers": ["platform.manage"],
  "/admin/batches": ["platform.manage"],
  "/admin/coding-problems": ["platform.manage"],
  "/admin/attendance": ["platform.manage"],
  "/admin/performance": ["platform.manage"],
  "/admin/events": ["platform.manage"],
  "/admin/articles": ["platform.manage"],
  "/admin/cms": ["platform.manage"],
  "/admin/communication": ["platform.manage"],
  "/admin/notifications": ["platform.manage"],
  "/admin/analytics": ["platform.manage"],
  "/admin/settings": ["platform.manage"],
  "/admin/audit-logs": ["audit.view"],
  "/admin/marketing/coupons": ["coupons.manage"],
  "/admin/crm": ["crm.view"],
  "/admin/support": ["support.manage"],

  // Career & placement operations — shared by admins and placement officers.
  "/admin/career/jobs": ["placement.jobs.view"],
  "/admin/career/companies": ["placement.jobs.view"],
  "/admin/career/applications": ["placement.applications.manage"],
  "/admin/career/referrals": ["placement.applications.manage"],
  "/admin/career/preparation": ["placement.students.view"],
  "/admin/career/resume-templates": ["placement.students.view"],
  "/admin/career/resources": ["placement.students.view"],
  "/admin/career/eligibility": ["placement.students.view"],
};

/** Where each role lands after sign-in, and where unauthorized access is sent. */
export const roleLanding: Record<PlatformRole, string> = {
  super_admin: "/admin/dashboard",
  admin: "/admin/dashboard",
  teacher: "/teacher/dashboard",
  mentor: "/teacher/students",
  placement_officer: "/admin/career/jobs",
  counsellor: "/admin/crm",
  support_agent: "/admin/support",
  student: "/student/dashboard",
};

function normalize(path: string) {
  const trimmed = path.replace(/\/+$/, "");
  return trimmed === "" ? "/" : trimmed;
}

/** Resolves the closest registered ancestor entry, so /admin/crm/lead-3 inherits /admin/crm. */
export function requiredPermissions(path: string): Permission[] | undefined {
  const target = normalize(path);
  if (routePermissions[target]) return routePermissions[target];
  const match = Object.keys(routePermissions)
    .filter((registered) => target.startsWith(`${registered}/`))
    .sort((a, b) => b.length - a.length)[0];
  return match ? routePermissions[match] : undefined;
}

export function canAccessPath(role: PlatformRole | undefined, path: string): boolean {
  if (!role) return false;
  const needed = requiredPermissions(path);
  // Unregistered portal paths stay closed to non-admin roles.
  if (!needed) return role === "admin" || role === "super_admin";
  return needed.some((permission) => can(role, permission));
}

export function landingFor(role: PlatformRole | undefined): string {
  return role ? roleLanding[role] : "/login";
}
