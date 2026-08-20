import { env } from "@/lib/env";
import {
  mockActivity,
  mockEnrollments,
  mockEvents,
  mockLeaderboard,
  mockStudentProfile,
  mockStudentStats,
} from "@/mock/lms";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  ActivityItem,
  Enrollment,
  LeaderboardEntry,
  PlatformEvent,
  StudentProfile,
  StudentStats,
} from "@/types/lms";

export const studentService = {
  async profile(): Promise<StudentProfile> {
    if (!env.useMocks) return apiRequest<StudentProfile>("/student/profile");
    return mockDelay(mockStudentProfile);
  },
  async stats(): Promise<StudentStats> {
    if (!env.useMocks) return apiRequest<StudentStats>("/student/stats");
    return mockDelay(mockStudentStats);
  },
  async enrollments(): Promise<Enrollment[]> {
    if (!env.useMocks) return apiRequest<Enrollment[]>("/student/enrollments");
    return mockDelay(mockEnrollments);
  },
  async activity(): Promise<ActivityItem[]> {
    if (!env.useMocks) return apiRequest<ActivityItem[]>("/student/activity");
    return mockDelay(mockActivity);
  },
  async liveClasses(): Promise<PlatformEvent[]> {
    if (!env.useMocks) return apiRequest<PlatformEvent[]>("/student/live-classes");
    return mockDelay(mockEvents);
  },
  async leaderboard(scope = "global"): Promise<LeaderboardEntry[]> {
    if (!env.useMocks)
      return apiRequest<LeaderboardEntry[]>("/student/leaderboard", { query: { scope } });
    return mockDelay(mockLeaderboard);
  },
};
