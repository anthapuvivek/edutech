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
import { apiRequest, mockDelay } from "@/services/api-client";
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
  async createStudent(payload: Record<string, unknown>): Promise<{ ok: true }> {
    if (!env.useMocks) return apiRequest("/admin/students", { method: "POST", body: payload });
    return mockDelay({ ok: true } as const);
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
