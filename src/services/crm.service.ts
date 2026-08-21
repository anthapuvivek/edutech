import { guardService } from "@/lib/authz";
import { env } from "@/lib/env";
import { buildLeadDetail, mockLeadOverview, mockLeads, mockStaff } from "@/mock/ops";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  Lead,
  LeadDetail,
  LeadFollowUp,
  LeadOverview,
  LeadStage,
  StaffMember,
} from "@/types/ops";

let leads = [...mockLeads];

/**
 * CRM / lead management abstraction.
 * Lead ownership and visibility are enforced by the backend; the `viewerId`
 * filter below only mirrors the API contract for the mock UI.
 */
const crmServiceRaw = {
  async overview(): Promise<LeadOverview> {
    if (!env.useMocks) return apiRequest<LeadOverview>("/admin/crm/overview");
    return mockDelay(mockLeadOverview);
  },

  async list(params?: {
    stage?: LeadStage | "all";
    source?: string;
    assignedToId?: string;
    search?: string;
  }): Promise<Lead[]> {
    if (!env.useMocks)
      return apiRequest<Lead[]>("/admin/crm/leads", { query: params as Record<string, string> });
    const term = params?.search?.toLowerCase().trim();
    return mockDelay(
      leads.filter((l) => {
        if (params?.stage && params.stage !== "all" && l.stage !== params.stage) return false;
        if (params?.source && params.source !== "all" && l.source !== params.source) return false;
        if (
          params?.assignedToId &&
          params.assignedToId !== "all" &&
          l.assignedToId !== params.assignedToId
        )
          return false;
        if (
          term &&
          !`${l.name} ${l.email} ${l.phone} ${l.courseInterest}`.toLowerCase().includes(term)
        )
          return false;
        return true;
      }),
    );
  },

  async detail(id: string): Promise<LeadDetail> {
    if (!env.useMocks) return apiRequest<LeadDetail>(`/admin/crm/leads/${id}`);
    const lead = leads.find((l) => l.id === id);
    if (!lead) throw new Error("Lead not found");
    return mockDelay(buildLeadDetail(lead));
  },

  async moveStage(id: string, stage: LeadStage): Promise<Lead> {
    if (!env.useMocks)
      return apiRequest<Lead>(`/admin/crm/leads/${id}/stage`, { method: "PATCH", body: { stage } });
    leads = leads.map((l) =>
      l.id === id ? { ...l, stage, lastContactAt: new Date().toISOString() } : l,
    );
    return mockDelay(
      leads.find((l) => l.id === id)!,
      120,
    );
  },

  async assign(id: string, staff: StaffMember): Promise<Lead> {
    if (!env.useMocks)
      return apiRequest<Lead>(`/admin/crm/leads/${id}/assign`, {
        method: "PATCH",
        body: { assignedToId: staff.id },
      });
    leads = leads.map((l) =>
      l.id === id ? { ...l, assignedTo: staff.name, assignedToId: staff.id } : l,
    );
    return mockDelay(
      leads.find((l) => l.id === id)!,
      150,
    );
  },

  async addNote(id: string, body: string): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/admin/crm/leads/${id}/notes`, { method: "POST", body: { body } });
    return mockDelay(undefined, 150);
  },

  async logCommunication(id: string, channel: string, summary: string): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/admin/crm/leads/${id}/communications`, {
        method: "POST",
        body: { channel, summary },
      });
    return mockDelay(undefined, 150);
  },

  async scheduleFollowUp(id: string, payload: Partial<LeadFollowUp>): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/admin/crm/leads/${id}/follow-ups`, {
        method: "POST",
        body: payload,
      });
    leads = leads.map((l) =>
      l.id === id ? { ...l, nextFollowUpAt: `${payload.date ?? ""}T09:00:00.000Z` } : l,
    );
    return mockDelay(undefined, 150);
  },

  async staff(): Promise<StaffMember[]> {
    if (!env.useMocks) return apiRequest<StaffMember[]>("/admin/staff");
    return mockDelay(mockStaff);
  },
};

export const crmService = guardService(crmServiceRaw, "crm.view", {
  moveStage: "crm.manage",
  addNote: "crm.manage",
  logCommunication: "crm.manage",
  scheduleFollowUp: "crm.manage",
  assign: "crm.assign",
});
