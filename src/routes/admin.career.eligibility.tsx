import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge, humanize } from "@/components/portal/AdminBits";
import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";
import { adminService } from "@/services/admin.service";
import type { EligibilityMetric, EligibilityRuleSet } from "@/types/admin";

export const Route = createFileRoute("/admin/career/eligibility")({
  head: () => ({
    meta: [
      { title: "Career Eligibility Rules — Learntrix Admin" },
      { name: "description", content: "Configure platform and company-specific eligibility thresholds for attendance, progress, coding and profile completion." },
      { property: "og:title", content: "Career Eligibility Rules — Learntrix Admin" },
      { property: "og:description", content: "Fully configurable career eligibility engine — no hard-coded thresholds." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminEligibility,
});

const metrics: Array<{ key: EligibilityMetric; label: string; suffix: string }> = [
  { key: "attendance", label: "Minimum attendance", suffix: "%" },
  { key: "courseProgress", label: "Minimum course progress", suffix: "%" },
  { key: "quizScore", label: "Minimum quiz average", suffix: "%" },
  { key: "codingScore", label: "Minimum coding score", suffix: "%" },
  { key: "assignmentCompletion", label: "Minimum assignment completion", suffix: "%" },
  { key: "profileCompletion", label: "Minimum profile completion", suffix: "%" },
  { key: "points", label: "Minimum points", suffix: "pts" },
  { key: "mockInterviewScore", label: "Minimum mock interview score", suffix: "%" },
];

function AdminEligibility() {
  const rules = useQuery({ queryKey: ["admin", "eligibility-rules"], queryFn: () => adminService.eligibilityRules() });
  const [draft, setDraft] = useState<EligibilityRuleSet[]>([]);

  useEffect(() => {
    if (rules.data) setDraft(rules.data);
  }, [rules.data]);

  function update(id: string, key: EligibilityMetric, value: string) {
    setDraft((prev) =>
      prev.map((r) => (r.id === id ? { ...r, thresholds: { ...r.thresholds, [key]: value === "" ? undefined : Number(value) } } : r)),
    );
  }

  async function save(rule: EligibilityRuleSet) {
    await adminService.saveEligibilityRule(rule);
    toast.success("Rule saved. The backend re-evaluates eligibility for affected students.");
  }

  if (rules.isPending) return <Skeleton className="h-[60vh] w-full" />;

  return (
    <>
      <PageHeader
        title="Career Eligibility Rules"
        description="Thresholds are configuration, not code. The backend evaluates every student against the active rule sets and returns the official status."
        action={
          <Button onClick={() => toast.info("New company rule sets are created from the company record.")}>
            <Plus className="size-4" aria-hidden /> New rule set
          </Button>
        }
      />

      <div className="mb-6 grid gap-3 sm:grid-cols-4">
        {[
          { label: "Career Eligible", value: "eligible" },
          { label: "Conditionally Eligible", value: "conditional" },
          { label: "Under Review", value: "under_review" },
          { label: "Not Eligible", value: "not_eligible" },
        ].map((s) => (
          <div key={s.value} className="surface-panel flex items-center justify-between p-4">
            <span className="text-sm">{s.label}</span>
            <StatusBadge value={s.value} />
          </div>
        ))}
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        {draft.map((rule) => (
          <Panel
            key={rule.id}
            title={rule.name}
            description={`${rule.scope === "platform" ? "Platform baseline" : "Company-specific"} · updated ${new Date(rule.updatedAt).toLocaleDateString()}`}
            action={
              <div className="flex items-center gap-2">
                <Label htmlFor={`active-${rule.id}`} className="text-xs text-muted-foreground">
                  Active
                </Label>
                <Switch
                  id={`active-${rule.id}`}
                  checked={rule.active}
                  onCheckedChange={(checked) =>
                    setDraft((prev) => prev.map((r) => (r.id === rule.id ? { ...r, active: checked } : r)))
                  }
                />
              </div>
            }
          >
            <div className="grid gap-3 sm:grid-cols-2">
              {metrics.map((m) => (
                <div key={m.key} className="space-y-1.5">
                  <Label htmlFor={`${rule.id}-${m.key}`} className="text-xs">
                    {m.label} ({m.suffix})
                  </Label>
                  <Input
                    id={`${rule.id}-${m.key}`}
                    type="number"
                    min={0}
                    max={m.suffix === "%" ? 100 : 100000}
                    value={rule.thresholds[m.key] ?? ""}
                    placeholder="Not required"
                    onChange={(e) => update(rule.id, m.key, e.target.value)}
                  />
                </div>
              ))}
            </div>
            <div className="mt-4 flex flex-wrap items-center gap-2">
              <Button size="sm" onClick={() => void save(rule)}>
                Save rule set
              </Button>
              <Badge variant="outline" className="gap-1">
                <ShieldCheck className="size-3" aria-hidden /> Audited change
              </Badge>
            </div>
          </Panel>
        ))}
      </div>

      <Panel title="How eligibility is presented to students" className="mt-6">
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="rounded-lg border border-border p-4">
            <p className="text-sm font-medium">Microsoft preparation track</p>
            <StatusBadge value="eligible" className="mt-2" />
          </div>
          <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4">
            <p className="text-sm font-medium">Google opportunity</p>
            <StatusBadge value="not_eligible" className="mt-2" />
            <p className="mt-2 text-xs text-muted-foreground">
              Reason: attendance below required threshold. Current attendance 72% · required 90%. Recommended action: attend upcoming
              classes and improve attendance.
            </p>
          </div>
        </div>
        <p className="mt-4 text-xs text-muted-foreground">
          Admins can override any student's status with a documented reason ({humanize("under_review")} included). Overrides are written to
          the audit log by the backend.
        </p>
      </Panel>
    </>
  );
}
