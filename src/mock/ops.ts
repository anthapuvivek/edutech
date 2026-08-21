import { mockCourses } from "@/mock/courses";
import type {
  Coupon,
  CouponOverview,
  Lead,
  LeadDetail,
  LeadOverview,
  Mentor,
  MentorActionItem,
  MentorAlert,
  MentorAssignment,
  MentorNote,
  MentorSession,
  MentorStats,
  MentorStudentRow,
  PlacementAnalytics,
  PlacementApplication,
  PlacementDrive,
  PlacementEligibleStudent,
  PlacementInterview,
  PlacementOffer,
  PlacementStats,
  RoleDefinition,
  StaffMember,
  SupportOverview,
  SupportTicket,
  TicketDetail,
  TicketMessage,
} from "@/types/ops";

const courseTitles = mockCourses.slice(0, 6).map((c) => c.title);
const first = [
  "Aarav",
  "Ishita",
  "Rohan",
  "Sneha",
  "Karthik",
  "Priya",
  "Aditya",
  "Fatima",
  "Neha",
  "Vivek",
  "Ananya",
  "Rahul",
  "Divya",
  "Sanjay",
  "Meghna",
  "Arjun",
];
const last = ["Sharma", "Nair", "Gupta", "Reddy", "Iyer", "Menon", "Rao", "Khan"];
const companies = [
  "Nexora Labs",
  "Fintrail",
  "Brightpath Analytics",
  "Corevault",
  "Helios Systems",
  "Quantly",
  "Northwind Tech",
];
const roles = [
  "Software Engineer",
  "Data Analyst",
  "Frontend Engineer",
  "QA Engineer",
  "Business Analyst",
  "Full Stack Developer",
];
const cities = ["Hyderabad", "Bengaluru", "Chennai", "Pune", "Delhi NCR", "Remote"];
const skillPool = [
  "React",
  "Python",
  "SQL",
  "TypeScript",
  "AWS",
  "Power BI",
  "FastAPI",
  "Excel",
  "DSA",
  "Tableau",
];

function pick<T>(arr: T[], i: number): T {
  return arr[i % arr.length]!;
}
function seeded(i: number, min: number, max: number) {
  const x = Math.abs(Math.sin((i + 1) * 12.9898) * 43758.5453);
  return Math.round(min + (x % 1) * (max - min));
}
function name(i: number) {
  return `${pick(first, i)} ${pick(last, i * 3 + 1)}`;
}
function daysFromNow(d: number) {
  return new Date(Date.now() + d * 86_400_000).toISOString();
}
function dateOnly(d: number) {
  return daysFromNow(d).slice(0, 10);
}

/* ------------------------------------------------------------------ coupons */

export const mockCoupons: Coupon[] = [
  {
    id: "cpn-1",
    code: "WELCOME20",
    name: "Welcome offer",
    description: "20% off the first enrollment for new learners.",
    discountType: "percentage",
    discountValue: 20,
    maximumDiscount: 8000,
    minimumPurchase: 10000,
    applicableCourses: [],
    applicableBatches: [],
    applicableUsers: [],
    startDate: dateOnly(-30),
    expiryDate: dateOnly(45),
    usageLimit: 1000,
    usagePerStudent: 1,
    status: "active",
    createdBy: "Nisha Verma",
    createdAt: daysFromNow(-30),
  },
  {
    id: "cpn-2",
    code: "EARLYBIRD5000",
    name: "Early bird batch offer",
    description: "Flat ₹5,000 off when you join before the batch starts.",
    discountType: "fixed",
    discountValue: 5000,
    minimumPurchase: 25000,
    applicableCourses: courseTitles.slice(0, 3),
    applicableBatches: ["FSD-2026-A", "DA-2026-B"],
    applicableUsers: [],
    startDate: dateOnly(-10),
    expiryDate: dateOnly(20),
    usageLimit: 250,
    usagePerStudent: 1,
    status: "active",
    createdBy: "Nisha Verma",
    createdAt: daysFromNow(-12),
  },
  {
    id: "cpn-3",
    code: "STUDENT10",
    name: "College student discount",
    description: "10% off for verified college students.",
    discountType: "percentage",
    discountValue: 10,
    maximumDiscount: 4000,
    applicableCourses: [],
    applicableBatches: [],
    applicableUsers: [],
    startDate: dateOnly(-60),
    expiryDate: dateOnly(120),
    usageLimit: 5000,
    usagePerStudent: 1,
    status: "active",
    createdBy: "Admin",
    createdAt: daysFromNow(-60),
  },
  {
    id: "cpn-4",
    code: "DIWALI25",
    name: "Festive campaign",
    description: "Seasonal campaign, scheduled to activate automatically.",
    discountType: "percentage",
    discountValue: 25,
    maximumDiscount: 12000,
    applicableCourses: [],
    applicableBatches: [],
    applicableUsers: [],
    startDate: dateOnly(30),
    expiryDate: dateOnly(60),
    usageLimit: 500,
    usagePerStudent: 1,
    status: "scheduled",
    createdBy: "Nisha Verma",
    createdAt: daysFromNow(-4),
  },
  {
    id: "cpn-5",
    code: "SUMMER15",
    name: "Summer intake",
    description: "Expired summer campaign retained for reporting.",
    discountType: "percentage",
    discountValue: 15,
    applicableCourses: [],
    applicableBatches: [],
    applicableUsers: [],
    startDate: dateOnly(-160),
    expiryDate: dateOnly(-60),
    usageLimit: 400,
    usagePerStudent: 1,
    status: "expired",
    createdBy: "Admin",
    createdAt: daysFromNow(-170),
  },
  {
    id: "cpn-6",
    code: "PARTNER30",
    name: "Corporate partner draft",
    description: "Draft campaign for corporate partners, not published yet.",
    discountType: "percentage",
    discountValue: 30,
    applicableCourses: [],
    applicableBatches: [],
    applicableUsers: ["corporate@partner.com"],
    startDate: dateOnly(5),
    expiryDate: dateOnly(90),
    usageLimit: 100,
    usagePerStudent: 1,
    status: "draft",
    createdBy: "Nisha Verma",
    createdAt: daysFromNow(-2),
  },
];

export const mockCouponOverview: CouponOverview = {
  totalCoupons: mockCoupons.length,
  activeCoupons: mockCoupons.filter((c) => c.status === "active").length,
  totalUsage: 1284,
  revenueGenerated: 41_260_000,
  discountGiven: 5_940_000,
  conversionRate: 31.4,
  perCoupon: mockCoupons.map((c, i) => {
    const usage = seeded(i + 3, 40, 520);
    const conversions = Math.round(usage * (seeded(i + 9, 25, 55) / 100));
    return {
      couponId: c.id,
      code: c.code,
      usage,
      revenue: usage * seeded(i + 11, 22000, 48000),
      discountGiven: usage * seeded(i + 5, 2500, 8000),
      conversions,
      conversionRate: Math.round((conversions / usage) * 1000) / 10,
    };
  }),
};

/* ---------------------------------------------------------------------- crm */

const counsellors = ["Priya Menon", "Rakesh Iyer", "Sana Qureshi", "Deepak Rao"];
const sources = [
  "Website",
  "Course Enquiry",
  "Demo Class",
  "Webinar",
  "WhatsApp",
  "Phone",
  "Referral",
  "Social Media",
  "Advertisement",
  "Other",
] as const;
const stages = [
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
] as const;

export const mockLeads: Lead[] = Array.from({ length: 42 }, (_, i) => ({
  id: `lead-${i + 1}`,
  name: name(i),
  phone: `+91 9${seeded(i, 100000000, 999999999)}`,
  email: `${pick(first, i).toLowerCase()}.${pick(last, i * 3 + 1).toLowerCase()}${i}@gmail.com`,
  courseInterest: pick(courseTitles, i),
  source: pick([...sources], i * 2),
  assignedTo: pick(counsellors, i),
  assignedToId: `staff-c${(i % counsellors.length) + 1}`,
  stage: pick([...stages], i * 3),
  lastContactAt: daysFromNow(-seeded(i + 2, 0, 14)),
  nextFollowUpAt: i % 3 === 0 ? daysFromNow(0) : daysFromNow(seeded(i, 1, 12)),
  createdAt: daysFromNow(-seeded(i + 7, 1, 60)),
  city: pick(cities, i),
  budget: seeded(i + 4, 20000, 90000),
  score: seeded(i + 6, 20, 98),
}));

export const mockLeadOverview: LeadOverview = {
  totalLeads: mockLeads.length,
  newLeads: mockLeads.filter((l) => l.stage === "New").length,
  followUpsToday: mockLeads.filter((l) => l.nextFollowUpAt?.slice(0, 10) === dateOnly(0)).length,
  demoRegistrations: mockLeads.filter((l) => l.stage === "Demo Scheduled").length,
  demoAttendees: mockLeads.filter((l) => l.stage === "Demo Attended").length,
  paymentPending: mockLeads.filter((l) => l.stage === "Payment Pending").length,
  convertedLeads: mockLeads.filter((l) => l.stage === "Converted").length,
  conversionRate:
    Math.round(
      (mockLeads.filter((l) => l.stage === "Converted").length / mockLeads.length) * 1000,
    ) / 10,
};

export function buildLeadDetail(lead: Lead): LeadDetail {
  const i = Number(lead.id.split("-")[1] ?? 1);
  return {
    ...lead,
    communications: [
      {
        id: `${lead.id}-c1`,
        leadId: lead.id,
        channel: "Call",
        summary: "Discussed curriculum, duration and fee structure.",
        by: lead.assignedTo,
        at: daysFromNow(-4),
      },
      {
        id: `${lead.id}-c2`,
        leadId: lead.id,
        channel: "WhatsApp",
        summary: "Shared brochure and demo class link.",
        by: lead.assignedTo,
        at: daysFromNow(-3),
      },
      {
        id: `${lead.id}-c3`,
        leadId: lead.id,
        channel: "Email",
        summary: "Sent fee breakdown with EARLYBIRD5000 coupon.",
        by: lead.assignedTo,
        at: daysFromNow(-1),
      },
    ],
    notes: [
      {
        id: `${lead.id}-n1`,
        leadId: lead.id,
        body: "Prefers weekend batch. Working professional.",
        by: lead.assignedTo,
        at: daysFromNow(-4),
      },
      {
        id: `${lead.id}-n2`,
        leadId: lead.id,
        body: "Comparing with another institute — highlight placement support.",
        by: lead.assignedTo,
        at: daysFromNow(-2),
      },
    ],
    followUps: [
      {
        id: `${lead.id}-f1`,
        leadId: lead.id,
        date: dateOnly(0),
        time: "16:30",
        notes: "Confirm demo attendance.",
        nextAction: "Call",
        assignedTo: lead.assignedTo,
        status: "Pending",
      },
      {
        id: `${lead.id}-f2`,
        leadId: lead.id,
        date: dateOnly(-3),
        time: "11:00",
        notes: "Introductory call.",
        nextAction: "Send brochure",
        assignedTo: lead.assignedTo,
        status: "Completed",
      },
    ],
    demoAttended: i % 2 === 0,
    enquiryHistory: [
      { id: `${lead.id}-e1`, subject: `Enquiry about ${lead.courseInterest}`, at: lead.createdAt },
    ],
    paymentHistory:
      lead.stage === "Converted"
        ? [{ id: `${lead.id}-p1`, amount: 35000, status: "Paid", at: daysFromNow(-2) }]
        : lead.stage === "Payment Pending"
          ? [{ id: `${lead.id}-p1`, amount: 5000, status: "Partial", at: daysFromNow(-1) }]
          : [],
    enrollmentStatus:
      lead.stage === "Converted"
        ? "Enrolled"
        : lead.stage === "Payment Pending"
          ? "Payment Pending"
          : "Not Enrolled",
  };
}

/* ------------------------------------------------------------------ support */

const categories = [
  "Course",
  "Technical Issue",
  "Payment",
  "Account",
  "Certificate",
  "Class",
  "Attendance",
  "Assignment",
  "Coding",
  "Career",
  "Job",
  "Resume",
  "Other",
] as const;
const priorities = ["Low", "Medium", "High", "Urgent"] as const;
const ticketStatuses = [
  "Open",
  "Assigned",
  "In Progress",
  "Waiting for Student",
  "Resolved",
  "Closed",
] as const;
const agents = ["Sana Qureshi", "Rahul Verma", "Support Desk", "Meera Krishnan"];

export const mockTickets: SupportTicket[] = Array.from({ length: 24 }, (_, i) => {
  const status = pick([...ticketStatuses], i);
  return {
    id: `tkt-${i + 1}`,
    reference: `LTX-T-${2400 + i}`,
    subject: pick(
      [
        "Unable to access recorded class",
        "Payment deducted but enrollment not showing",
        "Certificate name spelling correction",
        "Coding compiler timing out",
        "Attendance not marked for last session",
        "Resume review request",
        "Assignment submission failed",
        "Job application status unclear",
      ],
      i,
    ),
    category: pick([...categories], i),
    description: "Detailed description provided by the student when raising the ticket.",
    priority: pick([...priorities], i * 3),
    status,
    studentId: `stu-${(i % 12) + 1}`,
    studentName: name(i + 2),
    assignedTo: status === "Open" ? undefined : pick(agents, i),
    assignedRole: status === "Open" ? undefined : "Support Agent",
    createdAt: daysFromNow(-seeded(i, 0, 10)),
    updatedAt: daysFromNow(-seeded(i + 1, 0, 3)),
    attachments:
      i % 4 === 0 ? [{ id: `att-${i}`, name: "screenshot.png", sizeKb: 480, kind: "image" }] : [],
    escalatedTo: i % 7 === 0 ? "Admin" : undefined,
  };
});

export const mockSupportOverview: SupportOverview = {
  openTickets: mockTickets.filter(
    (t) => t.status === "Open" || t.status === "Assigned" || t.status === "In Progress",
  ).length,
  urgentTickets: mockTickets.filter((t) => t.priority === "Urgent").length,
  unassignedTickets: mockTickets.filter((t) => !t.assignedTo).length,
  myTickets: 6,
  resolvedToday: 4,
  averageResolutionHours: 7.4,
};

export function buildTicketDetail(ticket: SupportTicket): TicketDetail {
  const messages: TicketMessage[] = [
    {
      id: `${ticket.id}-m1`,
      ticketId: ticket.id,
      author: ticket.studentName,
      authorRole: "student",
      body: ticket.description,
      at: ticket.createdAt,
      internal: false,
      attachments: ticket.attachments,
    },
    {
      id: `${ticket.id}-m2`,
      ticketId: ticket.id,
      author: ticket.assignedTo ?? "Support Desk",
      authorRole: "staff",
      body: "Thanks for reaching out — we are checking this and will update you shortly.",
      at: daysFromNow(-1),
      internal: false,
      attachments: [],
    },
    {
      id: `${ticket.id}-m3`,
      ticketId: ticket.id,
      author: ticket.assignedTo ?? "Support Desk",
      authorRole: "staff",
      body: "Internal: student has been contacted by phone, awaiting confirmation from finance.",
      at: daysFromNow(-1),
      internal: true,
      attachments: [],
    },
  ];
  return { ...ticket, messages };
}

/* ------------------------------------------------------------------- mentor */

export const mockMentors: Mentor[] = [
  {
    id: "mnt-1",
    name: "Kavya Raghunathan",
    email: "mentor@learntrix.com",
    bio: "Engineering leader turned mentor. Guides learners on career strategy, project depth and interview readiness.",
    specialization: ["Career Strategy", "Full Stack", "System Design"],
    experienceYears: 11,
    availability: "Mon–Fri, 6–9 PM IST",
    studentsAssigned: 24,
    status: "active",
  },
  {
    id: "mnt-2",
    name: "Rohit Malhotra",
    email: "rohit.mentor@learntrix.com",
    bio: "Data leader mentoring analysts on portfolio projects and analytics interviews.",
    specialization: ["Data Analytics", "SQL", "Storytelling"],
    experienceYears: 9,
    availability: "Tue–Sat, 7–10 PM IST",
    studentsAssigned: 18,
    status: "active",
  },
  {
    id: "mnt-3",
    name: "Farah Siddiqui",
    email: "farah.mentor@learntrix.com",
    bio: "Product engineer focused on communication coaching and mock interviews.",
    specialization: ["Interview Prep", "Communication", "Frontend"],
    experienceYears: 7,
    availability: "Weekends, 10 AM–2 PM IST",
    studentsAssigned: 15,
    status: "active",
  },
];

export const mockMentorAssignments: MentorAssignment[] = [
  {
    id: "ma-1",
    mentorId: "mnt-1",
    mentorName: "Kavya Raghunathan",
    scope: "batch",
    targetId: "FSD-2026-A",
    targetName: "Full Stack 2026-A",
    assignedAt: daysFromNow(-40),
    assignedBy: "Nisha Verma",
  },
  {
    id: "ma-2",
    mentorId: "mnt-2",
    mentorName: "Rohit Malhotra",
    scope: "course",
    targetId: "c-2",
    targetName: courseTitles[1] ?? "Data Analytics",
    assignedAt: daysFromNow(-30),
    assignedBy: "Nisha Verma",
  },
  {
    id: "ma-3",
    mentorId: "mnt-3",
    mentorName: "Farah Siddiqui",
    scope: "student",
    targetId: "stu-4",
    targetName: name(4),
    assignedAt: daysFromNow(-12),
    assignedBy: "Nisha Verma",
  },
];

export const mockMentorStudents: MentorStudentRow[] = Array.from({ length: 14 }, (_, i) => {
  const attendance = seeded(i + 1, 55, 99);
  const progress = seeded(i + 5, 25, 96);
  return {
    id: `stu-${i + 1}`,
    name: name(i + 1),
    courseTitle: pick(courseTitles, i),
    batchName: pick(["FSD-2026-A", "DA-2026-B", "AI-2026-C"], i),
    progressPercent: progress,
    attendancePercent: attendance,
    careerReadiness: seeded(i + 8, 30, 95),
    quizScore: seeded(i + 3, 45, 96),
    codingScore: seeded(i + 6, 20, 94),
    lastSessionAt: daysFromNow(-seeded(i, 1, 20)),
    nextSessionAt: i % 3 === 0 ? daysFromNow(seeded(i, 1, 9)) : undefined,
    status: attendance < 70 ? "at_risk" : progress < 35 ? "inactive" : "on_track",
  };
});

export const mockMentorStats: MentorStats = {
  totalStudents: mockMentorStudents.length,
  activeStudents: mockMentorStudents.filter((s) => s.status === "on_track").length,
  upcomingSessions: 6,
  pendingTasks: 9,
  studentsAtRisk: mockMentorStudents.filter((s) => s.status !== "on_track").length,
  averageCareerReadiness: Math.round(
    mockMentorStudents.reduce((a, s) => a + s.careerReadiness, 0) / mockMentorStudents.length,
  ),
  averageProgress: Math.round(
    mockMentorStudents.reduce((a, s) => a + s.progressPercent, 0) / mockMentorStudents.length,
  ),
};

export const mockMentorSessions: MentorSession[] = Array.from({ length: 12 }, (_, i) => ({
  id: `ses-${i + 1}`,
  title: pick(
    [
      "Career roadmap review",
      "Project architecture review",
      "Mock interview",
      "Resume walkthrough",
      "Weekly check-in",
    ],
    i,
  ),
  kind: pick(
    [
      "Mentoring",
      "Career Session",
      "Project Review",
      "Interview Preparation",
      "Follow-up",
    ] as const,
    i,
  ),
  mentorId: "mnt-1",
  studentId: `stu-${(i % 14) + 1}`,
  studentName: name(i + 1),
  date: dateOnly(i < 6 ? i + 1 : -(i - 5)),
  time: pick(["18:30", "19:00", "20:00", "11:00"], i),
  durationMinutes: pick([30, 45, 60], i),
  platform: pick(["Google Meet", "Zoom", "Microsoft Teams"] as const, i),
  meetingUrl: "https://meet.google.com/ltx-mentor",
  agenda: "Review progress, unblock issues and set next actions.",
  notes: i > 5 ? "Discussed DSA plan; student to complete 20 problems." : undefined,
  status: i < 6 ? "Scheduled" : pick(["Completed", "Rescheduled", "Cancelled"] as const, i),
}));

export const mockMentorNotes: MentorNote[] = [
  {
    id: "mn-1",
    mentorId: "mnt-1",
    studentId: "stu-1",
    studentName: name(1),
    category: "Strengths",
    body: "Strong SQL fundamentals and consistent attendance.",
    at: daysFromNow(-6),
    private: true,
  },
  {
    id: "mn-2",
    mentorId: "mnt-1",
    studentId: "stu-1",
    studentName: name(1),
    category: "Technical Gaps",
    body: "Weak on system design tradeoffs; needs structured practice.",
    at: daysFromNow(-6),
    private: true,
  },
  {
    id: "mn-3",
    mentorId: "mnt-1",
    studentId: "stu-3",
    studentName: name(3),
    category: "Communication",
    body: "Hesitant in mock interviews — schedule two speaking drills.",
    at: daysFromNow(-3),
    private: true,
  },
  {
    id: "mn-4",
    mentorId: "mnt-1",
    studentId: "stu-5",
    studentName: name(5),
    category: "Career Goals",
    body: "Targeting data analyst roles in Bengaluru by Q4.",
    at: daysFromNow(-2),
    private: false,
  },
];

export const mockMentorActionItems: MentorActionItem[] = [
  {
    id: "ai-1",
    mentorId: "mnt-1",
    studentId: "stu-1",
    studentName: name(1),
    task: "Complete 20 coding problems",
    deadline: dateOnly(11),
    status: "In Progress",
  },
  {
    id: "ai-2",
    mentorId: "mnt-1",
    studentId: "stu-1",
    studentName: name(1),
    task: "Complete resume",
    deadline: dateOnly(6),
    status: "Not Started",
  },
  {
    id: "ai-3",
    mentorId: "mnt-1",
    studentId: "stu-3",
    studentName: name(3),
    task: "Ship capstone project README",
    deadline: dateOnly(-2),
    status: "Overdue",
  },
  {
    id: "ai-4",
    mentorId: "mnt-1",
    studentId: "stu-5",
    studentName: name(5),
    task: "Attend 2 mock interviews",
    deadline: dateOnly(9),
    status: "In Progress",
  },
];

export const mockMentorAlerts: MentorAlert[] = [
  {
    id: "al-1",
    kind: "Low attendance",
    studentName: name(2),
    detail: "Attendance dropped to 62% over the last 14 days.",
    severity: "critical",
    at: daysFromNow(-1),
  },
  {
    id: "al-2",
    kind: "No coding activity",
    studentName: name(4),
    detail: "No coding submissions in 9 days.",
    severity: "warning",
    at: daysFromNow(-1),
  },
  {
    id: "al-3",
    kind: "Upcoming interview",
    studentName: name(6),
    detail: "Technical interview with Fintrail in 2 days.",
    severity: "info",
    at: daysFromNow(0),
  },
  {
    id: "al-4",
    kind: "Missed mentor session",
    studentName: name(8),
    detail: "Missed the weekly check-in on " + dateOnly(-2),
    severity: "warning",
    at: daysFromNow(-2),
  },
  {
    id: "al-5",
    kind: "Low quiz performance",
    studentName: name(9),
    detail: "Last three quiz scores below 50%.",
    severity: "warning",
    at: daysFromNow(-3),
  },
];

/* ---------------------------------------------------------------- placement */

export const mockPlacementStats: PlacementStats = {
  eligibleStudents: 126,
  activeJobs: 18,
  placementDrives: 7,
  applications: 342,
  shortlisted: 128,
  interviews: 74,
  offers: 41,
  placements: 33,
};

export const mockEligibleStudents: PlacementEligibleStudent[] = Array.from(
  { length: 20 },
  (_, i) => {
    const attendance = seeded(i + 2, 62, 99);
    const progress = seeded(i + 4, 40, 100);
    const coding = seeded(i + 7, 30, 98);
    const readiness = Math.round((attendance + progress + coding) / 3);
    return {
      id: `stu-${i + 1}`,
      name: name(i + 1),
      courseTitle: pick(courseTitles, i),
      batchName: pick(["FSD-2026-A", "DA-2026-B", "AI-2026-C"], i),
      skills: [pick(skillPool, i), pick(skillPool, i + 3), pick(skillPool, i + 6)],
      attendancePercent: attendance,
      progressPercent: progress,
      codingScore: coding,
      resumeStatus: pick(["Not Started", "In Progress", "Ready", "ATS Verified"] as const, i),
      careerReadiness: readiness,
      eligibility: readiness >= 75 ? "eligible" : readiness >= 60 ? "conditional" : "not_eligible",
      placementStatus: pick(
        ["Not Applied", "Applied", "Interviewing", "Offer", "Placed"] as const,
        i,
      ),
    };
  },
);

export const mockDrives: PlacementDrive[] = Array.from({ length: 7 }, (_, i) => ({
  id: `drv-${i + 1}`,
  companyName: pick(companies, i),
  role: pick(roles, i),
  description:
    "Campus-style hiring drive with an online assessment followed by technical and HR rounds.",
  eligibilitySummary: "≥ 80% attendance · ≥ 70% course progress · coding score ≥ 60",
  applicationDeadline: dateOnly(seeded(i, 3, 21)),
  assessmentDate: dateOnly(seeded(i + 1, 22, 30)),
  interviewDate: dateOnly(seeded(i + 2, 31, 45)),
  openings: seeded(i + 3, 3, 25),
  eligibleStudents: seeded(i + 4, 40, 160),
  registeredStudents: seeded(i + 5, 10, 90),
  stage: pick(
    [
      "Published",
      "Registration",
      "Assessment",
      "Shortlisting",
      "Interview",
      "Offer",
      "Draft",
    ] as const,
    i,
  ),
  location: pick(cities, i),
  ctcRange: `₹${seeded(i, 4, 8)}–${seeded(i, 9, 16)} LPA`,
}));

export const mockPlacementApplications: PlacementApplication[] = Array.from(
  { length: 28 },
  (_, i) => ({
    id: `papp-${i + 1}`,
    studentId: `stu-${(i % 20) + 1}`,
    studentName: name(i + 1),
    companyName: pick(companies, i),
    role: pick(roles, i * 2),
    courseTitle: pick(courseTitles, i),
    eligibility: pick(["eligible", "eligible", "conditional"] as const, i),
    appliedAt: daysFromNow(-seeded(i, 1, 30)),
    status: pick(
      [
        "Applied",
        "Assessment",
        "Shortlisted",
        "Interview",
        "Final Round",
        "Selected",
        "Rejected",
        "Withdrawn",
      ] as const,
      i,
    ),
    interviewAt: i % 3 === 0 ? daysFromNow(seeded(i, 1, 10)) : undefined,
    result: i % 5 === 0 ? "Cleared round 1" : undefined,
    driveId: `drv-${(i % 7) + 1}`,
  }),
);

export const mockInterviews: PlacementInterview[] = Array.from({ length: 12 }, (_, i) => ({
  id: `int-${i + 1}`,
  studentId: `stu-${(i % 20) + 1}`,
  studentName: name(i + 1),
  companyName: pick(companies, i),
  role: pick(roles, i),
  round: pick(
    ["Technical Interview", "HR Interview", "Managerial Interview", "Mock Interview"] as const,
    i,
  ),
  date: dateOnly(i < 7 ? i + 1 : -(i - 6)),
  time: pick(["10:00", "11:30", "15:00", "17:30"], i),
  interviewer: pick(["Priyanka Shah", "Arun Kumar", "Hiring Panel", "Neeraj Bhat"], i),
  platform: pick(["Google Meet", "Zoom", "Microsoft Teams"] as const, i),
  meetingUrl: "https://meet.google.com/ltx-interview",
  status: i < 7 ? "Scheduled" : pick(["Completed", "Rescheduled", "Cancelled"] as const, i),
}));

export const mockOffers: PlacementOffer[] = Array.from({ length: 10 }, (_, i) => ({
  id: `off-${i + 1}`,
  studentId: `stu-${(i % 20) + 1}`,
  studentName: name(i + 2),
  companyName: pick(companies, i),
  role: pick(roles, i),
  offerDate: daysFromNow(-seeded(i, 1, 40)),
  joiningDate: dateOnly(seeded(i, 10, 70)),
  annualSalary: seeded(i, 450000, 1600000),
  location: pick(cities, i),
  status: pick(
    ["Offer Received", "Accepted", "Declined", "Joined", "Placement Verified"] as const,
    i,
  ),
  verifiedBy: i % 3 === 0 ? "Placement Office" : undefined,
}));

export const mockPlacementAnalytics: PlacementAnalytics = {
  totalEligible: 126,
  totalApplied: 342,
  shortlisted: 128,
  interviewed: 74,
  offers: 41,
  joined: 33,
  placementRate: 62.4,
  byCourse: courseTitles.map((c, i) => ({
    label: c,
    placements: seeded(i, 4, 28),
    eligible: seeded(i + 2, 20, 60),
  })),
  byBatch: ["FSD-2026-A", "DA-2026-B", "AI-2026-C", "FSD-2025-D"].map((b, i) => ({
    label: b,
    placements: seeded(i + 3, 5, 24),
    eligible: seeded(i + 5, 18, 50),
  })),
  byCompany: companies.map((c, i) => ({ label: c, placements: seeded(i + 7, 2, 14) })),
  byRole: roles.map((r, i) => ({ label: r, placements: seeded(i + 9, 3, 18) })),
  skillDemand: skillPool.slice(0, 8).map((s, i) => ({ label: s, demand: seeded(i + 11, 20, 96) })),
};

/* -------------------------------------------------------------- permissions */

export const mockRoleDefinitions: RoleDefinition[] = [
  {
    role: "super_admin",
    label: "Super Admin",
    description: "Unrestricted access across every module and configuration.",
    permissions: [
      "platform.manage",
      "users.manage",
      "coupons.manage",
      "crm.view",
      "crm.manage",
      "crm.assign",
      "support.view",
      "support.manage",
      "support.internal_notes",
      "mentor.students",
      "mentor.sessions",
      "mentor.assign",
      "placement.students.view",
      "placement.jobs.view",
      "placement.jobs.manage",
      "placement.jobs.publish",
      "placement.drives.manage",
      "placement.applications.manage",
      "placement.interviews.manage",
      "placement.offers.record",
      "placement.salary.view",
      "audit.view",
    ],
  },
  {
    role: "admin",
    label: "Admin",
    description: "Platform management: students, staff, content, marketing and governance.",
    permissions: [
      "platform.manage",
      "teaching.manage",
      "learning.view",
      "users.manage",
      "coupons.manage",
      "crm.view",
      "crm.manage",
      "crm.assign",
      "support.view",
      "support.manage",
      "support.internal_notes",
      "mentor.assign",
      "placement.students.view",
      "placement.jobs.view",
      "placement.jobs.manage",
      "placement.jobs.publish",
      "placement.drives.manage",
      "placement.applications.manage",
      "placement.interviews.manage",
      "placement.offers.record",
      "placement.salary.view",
      "audit.view",
    ],
  },
  {
    role: "teacher",
    label: "Teacher",
    description: "Assigned courses, batches and students.",
    permissions: [
      "teaching.manage",
      "learning.view",
      "mentor.students",
      "support.view",
      "support.internal_notes",
    ],
  },
  {
    role: "mentor",
    label: "Mentor",
    description: "Assigned students: guidance, sessions, notes and action plans.",
    permissions: ["mentor.students", "mentor.sessions", "learning.view", "support.view", "support.internal_notes"],
  },
  {
    role: "placement_officer",
    label: "Placement Officer",
    description:
      "Jobs, companies, drives, applications, interviews and offers. No platform administration.",
    permissions: [
      "placement.students.view",
      "placement.jobs.view",
      "placement.jobs.manage",
      "placement.jobs.publish",
      "placement.drives.manage",
      "placement.applications.manage",
      "placement.interviews.manage",
      "placement.offers.record",
      "learning.view",
      "support.view",
    ],
  },
  {
    role: "support_agent",
    label: "Support Agent",
    description: "Support tickets and student communication only.",
    permissions: ["support.view", "support.manage", "support.internal_notes"],
  },
  {
    role: "counsellor",
    label: "Counsellor",
    description: "CRM leads assigned to them, follow-ups and conversions.",
    permissions: ["crm.view", "crm.manage"],
  },
  {
    role: "student",
    label: "Student",
    description: "Own learning, support tickets, mentor and career data.",
    permissions: ["learning.own", "career.own"],
  },
];

export const mockStaff: StaffMember[] = [
  {
    id: "staff-c1",
    name: "Priya Menon",
    email: "priya.counsellor@learntrix.com",
    role: "counsellor",
    status: "active",
    assignedCount: 14,
    createdAt: daysFromNow(-200),
  },
  {
    id: "staff-c2",
    name: "Rakesh Iyer",
    email: "rakesh.counsellor@learntrix.com",
    role: "counsellor",
    status: "active",
    assignedCount: 11,
    createdAt: daysFromNow(-160),
  },
  {
    id: "staff-c3",
    name: "Sana Qureshi",
    email: "sana@learntrix.com",
    role: "support_agent",
    status: "active",
    assignedCount: 22,
    createdAt: daysFromNow(-140),
  },
  {
    id: "staff-c4",
    name: "Deepak Rao",
    email: "deepak.counsellor@learntrix.com",
    role: "counsellor",
    status: "inactive",
    assignedCount: 3,
    createdAt: daysFromNow(-90),
  },
  {
    id: "staff-p1",
    name: "Anita Deshmukh",
    email: "placement@learntrix.com",
    role: "placement_officer",
    status: "active",
    assignedCount: 126,
    createdAt: daysFromNow(-220),
  },
  {
    id: "staff-p2",
    name: "Vikram Sethi",
    email: "vikram.placement@learntrix.com",
    role: "placement_officer",
    status: "active",
    assignedCount: 64,
    createdAt: daysFromNow(-100),
  },
  {
    id: "staff-m1",
    name: "Kavya Raghunathan",
    email: "mentor@learntrix.com",
    role: "mentor",
    status: "active",
    assignedCount: 24,
    createdAt: daysFromNow(-300),
  },
  {
    id: "staff-m2",
    name: "Rohit Malhotra",
    email: "rohit.mentor@learntrix.com",
    role: "mentor",
    status: "active",
    assignedCount: 18,
    createdAt: daysFromNow(-180),
  },
];
