/**
 * Admin control-center domain types.
 *
 * IMPORTANT: every rule expressed here is a *display contract*. Authorization,
 * attendance maths, eligibility evaluation, payment verification and placement
 * status are all computed by the FastAPI backend. The frontend renders what the
 * backend returns and sends admin intent back through the service layer.
 */

export type StudentAccountStatus = "pending" | "active" | "suspended" | "inactive" | "archived";
export type CareerEligibilityStatus = "eligible" | "conditional" | "not_eligible" | "under_review";
export type PaymentStatus = "paid" | "partial" | "pending" | "refunded";
export type PlacementStatus = "not_started" | "job_seeking" | "interviewing" | "offered" | "placed";
export type AttendanceStatus = "present" | "absent" | "late" | "excused";

export interface AdminStudentRow {
  id: string;
  studentId: string;
  name: string;
  email: string;
  phone: string;
  courseTitle: string;
  batchName: string;
  trainerName: string;
  status: StudentAccountStatus;
  statusReason?: string | undefined;
  attendancePercent: number;
  progressPercent: number;
  quizScore: number;
  codingScore: number;
  assignmentCompletion: number;
  profileCompletion: number;
  points: number;
  rank: number;
  careerStatus: CareerEligibilityStatus;
  paymentStatus: PaymentStatus;
  placementStatus: PlacementStatus;
  location: string;
  college: string;
  graduationYear: number;
  enrolledAt: string;
}

export interface StudentDocument {
  id: string;
  kind: "profile_photo" | "resume" | "certificate" | "id_proof" | "project";
  name: string;
  uploadedAt: string;
  reviewStatus: "pending" | "approved" | "rejected";
}

export interface StudentTimelineEntry {
  id: string;
  stage: string;
  detail: string;
  occurredAt: string;
  state: "done" | "current" | "upcoming";
}

export interface AdminStudentDetail extends AdminStudentRow {
  dateOfBirth: string;
  gender: string;
  qualification: string;
  skills: string[];
  links: { github?: string | undefined; linkedin?: string | undefined; leetcode?: string | undefined; hackerrank?: string | undefined };
  documents: StudentDocument[];
  timeline: StudentTimelineEntry[];
  enrollments: Array<{
    id: string;
    courseTitle: string;
    batchName: string;
    trainerName: string;
    startDate: string;
    endDate: string;
    courseStatus: "not_started" | "in_progress" | "completed" | "on_hold";
    paymentStatus: PaymentStatus;
    careerEligible: boolean;
  }>;
  attendance: Array<{ date: string; classTitle: string; batchName: string; status: AttendanceStatus }>;
  assessments: Array<{ id: string; kind: "quiz" | "assignment" | "coding"; title: string; score: number; submittedAt: string }>;
  applications: Array<{ id: string; company: string; role: string; appliedAt: string; status: string }>;
  payments: Array<{ id: string; description: string; amount: number; status: PaymentStatus; paidAt: string }>;
  certificates: Array<{ id: string; title: string; issuedAt: string }>;
  activity: Array<{ id: string; label: string; occurredAt: string }>;
}

export interface AdminBatch {
  id: string;
  name: string;
  courseTitle: string;
  trainerName: string;
  startDate: string;
  endDate: string;
  days: string;
  time: string;
  mode: "Online" | "Offline" | "Hybrid";
  platform: "Google Meet" | "Zoom" | "Microsoft Teams" | "Other";
  meetingUrl?: string | undefined;
  capacity: number;
  enrolled: number;
  status: "Upcoming" | "Active" | "Completed" | "Cancelled";
  published: boolean;
}

export interface AdminTrainer {
  id: string;
  name: string;
  email: string;
  phone: string;
  headline: string;
  approvalStatus: "pending" | "approved" | "rejected";
  skills: string[];
  courses: string[];
  batches: number;
  students: number;
  classesThisMonth: number;
  rating: number;
  experienceYears: number;
  status: "active" | "inactive";
}

export interface AttendanceSummaryRow {
  studentId: string;
  studentName: string;
  courseTitle: string;
  batchName: string;
  trainerName: string;
  totalClasses: number;
  present: number;
  absent: number;
  late: number;
  excused: number;
  attendancePercent: number;
}

export type EligibilityMetric =
  | "attendance"
  | "courseProgress"
  | "quizScore"
  | "codingScore"
  | "assignmentCompletion"
  | "profileCompletion"
  | "points"
  | "mockInterviewScore";

export interface EligibilityRuleSet {
  id: string;
  name: string;
  scope: "platform" | "company";
  companyId?: string | undefined;
  active: boolean;
  thresholds: Partial<Record<EligibilityMetric, number>>;
  updatedAt: string;
}

export interface AdminCompany {
  id: string;
  name: string;
  logoUrl?: string | undefined;
  website: string;
  industry: string;
  description: string;
  locations: string[];
  roles: string[];
  officialPartner: boolean;
  verified: boolean;
  preparationTrack: boolean;
  openRoles: number;
}

export interface AdminJob {
  id: string;
  title: string;
  companyName: string;
  location: string;
  workMode: "Remote" | "Hybrid" | "Onsite";
  jobType: "Full Time" | "Internship" | "Contract" | "Part Time";
  salaryRange: string;
  experience: string;
  skills: string[];
  eligibleCourses: string[];
  minAttendance: number;
  minCodingScore: number;
  minCourseProgress: number;
  deadline: string;
  applicationMethod: "Internal" | "External URL" | "Referral";
  status: "Draft" | "Published" | "Unpublished" | "Expired" | "Archived";
  applicants: number;
  eligibleStudents: number;
}

export interface AdminApplication {
  id: string;
  studentName: string;
  studentId: string;
  jobTitle: string;
  companyName: string;
  appliedAt: string;
  eligible: boolean;
  status:
    | "Applied"
    | "Shortlisted"
    | "Assessment"
    | "Interview"
    | "Final Round"
    | "Offer"
    | "Rejected"
    | "Withdrawn";
}

export interface AdminReferral {
  id: string;
  companyName: string;
  role: string;
  studentName: string;
  referrerName: string;
  requestedAt: string;
  status: "Requested" | "Under Review" | "Approved" | "Referred" | "Rejected" | "Expired" | "Completed";
}

export interface AdminArticle {
  id: string;
  title: string;
  slug: string;
  authorName: string;
  authorRole: "admin" | "teacher" | "student";
  category: string;
  tags: string[];
  excerpt: string;
  seoTitle: string;
  seoDescription: string;
  publishDate: string;
  status: "Draft" | "In Review" | "Published" | "Scheduled" | "Archived";
  views: number;
}

export interface AuditLogEntry {
  id: string;
  actorName: string;
  actorRole: string;
  action: string;
  entity: string;
  recordLabel: string;
  previousValue?: string | undefined;
  newValue?: string | undefined;
  occurredAt: string;
}

export interface WhatsAppSettings {
  businessNumber: string;
  defaultMessage: string;
  courseEnquiryMessage: string;
  studentSupportMessage: string;
  careerSupportMessage: string;
  jobEnquiryMessage: string;
  enabled: boolean;
}

export interface AdminAlert {
  id: string;
  severity: "warning" | "info";
  title: string;
  count: number;
  actionLabel: string;
  to?: string | undefined;
}

export interface AdminOverview {
  metrics: {
    students: number;
    activeStudents: number;
    inactiveStudents: number;
    teachers: number;
    activeCourses: number;
    activeBatches: number;
    enrollments: number;
    todaysClasses: number;
    todaysAttendance: number;
    careerEligible: number;
    careerIneligible: number;
    activeJobs: number;
    applications: number;
    interviews: number;
    offers: number;
    placements: number;
    revenueThisMonth: number;
    newEnquiries: number;
  };
  studentGrowth: Array<{ month: string; students: number; enrollments: number }>;
  attendanceTrend: Array<{ week: string; attendance: number; completion: number }>;
  performance: Array<{ label: string; quiz: number; coding: number }>;
  placementFunnel: Array<{ stage: string; value: number }>;
  todaysOperations: Array<{ id: string; label: string; detail: string; count: number }>;
  alerts: AdminAlert[];
}

export interface AdminSetupStep {
  id: string;
  label: string;
  description: string;
  complete: boolean;
}
