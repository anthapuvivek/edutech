import { env } from "@/lib/env";
import { mockTeacherStats, mockTeacherStudents } from "@/mock/lms";
import { apiRequest, mockDelay } from "@/services/api-client";
import type { TeacherStats, TeacherStudentRow } from "@/types/lms";

export const teacherService = {
  async stats(): Promise<TeacherStats> {
    if (!env.useMocks) return apiRequest<TeacherStats>("/teacher/stats");
    return mockDelay(mockTeacherStats);
  },
  async students(courseTitle?: string): Promise<TeacherStudentRow[]> {
    if (!env.useMocks)
      return apiRequest<TeacherStudentRow[]>("/teacher/students", {
        query: { course: courseTitle },
      });
    const rows =
      courseTitle && courseTitle !== "All"
        ? mockTeacherStudents.filter((s) => s.courseTitle === courseTitle)
        : mockTeacherStudents;
    return mockDelay(rows);
  },
  async student(id: string): Promise<TeacherStudentRow | null> {
    if (!env.useMocks) return apiRequest<TeacherStudentRow | null>(`/teacher/students/${id}`);
    return mockDelay(mockTeacherStudents.find((s) => s.id === id) ?? null);
  },
};
