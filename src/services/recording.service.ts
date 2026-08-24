import { env } from "@/lib/env";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  CreateRecordingRequest,
  Recording,
  RecordingProgress,
  RecordingStatistics,
  UpdateRecordingRequest,
  UploadResponse,
} from "@/types/recording";

// Local in-memory mock recordings storage
let mockRecordings: Recording[] = [
  {
    id: "rec-1",
    courseId: "1", // Maps to "Full Stack Engineering Program"
    courseTitle: "Full Stack Engineering Program",
    moduleId: "mod-1",
    moduleTitle: "Module 1 · React & TypeScript",
    lessonId: "les-1",
    lessonTitle: "Introduction to React",
    teacherId: "u-teacher",
    teacherName: "Durga Prasad",
    title: "Java OOP - Inheritance & Polymorphism",
    description: "Deep dive into object-oriented concepts like classes, inheritance, and runtime polymorphism in Java.",
    classDate: "2026-08-21T09:00:00.000Z",
    videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    hlsManifestUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    thumbnailUrl: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=800&q=70",
    durationSeconds: 5040, // 1h 24m
    fileSizeBytes: 412984500,
    videoFormat: "mp4",
    status: "PUBLISHED",
    published: true,
    createdAt: "2026-08-21T10:30:00.000Z",
    publishedAt: "2026-08-21T11:00:00.000Z",
  },
  {
    id: "rec-2",
    courseId: "1",
    courseTitle: "Full Stack Engineering Program",
    moduleId: "mod-1",
    moduleTitle: "Module 1 · React & TypeScript",
    lessonId: "les-2",
    lessonTitle: "State management in practice",
    teacherId: "u-teacher",
    teacherName: "Durga Prasad",
    title: "Spring Boot REST Controller Design",
    description: "Learn how to build production-ready REST controllers in Spring Boot with validation and global exception handling.",
    classDate: "2026-08-22T04:00:00.000Z",
    videoUrl: "",
    thumbnailUrl: "",
    durationSeconds: 0,
    fileSizeBytes: 0,
    status: "DRAFT",
    published: false,
    createdAt: "2026-08-22T06:00:00.000Z",
  }
];

let mockProgress: Record<string, RecordingProgress> = {
  "rec-1": {
    watchedSeconds: 3376, // 67%
    durationSeconds: 5040,
    completed: false,
    lastWatchedAt: "2026-08-22T10:00:00.000Z",
  }
};

export const recordingService = {
  async createRecording(req: CreateRecordingRequest): Promise<Recording> {
    if (!env.useMocks) {
      return apiRequest<Recording>("/recordings", { method: "POST", body: req });
    }

    const newRecording: Recording = {
      id: `rec-${Date.now()}`,
      courseId: req.courseId,
      courseTitle: req.courseId === "1" ? "Full Stack Engineering Program" : "Course " + req.courseId,
      moduleId: req.moduleId,
      moduleTitle: "Module " + req.moduleId,
      lessonId: req.lessonId,
      lessonTitle: "Lesson " + req.lessonId,
      teacherId: "u-teacher",
      teacherName: "Durga Prasad",
      title: req.title,
      description: req.description ?? "",
      classDate: req.classDate,
      durationSeconds: 0,
      fileSizeBytes: 0,
      status: "DRAFT",
      published: false,
      createdAt: new Date().toISOString(),
    };

    mockRecordings.unshift(newRecording);
    return mockDelay(newRecording);
  },

  async updateRecording(id: string, req: UpdateRecordingRequest): Promise<Recording> {
    if (!env.useMocks) {
      return apiRequest<Recording>(`/recordings/${id}`, { method: "PUT", body: req });
    }

    const index = mockRecordings.findIndex((r) => r.id === id);
    if (index === -1) throw new Error("Recording not found");
    const updated: Recording = {
      ...mockRecordings[index]!,
      title: req.title,
      description: req.description ?? "",
      classDate: req.classDate ?? mockRecordings[index]!.classDate,
    };
    mockRecordings[index] = updated;
    return mockDelay(updated);
  },

  async getRecording(id: string): Promise<Recording> {
    if (!env.useMocks) {
      return apiRequest<Recording>(`/recordings/${id}`);
    }
    const rec = mockRecordings.find((r) => r.id === id);
    if (!rec) throw new Error("Recording not found");
    return mockDelay(rec);
  },

  async getTeacherRecordings(): Promise<{ items: Recording[] }> {
    if (!env.useMocks) {
      const pageResponse = await apiRequest<{ content: Recording[] }>("/recordings/teacher");
      return { items: pageResponse.content };
    }
    return mockDelay({ items: mockRecordings });
  },

  async getAdminRecordings(): Promise<{ items: Recording[] }> {
    if (!env.useMocks) {
      const pageResponse = await apiRequest<{ content: Recording[] }>("/admin/recordings");
      return { items: pageResponse.content };
    }
    return mockDelay({ items: mockRecordings });
  },

  async adminPublishRecording(id: string): Promise<Recording> {
    if (!env.useMocks) {
      return apiRequest<Recording>(`/admin/recordings/${id}/publish`, { method: "POST" });
    }
    return this.publishRecording(id);
  },

  async adminUnpublishRecording(id: string): Promise<Recording> {
    if (!env.useMocks) {
      return apiRequest<Recording>(`/admin/recordings/${id}/unpublish`, { method: "POST" });
    }
    return this.unpublishRecording(id);
  },

  async adminDeleteRecording(id: string): Promise<void> {
    if (!env.useMocks) {
      return apiRequest<void>(`/admin/recordings/${id}`, { method: "DELETE" });
    }
    return this.deleteRecording(id);
  },

  async getStudentRecordings(): Promise<Recording[]> {
    if (!env.useMocks) {
      return apiRequest<Recording[]>("/student/recordings");
    }
    return mockDelay(mockRecordings.filter((r) => r.published));
  },

  async getPublishedRecordingsForCourse(courseId: string): Promise<Recording[]> {
    if (!env.useMocks) {
      return apiRequest<Recording[]>(`/student/recordings/course/${courseId}`);
    }
    // Return mock recordings for this course
    return mockDelay(mockRecordings.filter((r) => r.published && r.courseId === courseId));
  },

  async publishRecording(id: string): Promise<Recording> {
    if (!env.useMocks) {
      return apiRequest<Recording>(`/recordings/${id}/publish`, { method: "POST" });
    }

    const index = mockRecordings.findIndex((r) => r.id === id);
    if (index === -1) throw new Error("Recording not found");
    const updated: Recording = {
      ...mockRecordings[index]!,
      published: true,
      status: "PUBLISHED",
      publishedAt: new Date().toISOString(),
    };
    mockRecordings[index] = updated;
    return mockDelay(updated);
  },

  async unpublishRecording(id: string): Promise<Recording> {
    if (!env.useMocks) {
      return apiRequest<Recording>(`/recordings/${id}/unpublish`, { method: "POST" });
    }

    const index = mockRecordings.findIndex((r) => r.id === id);
    if (index === -1) throw new Error("Recording not found");
    const updated: Recording = {
      ...mockRecordings[index]!,
      published: false,
      status: "UNPUBLISHED",
    };
    mockRecordings[index] = updated;
    return mockDelay(updated);
  },

  async deleteRecording(id: string): Promise<void> {
    if (!env.useMocks) {
      return apiRequest<void>(`/recordings/${id}`, { method: "DELETE" });
    }

    mockRecordings = mockRecordings.filter((r) => r.id !== id);
    return mockDelay(undefined);
  },

  async createUploadUrl(id: string, fileName: string): Promise<UploadResponse> {
    if (!env.useMocks) {
      return apiRequest<UploadResponse>(`/recordings/${id}/upload`, {
        method: "POST",
        query: { fileName },
      });
    }

    const key = `courses/1/recordings/${id}/original/${fileName}`;
    return mockDelay({
      uploadUrl: `http://localhost:8081/api/recordings/upload-local?key=${key}`,
      storageKey: key,
      expiresAt: new Date(Date.now() + 3600 * 1000).toISOString(),
    });
  },

  async completeUpload(id: string): Promise<Recording> {
    if (!env.useMocks) {
      return apiRequest<Recording>(`/recordings/${id}/upload-complete`, { method: "POST" });
    }

    const index = mockRecordings.findIndex((r) => r.id === id);
    if (index === -1) throw new Error("Recording not found");

    // Simulate async processing
    const updated: Recording = {
      ...mockRecordings[index]!,
      status: "PROCESSING",
    };
    mockRecordings[index] = updated;

    // Simulate complete transcoding process after 5 seconds in mock mode
    setTimeout(() => {
      const idx = mockRecordings.findIndex((r) => r.id === id);
      if (idx !== -1) {
        mockRecordings[idx] = {
          ...mockRecordings[idx]!,
          status: "READY",
          videoUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
          hlsManifestUrl: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
          thumbnailUrl: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=800&q=70",
          durationSeconds: 3600,
          fileSizeBytes: 300485900,
          videoFormat: "mp4",
        };
      }
    }, 5000);

    return mockDelay(updated);
  },

  async getProgress(id: string): Promise<RecordingProgress> {
    if (!env.useMocks) {
      return apiRequest<RecordingProgress>(`/student/recordings/${id}/progress`);
    }
    const progress = mockProgress[id] ?? {
      watchedSeconds: 0,
      durationSeconds: 0,
      completed: false,
      lastWatchedAt: new Date().toISOString(),
    };
    return mockDelay(progress);
  },

  async updateProgress(
    id: string,
    watchedSeconds: number,
    durationSeconds: number
  ): Promise<RecordingProgress> {
    if (!env.useMocks) {
      return apiRequest<RecordingProgress>(`/student/recordings/${id}/progress`, {
        method: "POST",
        body: { watchedSeconds, durationSeconds },
      });
    }

    const percent = durationSeconds > 0 ? (watchedSeconds / durationSeconds) * 100 : 0;
    const completed = percent >= 90;

    const progress: RecordingProgress = {
      watchedSeconds,
      durationSeconds,
      completed,
      lastWatchedAt: new Date().toISOString(),
    };
    mockProgress[id] = progress;
    return mockDelay(progress);
  },

  async completeRecording(id: string): Promise<RecordingProgress> {
    if (!env.useMocks) {
      return apiRequest<RecordingProgress>(`/student/recordings/${id}/complete`, {
        method: "POST",
      });
    }

    const current = mockProgress[id] ?? { watchedSeconds: 3600, durationSeconds: 3600, completed: true, lastWatchedAt: "" };
    const progress: RecordingProgress = {
      ...current,
      watchedSeconds: current.durationSeconds > 0 ? current.durationSeconds : 3600,
      completed: true,
      lastWatchedAt: new Date().toISOString(),
    };
    mockProgress[id] = progress;
    return mockDelay(progress);
  },

  async getStatistics(id: string): Promise<RecordingStatistics> {
    if (!env.useMocks) {
      return apiRequest<RecordingStatistics>(`/recordings/${id}/statistics`);
    }

    return mockDelay({
      recordingId: id,
      totalStudents: 120,
      studentsStarted: 98,
      studentsCompleted: 71,
      averageWatchPercentage: 76.5,
      averageWatchDuration: 3840,
      completionRate: 59.2,
    });
  },

  /**
   * Performs the actual binary upload to local storage or S3 presigned URL
   */
  async uploadLocalFile(
    uploadUrl: string,
    file: File,
    onProgress?: (pct: number) => void
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      
      // Determine if it is a local upload or AWS S3 presigned URL
      const isLocal = uploadUrl.includes("upload-local");

      xhr.open(isLocal ? "POST" : "PUT", uploadUrl, true);

      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable && onProgress) {
          const pct = Math.round((e.loaded / e.total) * 100);
          onProgress(pct);
        }
      };

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve();
        } else {
          reject(new Error(`Upload failed with status ${xhr.status}`));
        }
      };

      xhr.onerror = () => reject(new Error("Network upload error"));

      if (isLocal) {
        // Send as FormData for the local Multipart handler
        const formData = new FormData();
        // Extract key query param
        const urlObj = new URL(uploadUrl);
        const key = urlObj.searchParams.get("key") || "";
        formData.append("key", key);
        formData.append("file", file);
        xhr.send(formData);
      } else {
        // S3 direct upload
        xhr.setRequestHeader("Content-Type", file.type);
        xhr.send(file);
      }
    });
  },
};
