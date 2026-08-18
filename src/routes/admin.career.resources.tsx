import { createFileRoute } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

export const Route = createFileRoute("/admin/career/resources")({
  head: () => ({
    meta: [
      { title: "Career Resources — Learntrix Admin" },
      { name: "description", content: "Publish career guides, salary insights, negotiation advice and job search strategy resources." },
      { property: "og:title", content: "Career Resources — Learntrix Admin" },
      { property: "og:description", content: "Career guidance content library management." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminCareerResources,
});

const resources = [
  { id: "r1", title: "Negotiating your first offer", category: "Salary & Negotiation", format: "Guide", status: "Published", views: 3420 },
  { id: "r2", title: "India tech salary benchmarks 2026", category: "Salary Insights", format: "Report", status: "Published", views: 5810 },
  { id: "r3", title: "90-day job search plan", category: "Job Search Strategy", format: "Checklist", status: "Draft", views: 0 },
  { id: "r4", title: "Building a referral network", category: "Networking", format: "Guide", status: "Published", views: 1290 },
];

function AdminCareerResources() {
  return (
    <>
      <PageHeader
        title="Career Resources"
        description="Guidance content shown in the student career portal."
        action={
          <Button onClick={() => toast.info("Resources are authored in the content editor.")}>
            <Plus className="size-4" aria-hidden /> Add resource
          </Button>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Resources" value={resources.length} />
        <StatCard label="Published" value={resources.filter((r) => r.status === "Published").length} />
        <StatCard label="Categories" value={new Set(resources.map((r) => r.category)).size} />
        <StatCard label="Total views" value={resources.reduce((s, r) => s + r.views, 0).toLocaleString()} />
      </section>

      <Panel title="Resource library">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Title</TableHead>
                <TableHead>Category</TableHead>
                <TableHead>Format</TableHead>
                <TableHead className="text-right">Views</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {resources.map((r) => (
                <TableRow key={r.id}>
                  <TableCell className="font-medium">{r.title}</TableCell>
                  <TableCell>{r.category}</TableCell>
                  <TableCell>
                    <Badge variant="outline">{r.format}</Badge>
                  </TableCell>
                  <TableCell className="text-right">{r.views.toLocaleString()}</TableCell>
                  <TableCell>
                    <Badge variant={r.status === "Published" ? "success" : "outline"}>{r.status}</Badge>
                  </TableCell>
                  <TableCell className="space-x-2 text-right">
                    <Button size="sm" variant="outline" onClick={() => toast.info("Opening editor.")}>
                      Edit
                    </Button>
                    <Button size="sm" onClick={() => toast.success("Status updated.")}>
                      {r.status === "Published" ? "Unpublish" : "Publish"}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </Panel>
    </>
  );
}
