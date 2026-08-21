import { guardService } from "@/lib/authz";
import { env } from "@/lib/env";
import {
  mockMentorActionItems,
  mockMentorAlerts,
  mockMentorAssignments,
  mockMentorNotes,
  mockMentorSessions,
  mockMentorStats,
  mockMentorStudents,
  mockMentors,
} from "@/mock/ops";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  Mentor,
  MentorActionItem,
  MentorAlert,
  MentorAssignment,
  MentorNote,
  MentorSession,
  MentorStats,
  MentorStudentRow,
  StudentMentorView,
} from "@/types/ops";

let assignments = [...mockMentorAssignments];
let sessions = [...mockMentorSessions];
let notes = [...mockMentorNotes];
let actionItems = [...mockMentorActionItems];

/**
 * Mentoring abstraction. Mentors are distinct from teachers: they guide
 * students on career, projects and personal progress. Assignment authority
 * lives with admins and is enforced by the backend.
 */
const mentorServiceRaw = {
  async mentors(): Promise<Mentor[]> {
    if (!env.useMocks) return apiRequest<Mentor[]>("/mentors");
    return mockDelay(mockMentors);
  },

  async stats(): Promise<MentorStats> {
    if (!env.useMocks) return apiRequest<MentorStats>("/mentor/stats");
    return mockDelay(mockMentorStats);
  },

  async students(): Promise<MentorStudentRow[]> {
    if (!env.useMocks) return apiRequest<MentorStudentRow[]>("/mentor/students");
    return mockDelay(mockMentorStudents);
  },

  async sessions(): Promise<MentorSession[]> {
    if (!env.useMocks) return apiRequest<MentorSession[]>("/mentor/sessions");
    return mockDelay(sessions);
  },

  async createSession(payload: Partial<MentorSession>): Promise<MentorSession> {
    if (!env.useMocks)
      return apiRequest<MentorSession>("/mentor/sessions", { method: "POST", body: payload });
    const session: MentorSession = {
      id: `ses-${Date.now()}`,
      title: payload.title ?? "Mentoring session",
      kind: payload.kind ?? "Mentoring",
      mentorId: payload.mentorId ?? "mnt-1",
      studentId: payload.studentId ?? "stu-1",
      studentName: payload.studentName ?? "Student",
      date: payload.date ?? new Date().toISOString().slice(0, 10),
      time: payload.time ?? "18:30",
      durationMinutes: payload.durationMinutes ?? 45,
      platform: payload.platform ?? "Google Meet",
      meetingUrl: payload.meetingUrl,
      agenda: payload.agenda ?? "",
      notes: payload.notes,
      status: "Scheduled",
    };
    sessions = [session, ...sessions];
    return mockDelay(session, 200);
  },

  async updateSession(id: string, patch: Partial<MentorSession>): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/mentor/sessions/${id}`, { method: "PATCH", body: patch });
    sessions = sessions.map((s) => (s.id === id ? { ...s, ...patch } : s));
    return mockDelay(undefined, 150);
  },

  async notes(): Promise<MentorNote[]> {
    if (!env.useMocks) return apiRequest<MentorNote[]>("/mentor/notes");
    return mockDelay(notes);
  },

  async addNote(payload: Partial<MentorNote>): Promise<void> {
    if (!env.useMocks) return apiRequest<void>("/mentor/notes", { method: "POST", body: payload });
    notes = [
      {
        id: `mn-${Date.now()}`,
        mentorId: "mnt-1",
        studentId: payload.studentId ?? "stu-1",
        studentName: payload.studentName ?? "Student",
        category: payload.category ?? "Action Items",
        body: payload.body ?? "",
        at: new Date().toISOString(),
        private: payload.private ?? true,
      },
      ...notes,
    ];
    return mockDelay(undefined, 150);
  },

  async actionItems(): Promise<MentorActionItem[]> {
    if (!env.useMocks) return apiRequest<MentorActionItem[]>("/mentor/action-items");
    return mockDelay(actionItems);
  },

  async addActionItem(payload: Partial<MentorActionItem>): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>("/mentor/action-items", { method: "POST", body: payload });
    actionItems = [
      {
        id: `ai-${Date.now()}`,
        mentorId: "mnt-1",
        studentId: payload.studentId ?? "stu-1",
        studentName: payload.studentName ?? "Student",
        task: payload.task ?? "",
        deadline: payload.deadline ?? new Date().toISOString().slice(0, 10),
        status: "Not Started",
      },
      ...actionItems,
    ];
    return mockDelay(undefined, 150);
  },

  async alerts(): Promise<MentorAlert[]> {
    if (!env.useMocks) return apiRequest<MentorAlert[]>("/mentor/alerts");
    return mockDelay(mockMentorAlerts);
  },

  async assignments(): Promise<MentorAssignment[]> {
    if (!env.useMocks) return apiRequest<MentorAssignment[]>("/admin/mentor-assignments");
    return mockDelay(assignments);
  },

  async createAssignment(
    payload: Omit<MentorAssignment, "id" | "assignedAt" | "assignedBy">,
  ): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>("/admin/mentor-assignments", { method: "POST", body: payload });
    assignments = [
      {
        ...payload,
        id: `ma-${Date.now()}`,
        assignedAt: new Date().toISOString(),
        assignedBy: "Admin",
      },
      ...assignments,
    ];
    return mockDelay(undefined, 150);
  },

  async removeAssignment(id: string): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/admin/mentor-assignments/${id}`, { method: "DELETE" });
    assignments = assignments.filter((a) => a.id !== id);
    return mockDelay(undefined, 150);
  },

  /** What the signed-in student sees on "My Mentor". Private notes are excluded. */
  async studentView(): Promise<StudentMentorView> {
    if (!env.useMocks) return apiRequest<StudentMentorView>("/student/mentor");
    const mentor = mockMentors[0]!;
    const mine = sessions.filter((s) => s.studentId === "stu-1");
    return mockDelay({
      mentor,
      nextSession:
        mine.find((s) => s.status === "Scheduled") ??
        sessions.find((s) => s.status === "Scheduled"),
      sessions: mine.length ? mine : sessions.slice(0, 4),
      actionItems: actionItems.filter((a) => a.studentId === "stu-1"),
    });
  },
};

const guardedMentorCalls = guardService(
  {
    mentors: mentorServiceRaw.mentors,
    stats: mentorServiceRaw.stats,
    students: mentorServiceRaw.students,
    sessions: mentorServiceRaw.sessions,
    createSession: mentorServiceRaw.createSession,
    updateSession: mentorServiceRaw.updateSession,
    notes: mentorServiceRaw.notes,
    addNote: mentorServiceRaw.addNote,
    actionItems: mentorServiceRaw.actionItems,
    addActionItem: mentorServiceRaw.addActionItem,
    alerts: mentorServiceRaw.alerts,
    assignments: mentorServiceRaw.assignments,
    createAssignment: mentorServiceRaw.createAssignment,
    removeAssignment: mentorServiceRaw.removeAssignment,
  },
  "mentor.students",
  {
    createSession: "mentor.sessions",
    updateSession: "mentor.sessions",
    assignments: "mentor.assign",
    createAssignment: "mentor.assign",
    removeAssignment: "mentor.assign",
  },
);

/** studentView stays student-scoped and is authorized by the backend. */
export const mentorService = { ...mentorServiceRaw, ...guardedMentorCalls };
