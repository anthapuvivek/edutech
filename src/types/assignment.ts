/**
 * Assignment domain types.
 *
 * These mirror the backend DTOs in `dto/assignment`. Two things are load-bearing:
 *
 *  - `submissionStatus` is derived server-side from the due date and the grade. Never
 *    compute lateness or "graded" in the browser — the due date is the server's call.
 *  - The teacher counts are counts of DISTINCT students, not submissions. A student who
 *    resubmits must not read as two people having handed work in.
 */

export type AssignmentStatus = "DRAFT" | "PUBLISHED" | "CLOSED";

/** Server-derived. "Late" comes from comparing submittedAt to dueDate. */
export type SubmissionStatus = "Not Submitted" | "Submitted" | "Late" | "Graded";

export interface Assignment {
  id: string;
  courseId: string;
  courseTitle?: string | undefined;
  /** Null when course-wide rather than pinned to one cohort. */
  batchId?: string | null | undefined;
  batchName?: string | null | undefined;
  teacherId?: string | null | undefined;
  teacherName?: string | undefined;

  title: string;
  description?: string | undefined;
  dueDate?: string | undefined;
  points?: number | undefined;
  status: AssignmentStatus | string;
  createdAt?: string | undefined;

  /** Student view: their own submission state. */
  submitted?: boolean | undefined;
  submissionStatus?: SubmissionStatus | string | undefined;
  grade?: number | null | undefined;
  feedback?: string | null | undefined;

  /** Teacher listings only — distinct-student counts computed by the backend. */
  totalStudents?: number | null | undefined;
  submittedCount?: number | null | undefined;
  pendingCount?: number | null | undefined;
  lateCount?: number | null | undefined;
  gradedCount?: number | null | undefined;
}

export interface CreateAssignmentPayload {
  courseId: string;
  batchId?: string | undefined;
  title: string;
  description?: string | undefined;
  dueDate?: string | undefined;
  points?: number | undefined;
  /** Defaults to DRAFT server-side so nothing goes live by accident. */
  status?: AssignmentStatus | undefined;
}

export interface AssignmentSubmission {
  id: string;
  assignmentId: string;
  assignmentTitle?: string | undefined;
  studentId: string;
  studentName?: string | undefined;
  studentEmail?: string | undefined;
  submissionUrl?: string | undefined;
  submittedAt?: string | undefined;
  grade?: number | null | undefined;
  feedback?: string | null | undefined;
  status?: SubmissionStatus | string | undefined;
}

export interface SubmitAssignmentPayload {
  submissionUrl: string;
  notes?: string | undefined;
}

export interface GradeSubmissionPayload {
  /** Bounded by the assignment's own points value, enforced server-side. */
  grade: number;
  feedback?: string | undefined;
}

/** Badge tone for a submission state, shared by both portals. */
export function submissionTone(
  status: string | null | undefined,
): "default" | "secondary" | "destructive" | "outline" {
  switch (status) {
    case "Graded":
      return "default";
    case "Submitted":
      return "secondary";
    case "Late":
      return "destructive";
    default:
      return "outline";
  }
}
