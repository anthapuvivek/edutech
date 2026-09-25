import { env } from "@/lib/env";
import { apiRequest, mockDelay } from "@/services/api-client";
import type { AiGenerationPayload, AiGenerationResult } from "@/types/ai";

/**
 * AI Question Assistant service.
 *
 * React never talks to Gemini and never sees its API key — the key lives only in the Spring
 * Boot backend. This is the same arrangement as Judge0: the browser asks LearntriX, and
 * LearntriX talks to the external service.
 *
 * The response is drafts only. Saving goes through `quizService` / `codingService`, so the
 * assistant cannot create or publish anything on its own.
 */
export const aiService = {
  /** Teacher-only. The backend derives the teacher from the token and checks batch ownership. */
  async generate(payload: AiGenerationPayload): Promise<AiGenerationResult> {
    if (!env.useMocks)
      return apiRequest<AiGenerationResult>("/teacher/ai/question-assistant", {
        method: "POST",
        body: payload,
      });
    return mockDelay({ type: payload.type, courseId: payload.courseId } as AiGenerationResult);
  },
};
