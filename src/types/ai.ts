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

export type AiContentType = "QUIZ" | "CODING" | "ASSIGNMENT";

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

/** One generated assignment brief, before any teacher has approved it. */
export interface AiAssignmentDraft {
  title: string;
  description: string;
  points: number;
  difficulty: AiDifficulty | string;
  /** Suggested days from today; the teacher still picks the real date. */
  suggestedDueInDays?: number | null | undefined;
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
  existingAssignments?: AiAssignmentDraft[] | undefined;
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
  assignments?: AiAssignmentDraft[] | null | undefined;
  /** Non-fatal notes, e.g. a question the backend discarded as invalid. */
  warnings?: string[] | null | undefined;
}
