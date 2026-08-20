import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { BadgeCheck, Plus } from "lucide-react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/career/companies")({
  head: () => ({
    meta: [
      { title: "Companies — Learntrix Admin" },
      {
        name: "description",
        content:
          "Manage company profiles, preparation tracks, verification status and company-specific eligibility rules.",
      },
      { property: "og:title", content: "Companies — Learntrix Admin" },
      {
        property: "og:description",
        content: "Company records, verification and preparation tracks.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminCompanies,
});

function AdminCompanies() {
  const companies = useQuery({
    queryKey: ["admin", "companies"],
    queryFn: () => adminService.companies(),
  });
  const data = companies.data ?? [];

  return (
    <>
      <PageHeader
        title="Companies"
        description="A company is shown publicly as an official partner only when an admin explicitly verifies and marks it."
        action={
          <Button
            onClick={() => toast.info("Company creation posts to the backend company service.")}
          >
            <Plus className="size-4" aria-hidden /> Add company
          </Button>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Companies" value={data.length} />
        <StatCard label="Official partners" value={data.filter((c) => c.officialPartner).length} />
        <StatCard
          label="Preparation tracks"
          value={data.filter((c) => c.preparationTrack).length}
        />
        <StatCard label="Open roles" value={data.reduce((s, c) => s + c.openRoles, 0)} />
      </section>

      {companies.isPending ? (
        <Skeleton className="h-72 w-full" />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {data.map((c) => (
            <Panel
              key={c.id}
              title={c.name}
              description={`${c.industry} · ${c.locations.join(", ")}`}
              action={
                c.officialPartner ? (
                  <Badge variant="success" className="gap-1">
                    <BadgeCheck className="size-3" aria-hidden /> Official partner
                  </Badge>
                ) : (
                  <Badge variant="outline">Preparation track</Badge>
                )
              }
            >
              <p className="text-sm text-muted-foreground">{c.description}</p>
              <div className="mt-3 flex flex-wrap gap-1.5">
                {c.roles.map((role) => (
                  <Badge key={role} variant="outline">
                    {role}
                  </Badge>
                ))}
              </div>
              <div className="mt-4 grid gap-3 sm:grid-cols-3">
                {[
                  { label: "Verified", checked: c.verified },
                  { label: "Official partner", checked: c.officialPartner },
                  { label: "Preparation track", checked: c.preparationTrack },
                ].map((toggle) => (
                  <label
                    key={toggle.label}
                    className="flex items-center justify-between gap-2 rounded-md border border-border px-3 py-2 text-xs"
                  >
                    {toggle.label}
                    <Switch
                      checked={toggle.checked}
                      onCheckedChange={() =>
                        toast.success(`${toggle.label} change submitted for ${c.name}.`)
                      }
                      aria-label={`${toggle.label} for ${c.name}`}
                    />
                  </label>
                ))}
              </div>
              <div className="mt-4 flex flex-wrap gap-2">
                <Button size="sm" variant="outline" asChild>
                  <a href={c.website} target="_blank" rel="noreferrer noopener">
                    Website
                  </a>
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() =>
                    toast.info("Company eligibility rules open in the eligibility engine.")
                  }
                >
                  Eligibility rules
                </Button>
              </div>
            </Panel>
          ))}
        </div>
      )}
    </>
  );
}
