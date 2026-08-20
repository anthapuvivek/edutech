import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/career/referrals")({
  head: () => ({
    meta: [
      { title: "Referrals — Learntrix Admin" },
      {
        name: "description",
        content:
          "Review, approve and track alumni and partner referral requests raised by students.",
      },
      { property: "og:title", content: "Referrals — Learntrix Admin" },
      { property: "og:description", content: "Referral request workflow with admin approval." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminReferrals,
});

function AdminReferrals() {
  const referrals = useQuery({
    queryKey: ["admin", "referrals"],
    queryFn: () => adminService.referrals(),
  });
  const data = referrals.data ?? [];

  async function update(id: string, status: string) {
    await adminService.updateReferral(id, status);
    toast.success(`Referral marked as ${status}.`);
  }

  return (
    <>
      <PageHeader
        title="Referrals"
        description="Referral requests never reach a company until an admin approves them."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Requests" value={data.length} />
        <StatCard
          label="Under review"
          value={data.filter((r) => r.status === "Under Review" || r.status === "Requested").length}
        />
        <StatCard
          label="Approved"
          value={data.filter((r) => r.status === "Approved" || r.status === "Referred").length}
        />
        <StatCard label="Completed" value={data.filter((r) => r.status === "Completed").length} />
      </section>

      <Panel title="Referral requests">
        {referrals.isPending ? (
          <Skeleton className="h-72 w-full" />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Student</TableHead>
                  <TableHead>Company / Role</TableHead>
                  <TableHead>Referrer</TableHead>
                  <TableHead>Requested</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell className="font-medium">{r.studentName}</TableCell>
                    <TableCell>
                      {r.companyName}
                      <div className="text-xs text-muted-foreground">{r.role}</div>
                    </TableCell>
                    <TableCell>{r.referrerName}</TableCell>
                    <TableCell className="text-sm">
                      {new Date(r.requestedAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell>
                      <StatusBadge value={r.status} />
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      <Button size="sm" onClick={() => void update(r.id, "Approved")}>
                        Approve
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => void update(r.id, "Rejected")}
                      >
                        Reject
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Panel>
    </>
  );
}
