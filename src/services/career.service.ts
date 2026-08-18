import { env } from "@/lib/env";
import {
  mockCareerDashboard,
  mockCareerEligibility,
  mockCareerProfile,
  mockCareerReadiness,
  mockCareerRoadmap,
  mockSkillGap,
} from "@/mock/career";
import { apiRequest, mockDelay } from "@/services/api-client";
import type {
  CareerDashboard,
  CareerEligibility,
  CareerOnboardingPayload,
  CareerProfile,
  CareerReadiness,
  CareerRoadmap,
  SkillGap,
} from "@/types/career";

/**
 * Career eligibility, readiness and profile.
 * Eligibility and readiness are authoritative on the backend — the frontend
 * only renders what the API returns and never derives placement outcomes.
 */
export const careerService = {
  async eligibility(): Promise<CareerEligibility> {
    if (!env.useMocks) return apiRequest<CareerEligibility>("/career/eligibility");
    const stored = readOnboardingFlag();
    return mockDelay({ ...mockCareerEligibility, onboardingCompleted: stored });
  },
  async dashboard(): Promise<CareerDashboard> {
    if (!env.useMocks) return apiRequest<CareerDashboard>("/career/dashboard");
    return mockDelay(mockCareerDashboard);
  },
  async readiness(): Promise<CareerReadiness> {
    if (!env.useMocks) return apiRequest<CareerReadiness>("/career/readiness");
    return mockDelay(mockCareerReadiness);
  },
  async profile(): Promise<CareerProfile> {
    if (!env.useMocks) return apiRequest<CareerProfile>("/career/profile");
    return mockDelay(mockCareerProfile);
  },
  async updateProfile(patch: Partial<CareerProfile>): Promise<CareerProfile> {
    if (!env.useMocks)
      return apiRequest<CareerProfile>("/career/profile", { method: "PATCH", body: patch });
    return mockDelay({ ...mockCareerProfile, ...patch });
  },
  async completeOnboarding(payload: CareerOnboardingPayload): Promise<CareerEligibility> {
    if (!env.useMocks)
      return apiRequest<CareerEligibility>("/career/onboarding", { method: "POST", body: payload });
    writeOnboardingFlag();
    return mockDelay({ ...mockCareerEligibility, onboardingCompleted: true });
  },
  async roadmap(): Promise<CareerRoadmap> {
    if (!env.useMocks) return apiRequest<CareerRoadmap>("/career/roadmap");
    return mockDelay(mockCareerRoadmap);
  },
  async skillGap(): Promise<SkillGap> {
    if (!env.useMocks) return apiRequest<SkillGap>("/career/skill-gap");
    return mockDelay(mockSkillGap);
  },
};

const ONBOARDING_KEY = "learntrix.career.onboarded";

function readOnboardingFlag() {
  if (typeof window === "undefined") return false;
  return window.localStorage.getItem(ONBOARDING_KEY) === "true";
}

function writeOnboardingFlag() {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(ONBOARDING_KEY, "true");
}
