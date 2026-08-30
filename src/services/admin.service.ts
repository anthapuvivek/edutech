import { env } from "@/lib/env";
import { guardService } from "@/lib/authz";
import {
  mockAdminApplications,
  mockAdminArticles,
  mockAdminCompanies,
  mockAdminJobs,
  mockAdminOverview,
  mockAdminReferrals,
  mockAdminStudentDetail,
  mockAdminStudents,
  mockAttendanceSummary,
  mockAuditLogs,
  mockBatches,
  mockEligibilityRules,
  mockSetupSteps,
  mockTrainers,
  mockWhatsAppSettings,
} from "@/mock/admin";
import { mockAdminStats, mockEvents } from "@/mock/lms";
import { mockCourses } from "@/mock/courses";
import { apiRequest, mockDelay } from "@/services/api-client";
import type { Course } from "@/types";
import type {
  AdminApplication,
  AdminArticle,
  AdminBatch,
  AdminCompany,
  AdminJob,
  AdminOverview,
  AdminReferral,
  AdminSetupStep,
  AdminStudentDetail,
  AdminStudentRow,
  AdminTrainer,
  AttendanceSummaryRow,
  AuditLogEntry,
  EligibilityRuleSet,
  StudentAccountStatus,
  WhatsAppSettings,
} from "@/types/admin";
import type { AdminStats, PlatformEvent } from "@/types/lms";

/**
 * Admin control-center service.
 *
 * Every method has an HTTP branch so the FastAPI backend can be connected
 * without touching UI code. All writes below are intent submissions — the
 * backend remains the authority for authorization, attendance maths,
 * eligibility evaluation, payments and placement verification.
 */
const adminServiceRaw = {
  async stats(): Promise<AdminStats> {
    if (!env.useMocks) return apiRequest<AdminStats>("/admin/stats");
    return mockDelay(mockAdminStats);
  },
  async overview(): Promise<AdminOverview> {
    if (!env.useMocks) return apiRequest<AdminOverview>("/admin/overview");
    return mockDelay(mockAdminOverview);
  },
  async setupSteps(): Promise<AdminSetupStep[]> {
    if (!env.useMocks) return apiRequest<AdminSetupStep[]>("/admin/setup");
    return mockDelay(mockSetupSteps);
  },
  async events(): Promise<PlatformEvent[]> {
    if (!env.useMocks) return apiRequest<PlatformEvent[]>("/admin/events");
    return mockDelay(mockEvents);
  },

  // Students -----------------------------------------------------------------
  async students(): Promise<AdminStudentRow[]> {
    if (!env.useMocks) return apiRequest<AdminStudentRow[]>("/admin/students");
    return mockDelay(mockAdminStudents);
  },
  async student(id: string): Promise<AdminStudentDetail> {
    if (!env.useMocks) return apiRequest<AdminStudentDetail>(`/admin/students/${id}`);
    return mockDelay(mockAdminStudentDetail(id));
  },
  async createStudent(payload: Record<string, unknown>): Promise<{ ok: boolean; id?: string; email?: string; identifier?: string; emailStatus?: string; message?: string }> {
    if (!env.useMocks) return apiRequest("/admin/students", { method: "POST", body: payload });
    return mockDelay({ ok: true, identifier: "LTX-2026-9999", emailStatus: "SENT", message: "Student onboarded successfully. Activation email sent." });
  },
  async resendStudentWelcomeEmail(id: string): Promise<{ ok: boolean; emailStatus?: string; message?: string }> {
    if (!env.useMocks) return apiRequest(`/admin/students/${id}/resend-welcome-email`, { method: "POST" });
    return mockDelay({ ok: true, emailStatus: "SENT", message: "Welcome email resent successfully." });
  },
  async deleteStudent(id: string): Promise<{ ok: boolean; message?: string }> {
    if (!env.useMocks) return apiRequest(`/admin/students/${id}`, { method: "DELETE" });
    return mockDelay({ ok: true, message: "Student account has been deactivated successfully." });
  },
  async setStudentStatus(
    id: string,
    status: StudentAccountStatus,
    reason?: string,
  ): Promise<{ ok: true }> {
    if (!env.useMocks)
      return apiRequest(`/admin/students/${id}/status`, {
        method: "PATCH",
        body: { status, reason },
      });
    return mockDelay({ ok: true } as const);
  },
  async createEnrollment(payload: Record<string, unknown>): Promise<{ ok: true }> {
    if (!env.useMocks) return apiRequest("/admin/enrollments", { method: "POST", body: payload });
    return mockDelay({ ok: true } as const);
  },
  async getStudentAllocations(studentId: string): Promise<any[]> {
    if (!env.useMocks) return apiRequest<any[]>(`/admin/students/${studentId}/allocations`);
    return mockDelay([]);
  },
  async allocateStudent(payload: {
    studentId: string;
    courseId: string;
    teacherId?: string | undefined;
    batchId?: string | undefined;
    status?: string | undefined;
  }): Promise<any> {
    if (!env.useMocks) return apiRequest("/admin/allocations", { method: "POST", body: payload });
    return mockDelay({ id: `alloc-${Date.now()}`, ...payload });
  },
  async reassignAllocation(id: string, payload: {
    studentId: string;
    courseId: string;
    teacherId?: string | undefined;
    batchId?: string | undefined;
    status?: string | undefined;
  }): Promise<any> {
    if (!env.useMocks) return apiRequest(`/admin/allocations/${id}`, { method: "PUT", body: payload });
    return mockDelay({ id, ...payload });
  },
  async removeAllocation(id: string): Promise<{ ok: boolean }> {
    if (!env.useMocks) return apiRequest<{ ok: boolean }>(`/admin/allocations/${id}`, { method: "DELETE" });
    return mockDelay({ ok: true });
  },
  async overrideEligibility(id: string, payload: Record<string, unknown>): Promise<{ ok: true }> {
    if (!env.useMocks)
      return apiRequest(`/admin/students/${id}/eligibility`, { method: "PATCH", body: payload });
    return mockDelay({ ok: true } as const);
  },

  // Batches & trainers -------------------------------------------------------
  async batches(): Promise<AdminBatch[]> {
    if (!env.useMocks) return apiRequest<AdminBatch[]>("/admin/batches");
    return mockDelay(mockBatches);
  },
  async saveBatch(payload: Record<string, unknown>): Promise<{ ok: true }> {
    if (!env.useMocks) return apiRequest("/admin/batches", { method: "POST", body: payload });
    return mockDelay({ ok: true } as const);
  },
  async trainers(): Promise<AdminTrainer[]> {
    if (!env.useMocks) return apiRequest<AdminTrainer[]>("/admin/trainers");
    return mockDelay(mockTrainers);
  },
  async createTeacher(payload: Record<string, unknown>): Promise<{ ok: boolean; id?: string; email?: string; identifier?: string; emailStatus?: string; message?: string }> {
    if (!env.useMocks) return apiRequest("/admin/teachers", { method: "POST", body: payload });
    return mockDelay({ ok: true, identifier: "LTX-T-2026-9999", emailStatus: "SENT", message: "Teacher onboarded successfully. Activation email sent." });
  },
  async resendTeacherWelcomeEmail(id: string): Promise<{ ok: boolean; emailStatus?: string; message?: string }> {
    if (!env.useMocks) return apiRequest(`/admin/teachers/${id}/resend-welcome-email`, { method: "POST" });
    return mockDelay({ ok: true, emailStatus: "SENT", message: "Welcome email resent successfully." });
  },
  async deleteTeacher(id: string): Promise<{ ok: boolean; message?: string }> {
    if (!env.useMocks) return apiRequest(`/admin/teachers/${id}`, { method: "DELETE" });
    return mockDelay({ ok: true, message: "Teacher account has been deactivated successfully." });
  },
  async approveTrainer(id: string, approve: boolean): Promise<{ ok: true }> {
    if (!env.useMocks)
      return apiRequest(`/admin/trainers/${id}/approval`, { method: "PATCH", body: { approve } });
    return mockDelay({ ok: true } as const);
  },

  // Attendance ---------------------------------------------------------------
  async attendance(): Promise<AttendanceSummaryRow[]> {
    if (!env.useMocks) return apiRequest<AttendanceSummaryRow[]>("/admin/attendance");
    return mockDelay(mockAttendanceSummary);
  },
  async correctAttendance(payload: Record<string, unknown>): Promise<{ ok: true }> {
    if (!env.useMocks) return apiRequest("/admin/attendance", { method: "PATCH", body: payload });
    return mockDelay({ ok: true } as const);
  },

  // Career -------------------------------------------------------------------
  async eligibilityRules(): Promise<EligibilityRuleSet[]> {
    if (!env.useMocks) return apiRequest<EligibilityRuleSet[]>("/admin/career/eligibility-rules");
    return mockDelay(mockEligibilityRules);
  },
  async saveEligibilityRule(rule: EligibilityRuleSet): Promise<{ ok: true }> {
    if (!env.useMocks)
      return apiRequest(`/admin/career/eligibility-rules/${rule.id}`, {
        method: "PUT",
        body: rule,
      });
    return mockDelay({ ok: true } as const);
  },
  async jobs(): Promise<AdminJob[]> {
    if (!env.useMocks) return apiRequest<AdminJob[]>("/admin/career/jobs");
    return mockDelay(mockAdminJobs);
  },
  async saveJob(payload: Record<string, unknown>): Promise<{ ok: true }> {
    if (!env.useMocks) return apiRequest("/admin/career/jobs", { method: "POST", body: payload });
    return mockDelay({ ok: true } as const);
  },
  async companies(): Promise<AdminCompany[]> {
    if (!env.useMocks) return apiRequest<AdminCompany[]>("/admin/career/companies");
    return mockDelay(mockAdminCompanies);
  },
  async applications(): Promise<AdminApplication[]> {
    if (!env.useMocks) return apiRequest<AdminApplication[]>("/admin/career/applications");
    return mockDelay(mockAdminApplications);
  },
  async referrals(): Promise<AdminReferral[]> {
    if (!env.useMocks) return apiRequest<AdminReferral[]>("/admin/career/referrals");
    return mockDelay(mockAdminReferrals);
  },
  async updateReferral(id: string, status: string): Promise<{ ok: true }> {
    if (!env.useMocks)
      return apiRequest(`/admin/career/referrals/${id}`, { method: "PATCH", body: { status } });
    return mockDelay({ ok: true } as const);
  },

  // Content & communication --------------------------------------------------
  async articles(): Promise<AdminArticle[]> {
    if (!env.useMocks) return apiRequest<AdminArticle[]>("/admin/articles");
    return mockDelay(mockAdminArticles);
  },
  async saveArticle(payload: Record<string, unknown>): Promise<{ ok: true }> {
    if (!env.useMocks) return apiRequest("/admin/articles", { method: "POST", body: payload });
    return mockDelay({ ok: true } as const);
  },
  async whatsappSettings(): Promise<WhatsAppSettings> {
    if (!env.useMocks) return apiRequest<WhatsAppSettings>("/admin/communication/whatsapp");
    return mockDelay(mockWhatsAppSettings);
  },
  async saveWhatsappSettings(payload: WhatsAppSettings): Promise<{ ok: true }> {
    if (!env.useMocks)
      return apiRequest("/admin/communication/whatsapp", { method: "PUT", body: payload });
    return mockDelay({ ok: true } as const);
  },
  async sendNotification(payload: Record<string, unknown>): Promise<{ ok: true }> {
    if (!env.useMocks)
      return apiRequest("/admin/communication/notifications", { method: "POST", body: payload });
    return mockDelay({ ok: true } as const);
  },

  // Governance ---------------------------------------------------------------
  async auditLogs(): Promise<AuditLogEntry[]> {
    if (!env.useMocks) return apiRequest<AuditLogEntry[]>("/admin/audit-logs");
    return mockDelay(mockAuditLogs);
  },

  // Courses ------------------------------------------------------------------
  async courses(query?: { search?: string; category?: string; status?: string; sort?: string }): Promise<Course[]> {
    if (!env.useMocks) return apiRequest<Course[]>("/admin/courses", { query: query as Record<string, string | number | boolean | undefined> });
    return mockDelay(mockCourses as Course[]);
  },
  async course(id: string): Promise<Course> {
    if (!env.useMocks) return apiRequest<Course>(`/admin/courses/${id}`);
    return mockDelay((mockCourses.find((c) => c.id === id) || mockCourses[0]) as Course);
  },
  async createCourse(payload: Record<string, unknown>): Promise<Course> {
    if (!env.useMocks) return apiRequest<Course>("/admin/courses", { method: "POST", body: payload });
    return mockDelay({ id: `crs-${Date.now()}`, ...payload } as unknown as Course);
  },
  async updateCourse(id: string, payload: Record<string, unknown>): Promise<Course> {
    if (!env.useMocks) return apiRequest<Course>(`/admin/courses/${id}`, { method: "PUT", body: payload });
    return mockDelay({ id, ...payload } as unknown as Course);
  },
  async publishCourse(id: string): Promise<Course> {
    if (!env.useMocks) return apiRequest<Course>(`/admin/courses/${id}/publish`, { method: "PATCH" });
    return mockDelay({ id, status: "PUBLISHED" } as unknown as Course);
  },
  async unpublishCourse(id: string): Promise<Course> {
    if (!env.useMocks) return apiRequest<Course>(`/admin/courses/${id}/unpublish`, { method: "PATCH" });
    return mockDelay({ id, status: "DRAFT" } as unknown as Course);
  },
  async setCourseStatus(id: string, status: string): Promise<Course> {
    if (!env.useMocks) return apiRequest<Course>(`/admin/courses/${id}/status`, { method: "PATCH", body: { status } });
    return mockDelay({ id, status } as unknown as Course);
  },
  async deleteCourse(id: string): Promise<{ ok: boolean }> {
    if (!env.useMocks) return apiRequest<{ ok: boolean }>(`/admin/courses/${id}`, { method: "DELETE" });
    return mockDelay({ ok: true });
  },
};

/**
 * Every admin call is permission-checked before it leaves the client, and the
 * same permission is re-enforced by the API. Career/placement methods are
 * shared with the Placement Officer role; everything else stays admin-only.
 */
export const adminService = guardService(adminServiceRaw, "platform.manage", {
  jobs: "placement.jobs.view",
  saveJob: "placement.jobs.manage",
  companies: "placement.jobs.view",
  applications: "placement.applications.manage",
  referrals: "placement.applications.manage",
  updateReferral: "placement.applications.manage",
  eligibilityRules: "placement.students.view",
  auditLogs: "audit.view",
});

