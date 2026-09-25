import { env } from "@/lib/env";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  CodingPerformance,
  CodingProblem,
  CodingProblemPerformance,
  CodingSubmission,
  CodingTestCasePayload,
  CreateCodingProblemPayload,
  ExternalCodingProblem,
  ExternalCodingProblemPayload,
  CodingProblemProgress,
  RunCodePayload,
  RunCodeResult,
  TeacherCodingTestCase,
} from "@/types/coding";

/**
 * Coding practice service.
 *
 * Every call goes to the LearntriX backend. React never talks to the execution service
 * directly and never sees its API key — Run and Submit are backend endpoints that forward
 * to the sandbox server-side.
 *
 * Authorization is entirely the server's: course access, batch membership, publication
 * state and the sample/hidden test-case boundary. Nothing here filters for security.
 */
export const codingService = {
  // ---------------- teacher: authoring ----------------

  async teacherProblems(): Promise<CodingProblem[]> {
    if (!env.useMocks) return apiRequest<CodingProblem[]>("/teacher/coding-problems");
    return mockDelay([]);
  },

  async createProblem(payload: CreateCodingProblemPayload): Promise<CodingProblem> {
    if (!env.useMocks)
      return apiRequest<CodingProblem>("/teacher/coding-problems", {
        method: "POST",
        body: payload,
      });
    return mockDelay({
      ...payload,
      id: `p-${Date.now()}`,
      status: "DRAFT",
    } as unknown as CodingProblem);
  },

  /** Full case list including hidden ones. Teacher-only endpoint. */
  async teacherTestCases(problemId: string): Promise<TeacherCodingTestCase[]> {
    if (!env.useMocks)
      return apiRequest<TeacherCodingTestCase[]>(
        `/teacher/coding-problems/${problemId}/test-cases`,
      );
    return mockDelay([]);
  },

  async addTestCase(
    problemId: string,
    payload: CodingTestCasePayload,
  ): Promise<TeacherCodingTestCase> {
    if (!env.useMocks)
      return apiRequest<TeacherCodingTestCase>(`/teacher/coding-problems/${problemId}/test-cases`, {
        method: "POST",
        body: payload,
      });
    return mockDelay({ ...payload, id: `tc-${Date.now()}` } as unknown as TeacherCodingTestCase);
  },

  async deleteTestCase(problemId: string, testCaseId: string): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/teacher/coding-problems/${problemId}/test-cases/${testCaseId}`, {
        method: "DELETE",
      });
    return mockDelay(undefined as void);
  },

  /** The backend refuses to publish a problem that has no test cases. */
  async publishProblem(problemId: string): Promise<CodingProblem> {
    if (!env.useMocks)
      return apiRequest<CodingProblem>(`/teacher/coding-problems/${problemId}/publish`, {
        method: "PUT",
      });
    return mockDelay({ id: problemId, status: "PUBLISHED" } as unknown as CodingProblem);
  },

  async unpublishProblem(problemId: string): Promise<CodingProblem> {
    if (!env.useMocks)
      return apiRequest<CodingProblem>(`/teacher/coding-problems/${problemId}/unpublish`, {
        method: "PUT",
      });
    return mockDelay({ id: problemId, status: "DRAFT" } as unknown as CodingProblem);
  },

  /** Cohort standing on one problem. Counts are of students, not submissions. */
  async problemPerformance(problemId: string): Promise<CodingProblemPerformance> {
    if (!env.useMocks)
      return apiRequest<CodingProblemPerformance>(
        `/teacher/coding-problems/${problemId}/performance`,
      );
    return mockDelay({} as CodingProblemPerformance);
  },

  /** Every submission on one problem. Refused unless the caller owns the problem. */
  async problemSubmissions(problemId: string): Promise<CodingSubmission[]> {
    if (!env.useMocks)
      return apiRequest<CodingSubmission[]>(`/teacher/coding-problems/${problemId}/submissions`);
    return mockDelay([]);
  },

  async updateTestCase(
    problemId: string,
    testCaseId: string,
    payload: CodingTestCasePayload,
  ): Promise<TeacherCodingTestCase> {
    if (!env.useMocks)
      return apiRequest<TeacherCodingTestCase>(
        `/teacher/coding-problems/${problemId}/test-cases/${testCaseId}`,
        { method: "PUT", body: payload },
      );
    return mockDelay({ ...payload, id: testCaseId } as unknown as TeacherCodingTestCase);
  },
  /** Refused by the backend unless the student is actually assigned to the caller. */
  async studentPerformance(studentId: string): Promise<CodingPerformance> {
    if (!env.useMocks)
      return apiRequest<CodingPerformance>(`/teacher/students/${studentId}/coding-performance`);
    return mockDelay({} as CodingPerformance);
  },

  // ---------------- student: solving ----------------

  /** Only published problems the student is authorised for; batch rules applied server-side. */
  async studentProblems(): Promise<CodingProblem[]> {
    if (!env.useMocks) return apiRequest<CodingProblem[]>("/student/coding-problems");
    return mockDelay([]);
  },

  /** Detail carrying SAMPLE test cases only. */
  async studentProblem(problemId: string): Promise<CodingProblem> {
    if (!env.useMocks) return apiRequest<CodingProblem>(`/student/coding-problems/${problemId}`);
    return mockDelay({} as CodingProblem);
  },

  /** Runs against sample cases only. Nothing is stored. */
  async runCode(problemId: string, payload: RunCodePayload): Promise<RunCodeResult> {
    if (!env.useMocks)
      return apiRequest<RunCodeResult>(`/student/coding-problems/${problemId}/run`, {
        method: "POST",
        body: payload,
      });
    return mockDelay({ status: "ERROR", passedCount: 0, totalCount: 0, results: [] });
  },

  /** Judges against every case, hidden included, and records the verdict. */
  async submitCode(problemId: string, payload: RunCodePayload): Promise<CodingSubmission> {
    if (!env.useMocks)
      return apiRequest<CodingSubmission>(`/student/coding-problems/${problemId}/submit`, {
        method: "POST",
        body: payload,
      });
    return mockDelay({} as CodingSubmission);
  },

  async mySubmissions(): Promise<CodingSubmission[]> {
    if (!env.useMocks) return apiRequest<CodingSubmission[]>("/student/coding-submissions");
    return mockDelay([]);
  },

  async myPerformance(): Promise<CodingPerformance> {
    if (!env.useMocks) return apiRequest<CodingPerformance>("/student/coding-performance");
    return mockDelay({} as CodingPerformance);
  },
};

/**
 * Coding assignments — externally hosted problems.
 *
 * LearnTriX stores a link, not a problem. Nothing here executes code, so this path costs
 * nothing to run. Authorization stays entirely server-side: course access, batch
 * membership and published state are all decided by the backend.
 */
export const codingAssignmentService = {
  // ---------------- teacher ----------------

  async teacherList(): Promise<ExternalCodingProblem[]> {
    if (!env.useMocks) return apiRequest<ExternalCodingProblem[]>("/teacher/coding-assignments");
    return mockDelay([]);
  },

  async create(payload: ExternalCodingProblemPayload): Promise<ExternalCodingProblem> {
    if (!env.useMocks)
      return apiRequest<ExternalCodingProblem>("/teacher/coding-assignments", {
        method: "POST",
        body: payload,
      });
    return mockDelay({ ...payload, id: `x-${Date.now()}` } as unknown as ExternalCodingProblem);
  },

  async update(id: string, payload: ExternalCodingProblemPayload): Promise<ExternalCodingProblem> {
    if (!env.useMocks)
      return apiRequest<ExternalCodingProblem>(`/teacher/coding-assignments/${id}`, {
        method: "PUT",
        body: payload,
      });
    return mockDelay({ ...payload, id } as unknown as ExternalCodingProblem);
  },

  /** Active maps onto PUBLISHED; inactive hides it from students. */
  async setActive(id: string, active: boolean): Promise<ExternalCodingProblem> {
    if (!env.useMocks)
      return apiRequest<ExternalCodingProblem>(`/teacher/coding-assignments/${id}/active`, {
        method: "PUT",
        body: { active },
      });
    return mockDelay({ id, active } as unknown as ExternalCodingProblem);
  },

  async remove(id: string): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/teacher/coding-assignments/${id}`, { method: "DELETE" });
    return mockDelay(undefined as void);
  },

  async progress(id: string): Promise<CodingProblemProgress> {
    if (!env.useMocks)
      return apiRequest<CodingProblemProgress>(`/teacher/coding-assignments/${id}/progress`);
    return mockDelay({
      problemId: id,
      problemTitle: "",
      totalStudents: 0,
      completed: 0,
      inProgress: 0,
      notStarted: 0,
      students: [],
    });
  },

  // ---------------- student ----------------

  /** Only active problems for courses/batches the caller is actually enrolled in. */
  async studentList(): Promise<ExternalCodingProblem[]> {
    if (!env.useMocks) return apiRequest<ExternalCodingProblem[]>("/student/coding-assignments");
    return mockDelay([]);
  },

  /**
   * Self-reported progress. LearnTriX cannot verify a solve on LeetCode, and the backend
   * keys the record on the authenticated student — no id is sent from here.
   */
  async setCompleted(id: string, completed: boolean): Promise<ExternalCodingProblem> {
    if (!env.useMocks)
      return apiRequest<ExternalCodingProblem>(`/student/coding-assignments/${id}/completion`, {
        method: "PUT",
        body: { completed },
      });
    return mockDelay({ id, completed } as unknown as ExternalCodingProblem);
  },

  async markOpened(id: string): Promise<ExternalCodingProblem> {
    if (!env.useMocks)
      return apiRequest<ExternalCodingProblem>(`/student/coding-assignments/${id}/open`, {
        method: "POST",
      });
    return mockDelay({ id, progressStatus: "OPENED" } as unknown as ExternalCodingProblem);
  },

  async complete(id: string): Promise<ExternalCodingProblem> {
    if (!env.useMocks)
      return apiRequest<ExternalCodingProblem>(`/student/coding-assignments/${id}/complete`, {
        method: "POST",
      });
    return mockDelay({
      id,
      progressStatus: "COMPLETED",
      completed: true,
    } as unknown as ExternalCodingProblem);
  },
};
