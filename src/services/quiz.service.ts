import { env } from "@/lib/env";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  CreateQuizPayload,
  Quiz,
  QuizAttemptResult,
  QuizQuestionPayload,
  StudentQuizDetail,
  SubmitQuizPayload,
  TeacherQuizAttempt,
  TeacherQuizQuestion,
} from "@/types/quiz";

/**
 * Quiz service.
 *
 * Every call goes to the LearntriX backend, which owns all authorization: course access,
 * batch membership, publication state and scoring. Nothing here filters for security —
 * the server already returns only what the caller may see.
 */
export const quizService = {
  // ---------------- teacher: authoring ----------------

  async teacherQuizzes(): Promise<Quiz[]> {
    if (!env.useMocks) return apiRequest<Quiz[]>("/teacher/quizzes");
    return mockDelay([]);
  },

  async createQuiz(payload: CreateQuizPayload): Promise<Quiz> {
    if (!env.useMocks)
      return apiRequest<Quiz>("/teacher/quizzes", { method: "POST", body: payload });
    return mockDelay({ ...payload, id: `quiz-${Date.now()}`, status: "DRAFT" } as unknown as Quiz);
  },

  async teacherQuestions(quizId: string): Promise<TeacherQuizQuestion[]> {
    if (!env.useMocks)
      return apiRequest<TeacherQuizQuestion[]>(`/teacher/quizzes/${quizId}/questions`);
    return mockDelay([]);
  },

  async addQuestion(quizId: string, payload: QuizQuestionPayload): Promise<TeacherQuizQuestion> {
    if (!env.useMocks)
      return apiRequest<TeacherQuizQuestion>(`/teacher/quizzes/${quizId}/questions`, {
        method: "POST",
        body: payload,
      });
    return mockDelay({ ...payload, id: `q-${Date.now()}` } as unknown as TeacherQuizQuestion);
  },

  async deleteQuestion(quizId: string, questionId: string): Promise<{ ok: boolean }> {
    if (!env.useMocks)
      return apiRequest<{ ok: boolean }>(`/teacher/quizzes/${quizId}/questions/${questionId}`, {
        method: "DELETE",
      });
    return mockDelay({ ok: true });
  },

  /** Publishing is refused by the backend unless the quiz has at least one question. */
  async publishQuiz(quizId: string): Promise<Quiz> {
    if (!env.useMocks)
      return apiRequest<Quiz>(`/teacher/quizzes/${quizId}/publish`, { method: "POST" });
    return mockDelay({ id: quizId, status: "PUBLISHED" } as unknown as Quiz);
  },

  async unpublishQuiz(quizId: string): Promise<Quiz> {
    if (!env.useMocks)
      return apiRequest<Quiz>(`/teacher/quizzes/${quizId}/unpublish`, { method: "POST" });
    return mockDelay({ id: quizId, status: "DRAFT" } as unknown as Quiz);
  },

  async quizPerformance(quizId: string): Promise<TeacherQuizAttempt[]> {
    if (!env.useMocks)
      return apiRequest<TeacherQuizAttempt[]>(`/teacher/quizzes/${quizId}/performance`);
    return mockDelay([]);
  },

  // ---------------- student: attempting ----------------

  /** Only published quizzes the student is authorised for, batch rules applied server-side. */
  async studentQuizzes(): Promise<Quiz[]> {
    if (!env.useMocks) return apiRequest<Quiz[]>("/student/quizzes");
    return mockDelay([]);
  },

  /** Questions and options with no answer key. */
  async studentQuizDetail(quizId: string): Promise<StudentQuizDetail> {
    if (!env.useMocks)
      return apiRequest<StudentQuizDetail>(`/student/quizzes/${quizId}/questions`);
    return mockDelay({ quiz: {}, questions: [] } as unknown as StudentQuizDetail);
  },

  async startAttempt(quizId: string): Promise<QuizAttemptResult> {
    if (!env.useMocks)
      return apiRequest<QuizAttemptResult>(`/student/quizzes/${quizId}/start`, { method: "POST" });
    return mockDelay({} as QuizAttemptResult);
  },

  /**
   * Sends only questionId/selectedOptionId pairs. The score comes back computed by the
   * backend from its own answer key; nothing sent from here can influence it.
   */
  async submitAttempt(quizId: string, payload: SubmitQuizPayload): Promise<QuizAttemptResult> {
    if (!env.useMocks)
      return apiRequest<QuizAttemptResult>(`/student/quizzes/${quizId}/submit`, {
        method: "POST",
        body: payload,
      });
    return mockDelay({} as QuizAttemptResult);
  },

  async myAttempts(): Promise<QuizAttemptResult[]> {
    if (!env.useMocks) return apiRequest<QuizAttemptResult[]>("/student/quizzes/attempts/me");
    return mockDelay([]);
  },
};
