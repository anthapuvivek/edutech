/**
 * Quiz domain types.
 *
 * These mirror the backend DTOs in `dto/quiz`. The split between teacher-facing and
 * student-facing shapes is deliberate and load-bearing: `StudentQuizOption` has no
 * `correct` field because the backend never sends one before submission. Do not add it.
 */

export type QuizStatus = "DRAFT" | "PUBLISHED";

export interface Quiz {
  id: string;
  courseId: string;
  courseTitle: string;
  /** Null when the quiz is course-wide rather than pinned to one cohort. */
  batchId?: string | null | undefined;
  batchName?: string | null | undefined;
  questionCount?: number | undefined;
  teacherId?: string | null | undefined;
  teacherName?: string | undefined;
  title: string;
  description?: string | undefined;
  timeLimitMinutes?: number | undefined;
  passingScore?: number | undefined;
  status: QuizStatus | string;
  createdAt?: string | undefined;
  attempted: boolean;
  score?: number | null | undefined;
  attemptStatus?: string | undefined;
}

// ---------- teacher: authoring ----------

export interface TeacherQuizOption {
  id: string;
  optionText: string;
  correct: boolean;
  sequenceNumber?: number | undefined;
}

export interface TeacherQuizQuestion {
  id: string;
  questionText: string;
  questionType?: string | undefined;
  points?: number | undefined;
  sequenceNumber?: number | undefined;
  explanation?: string | undefined;
  options: TeacherQuizOption[];
}

export interface CreateQuizPayload {
  courseId: string;
  batchId?: string | undefined;
  title: string;
  description?: string | undefined;
  timeLimitMinutes?: number | undefined;
  passingScore?: number | undefined;
  status?: QuizStatus | undefined;
}

export interface QuizQuestionPayload {
  questionText: string;
  questionType?: string | undefined;
  points?: number | undefined;
  sequenceNumber?: number | undefined;
  explanation?: string | undefined;
  options: Array<{
    id?: string | undefined;
    optionText: string;
    correct: boolean;
    sequenceNumber?: number | undefined;
  }>;
}

// ---------- student: attempting ----------

/** No `correct` field by design — the answer key stays on the server. */
export interface StudentQuizOption {
  id: string;
  optionText: string;
  sequenceNumber?: number | undefined;
}

export interface StudentQuizQuestion {
  id: string;
  questionText: string;
  questionType?: string | undefined;
  points?: number | undefined;
  sequenceNumber?: number | undefined;
  options: StudentQuizOption[];
}

export interface StudentQuizDetail {
  quiz: Quiz;
  questions: StudentQuizQuestion[];
}

export interface SubmitQuizPayload {
  answers: Array<{ questionId: string; selectedOptionId?: string | undefined }>;
}

/** Returned only after submission; this is where the correct option is finally disclosed. */
export interface QuizAnswerResult {
  questionId: string;
  questionText: string;
  selectedOptionId?: string | null | undefined;
  correctOptionId?: string | null | undefined;
  correct: boolean;
  pointsAwarded?: number | undefined;
  explanation?: string | null | undefined;
}

export interface QuizAttemptResult {
  attemptId: string;
  quizId: string;
  quizTitle: string;
  score?: number | undefined;
  passingScore?: number | undefined;
  passed: boolean;
  totalQuestions?: number | undefined;
  correctAnswers?: number | undefined;
  status?: string | undefined;
  startedAt?: string | undefined;
  submittedAt?: string | null | undefined;
  answers: QuizAnswerResult[];
}

// ---------- teacher: performance ----------

export interface TeacherQuizAttempt {
  attemptId: string;
  quizId: string;
  quizTitle: string;
  studentId: string;
  studentName: string;
  studentEmail?: string | undefined;
  score?: number | undefined;
  passed: boolean;
  correctAnswers?: number | undefined;
  totalQuestions?: number | undefined;
  status?: string | undefined;
  submittedAt?: string | null | undefined;
}
