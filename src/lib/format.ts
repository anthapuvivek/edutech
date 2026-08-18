export function formatPrice(amount: number, currency: "INR" | "USD" = "INR") {
  return new Intl.NumberFormat(currency === "INR" ? "en-IN" : "en-US", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatCompact(value: number) {
  return new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 }).format(value);
}

export function discountPercent(price: number, original?: number) {
  if (!original || original <= price) return null;
  return Math.round(((original - price) / original) * 100);
}
