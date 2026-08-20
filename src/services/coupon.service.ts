import { env } from "@/lib/env";
import { mockCouponOverview, mockCoupons } from "@/mock/ops";
import { apiRequest, mockDelay } from "@/services/api-client";
import type { Coupon, CouponOverview, CouponValidationResult } from "@/types/ops";

let coupons = [...mockCoupons];

/**
 * Coupon & promotion abstraction.
 * IMPORTANT: validation, discount computation and final pricing are backend
 * responsibilities. The mock branch only mirrors what the API will return so
 * the checkout UI can be reviewed — it is never the source of truth.
 */
export const couponService = {
  async list(): Promise<Coupon[]> {
    if (!env.useMocks) return apiRequest<Coupon[]>("/admin/coupons");
    return mockDelay(coupons);
  },

  async overview(): Promise<CouponOverview> {
    if (!env.useMocks) return apiRequest<CouponOverview>("/admin/coupons/overview");
    return mockDelay(mockCouponOverview);
  },

  async create(payload: Partial<Coupon>): Promise<Coupon> {
    if (!env.useMocks)
      return apiRequest<Coupon>("/admin/coupons", { method: "POST", body: payload });
    const coupon: Coupon = {
      id: `cpn-${Date.now()}`,
      code: (payload.code ?? "NEWCODE").toUpperCase(),
      name: payload.name ?? "Untitled campaign",
      description: payload.description ?? "",
      discountType: payload.discountType ?? "percentage",
      discountValue: payload.discountValue ?? 10,
      maximumDiscount: payload.maximumDiscount,
      minimumPurchase: payload.minimumPurchase,
      applicableCourses: payload.applicableCourses ?? [],
      applicableBatches: payload.applicableBatches ?? [],
      applicableUsers: payload.applicableUsers ?? [],
      startDate: payload.startDate ?? new Date().toISOString().slice(0, 10),
      expiryDate:
        payload.expiryDate ?? new Date(Date.now() + 30 * 86_400_000).toISOString().slice(0, 10),
      usageLimit: payload.usageLimit ?? 100,
      usagePerStudent: payload.usagePerStudent ?? 1,
      status: payload.status ?? "draft",
      createdBy: "Admin",
      createdAt: new Date().toISOString(),
    };
    coupons = [coupon, ...coupons];
    return mockDelay(coupon, 200);
  },

  async update(id: string, patch: Partial<Coupon>): Promise<Coupon> {
    if (!env.useMocks)
      return apiRequest<Coupon>(`/admin/coupons/${id}`, { method: "PATCH", body: patch });
    coupons = coupons.map((c) => (c.id === id ? { ...c, ...patch } : c));
    return mockDelay(
      coupons.find((c) => c.id === id)!,
      150,
    );
  },

  async duplicate(id: string): Promise<Coupon> {
    if (!env.useMocks)
      return apiRequest<Coupon>(`/admin/coupons/${id}/duplicate`, { method: "POST" });
    const source = coupons.find((c) => c.id === id)!;
    const copy: Coupon = {
      ...source,
      id: `cpn-${Date.now()}`,
      code: `${source.code}-COPY`,
      status: "draft",
      createdAt: new Date().toISOString(),
    };
    coupons = [copy, ...coupons];
    return mockDelay(copy, 150);
  },

  async remove(id: string): Promise<void> {
    if (!env.useMocks) return apiRequest<void>(`/admin/coupons/${id}`, { method: "DELETE" });
    coupons = coupons.filter((c) => c.id !== id);
    return mockDelay(undefined, 150);
  },

  /** Server-authoritative in production; the mock mirrors the response shape. */
  async validate(input: {
    code: string;
    coursePrice: number;
    currency?: string;
  }): Promise<CouponValidationResult> {
    const code = input.code.trim().toUpperCase();
    if (!env.useMocks) {
      return apiRequest<CouponValidationResult>("/checkout/coupons/validate", {
        method: "POST",
        body: { ...input, code },
      });
    }
    const currency = input.currency ?? "INR";
    const coupon = coupons.find((c) => c.code.toUpperCase() === code);
    if (!coupon) {
      return mockDelay(
        { valid: false, code, reason: "invalid", message: "This coupon code is not valid." },
        400,
      );
    }
    if (coupon.status === "expired" || Date.parse(coupon.expiryDate) < Date.now()) {
      return mockDelay(
        { valid: false, code, reason: "expired", message: "This coupon has expired." },
        400,
      );
    }
    if (coupon.status !== "active") {
      return mockDelay(
        {
          valid: false,
          code,
          reason: "not_applicable",
          message: "This coupon is not applicable to this course yet.",
        },
        400,
      );
    }
    if (coupon.minimumPurchase && input.coursePrice < coupon.minimumPurchase) {
      return mockDelay(
        {
          valid: false,
          code,
          reason: "minimum_purchase",
          message: `A minimum purchase of ₹${coupon.minimumPurchase.toLocaleString("en-IN")} is required.`,
        },
        400,
      );
    }
    if (code === "SUMMER15") {
      return mockDelay(
        {
          valid: false,
          code,
          reason: "usage_limit",
          message: "This coupon has reached its usage limit.",
        },
        400,
      );
    }
    const raw =
      coupon.discountType === "percentage"
        ? Math.round((input.coursePrice * coupon.discountValue) / 100)
        : coupon.discountValue;
    const discount = Math.min(raw, coupon.maximumDiscount ?? raw, input.coursePrice);
    return mockDelay(
      {
        valid: true,
        code,
        message: `${coupon.name} applied.`,
        quote: {
          coursePrice: input.coursePrice,
          discount,
          finalPrice: input.coursePrice - discount,
          currency,
        },
      },
      400,
    );
  },
};
