import { env } from "@/lib/env";
import { apiRequest, mockDelay } from "@/services/api-client";
import type { EnquiryPayload } from "@/types";

export interface EnquiryResult {
  id: string;
  status: "New";
  createdAt: string;
}

export const enquiryService = {
  async submit(payload: EnquiryPayload): Promise<EnquiryResult> {
    if (!env.useMocks)
      return apiRequest<EnquiryResult>("/enquiries", { method: "POST", body: payload });
    return mockDelay(
      {
        id: `enq-${Math.random().toString(36).slice(2, 9)}`,
        status: "New" as const,
        createdAt: new Date().toISOString(),
      },
      700,
    );
  },
};
