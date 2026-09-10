export function formatPrice(amount: number, currency: "INR" | "USD" = "INR") {
  return new Intl.NumberFormat(currency === "INR" ? "en-IN" : "en-US", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatCompact(value: number) {
  return new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 }).format(
    value,
  );
}

export function discountPercent(price: number, original?: number) {
  if (!original || original <= price) return null;
  return Math.round(((original - price) / original) * 100);
}

/**
 * Renders a recording's curriculum mapping for display. Both parts are optional - a
 * trainer can upload a class before the course has modules or lessons - so this collapses
 * to whichever parts exist and falls back to a neutral label when neither does.
 */
export function formatCurriculumPath(moduleTitle?: string, lessonTitle?: string) {
  const parts = [moduleTitle, lessonTitle].filter((p): p is string => !!p && p.trim() !== "");
  return parts.length > 0 ? parts.join(" · ") : "Not mapped to curriculum";
}
