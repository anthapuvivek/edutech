/**
 * Progress and analytics types.
 *
 * These mirror `dto/progress`. Two conventions matter:
 *
 *  - `applicable: false` means the course has none of that activity. Render "—", never 0%,
 *    or a course with no quizzes will look like a student who failed them.
 *  - `unavailableMetrics` names things the platform genuinely cannot measure (attendance
 *    has no table). Show the reason rather than a placeholder number.
 */

export interface ActivityBreakdown {
  area: string;
  applicable: boolean;
  completed?: number | null | undefined;
  total?: number | null | undefined;
  percent?: number | null | undefined;
  averageScore?: number | null | undefined;
}

export interface StudentProgress {
  studentId: string;
  studentName?: string | undefined;
  studentEmail?: string | undefined;
  /** Mean of the applicable areas only. */
  overallProgress: number;
  areas: ActivityBreakdown[];
  quizzesAttempted?: number | undefined;
  quizzesTotal?: number | undefined;
  averageQuizScore?: number | null | undefined;
  assignmentsSubmitted?: number | undefined;
  assignmentsTotal?: number | undefined;
  averageAssignmentScore?: number | null | undefined;
  codingSolved?: number | undefined;
  codingTotal?: number | undefined;
  recordingsWatched?: number | undefined;
  recordingsTotal?: number | undefined;
  lastActivityAt?: string | null | undefined;
  unavailableMetrics?: string[] | undefined;
}

export interface BatchProgress {
  batchId: string;
  batchName?: string | undefined;
  courseId?: string | null | undefined;
  courseTitle?: string | null | undefined;
  studentCount: number;
  averageProgress?: number | null | undefined;
  averageQuizScore?: number | null | undefined;
  averageAssignmentScore?: number | null | undefined;
  students: StudentProgress[];
  unavailableMetrics?: string[] | undefined;
}

/** Flagged by stated thresholds only — no model, no prediction. */
export interface AtRiskStudent {
  studentId: string;
  studentName?: string | undefined;
  overallProgress?: number | null | undefined;
  averageQuizScore?: number | null | undefined;
  missedAssignments?: number | null | undefined;
  /** One entry per triggered rule, shown verbatim so a teacher can disagree with it. */
  reasons: string[];
}

export interface Analytics {
  scope: "STUDENT" | "TEACHER" | string;
  subjectId?: string | undefined;
  subjectName?: string | undefined;
  studentCount?: number | null | undefined;
  averageProgress?: number | null | undefined;
  averageQuizScore?: number | null | undefined;
  averageAssignmentScore?: number | null | undefined;
  quizzesAttempted?: number | null | undefined;
  assignmentsSubmitted?: number | null | undefined;
  codingSolved?: number | null | undefined;
  recordingsWatched?: number | null | undefined;
  assignmentSubmissionRate?: number | null | undefined;
  quizParticipationRate?: number | null | undefined;
  performanceDistribution?: Record<string, number> | null | undefined;
  atRiskStudents?: AtRiskStudent[] | null | undefined;
  unavailableMetrics?: string[] | undefined;
}

/** Renders a percentage, or an em dash when the metric does not apply. */
export function pct(value: number | null | undefined): string {
  return value == null ? "—" : `${value}%`;
}
