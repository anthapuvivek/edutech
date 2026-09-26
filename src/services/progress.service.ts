import { env } from "@/lib/env";
import { apiRequest, mockDelay } from "@/services/api-client";
import type { Analytics, BatchProgress, StudentProgress } from "@/types/progress";

/**
 * Progress and analytics service.
 *
 * Every figure is computed by the backend from stored activity — quizzes, assignments,
 * coding completions and watched recordings. Nothing is estimated here, and the student
 * endpoints take no student id, so there is nothing a client could tamper with.
 */
export const progressService = {
  // ---------------- student: own data only ----------------

  async myProgress(): Promise<StudentProgress> {
    if (!env.useMocks) return apiRequest<StudentProgress>("/student/progress");
    return mockDelay({} as StudentProgress);
  },

  async myAnalytics(): Promise<Analytics> {
    if (!env.useMocks) return apiRequest<Analytics>("/student/analytics");
    return mockDelay({} as Analytics);
  },

  // ---------------- teacher: authorized batches only ----------------

  async batchProgress(): Promise<BatchProgress[]> {
    if (!env.useMocks) return apiRequest<BatchProgress[]>("/teacher/progress");
    return mockDelay([]);
  },

  /** Refused unless the student is on a batch this teacher runs. */
  async studentProgress(studentId: string): Promise<StudentProgress> {
    if (!env.useMocks) return apiRequest<StudentProgress>(`/teacher/progress/${studentId}`);
    return mockDelay({} as StudentProgress);
  },

  async teacherAnalytics(): Promise<Analytics> {
    if (!env.useMocks) return apiRequest<Analytics>("/teacher/analytics");
    return mockDelay({} as Analytics);
  },
};
