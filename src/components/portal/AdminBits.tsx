import type { ReactNode } from "react";

import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

type BadgeVariant = "default" | "secondary" | "outline" | "destructive" | "success" | "warning" | "info";

const tone: Record<string, BadgeVariant> = {
  active: "success",
  approved: "success",
  published: "success",
  paid: "success",
  eligible: "success",
  placed: "success",
  offer: "success",
  offered: "success",
  completed: "success",
  referred: "success",
  present: "success",

  pending: "warning",
  partial: "warning",
  late: "warning",
  conditional: "warning",
  under_review: "warning",
  "under review": "warning",
  draft: "warning",
  scheduled: "warning",
  "in review": "warning",
  upcoming: "warning",
  requested: "warning",
  interviewing: "warning",
  interview: "warning",
  assessment: "warning",
  shortlisted: "info",
  applied: "info",
  excused: "info",
  job_seeking: "info",
  "final round": "info",

  suspended: "destructive",
  rejected: "destructive",
  cancelled: "destructive",
  expired: "destructive",
  absent: "destructive",
  not_eligible: "destructive",
  refunded: "destructive",
};

export function humanize(value: string) {
  return value.replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
}

export function StatusBadge({ value, className }: { value: string; className?: string }) {
  const key = value.toLowerCase();
  return (
    <Badge variant={tone[key] ?? "secondary"} className={cn("capitalize", className)}>
      {humanize(value)}
    </Badge>
  );
}

export function Panel({
  title,
  description,
  action,
  children,
  className,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={cn("surface-panel p-5", className)}>
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold">{title}</h2>
          {description ? <p className="mt-1 text-xs text-muted-foreground">{description}</p> : null}
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

export function MetricBar({ label, value, suffix = "%" }: { label: string; value: number; suffix?: string }) {
  return (
    <div>
      <div className="flex items-center justify-between text-xs">
        <span className="text-muted-foreground">{label}</span>
        <span className="font-medium">
          {value}
          {suffix}
        </span>
      </div>
      <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-muted">
        <div
          className={cn("h-full rounded-full", value >= 80 ? "bg-accent" : value >= 60 ? "bg-primary" : "bg-destructive")}
          style={{ width: `${Math.min(Math.max(value, 0), 100)}%` }}
        />
      </div>
    </div>
  );
}
