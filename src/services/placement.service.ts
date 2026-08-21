import { guardService } from "@/lib/authz";
import { env } from "@/lib/env";
import {
  mockDrives,
  mockEligibleStudents,
  mockInterviews,
  mockOffers,
  mockPlacementAnalytics,
  mockPlacementApplications,
  mockPlacementStats,
} from "@/mock/ops";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  PlacementAnalytics,
  PlacementApplication,
  PlacementApplicationStatus,
  PlacementDrive,
  PlacementEligibleStudent,
  PlacementInterview,
  PlacementOffer,
  PlacementStats,
  StudentDriveView,
} from "@/types/ops";

let drives = [...mockDrives];
let applications = [...mockPlacementApplications];
let interviews = [...mockInterviews];
let offers = [...mockOffers];

/**
 * Placement abstraction for the Placement Officer role.
 * Eligibility, application status transitions and placement confirmation are
 * decided by the backend — nothing here grants authority on its own.
 */
const placementServiceRaw = {
  async stats(): Promise<PlacementStats> {
    if (!env.useMocks) return apiRequest<PlacementStats>("/placement/stats");
    return mockDelay(mockPlacementStats);
  },

  async analytics(): Promise<PlacementAnalytics> {
    if (!env.useMocks) return apiRequest<PlacementAnalytics>("/placement/analytics");
    return mockDelay(mockPlacementAnalytics);
  },

  async students(params?: {
    eligibility?: string;
    search?: string;
  }): Promise<PlacementEligibleStudent[]> {
    if (!env.useMocks)
      return apiRequest<PlacementEligibleStudent[]>("/placement/students", {
        query: params as Record<string, string>,
      });
    const term = params?.search?.toLowerCase().trim();
    return mockDelay(
      mockEligibleStudents.filter((s) => {
        if (
          params?.eligibility &&
          params.eligibility !== "all" &&
          s.eligibility !== params.eligibility
        )
          return false;
        if (
          term &&
          !`${s.name} ${s.courseTitle} ${s.skills.join(" ")}`.toLowerCase().includes(term)
        )
          return false;
        return true;
      }),
    );
  },

  async drives(): Promise<PlacementDrive[]> {
    if (!env.useMocks) return apiRequest<PlacementDrive[]>("/placement/drives");
    return mockDelay(drives);
  },

  async createDrive(payload: Partial<PlacementDrive>): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>("/placement/drives", { method: "POST", body: payload });
    drives = [
      {
        id: `drv-${Date.now()}`,
        companyName: payload.companyName ?? "New company",
        role: payload.role ?? "Role",
        description: payload.description ?? "",
        eligibilitySummary: payload.eligibilitySummary ?? "Defined by the eligibility engine",
        applicationDeadline: payload.applicationDeadline ?? new Date().toISOString().slice(0, 10),
        assessmentDate: payload.assessmentDate,
        interviewDate: payload.interviewDate,
        openings: payload.openings ?? 1,
        eligibleStudents: 0,
        registeredStudents: 0,
        stage: payload.stage ?? "Draft",
        location: payload.location ?? "Remote",
        ctcRange: payload.ctcRange ?? "—",
      },
      ...drives,
    ];
    return mockDelay(undefined, 200);
  },

  async updateDriveStage(id: string, stage: PlacementDrive["stage"]): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/placement/drives/${id}/stage`, {
        method: "PATCH",
        body: { stage },
      });
    drives = drives.map((d) => (d.id === id ? { ...d, stage } : d));
    return mockDelay(undefined, 150);
  },

  async applications(params?: {
    status?: string;
    search?: string;
  }): Promise<PlacementApplication[]> {
    if (!env.useMocks)
      return apiRequest<PlacementApplication[]>("/placement/applications", {
        query: params as Record<string, string>,
      });
    const term = params?.search?.toLowerCase().trim();
    return mockDelay(
      applications.filter((a) => {
        if (params?.status && params.status !== "all" && a.status !== params.status) return false;
        if (term && !`${a.studentName} ${a.companyName} ${a.role}`.toLowerCase().includes(term))
          return false;
        return true;
      }),
    );
  },

  async updateApplicationStatus(ids: string[], status: PlacementApplicationStatus): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>("/placement/applications/status", {
        method: "PATCH",
        body: { ids, status },
      });
    applications = applications.map((a) => (ids.includes(a.id) ? { ...a, status } : a));
    return mockDelay(undefined, 200);
  },

  async interviews(): Promise<PlacementInterview[]> {
    if (!env.useMocks) return apiRequest<PlacementInterview[]>("/placement/interviews");
    return mockDelay(interviews);
  },

  async scheduleInterview(payload: Partial<PlacementInterview>): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>("/placement/interviews", { method: "POST", body: payload });
    interviews = [
      {
        id: `int-${Date.now()}`,
        studentId: payload.studentId ?? "stu-1",
        studentName: payload.studentName ?? "Student",
        companyName: payload.companyName ?? "Company",
        role: payload.role ?? "Role",
        round: payload.round ?? "Technical Interview",
        date: payload.date ?? new Date().toISOString().slice(0, 10),
        time: payload.time ?? "10:00",
        interviewer: payload.interviewer ?? "Hiring Panel",
        platform: payload.platform ?? "Google Meet",
        meetingUrl: payload.meetingUrl,
        status: "Scheduled",
      },
      ...interviews,
    ];
    return mockDelay(undefined, 200);
  },

  async offers(): Promise<PlacementOffer[]> {
    if (!env.useMocks) return apiRequest<PlacementOffer[]>("/placement/offers");
    return mockDelay(offers);
  },

  async recordOffer(payload: Partial<PlacementOffer>): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>("/placement/offers", { method: "POST", body: payload });
    offers = [
      {
        id: `off-${Date.now()}`,
        studentId: payload.studentId ?? "stu-1",
        studentName: payload.studentName ?? "Student",
        companyName: payload.companyName ?? "Company",
        role: payload.role ?? "Role",
        offerDate: payload.offerDate ?? new Date().toISOString().slice(0, 10),
        joiningDate: payload.joiningDate,
        annualSalary: payload.annualSalary,
        location: payload.location ?? "Remote",
        status: "Offer Received",
      },
      ...offers,
    ];
    return mockDelay(undefined, 200);
  },

  /** Placement status is only advanced by an authorised confirmation. */
  async updateOfferStatus(id: string, status: PlacementOffer["status"]): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/placement/offers/${id}/status`, {
        method: "PATCH",
        body: { status },
      });
    offers = offers.map((o) =>
      o.id === id
        ? {
            ...o,
            status,
            verifiedBy: status === "Placement Verified" ? "Placement Office" : o.verifiedBy,
          }
        : o,
    );
    return mockDelay(undefined, 150);
  },

  /** Drives the signed-in student is eligible for. */
  async studentDrives(): Promise<StudentDriveView[]> {
    if (!env.useMocks) return apiRequest<StudentDriveView[]>("/student/placement-drives");
    return mockDelay(
      drives
        .filter((d) => d.stage !== "Draft" && d.stage !== "Cancelled")
        .map((d, i) => ({
          ...d,
          applicationStatus:
            i % 3 === 0
              ? (["Applied", "Assessment", "Shortlisted", "Interview"] as const)[i % 4]
              : undefined,
        })),
    );
  },

  async applyToDrive(driveId: string): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/student/placement-drives/${driveId}/apply`, { method: "POST" });
    return mockDelay(undefined, 250);
  },
};

const guardedPlacementCalls = guardService(
  {
    stats: placementServiceRaw.stats,
    analytics: placementServiceRaw.analytics,
    students: placementServiceRaw.students,
    drives: placementServiceRaw.drives,
    createDrive: placementServiceRaw.createDrive,
    updateDriveStage: placementServiceRaw.updateDriveStage,
    applications: placementServiceRaw.applications,
    updateApplicationStatus: placementServiceRaw.updateApplicationStatus,
    interviews: placementServiceRaw.interviews,
    scheduleInterview: placementServiceRaw.scheduleInterview,
    offers: placementServiceRaw.offers,
    recordOffer: placementServiceRaw.recordOffer,
    updateOfferStatus: placementServiceRaw.updateOfferStatus,
  },
  "placement.students.view",
  {
    createDrive: "placement.drives.manage",
    updateDriveStage: "placement.drives.manage",
    applications: "placement.applications.manage",
    updateApplicationStatus: "placement.applications.manage",
    interviews: "placement.interviews.manage",
    scheduleInterview: "placement.interviews.manage",
    offers: "placement.offers.record",
    recordOffer: "placement.offers.record",
    updateOfferStatus: "placement.offers.record",
  },
);

/** Student-facing drive listing/apply stay student-scoped (backend authorized). */
export const placementService = { ...placementServiceRaw, ...guardedPlacementCalls };
