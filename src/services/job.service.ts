import { env } from "@/lib/env";
import { mockApplications, mockCompanies, mockJobs, mockReferralOpportunities } from "@/mock/career";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  Company,
  Job,
  JobApplication,
  ReferralOpportunity,
} from "@/types/career";

export interface JobQuery {
  search?: string | undefined;
  type?: string | undefined;
  workMode?: string | undefined;
  sort?: "newest" | "relevance" | "deadline" | undefined;
}

/** Job, company, application and referral reads. Backend enforces job access rules. */
export const jobService = {
  async list(query: JobQuery = {}): Promise<Job[]> {
    if (!env.useMocks) return apiRequest<Job[]>("/career/jobs", { query: { ...query } });
    const search = (query.search ?? "").toLowerCase();
    const rows = mockJobs.filter((job) => {
      const matchesSearch =
        !search ||
        job.title.toLowerCase().includes(search) ||
        job.companyName.toLowerCase().includes(search) ||
        job.skills.some((s) => s.toLowerCase().includes(search));
      const matchesType = !query.type || query.type === "all" || job.type === query.type;
      const matchesMode = !query.workMode || query.workMode === "all" || job.workMode === query.workMode;
      return matchesSearch && matchesType && matchesMode;
    });
    const sorted = [...rows].sort((a, b) => {
      if (query.sort === "deadline") return a.deadline.localeCompare(b.deadline);
      if (query.sort === "newest") return b.postedAt.localeCompare(a.postedAt);
      return b.matchPercent - a.matchPercent;
    });
    return mockDelay(sorted);
  },
  async companies(): Promise<Company[]> {
    if (!env.useMocks) return apiRequest<Company[]>("/career/companies");
    return mockDelay(mockCompanies);
  },
  async applications(): Promise<JobApplication[]> {
    if (!env.useMocks) return apiRequest<JobApplication[]>("/career/applications");
    return mockDelay(mockApplications);
  },
  async referralOpportunities(): Promise<ReferralOpportunity[]> {
    if (!env.useMocks) return apiRequest<ReferralOpportunity[]>("/career/referrals");
    return mockDelay(mockReferralOpportunities);
  },
};
