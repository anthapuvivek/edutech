/**
 * AI Question Assistant types.
 *
 * These mirror `dto/ai` on the backend. Everything here is a *draft*: nothing has been
 * written to the database, and nothing can be until the teacher explicitly adds it through
 * the existing quiz or coding APIs — which validate again on the way in.
 *
 * Note there is no teacherId anywhere. Identity comes from the JWT; a field for it here
 * would be an impersonation vector.
 */

export type AiContentType = "QUIZ" | "CODING";

export type AiDifficulty = "EASY" | "MEDIUM" | "HARD";

export interface AiQuizQuestionDraft {
  question: string;
  options: string[];
  /** Zero-based index into `options`. */
  correctOption: number;
  marks: number;
  difficulty: AiDifficulty | string;
  explanation: string;
}

export interface AiCodingTestCaseDraft {
  input?: string | null | undefined;
  expectedOutput: string;
  /** Sample cases are shown to students and drive Run Code; hidden ones never are. */
  sample: boolean;
}

export interface AiCodingProblemDraft {
  title: string;
  description: string;
  language: string;
  difficulty: AiDifficulty | string;
  constraints?: string[] | null | undefined;
  sampleInput?: string | null | undefined;
  sampleOutput?: string | null | undefined;
  testCases: AiCodingTestCaseDraft[];
}

export interface AiGenerationPayload {
  courseId: string;
  batchId?: string | undefined;
  type: AiContentType;
  difficulty?: AiDifficulty | undefined;
  topic: string;
  count?: number | undefined;
  instructions?: string | undefined;
  language?: string | undefined;
  /** Regeneration context — sent from the browser rather than stored server-side. */
  existingQuestions?: AiQuizQuestionDraft[] | undefined;
  existingProblems?: AiCodingProblemDraft[] | undefined;
  regenerateInstruction?: string | undefined;
}

export interface AiGenerationResult {
  type: AiContentType | string;
  courseId: string;
  courseTitle?: string | undefined;
  batchId?: string | null | undefined;
  batchName?: string | null | undefined;
  questions?: AiQuizQuestionDraft[] | null | undefined;
  problems?: AiCodingProblemDraft[] | null | undefined;
  /** Non-fatal notes, e.g. a question the backend discarded as invalid. */
  warnings?: string[] | null | undefined;
}
