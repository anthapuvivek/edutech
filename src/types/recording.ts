export type RecordingStatus =
  "DRAFT" | "UPLOADING" | "PROCESSING" | "READY" | "PUBLISHED" | "UNPUBLISHED" | "FAILED";

export interface Recording {
  id: string;
  courseId: string;
  courseTitle: string;
  // Optional: a recording can be filed against the course without a curriculum mapping.
  // Spelled `| undefined` because exactOptionalPropertyTypes is on and the API really
  // does send these back absent.
  moduleId?: string | undefined;
  moduleTitle?: string | undefined;
  lessonId?: string | undefined;
  lessonTitle?: string | undefined;
  teacherId: string;
  teacherName: string;
  title: string;
  description?: string;
  classDate: string;
  videoStorageKey?: string;
  videoUrl?: string;
  hlsManifestUrl?: string;
  thumbnailUrl?: string;
  durationSeconds: number;
  fileSizeBytes: number;
  videoFormat?: string;
  status: RecordingStatus;
  published: boolean;
  createdAt: string;
  publishedAt?: string;
}

export interface CreateRecordingRequest {
  courseId: string;
  /** Optional. Send undefined when the course has no modules, or to skip the mapping. */
  moduleId?: string;
  lessonId?: string;
  title: string;
  description?: string;
  classDate?: string;
}

export interface UpdateRecordingRequest {
  title: string;
  description?: string;
  classDate?: string;
}

export interface UploadResponse {
  uploadUrl: string;
  storageKey: string;
  expiresAt: string;
}

export interface RecordingProgress {
  watchedSeconds: number;
  durationSeconds: number;
  completed: boolean;
  lastWatchedAt: string;
}

export interface RecordingStatistics {
  recordingId?: string;
  totalStudents: number;
  studentsStarted: number;
  studentsCompleted: number;
  averageWatchPercentage: number;
  averageWatchDuration: number;
  completionRate: number;
}
