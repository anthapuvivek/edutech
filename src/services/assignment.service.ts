import { env } from "@/lib/env";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  Assignment,
  AssignmentSubmission,
  CreateAssignmentPayload,
  GradeSubmissionPayload,
  SubmitAssignmentPayload,
} from "@/types/assignment";

/**
 * Assignment service.
 *
 * Every call goes to the LearntriX backend, which owns all authorization: course access,
 * batch membership, publication state, grade bounds and lateness. Nothing here filters for
 * security — the server already returns only what the caller may see.
 */
export const assignmentService = {
  // ---------------- teacher: authoring ----------------

  /** Includes drafts and the distinct-student submission counts. */
  async teacherAssignments(): Promise<Assignment[]> {
    if (!env.useMocks) return apiRequest<Assignment[]>("/teacher/assignments");
    return mockDelay([]);
  },

  async teacherAssignment(id: string): Promise<Assignment> {
    if (!env.useMocks) return apiRequest<Assignment>(`/teacher/assignments/${id}`);
    return mockDelay({} as Assignment);
  },

  async createAssignment(payload: CreateAssignmentPayload): Promise<Assignment> {
    if (!env.useMocks)
      return apiRequest<Assignment>("/teacher/assignments", { method: "POST", body: payload });
    return mockDelay({ ...payload, id: `a-${Date.now()}`, status: "DRAFT" } as unknown as Assignment);
  },

  async updateAssignment(id: string, payload: CreateAssignmentPayload): Promise<Assignment> {
    if (!env.useMocks)
      return apiRequest<Assignment>(`/teacher/assignments/${id}`, { method: "PUT", body: payload });
    return mockDelay({ ...payload, id } as unknown as Assignment);
  },

  async publishAssignment(id: string): Promise<Assignment> {
    if (!env.useMocks)
      return apiRequest<Assignment>(`/teacher/assignments/${id}/publish`, { method: "PUT" });
    return mockDelay({ id, status: "PUBLISHED" } as unknown as Assignment);
  },

  async unpublishAssignment(id: string): Promise<Assignment> {
    if (!env.useMocks)
      return apiRequest<Assignment>(`/teacher/assignments/${id}/unpublish`, { method: "PUT" });
    return mockDelay({ id, status: "DRAFT" } as unknown as Assignment);
  },

  /** The backend refuses to delete an assignment that already has submissions. */
  async deleteAssignment(id: string): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/teacher/assignments/${id}`, { method: "DELETE" });
    return mockDelay(undefined as void);
  },

  // ---------------- teacher: grading ----------------

  async submissions(assignmentId: string): Promise<AssignmentSubmission[]> {
    if (!env.useMocks)
      return apiRequest<AssignmentSubmission[]>(`/teacher/assignments/${assignmentId}/submissions`);
    return mockDelay([]);
  },

  /** Grade is bounded by the assignment's points value, checked server-side. */
  async gradeSubmission(
    assignmentId: string,
    submissionId: string,
    payload: GradeSubmissionPayload,
  ): Promise<AssignmentSubmission> {
    if (!env.useMocks)
      return apiRequest<AssignmentSubmission>(
        `/teacher/assignments/${assignmentId}/submissions/${submissionId}/grade`,
        { method: "POST", body: payload },
      );
    return mockDelay({} as AssignmentSubmission);
  },

  // ---------------- student ----------------

  /** Only published assignments for the student's own course and batch. */
  async studentAssignments(): Promise<Assignment[]> {
    if (!env.useMocks) return apiRequest<Assignment[]>("/student/assignments");
    return mockDelay([]);
  },

  async studentAssignment(id: string): Promise<Assignment> {
    if (!env.useMocks) return apiRequest<Assignment>(`/student/assignments/${id}`);
    return mockDelay({} as Assignment);
  },

  /**
   * Resubmitting updates the existing row rather than adding one, and the server decides
   * whether it counts as Late — no timestamp is sent from here.
   */
  async submitAssignment(
    id: string,
    payload: SubmitAssignmentPayload,
  ): Promise<AssignmentSubmission> {
    if (!env.useMocks)
      return apiRequest<AssignmentSubmission>(`/student/assignments/${id}/submit`, {
        method: "POST",
        body: payload,
      });
    return mockDelay({} as AssignmentSubmission);
  },
};
