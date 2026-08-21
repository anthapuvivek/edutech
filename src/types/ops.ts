/**
 * Operations domain types for the enterprise modules:
 * Coupons & Promotions, CRM/Leads, Support Desk, Mentoring, Placement.
 *
 * These mirror the future FastAPI/PostgreSQL contracts. Any business-critical
 * decision (coupon validity, pricing, ownership, eligibility, placement status)
 * is computed by the backend — the mock services below only simulate it so the
 * UI can be built and reviewed.
 */

/* ------------------------------------------------------------------ coupons */

export type DiscountType = "percentage" | "fixed";
export type CouponStatus = "draft" | "active" | "scheduled" | "expired" | "disabled";

export interface Coupon {
  id: string;
  code: string;
  name: string;
  description: string;
  discountType: DiscountType;
  discountValue: number;
  maximumDiscount?: number | undefined;
  minimumPurchase?: number | undefined;
  applicableCourses: string[];
  applicableBatches: string[];
  applicableUsers: string[];
  startDate: string;
  expiryDate: string;
  usageLimit: number;
  usagePerStudent: number;
  status: CouponStatus;
  createdBy: string;
  createdAt: string;
}

export interface CouponUsageStat {
  couponId: string;
  code: string;
  usage: number;
  revenue: number;
  discountGiven: number;
  conversions: number;
  conversionRate: number;
}

export interface CouponOverview {
  totalCoupons: number;
  activeCoupons: number;
  totalUsage: number;
  revenueGenerated: number;
  discountGiven: number;
  conversionRate: number;
  perCoupon: CouponUsageStat[];
}

export type CouponRejectionReason =
  "invalid" | "expired" | "not_applicable" | "usage_limit" | "minimum_purchase";

export interface CouponValidationResult {
  valid: boolean;
  code: string;
  reason?: CouponRejectionReason | undefined;
  message: string;
  /** Server-computed quote. The frontend never decides the final price. */
  quote?:
    | {
        coursePrice: number;
        discount: number;
        finalPrice: number;
        currency: string;
      }
    | undefined;
}

/* ---------------------------------------------------------------------- crm */

export type LeadSource =
  | "Website"
  | "Course Enquiry"
  | "Demo Class"
  | "Webinar"
  | "WhatsApp"
  | "Phone"
  | "Referral"
  | "Social Media"
  | "Advertisement"
  | "Other";

export type LeadStage =
  | "New"
  | "Contacted"
  | "Interested"
  | "Demo Scheduled"
  | "Demo Attended"
  | "Follow-up"
  | "Payment Pending"
  | "Converted"
  | "Not Interested"
  | "Lost";

export const leadStages: LeadStage[] = [
  "New",
  "Contacted",
  "Interested",
  "Demo Scheduled",
  "Demo Attended",
  "Follow-up",
  "Payment Pending",
  "Converted",
  "Not Interested",
  "Lost",
];

export const leadKanbanStages: LeadStage[] = [
  "New",
  "Contacted",
  "Interested",
  "Demo Scheduled",
  "Follow-up",
  "Payment Pending",
  "Converted",
  "Lost",
];

export interface Lead {
  id: string;
  name: string;
  phone: string;
  email: string;
  courseInterest: string;
  source: LeadSource;
  assignedTo: string;
  assignedToId: string;
  stage: LeadStage;
  lastContactAt: string;
  nextFollowUpAt?: string | undefined;
  createdAt: string;
  city?: string | undefined;
  budget?: number | undefined;
  score: number;
}

export interface LeadOverview {
  totalLeads: number;
  newLeads: number;
  followUpsToday: number;
  demoRegistrations: number;
  demoAttendees: number;
  paymentPending: number;
  convertedLeads: number;
  conversionRate: number;
}

export type FollowUpStatus = "Pending" | "Completed" | "Missed" | "Rescheduled";

export interface LeadFollowUp {
  id: string;
  leadId: string;
  date: string;
  time: string;
  notes: string;
  nextAction: string;
  assignedTo: string;
  status: FollowUpStatus;
}

export type CommunicationChannel = "Call" | "WhatsApp" | "Email" | "In-app";

export interface LeadCommunication {
  id: string;
  leadId: string;
  channel: CommunicationChannel;
  summary: string;
  by: string;
  at: string;
}

export interface LeadNote {
  id: string;
  leadId: string;
  body: string;
  by: string;
  at: string;
}

export interface LeadDetail extends Lead {
  communications: LeadCommunication[];
  notes: LeadNote[];
  followUps: LeadFollowUp[];
  demoAttended: boolean;
  enquiryHistory: Array<{ id: string; subject: string; at: string }>;
  paymentHistory: Array<{ id: string; amount: number; status: string; at: string }>;
  enrollmentStatus: "Not Enrolled" | "Payment Pending" | "Enrolled";
}

/* ------------------------------------------------------------------ support */

export type TicketCategory =
  | "Course"
  | "Technical Issue"
  | "Payment"
  | "Account"
  | "Certificate"
  | "Class"
  | "Attendance"
  | "Assignment"
  | "Coding"
  | "Career"
  | "Job"
  | "Resume"
  | "Other";

export type TicketPriority = "Low" | "Medium" | "High" | "Urgent";

export type TicketStatus =
  "Open" | "Assigned" | "In Progress" | "Waiting for Student" | "Resolved" | "Closed";

export interface TicketAttachment {
  id: string;
  name: string;
  sizeKb: number;
  kind: "image" | "pdf" | "document";
}

export interface TicketMessage {
  id: string;
  ticketId: string;
  author: string;
  authorRole: "student" | "staff";
  body: string;
  at: string;
  /** Internal notes are staff-only and must never be rendered to a student. */
  internal: boolean;
  attachments: TicketAttachment[];
}

export interface SupportTicket {
  id: string;
  reference: string;
  subject: string;
  category: TicketCategory;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  studentId: string;
  studentName: string;
  assignedTo?: string | undefined;
  assignedRole?: string | undefined;
  createdAt: string;
  updatedAt: string;
  attachments: TicketAttachment[];
  escalatedTo?: string | undefined;
}

export interface TicketDetail extends SupportTicket {
  messages: TicketMessage[];
}

export interface SupportOverview {
  openTickets: number;
  urgentTickets: number;
  unassignedTickets: number;
  myTickets: number;
  resolvedToday: number;
  averageResolutionHours: number;
}

/* ------------------------------------------------------------------- mentor */

export interface Mentor {
  id: string;
  name: string;
  email: string;
  photoUrl?: string | undefined;
  bio: string;
  specialization: string[];
  experienceYears: number;
  availability: string;
  studentsAssigned: number;
  status: "active" | "suspended";
}

export interface MentorAssignment {
  id: string;
  mentorId: string;
  mentorName: string;
  scope: "student" | "batch" | "course";
  targetId: string;
  targetName: string;
  assignedAt: string;
  assignedBy: string;
}

export interface MentorStudentRow {
  id: string;
  name: string;
  courseTitle: string;
  batchName: string;
  progressPercent: number;
  attendancePercent: number;
  careerReadiness: number;
  quizScore: number;
  codingScore: number;
  lastSessionAt?: string | undefined;
  nextSessionAt?: string | undefined;
  status: "on_track" | "at_risk" | "inactive";
}

export interface MentorStats {
  totalStudents: number;
  activeStudents: number;
  upcomingSessions: number;
  pendingTasks: number;
  studentsAtRisk: number;
  averageCareerReadiness: number;
  averageProgress: number;
}

export type SessionStatus = "Scheduled" | "Completed" | "Cancelled" | "Rescheduled";
export type SessionPlatform = "Google Meet" | "Zoom" | "Microsoft Teams" | "Other";
export type SessionKind =
  "Mentoring" | "Career Session" | "Project Review" | "Interview Preparation" | "Follow-up";

export interface MentorSession {
  id: string;
  title: string;
  kind: SessionKind;
  mentorId: string;
  studentId: string;
  studentName: string;
  date: string;
  time: string;
  durationMinutes: number;
  platform: SessionPlatform;
  meetingUrl?: string | undefined;
  agenda: string;
  notes?: string | undefined;
  status: SessionStatus;
}

export interface MentorNote {
  id: string;
  mentorId: string;
  studentId: string;
  studentName: string;
  category:
    | "Strengths"
    | "Weaknesses"
    | "Career Goals"
    | "Technical Gaps"
    | "Communication"
    | "Action Items"
    | "Next Steps";
  body: string;
  at: string;
  /** Private notes stay with staff; students never see them. */
  private: boolean;
}

export interface MentorActionItem {
  id: string;
  mentorId: string;
  studentId: string;
  studentName: string;
  task: string;
  deadline: string;
  status: "Not Started" | "In Progress" | "Completed" | "Overdue";
}

export interface MentorAlert {
  id: string;
  kind:
    | "Student inactive"
    | "Low attendance"
    | "Low quiz performance"
    | "No coding activity"
    | "Upcoming interview"
    | "Missed mentor session";
  studentName: string;
  detail: string;
  severity: "info" | "warning" | "critical";
  at: string;
}

export interface StudentMentorView {
  mentor: Mentor;
  nextSession?: MentorSession | undefined;
  sessions: MentorSession[];
  actionItems: MentorActionItem[];
}

/* ---------------------------------------------------------------- placement */

export interface PlacementStats {
  eligibleStudents: number;
  activeJobs: number;
  placementDrives: number;
  applications: number;
  shortlisted: number;
  interviews: number;
  offers: number;
  placements: number;
}

export interface PlacementFunnelPoint {
  stage: string;
  count: number;
}

export interface PlacementEligibleStudent {
  id: string;
  name: string;
  courseTitle: string;
  batchName: string;
  skills: string[];
  attendancePercent: number;
  progressPercent: number;
  codingScore: number;
  resumeStatus: "Not Started" | "In Progress" | "Ready" | "ATS Verified";
  careerReadiness: number;
  eligibility: "eligible" | "conditional" | "not_eligible";
  placementStatus: "Not Applied" | "Applied" | "Interviewing" | "Offer" | "Placed";
}

export type DriveStage =
  | "Draft"
  | "Published"
  | "Registration"
  | "Assessment"
  | "Shortlisting"
  | "Interview"
  | "Offer"
  | "Completed"
  | "Cancelled";

export interface PlacementDrive {
  id: string;
  companyName: string;
  role: string;
  description: string;
  eligibilitySummary: string;
  applicationDeadline: string;
  assessmentDate?: string | undefined;
  interviewDate?: string | undefined;
  openings: number;
  eligibleStudents: number;
  registeredStudents: number;
  stage: DriveStage;
  location: string;
  ctcRange: string;
}

export type PlacementApplicationStatus =
  | "Applied"
  | "Assessment"
  | "Shortlisted"
  | "Interview"
  | "Final Round"
  | "Selected"
  | "Rejected"
  | "Withdrawn";

export interface PlacementApplication {
  id: string;
  studentId: string;
  studentName: string;
  companyName: string;
  role: string;
  courseTitle: string;
  eligibility: "eligible" | "conditional" | "not_eligible";
  appliedAt: string;
  status: PlacementApplicationStatus;
  interviewAt?: string | undefined;
  result?: string | undefined;
  driveId?: string | undefined;
}

export type InterviewRound =
  "Technical Interview" | "HR Interview" | "Managerial Interview" | "Mock Interview";

export interface PlacementInterview {
  id: string;
  studentName: string;
  studentId: string;
  companyName: string;
  role: string;
  round: InterviewRound;
  date: string;
  time: string;
  interviewer: string;
  platform: SessionPlatform;
  meetingUrl?: string | undefined;
  status: "Scheduled" | "Completed" | "Cancelled" | "Rescheduled";
}

export type OfferStatus =
  "Offer Received" | "Accepted" | "Declined" | "Joined" | "Placement Verified";

export interface PlacementOffer {
  id: string;
  studentId: string;
  studentName: string;
  companyName: string;
  role: string;
  offerDate: string;
  joiningDate?: string | undefined;
  /** Only rendered when the viewer holds the placement.salary.view permission. */
  annualSalary?: number | undefined;
  location: string;
  status: OfferStatus;
  verifiedBy?: string | undefined;
}

export interface PlacementAnalytics {
  totalEligible: number;
  totalApplied: number;
  shortlisted: number;
  interviewed: number;
  offers: number;
  joined: number;
  placementRate: number;
  byCourse: Array<{ label: string; placements: number; eligible: number }>;
  byBatch: Array<{ label: string; placements: number; eligible: number }>;
  byCompany: Array<{ label: string; placements: number }>;
  byRole: Array<{ label: string; placements: number }>;
  skillDemand: Array<{ label: string; demand: number }>;
}

export interface StudentDriveView extends PlacementDrive {
  applicationStatus?: PlacementApplicationStatus | undefined;
}

/* -------------------------------------------------------------- permissions */

export type PlatformRole =
  | "super_admin"
  | "admin"
  | "teacher"
  | "mentor"
  | "placement_officer"
  | "support_agent"
  | "counsellor"
  | "student";

export type Permission =
  | "platform.manage"
  | "teaching.manage"
  | "learning.view"
  | "users.manage"
  | "coupons.manage"
  | "crm.view"
  | "crm.manage"
  | "crm.assign"
  | "support.view"
  | "support.manage"
  | "support.internal_notes"
  | "mentor.students"
  | "mentor.sessions"
  | "mentor.assign"
  | "placement.students.view"
  | "placement.jobs.view"
  | "placement.jobs.manage"
  | "placement.jobs.publish"
  | "placement.drives.manage"
  | "placement.applications.manage"
  | "placement.interviews.manage"
  | "placement.offers.record"
  | "placement.salary.view"
  | "audit.view"
  | "learning.own"
  | "career.own";

export interface RoleDefinition {
  role: PlatformRole;
  label: string;
  description: string;
  permissions: Permission[];
}

export interface StaffMember {
  id: string;
  name: string;
  email: string;
  role: PlatformRole;
  status: "active" | "inactive" | "suspended";
  assignedCount: number;
  createdAt: string;
}
