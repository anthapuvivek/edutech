export type RecordingStatus =
  | "DRAFT"
  | "UPLOADING"
  | "PROCESSING"
  | "READY"
  | "PUBLISHED"
  | "UNPUBLISHED"
  | "FAILED";

export interface Recording {
  id: string;
  courseId: string;
  courseTitle: string;
  moduleId: string;
  moduleTitle: string;
  lessonId: string;
  lessonTitle: string;
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
  moduleId: string;
  lessonId: string;
  title: string;
  description?: string;
  classDate: string;
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
