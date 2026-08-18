import { mockCourses } from "@/mock/courses";
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
  WhatsAppSettings,
} from "@/types/admin";

const courseTitles = mockCourses.slice(0, 6).map((c) => c.title);
const trainers = ["Durga Prasad", "Meera Krishnan", "Vikram Sethi", "Ananya Bose"];
const colleges = ["VIT Vellore", "Osmania University", "NIT Warangal", "Anna University", "IIIT Hyderabad"];
const cities = ["Hyderabad", "Bengaluru", "Chennai", "Pune", "Delhi NCR", "Remote"];

const firstNames = [
  "Aarav", "Ishita", "Rohan", "Sneha", "Karthik", "Priya", "Aditya", "Fatima",
  "Neha", "Vivek", "Ananya", "Rahul", "Divya", "Sanjay", "Meghna", "Arjun",
  "Tanvi", "Nikhil", "Pooja", "Harsh", "Riya", "Manoj", "Swathi", "Imran",
];
const lastNames = ["Sharma", "Nair", "Gupta", "Reddy", "Iyer", "Menon", "Rao", "Khan"];

function pick<T>(arr: T[], i: number): T {
  return arr[i % arr.length]!;
}

function seeded(i: number, min: number, max: number) {
  const x = Math.abs(Math.sin(i * 12.9898) * 43758.5453);
  return Math.round(min + (x % 1) * (max - min));
}

export const mockAdminStudents: AdminStudentRow[] = Array.from({ length: 24 }).map((_, i) => {
  const name = `${pick(firstNames, i)} ${pick(lastNames, i * 3)}`;
  const attendance = seeded(i + 1, 52, 99);
  const progress = seeded(i + 7, 12, 100);
  const coding = seeded(i + 11, 30, 98);
  const quiz = seeded(i + 17, 38, 97);
  const profile = seeded(i + 23, 45, 100);
  const careerStatus =
    attendance >= 80 && progress >= 70 && coding >= 70
      ? "eligible"
      : attendance >= 70 && progress >= 50
        ? "conditional"
        : i % 7 === 0
          ? "under_review"
          : "not_eligible";
  const status = (["active", "active", "active", "pending", "active", "suspended", "inactive", "active"] as const)[i % 8]!;
  return {
    id: `stu-${i + 1}`,
    studentId: `LTX-2026-${(400 + i).toString().padStart(4, "0")}`,
    name,
    email: `${name.split(" ")[0]!.toLowerCase()}.${i + 1}@example.com`,
    phone: `+91 9${seeded(i + 31, 100000000, 899999999)}`,
    courseTitle: pick(courseTitles, i),
    batchName: `B-${2026}-${(i % 6) + 1}`,
    trainerName: pick(trainers, i),
    status,
    statusReason: status === "suspended" ? "Fee default — pending finance review" : undefined,
    attendancePercent: attendance,
    progressPercent: progress,
    quizScore: quiz,
    codingScore: coding,
    assignmentCompletion: seeded(i + 41, 20, 100),
    profileCompletion: profile,
    points: seeded(i + 53, 400, 13000),
    rank: i + 1,
    careerStatus,
    paymentStatus: (["paid", "paid", "partial", "pending", "paid", "refunded"] as const)[i % 6]!,
    placementStatus: (["job_seeking", "interviewing", "not_started", "placed", "offered", "job_seeking"] as const)[i % 6]!,
    location: pick(cities, i),
    college: pick(colleges, i),
    graduationYear: 2023 + (i % 4),
    enrolledAt: `2026-0${(i % 8) + 1}-1${i % 9}T09:00:00.000Z`,
  };
});

export function mockAdminStudentDetail(id: string): AdminStudentDetail {
  const row = mockAdminStudents.find((s) => s.id === id) ?? mockAdminStudents[0]!;
  return {
    ...row,
    dateOfBirth: "2002-04-18",
    gender: "Prefer not to say",
    qualification: "B.Tech Computer Science",
    skills: ["Java", "Spring Boot", "React", "SQL", "AWS"],
    links: {
      github: "https://github.com/example",
      linkedin: "https://linkedin.com/in/example",
      leetcode: "https://leetcode.com/example",
      hackerrank: "https://hackerrank.com/example",
    },
    documents: [
      { id: "d1", kind: "profile_photo", name: "profile.jpg", uploadedAt: "2026-06-02", reviewStatus: "approved" },
      { id: "d2", kind: "resume", name: "resume-v3.pdf", uploadedAt: "2026-08-04", reviewStatus: "pending" },
      { id: "d3", kind: "certificate", name: "aws-cloud-practitioner.pdf", uploadedAt: "2026-07-19", reviewStatus: "approved" },
      { id: "d4", kind: "id_proof", name: "college-id.png", uploadedAt: "2026-05-28", reviewStatus: "approved" },
    ],
    timeline: [
      { id: "t1", stage: "Registration", detail: "Self-registered via website", occurredAt: "2026-01-12", state: "done" },
      { id: "t2", stage: "Approval", detail: "Approved by Nisha Verma", occurredAt: "2026-01-13", state: "done" },
      { id: "t3", stage: "Payment", detail: `${row.paymentStatus} · installment plan`, occurredAt: "2026-01-14", state: "done" },
      { id: "t4", stage: "Batch assignment", detail: `${row.batchName} · ${row.trainerName}`, occurredAt: "2026-01-15", state: "done" },
      { id: "t5", stage: "Learning", detail: `${row.progressPercent}% course progress`, occurredAt: "2026-08-18", state: "current" },
      { id: "t6", stage: "Career eligibility", detail: row.careerStatus.replace("_", " "), occurredAt: "2026-08-18", state: row.careerStatus === "eligible" ? "done" : "current" },
      { id: "t7", stage: "Placement", detail: "Awaiting verified offer", occurredAt: "—", state: "upcoming" },
    ],
    enrollments: [
      {
        id: "en1",
        courseTitle: row.courseTitle,
        batchName: row.batchName,
        trainerName: row.trainerName,
        startDate: "2026-01-20",
        endDate: "2026-09-20",
        courseStatus: row.progressPercent >= 100 ? "completed" : "in_progress",
        paymentStatus: row.paymentStatus,
        careerEligible: row.careerStatus === "eligible",
      },
    ],
    attendance: Array.from({ length: 8 }).map((_, i) => ({
      date: `2026-08-${(10 + i).toString().padStart(2, "0")}`,
      classTitle: `Module ${i + 3} · Session ${i + 1}`,
      batchName: row.batchName,
      status: (["present", "present", "late", "present", "absent", "present", "excused", "present"] as const)[i]!,
    })),
    assessments: [
      { id: "as1", kind: "quiz", title: "Module 4 Quiz", score: row.quizScore, submittedAt: "2026-08-12" },
      { id: "as2", kind: "assignment", title: "REST API assignment", score: row.assignmentCompletion, submittedAt: "2026-08-09" },
      { id: "as3", kind: "coding", title: "DSA Sprint 3", score: row.codingScore, submittedAt: "2026-08-15" },
    ],
    applications: [
      { id: "ap1", company: "Microsoft", role: "SDE Intern", appliedAt: "2026-08-01", status: "Interview" },
      { id: "ap2", company: "Wipro", role: "Java Developer", appliedAt: "2026-07-22", status: "Shortlisted" },
    ],
    payments: [
      { id: "p1", description: "Installment 1", amount: 45000, status: "paid", paidAt: "2026-01-14" },
      { id: "p2", description: "Installment 2", amount: 45000, status: row.paymentStatus, paidAt: "2026-06-14" },
    ],
    certificates: [{ id: "c1", title: "Foundations of Programming", issuedAt: "2026-04-02" }],
    activity: [
      { id: "ac1", label: "Completed lesson · Spring Security", occurredAt: "2026-08-18 09:12" },
      { id: "ac2", label: "Solved 3 coding problems", occurredAt: "2026-08-17 20:40" },
      { id: "ac3", label: "Uploaded resume-v3.pdf", occurredAt: "2026-08-04 11:02" },
    ],
  };
}

export const mockBatches: AdminBatch[] = Array.from({ length: 8 }).map((_, i) => ({
  id: `batch-${i + 1}`,
  name: `B-2026-${i + 1}`,
  courseTitle: pick(courseTitles, i),
  trainerName: pick(trainers, i),
  startDate: `2026-0${(i % 8) + 1}-05`,
  endDate: `2026-1${i % 3}-20`,
  days: i % 2 ? "Mon, Wed, Fri" : "Tue, Thu, Sat",
  time: i % 2 ? "07:00 – 08:30" : "19:00 – 20:30",
  mode: (["Online", "Hybrid", "Online", "Offline"] as const)[i % 4]!,
  platform: (["Google Meet", "Zoom", "Microsoft Teams", "Google Meet"] as const)[i % 4]!,
  meetingUrl: "https://meet.google.com/batch-link",
  capacity: 60,
  enrolled: seeded(i + 3, 18, 60),
  status: (["Active", "Active", "Upcoming", "Completed", "Active", "Cancelled"] as const)[i % 6]!,
  published: i % 5 !== 0,
}));

export const mockTrainers: AdminTrainer[] = [
  "Durga Prasad",
  "Meera Krishnan",
  "Vikram Sethi",
  "Ananya Bose",
  "Rakesh Menon",
  "Sara Thomas",
].map((name, i) => ({
  id: `trn-${i + 1}`,
  name,
  email: `${name.split(" ")[0]!.toLowerCase()}@learntrix.com`,
  phone: `+91 98${seeded(i + 5, 10000000, 89999999)}`,
  headline: pick(["Full Stack Architect", "Data Science Lead", "Cloud & DevOps Coach", "DSA Mentor"], i),
  approvalStatus: (["approved", "approved", "pending", "approved", "approved", "pending"] as const)[i]!,
  skills: pick([["Java", "Spring"], ["Python", "ML"], ["AWS", "Kubernetes"], ["DSA", "System Design"]], i),
  courses: [pick(courseTitles, i), pick(courseTitles, i + 2)],
  batches: seeded(i + 9, 1, 5),
  students: seeded(i + 13, 40, 220),
  classesThisMonth: seeded(i + 19, 6, 32),
  rating: Number((4.2 + (i % 6) * 0.12).toFixed(2)),
  experienceYears: 6 + i,
  status: i === 5 ? "inactive" : "active",
}));

export const mockAttendanceSummary: AttendanceSummaryRow[] = mockAdminStudents.slice(0, 16).map((s, i) => {
  const total = 48;
  const present = Math.round((s.attendancePercent / 100) * total);
  const late = seeded(i + 2, 0, 5);
  const excused = seeded(i + 4, 0, 3);
  return {
    studentId: s.id,
    studentName: s.name,
    courseTitle: s.courseTitle,
    batchName: s.batchName,
    trainerName: s.trainerName,
    totalClasses: total,
    present,
    absent: Math.max(total - present - late - excused, 0),
    late,
    excused,
    attendancePercent: s.attendancePercent,
  };
});

export const mockEligibilityRules: EligibilityRuleSet[] = [
  {
    id: "rule-platform",
    name: "Platform baseline eligibility",
    scope: "platform",
    active: true,
    thresholds: {
      attendance: 80,
      courseProgress: 70,
      quizScore: 60,
      codingScore: 70,
      assignmentCompletion: 70,
      profileCompletion: 90,
      points: 3000,
      mockInterviewScore: 60,
    },
    updatedAt: "2026-08-10T09:00:00.000Z",
  },
  {
    id: "rule-microsoft",
    name: "Microsoft preparation track",
    scope: "company",
    companyId: "co-1",
    active: true,
    thresholds: { attendance: 85, codingScore: 75, courseProgress: 75 },
    updatedAt: "2026-08-14T09:00:00.000Z",
  },
  {
    id: "rule-google",
    name: "Google opportunity track",
    scope: "company",
    companyId: "co-2",
    active: true,
    thresholds: { attendance: 90, codingScore: 80, courseProgress: 75, quizScore: 70 },
    updatedAt: "2026-08-15T09:00:00.000Z",
  },
  {
    id: "rule-service",
    name: "Service company track",
    scope: "company",
    companyId: "co-3",
    active: false,
    thresholds: { attendance: 80 },
    updatedAt: "2026-07-30T09:00:00.000Z",
  },
];

export const mockAdminCompanies: AdminCompany[] = [
  {
    id: "co-1",
    name: "Microsoft",
    website: "https://microsoft.com",
    industry: "Technology",
    description: "Preparation track covering DSA, system design and behavioural rounds.",
    locations: ["Hyderabad", "Bengaluru"],
    roles: ["SDE", "SDE Intern", "Data Engineer"],
    officialPartner: false,
    verified: false,
    preparationTrack: true,
    openRoles: 3,
  },
  {
    id: "co-2",
    name: "Google",
    website: "https://google.com",
    industry: "Technology",
    description: "Interview preparation content only. Not an official hiring partner.",
    locations: ["Bengaluru", "Remote"],
    roles: ["SWE", "SRE"],
    officialPartner: false,
    verified: false,
    preparationTrack: true,
    openRoles: 1,
  },
  {
    id: "co-3",
    name: "Wipro",
    website: "https://wipro.com",
    industry: "IT Services",
    description: "Verified hiring partner for Full Stack and Cloud programs.",
    locations: ["Hyderabad", "Pune", "Chennai"],
    roles: ["Java Developer", "Cloud Associate"],
    officialPartner: true,
    verified: true,
    preparationTrack: true,
    openRoles: 6,
  },
  {
    id: "co-4",
    name: "Zoho",
    website: "https://zoho.com",
    industry: "Product",
    description: "Preparation track for product engineering interviews.",
    locations: ["Chennai"],
    roles: ["Member Technical Staff"],
    officialPartner: false,
    verified: true,
    preparationTrack: true,
    openRoles: 2,
  },
];

export const mockAdminJobs: AdminJob[] = Array.from({ length: 9 }).map((_, i) => ({
  id: `job-${i + 1}`,
  title: pick(["Java Developer", "SDE Intern", "Data Analyst", "Cloud Engineer", "Frontend Engineer"], i),
  companyName: pick(mockAdminCompanies.map((c) => c.name), i),
  location: pick(cities, i),
  workMode: (["Hybrid", "Remote", "Onsite"] as const)[i % 3]!,
  jobType: (["Full Time", "Internship", "Full Time", "Contract"] as const)[i % 4]!,
  salaryRange: pick(["₹6–9 LPA", "₹25k/mo stipend", "₹10–14 LPA", "₹4.5–7 LPA"], i),
  experience: pick(["0–1 yrs", "Fresher", "1–3 yrs"], i),
  skills: pick([["Java", "Spring Boot"], ["React", "TypeScript"], ["Python", "SQL"], ["AWS", "Terraform"]], i),
  eligibleCourses: [pick(courseTitles, i), pick(courseTitles, i + 1)],
  minAttendance: [80, 85, 75, 90][i % 4]!,
  minCodingScore: [70, 75, 60, 80][i % 4]!,
  minCourseProgress: [70, 75, 60, 80][i % 4]!,
  deadline: `2026-09-${(5 + i).toString().padStart(2, "0")}`,
  applicationMethod: (["Internal", "External URL", "Referral"] as const)[i % 3]!,
  status: (["Published", "Published", "Draft", "Published", "Expired", "Unpublished"] as const)[i % 6]!,
  applicants: seeded(i + 6, 4, 180),
  eligibleStudents: seeded(i + 8, 20, 420),
}));

export const mockAdminApplications: AdminApplication[] = Array.from({ length: 14 }).map((_, i) => {
  const student = mockAdminStudents[i]!;
  const job = mockAdminJobs[i % mockAdminJobs.length]!;
  return {
    id: `app-${i + 1}`,
    studentName: student.name,
    studentId: student.id,
    jobTitle: job.title,
    companyName: job.companyName,
    appliedAt: `2026-08-${(2 + i).toString().padStart(2, "0")}`,
    eligible: student.careerStatus === "eligible",
    status: (["Applied", "Shortlisted", "Assessment", "Interview", "Final Round", "Offer", "Rejected", "Withdrawn"] as const)[i % 8]!,
  };
});

export const mockAdminReferrals: AdminReferral[] = Array.from({ length: 8 }).map((_, i) => ({
  id: `ref-${i + 1}`,
  companyName: pick(mockAdminCompanies.map((c) => c.name), i),
  role: pick(["Java Developer", "SDE Intern", "Cloud Engineer"], i),
  studentName: mockAdminStudents[i + 2]!.name,
  referrerName: pick(["Alumni · Rohit K", "Alumni · Shreya M", "Trainer · Durga Prasad"], i),
  requestedAt: `2026-08-${(6 + i).toString().padStart(2, "0")}`,
  status: (["Requested", "Under Review", "Approved", "Referred", "Rejected", "Expired", "Completed", "Requested"] as const)[i]!,
}));

export const mockAdminArticles: AdminArticle[] = [
  {
    id: "art-1",
    title: "How to crack product-based company interviews in 2026",
    slug: "crack-product-interviews-2026",
    authorName: "Nisha Verma",
    authorRole: "admin",
    category: "Interview",
    tags: ["interview", "dsa", "career"],
    excerpt: "A structured 12-week preparation roadmap covering DSA, system design and behavioural rounds.",
    seoTitle: "Product company interview preparation roadmap (2026)",
    seoDescription: "A 12-week roadmap to prepare for product-based company interviews.",
    publishDate: "2026-08-10",
    status: "Published",
    views: 4820,
  },
  {
    id: "art-2",
    title: "Resume formats that pass ATS screening",
    slug: "ats-friendly-resume-formats",
    authorName: "Durga Prasad",
    authorRole: "teacher",
    category: "Resume",
    tags: ["resume", "ats"],
    excerpt: "What recruiters and parsers actually look for in a fresher resume.",
    seoTitle: "ATS-friendly resume formats for freshers",
    seoDescription: "Resume structure, keywords and formatting that survive ATS parsing.",
    publishDate: "2026-08-16",
    status: "In Review",
    views: 0,
  },
  {
    id: "art-3",
    title: "Cloud career paths: AWS vs Azure vs GCP",
    slug: "cloud-career-paths",
    authorName: "Meera Krishnan",
    authorRole: "teacher",
    category: "Cloud",
    tags: ["cloud", "career"],
    excerpt: "Comparing certification tracks and job market demand across the three major clouds.",
    seoTitle: "AWS vs Azure vs GCP career paths",
    seoDescription: "Which cloud certification track fits your career goals.",
    publishDate: "2026-08-24",
    status: "Scheduled",
    views: 0,
  },
  {
    id: "art-4",
    title: "Weekly job market update — August 2026",
    slug: "job-market-update-aug-2026",
    authorName: "Nisha Verma",
    authorRole: "admin",
    category: "Job Market",
    tags: ["jobs", "market"],
    excerpt: "Hiring signals across services, product and GCC companies this month.",
    seoTitle: "India tech job market update — August 2026",
    seoDescription: "Hiring trends across services, product and GCC companies.",
    publishDate: "2026-08-18",
    status: "Draft",
    views: 0,
  },
];

export const mockAuditLogs: AuditLogEntry[] = [
  {
    id: "log-1",
    actorName: "Nisha Verma",
    actorRole: "admin",
    action: "Updated attendance",
    entity: "Attendance",
    recordLabel: "Aarav Sharma · 2026-08-14",
    previousValue: "Absent",
    newValue: "Excused",
    occurredAt: "2026-08-18 10:22",
  },
  {
    id: "log-2",
    actorName: "Nisha Verma",
    actorRole: "admin",
    action: "Published job",
    entity: "Job",
    recordLabel: "Java Developer · Wipro",
    previousValue: "Draft",
    newValue: "Published",
    occurredAt: "2026-08-18 09:41",
  },
  {
    id: "log-3",
    actorName: "Nisha Verma",
    actorRole: "admin",
    action: "Changed eligibility rule",
    entity: "Eligibility",
    recordLabel: "Microsoft preparation track · attendance",
    previousValue: "80%",
    newValue: "85%",
    occurredAt: "2026-08-17 18:05",
  },
  {
    id: "log-4",
    actorName: "Nisha Verma",
    actorRole: "admin",
    action: "Overrode career eligibility",
    entity: "Student",
    recordLabel: "Sneha Reddy",
    previousValue: "Not eligible",
    newValue: "Under review — medical leave documented",
    occurredAt: "2026-08-17 12:30",
  },
  {
    id: "log-5",
    actorName: "Nisha Verma",
    actorRole: "admin",
    action: "Assigned trainer",
    entity: "Batch",
    recordLabel: "B-2026-3",
    previousValue: "Unassigned",
    newValue: "Vikram Sethi",
    occurredAt: "2026-08-16 15:14",
  },
];

export const mockWhatsAppSettings: WhatsAppSettings = {
  businessNumber: "+91 90000 12345",
  defaultMessage: "Hello Learntrix, I would like to know more about your programs.",
  courseEnquiryMessage: "Hello, I am interested in the {course} course.",
  studentSupportMessage: "Hello, I am a student and need help with my course.",
  careerSupportMessage: "Hello, I need support with the career and placement services.",
  jobEnquiryMessage: "Hello, I have a question about the {job} opening.",
  enabled: true,
};

export const mockAdminOverview: AdminOverview = {
  metrics: {
    students: 12480,
    activeStudents: 10342,
    inactiveStudents: 2138,
    teachers: 46,
    activeCourses: 128,
    activeBatches: 21,
    enrollments: 18640,
    todaysClasses: 14,
    todaysAttendance: 88,
    careerEligible: 4126,
    careerIneligible: 1874,
    activeJobs: 63,
    applications: 2412,
    interviews: 486,
    offers: 173,
    placements: 141,
    revenueThisMonth: 4820000,
    newEnquiries: 87,
  },
  studentGrowth: [
    { month: "Mar", students: 7420, enrollments: 9800 },
    { month: "Apr", students: 8310, enrollments: 11240 },
    { month: "May", students: 9240, enrollments: 12980 },
    { month: "Jun", students: 10480, enrollments: 14760 },
    { month: "Jul", students: 11530, enrollments: 16820 },
    { month: "Aug", students: 12480, enrollments: 18640 },
  ],
  attendanceTrend: [
    { week: "W1", attendance: 84, completion: 61 },
    { week: "W2", attendance: 87, completion: 64 },
    { week: "W3", attendance: 82, completion: 68 },
    { week: "W4", attendance: 88, completion: 72 },
    { week: "W5", attendance: 91, completion: 75 },
    { week: "W6", attendance: 89, completion: 78 },
  ],
  performance: [
    { label: "Full Stack", quiz: 81, coding: 74 },
    { label: "Data Science", quiz: 86, coding: 69 },
    { label: "Cloud", quiz: 78, coding: 71 },
    { label: "DSA", quiz: 74, coding: 88 },
    { label: "AI/ML", quiz: 83, coding: 76 },
  ],
  placementFunnel: [
    { stage: "Eligible", value: 4126 },
    { stage: "Applied", value: 2412 },
    { stage: "Shortlisted", value: 962 },
    { stage: "Interviews", value: 486 },
    { stage: "Offers", value: 173 },
    { stage: "Placed", value: 141 },
  ],
  todaysOperations: [
    { id: "op1", label: "Today's classes", detail: "Across 9 batches", count: 14 },
    { id: "op2", label: "Upcoming events", detail: "Demo classes & webinars this week", count: 6 },
    { id: "op3", label: "Pending enquiries", detail: "Awaiting first response", count: 87 },
    { id: "op4", label: "Pending student approvals", detail: "New registrations", count: 23 },
    { id: "op5", label: "Pending teacher approvals", detail: "Trainer applications", count: 2 },
    { id: "op6", label: "New job applications", detail: "Submitted in last 24h", count: 41 },
    { id: "op7", label: "Referral requests", detail: "Awaiting review", count: 8 },
  ],
  alerts: [
    { id: "al1", severity: "warning", title: "Students with attendance below threshold", count: 32, actionLabel: "Review attendance", to: "/admin/attendance" },
    { id: "al2", severity: "warning", title: "Trainer approvals pending", count: 2, actionLabel: "Review trainers", to: "/admin/teachers" },
    { id: "al3", severity: "info", title: "New enquiries", count: 87, actionLabel: "Open communication", to: "/admin/communication" },
    { id: "al4", severity: "info", title: "Referral requests pending", count: 8, actionLabel: "Review referrals", to: "/admin/career/referrals" },
    { id: "al5", severity: "warning", title: "Jobs expiring in 7 days", count: 5, actionLabel: "Review jobs", to: "/admin/career/jobs" },
    { id: "al6", severity: "info", title: "Unpublished articles", count: 3, actionLabel: "Open articles", to: "/admin/articles" },
    { id: "al7", severity: "warning", title: "Student documents pending review", count: 17, actionLabel: "Review students", to: "/admin/students" },
  ],
};

export const mockSetupSteps: AdminSetupStep[] = [
  { id: "s1", label: "Company profile", description: "Brand, contact details and legal information", complete: true },
  { id: "s2", label: "Courses", description: "Publish your course catalogue", complete: true },
  { id: "s3", label: "Teachers", description: "Invite and approve trainers", complete: true },
  { id: "s4", label: "Batches", description: "Create batches and schedules", complete: true },
  { id: "s5", label: "Students", description: "Onboard or import students", complete: true },
  { id: "s6", label: "Schedules", description: "Live class calendar and meeting links", complete: false },
  { id: "s7", label: "Career rules", description: "Configure eligibility thresholds", complete: true },
  { id: "s8", label: "Jobs", description: "Publish first job opportunities", complete: false },
  { id: "s9", label: "Communication / WhatsApp", description: "Business number and message templates", complete: false },
  { id: "s10", label: "Website content", description: "Banners, testimonials and FAQs", complete: false },
];
