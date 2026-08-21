import { guardService } from "@/lib/authz";
import { env } from "@/lib/env";
import { buildTicketDetail, mockStaff, mockSupportOverview, mockTickets } from "@/mock/ops";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  StaffMember,
  SupportOverview,
  SupportTicket,
  TicketCategory,
  TicketDetail,
  TicketPriority,
  TicketStatus,
} from "@/types/ops";

let tickets = [...mockTickets];

/**
 * Support desk abstraction.
 * Ticket visibility, internal-note access and escalation rights are enforced
 * by the backend. Internal notes are filtered here as a UI safeguard only.
 */
const supportServiceRaw = {
  async overview(): Promise<SupportOverview> {
    if (!env.useMocks) return apiRequest<SupportOverview>("/support/overview");
    return mockDelay(mockSupportOverview);
  },

  async list(params?: {
    status?: string;
    priority?: string;
    category?: string;
    assignedTo?: string;
    search?: string;
  }): Promise<SupportTicket[]> {
    if (!env.useMocks)
      return apiRequest<SupportTicket[]>("/support/tickets", {
        query: params as Record<string, string>,
      });
    const term = params?.search?.toLowerCase().trim();
    return mockDelay(
      tickets.filter((t) => {
        if (params?.status && params.status !== "all" && t.status !== params.status) return false;
        if (params?.priority && params.priority !== "all" && t.priority !== params.priority)
          return false;
        if (params?.category && params.category !== "all" && t.category !== params.category)
          return false;
        if (params?.assignedTo && params.assignedTo !== "all" && t.assignedTo !== params.assignedTo)
          return false;
        if (term && !`${t.reference} ${t.subject} ${t.studentName}`.toLowerCase().includes(term))
          return false;
        return true;
      }),
    );
  },

  /** Tickets raised by the signed-in student. */
  async myTickets(studentName: string): Promise<SupportTicket[]> {
    if (!env.useMocks) return apiRequest<SupportTicket[]>("/support/tickets/mine");
    return mockDelay(
      tickets
        .slice(0, 6)
        .map((t, i) => (i < 3 ? { ...t, studentName } : t))
        .filter((_, i) => i < 6),
    );
  },

  async detail(id: string, viewer: "student" | "staff"): Promise<TicketDetail> {
    if (!env.useMocks) return apiRequest<TicketDetail>(`/support/tickets/${id}`);
    const ticket = tickets.find((t) => t.id === id);
    if (!ticket) throw new Error("Ticket not found");
    const detail = buildTicketDetail(ticket);
    if (viewer === "student")
      return mockDelay({ ...detail, messages: detail.messages.filter((m) => !m.internal) });
    return mockDelay(detail);
  },

  async create(payload: {
    subject: string;
    category: TicketCategory;
    description: string;
    priority: TicketPriority;
    studentName: string;
    attachmentNames?: string[];
  }): Promise<SupportTicket> {
    if (!env.useMocks)
      return apiRequest<SupportTicket>("/support/tickets", { method: "POST", body: payload });
    const ticket: SupportTicket = {
      id: `tkt-${Date.now()}`,
      reference: `LTX-T-${2500 + tickets.length}`,
      subject: payload.subject,
      category: payload.category,
      description: payload.description,
      priority: payload.priority,
      status: "Open",
      studentId: "stu-self",
      studentName: payload.studentName,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      attachments: (payload.attachmentNames ?? []).map((n, i) => ({
        id: `a-${i}`,
        name: n,
        sizeKb: 320,
        kind: "document" as const,
      })),
    };
    tickets = [ticket, ...tickets];
    return mockDelay(ticket, 250);
  },

  async reply(
    id: string,
    body: string,
    options: { internal: boolean; author: string; authorRole: "student" | "staff" },
  ): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/support/tickets/${id}/messages`, {
        method: "POST",
        body: { body, ...options },
      });
    return mockDelay(undefined, 200);
  },

  async updateStatus(id: string, status: TicketStatus): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/support/tickets/${id}/status`, {
        method: "PATCH",
        body: { status },
      });
    tickets = tickets.map((t) =>
      t.id === id ? { ...t, status, updatedAt: new Date().toISOString() } : t,
    );
    return mockDelay(undefined, 150);
  },

  async assign(id: string, staff: StaffMember): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/support/tickets/${id}/assign`, {
        method: "PATCH",
        body: { assigneeId: staff.id },
      });
    tickets = tickets.map((t) =>
      t.id === id
        ? {
            ...t,
            assignedTo: staff.name,
            assignedRole: staff.role,
            status: t.status === "Open" ? "Assigned" : t.status,
          }
        : t,
    );
    return mockDelay(undefined, 150);
  },

  async escalate(id: string, to: string): Promise<void> {
    if (!env.useMocks)
      return apiRequest<void>(`/support/tickets/${id}/escalate`, { method: "POST", body: { to } });
    tickets = tickets.map((t) => (t.id === id ? { ...t, escalatedTo: to } : t));
    return mockDelay(undefined, 150);
  },

  async agents(): Promise<StaffMember[]> {
    if (!env.useMocks) return apiRequest<StaffMember[]>("/support/agents");
    return mockDelay(mockStaff);
  },
};

const guardedStaffCalls = guardService(
  {
    overview: supportServiceRaw.overview,
    list: supportServiceRaw.list,
    updateStatus: supportServiceRaw.updateStatus,
    assign: supportServiceRaw.assign,
    escalate: supportServiceRaw.escalate,
    agents: supportServiceRaw.agents,
  },
  "support.view",
  { updateStatus: "support.manage", assign: "support.manage", escalate: "support.manage", agents: "support.manage" },
);

/**
 * Staff queues are permission-checked; student-scoped calls (own tickets,
 * replies) are authorized by the backend against the session owner.
 */
export const supportService = {
  ...supportServiceRaw,
  ...guardedStaffCalls,
};
