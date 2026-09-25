/**
 * Coding practice domain types.
 *
 * These mirror the backend DTOs in `dto/coding`. The split between teacher-facing and
 * student-facing test cases is deliberate and load-bearing:
 *
 *  - `TeacherCodingTestCase` carries `sample` plus the expected output of EVERY case.
 *  - `StudentCodingTestCase` is only ever populated from SAMPLE cases. The backend filters
 *    to samples before mapping, so a hidden case never reaches this shape.
 *
 * Do not add hidden-case fields to the student types, and do not reuse the teacher type in
 * a student page — that is the whole disclosure boundary.
 */

export type CodingStatus = "DRAFT" | "PUBLISHED";

export type CodingDifficulty = "EASY" | "MEDIUM" | "HARD";

/**
 * Verdicts the judge can return. `ERROR` covers an unreachable or unconfigured execution
 * service, which is a normal state rather than a bug.
 */
export type SubmissionStatus =
  | "ACCEPTED"
  | "WRONG_ANSWER"
  | "COMPILE_ERROR"
  | "RUNTIME_ERROR"
  | "TIME_LIMIT_EXCEEDED"
  | "MEMORY_LIMIT_EXCEEDED"
  | "ERROR"
  | "QUEUED";

/** Languages the backend maps to a Judge0 language id. */
export const CODING_LANGUAGES = [
  { value: "java", label: "Java" },
  { value: "python", label: "Python" },
  { value: "cpp", label: "C++" },
  { value: "c", label: "C" },
  { value: "javascript", label: "JavaScript" },
  { value: "typescript", label: "TypeScript" },
  { value: "csharp", label: "C#" },
  { value: "go", label: "Go" },
] as const;

/** Sample case only — never a hidden one. */
export interface StudentCodingTestCase {
  id: string;
  inputData?: string | null | undefined;
  expectedOutput: string;
  sequenceNumber?: number | undefined;
}

/** Teacher view: includes hidden cases. Never render this in a student page. */
export interface TeacherCodingTestCase {
  id: string;
  inputData?: string | null | undefined;
  expectedOutput: string;
  sample: boolean;
  sequenceNumber?: number | undefined;
  points?: number | undefined;
}

export interface CodingProblem {
  id: string;
  courseId: string;
  courseTitle?: string | undefined;
  /** Null when the problem is course-wide rather than pinned to one cohort. */
  batchId?: string | null | undefined;
  batchName?: string | null | undefined;
  teacherId?: string | null | undefined;
  teacherName?: string | null | undefined;
  title: string;
  slug?: string | undefined;
  description: string;
  difficulty: CodingDifficulty | string;
  constraintsText?: string | null | undefined;
  starterCode?: string | null | undefined;
  language: string;
  timeLimitMs?: number | undefined;
  memoryLimitMb?: number | undefined;
  status: CodingStatus | string;
  /** Total cases including hidden ones — a count is safe to show, the contents are not. */
  testCaseCount?: number | undefined;
  createdAt?: string | undefined;
  /** Present only on the student detail call. */
  sampleTestCases?: StudentCodingTestCase[] | null | undefined;
  /** The requesting student's best outcome so far, or null if never attempted. */
  myStatus?: SubmissionStatus | string | null | undefined;
}

export interface CreateCodingProblemPayload {
  courseId: string;
  batchId?: string | undefined;
  moduleId?: string | undefined;
  title: string;
  description: string;
  difficulty?: string | undefined;
  constraintsText?: string | undefined;
  starterCode?: string | undefined;
  language?: string | undefined;
  timeLimitMs?: number | undefined;
  memoryLimitMb?: number | undefined;
  status?: CodingStatus | undefined;
}

export interface CodingTestCasePayload {
  inputData?: string | undefined;
  expectedOutput: string;
  sample: boolean;
  sequenceNumber?: number | undefined;
  points?: number | undefined;
}

export interface RunCodePayload {
  sourceCode: string;
  language?: string | undefined;
}

/**
 * One case outcome from Run Code.
 *
 * `input` and `expectedOutput` are populated only because Run executes sample cases, which
 * the student can already read on the problem page. Submit leaves them null.
 */
export interface TestCaseResult {
  sequenceNumber?: number | undefined;
  input?: string | null | undefined;
  expectedOutput?: string | null | undefined;
  actualOutput?: string | null | undefined;
  passed: boolean;
  status: string;
  message?: string | null | undefined;
  runtimeMs?: number | null | undefined;
}

export interface RunCodeResult {
  status: SubmissionStatus | string;
  message?: string | null | undefined;
  passedCount: number;
  totalCount: number;
  results: TestCaseResult[];
}

/** Aggregate counts only — a submission never discloses which hidden case failed. */
export interface CodingSubmission {
  id: string;
  problemId: string;
  problemTitle?: string | undefined;
  studentId: string;
  studentName?: string | undefined;
  language: string;
  status: SubmissionStatus | string;
  passedCount: number;
  totalCount: number;
  runtimeMs?: number | null | undefined;
  message?: string | null | undefined;
  submittedAt?: string | undefined;
  /** Returned only to the owner of the submission. */
  sourceCode?: string | null | undefined;
}

export interface CodingPerformance {
  studentId: string;
  studentName?: string | undefined;
  problemsAvailable: number;
  problemsAttempted: number;
  problemsSolved: number;
  totalSubmissions: number;
  acceptedSubmissions: number;
  wrongAnswers: number;
  compileErrors: number;
  runtimeErrors: number;
  timeLimitExceeded: number;
  acceptanceRate: number;
  lastSubmissionAt?: string | null | undefined;
}

/** Human label for a verdict, used by both the student and teacher pages. */
export function verdictLabel(status: string | null | undefined): string {
  switch (status) {
    case "ACCEPTED":
      return "Accepted";
    case "WRONG_ANSWER":
      return "Wrong answer";
    case "COMPILE_ERROR":
      return "Compile error";
    case "RUNTIME_ERROR":
      return "Runtime error";
    case "TIME_LIMIT_EXCEEDED":
      return "Time limit exceeded";
    case "MEMORY_LIMIT_EXCEEDED":
      return "Memory limit exceeded";
    case "ERROR":
      return "Execution unavailable";
    case "QUEUED":
      return "Queued";
    case "PASSED":
      return "Passed";
    default:
      return status ?? "Not attempted";
  }
}

/** One student's standing on one problem, collapsed from all their submissions. */
export interface CodingStudentStatus {
  studentId: string;
  studentName?: string | undefined;
  studentEmail?: string | undefined;
  /** SOLVED | ATTEMPTED | NOT_ATTEMPTED — derived server-side, never stored. */
  status: "SOLVED" | "ATTEMPTED" | "NOT_ATTEMPTED" | string;
  bestScore?: number | null | undefined;
  bestPassedCount?: number | null | undefined;
  totalCount?: number | null | undefined;
  submissionCount?: number | undefined;
  lastSubmissionAt?: string | null | undefined;
}

/**
 * Cohort-wide standing on one problem.
 *
 * Every headline count is a count of DISTINCT students, not of submissions — a student who
 * submits four times and is accepted twice counts once as attempted and once as solved.
 */
export interface CodingProblemPerformance {
  problemId: string;
  problemTitle?: string | undefined;
  batchId?: string | null | undefined;
  batchName?: string | null | undefined;
  studentsInScope: number;
  studentsAttempted: number;
  studentsSolved: number;
  totalSubmissions: number;
  students: CodingStudentStatus[];
}

// ---------------------------------------------------------------------------
// External coding assignments
// ---------------------------------------------------------------------------
// Coding Practice now assigns problems hosted on LeetCode, HackerRank and friends.
// LearnTriX stores a link and the teacher's own notes — it never fetches, mirrors or
// scrapes the problem statement, and it runs no code.

export const CODING_PLATFORMS = [
  { value: "LEETCODE", label: "LeetCode" },
  { value: "HACKERRANK", label: "HackerRank" },
  { value: "GEEKSFORGEEKS", label: "GeeksforGeeks" },
  { value: "CODECHEF", label: "CodeChef" },
  { value: "OTHER", label: "Other" },
] as const;

export const CODING_DIFFICULTIES = ["EASY", "MEDIUM", "HARD"] as const;

export interface ExternalCodingProblem {
  id: string;
  courseId: string;
  courseTitle?: string | undefined;
  /** Null when course-wide rather than pinned to one cohort. */
  batchId?: string | null | undefined;
  batchName?: string | null | undefined;
  teacherId?: string | null | undefined;
  teacherName?: string | null | undefined;

  platform: string;
  problemNumber?: string | null | undefined;
  /** The link the student opens in a new tab. Always http/https — validated server-side. */
  problemUrl: string;
  title: string;
  difficulty?: string | undefined;
  topics?: string | null | undefined;
  /** Teacher-authored notes only, never copied from the external platform. */
  description?: string | null | undefined;

  status?: string | undefined;
  active: boolean;
  createdAt?: string | undefined;
  deadline?: string | null | undefined;

  /** The requesting student's own self-reported state. Absent in teacher listings. */
  completed?: boolean | null | undefined;
  completedAt?: string | null | undefined;
  openedAt?: string | null | undefined;
  progressStatus?: "NOT_STARTED" | "OPENED" | "IN_PROGRESS" | "COMPLETED" | undefined;
  /** How many students marked it done. Teacher listings only. */
  completedCount?: number | null | undefined;
}

export interface ExternalCodingProblemPayload {
  courseId: string;
  batchId: string;
  platform: string;
  problemNumber?: string | undefined;
  problemUrl: string;
  title: string;
  difficulty?: string | undefined;
  topics?: string | undefined;
  description?: string | undefined;
  active?: boolean | undefined;
  deadline?: string | undefined;
}

export interface CodingProblemProgress {
  problemId: string;
  problemTitle: string;
  totalStudents: number;
  completed: number;
  inProgress: number;
  notStarted: number;
  students: Array<{
    studentId: string;
    studentName: string;
    studentEmail?: string;
    status: "NOT_STARTED" | "OPENED" | "IN_PROGRESS" | "COMPLETED";
    openedAt?: string | null;
    completedAt?: string | null;
  }>;
}

/** Human label for a platform code. */
export function platformLabel(value: string | null | undefined): string {
  return CODING_PLATFORMS.find((p) => p.value === value)?.label ?? value ?? "Other";
}
