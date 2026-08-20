/**
 * Career & Placement domain types (Phase 3).
 * These mirror the future FastAPI contracts — the backend remains the source of
 * truth for eligibility, readiness scores and verified placement outcomes.
 */

export type CareerAccessState = "eligible" | "not_eligible" | "pending_review";

export interface CareerEligibility {
  state: CareerAccessState;
  /** Backend-provided human explanation shown when access is denied. */
  reason: string;
  eligiblePrograms: Array<{ courseSlug: string; courseTitle: string }>;
  onboardingCompleted: boolean;
  checkedAt: string;
}

export type WorkMode = "Remote" | "Hybrid" | "Onsite";
export type JobType = "Full Time" | "Internship" | "Part Time" | "Contract" | "Graduate/Fresher";

export type JobCategory =
  | "Software Engineering"
  | "Full Stack Development"
  | "Backend Development"
  | "Frontend Development"
  | "Data Science"
  | "Data Analytics"
  | "AI/ML"
  | "Cloud"
  | "DevOps"
  | "Cyber Security"
  | "Testing"
  | "Business Analytics"
  | "Other";

export interface Company {
  id: string;
  name: string;
  logoUrl?: string | undefined;
  industry: string;
  locations: string[];
  /** Only true when an admin has explicitly verified the partnership. */
  officialPartner: boolean;
  preparationTrack: boolean;
  typicalRoles: string[];
  requiredSkills: string[];
  difficulty: "Moderate" | "Challenging" | "Highly Competitive";
  averagePreparationWeeks: number;
}

export interface Job {
  id: string;
  title: string;
  companyId: string;
  companyName: string;
  category: JobCategory;
  type: JobType;
  location: string;
  workMode: WorkMode;
  experience: string;
  salaryRange?: string | undefined;
  skills: string[];
  postedAt: string;
  deadline: string;
  /** Backend-computed match score for the signed-in student. */
  matchPercent: number;
  saved: boolean;
}

export type ApplicationStatus =
  | "Saved"
  | "Applied"
  | "Assessment"
  | "Shortlisted"
  | "Interview"
  | "Final Round"
  | "Offer"
  | "Rejected"
  | "Withdrawn";

export interface JobApplication {
  id: string;
  jobId: string;
  companyName: string;
  role: string;
  appliedAt: string;
  status: ApplicationStatus;
  nextAction?: string | undefined;
  deadline?: string | undefined;
  interviewAt?: string | undefined;
}

export type ReferralStatus =
  "Requested" | "Under Review" | "Accepted" | "Referred" | "Rejected" | "Expired" | "Completed";

export interface ReferralOpportunity {
  id: string;
  companyName: string;
  role: string;
  location: string;
  referralType: "Job referral" | "Employee referral" | "Platform referral";
  eligibility: string;
  deadline: string;
  seatsAvailable: number;
  verified: boolean;
}

export interface ReferralRequest {
  id: string;
  opportunityId: string;
  companyName: string;
  role: string;
  requestedAt: string;
  status: ReferralStatus;
  notes?: string | undefined;
}

export interface CareerReadiness {
  /** Backend-calculated; the frontend only renders it. */
  overall: number;
  components: Array<{ label: string; score: number }>;
  updatedAt: string;
}

export interface CareerProfile {
  studentId: string;
  name: string;
  headline: string;
  education: string;
  courseTitle: string;
  skills: string[];
  preferredRoles: string[];
  preferredLocations: string[];
  workMode: WorkMode;
  experienceLevel: "Fresher" | "0-1 years" | "1-3 years" | "3+ years";
  expectedSalary?: string | undefined;
  availability: string;
  links: {
    linkedin?: string | undefined;
    github?: string | undefined;
    portfolio?: string | undefined;
    leetcode?: string | undefined;
    hackerrank?: string | undefined;
  };
  completionPercent: number;
  missingItems: string[];
}

export interface CareerOnboardingPayload {
  targetRole: string;
  preferredLocation: string;
  experienceLevel: CareerProfile["experienceLevel"];
  workMode: WorkMode;
  targetCompanies: string[];
  careerGoal: string;
}

export interface SkillGap {
  targetRole: string;
  strong: string[];
  needsImprovement: Array<{
    skill: string;
    level: number;
    recommendedCourseSlug?: string | undefined;
  }>;
  missing: string[];
}

export interface CareerRoadmapStep {
  label: string;
  progressPercent: number;
}

export interface CareerRoadmap {
  goal: string;
  steps: CareerRoadmapStep[];
  nextRecommended: string[];
}

export interface PlacementChecklistItem {
  label: string;
  status: "complete" | "in_progress" | "pending";
  detail?: string | undefined;
}

export type PlacementStatus =
  | "Career Preparation"
  | "Job Seeking"
  | "Interviewing"
  | "Offer Received"
  | "Placed"
  | "Not Looking"
  | "Career Paused";

export interface CareerNotification {
  id: string;
  kind: "job" | "deadline" | "interview" | "referral" | "resume" | "skill";
  title: string;
  createdAt: string;
}

export interface CareerDashboard {
  readiness: CareerReadiness;
  placementStatus: PlacementStatus;
  recommendedJobs: Job[];
  recommendedInternships: Job[];
  applications: JobApplication[];
  upcomingInterviews: JobApplication[];
  referralOpportunities: ReferralOpportunity[];
  savedJobs: Job[];
  skillsToImprove: string[];
  checklist: PlacementChecklistItem[];
  notifications: CareerNotification[];
}
