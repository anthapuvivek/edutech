import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
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

export const Route = createFileRoute("/admin/audit-logs")({
  head: () => ({
    meta: [
      { title: "Audit Logs — Learntrix Admin" },
      {
        name: "description",
        content:
          "Immutable record of who changed what and when, with previous and new values for every admin action.",
      },
      { property: "og:title", content: "Audit Logs — Learntrix Admin" },
      {
        property: "og:description",
        content: "Full accountability trail for platform administration.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminAuditLogs,
});

function AdminAuditLogs() {
  const logs = useQuery({
    queryKey: ["admin", "audit-logs"],
    queryFn: () => adminService.auditLogs(),
  });
  const [q, setQ] = useState("");
  const data = logs.data ?? [];
  const rows = useMemo(() => {
    const needle = q.trim().toLowerCase();
    if (!needle) return data;
    return data.filter((l) =>
      [l.actorName, l.action, l.entity, l.recordLabel].some((v) =>
        v.toLowerCase().includes(needle),
      ),
    );
  }, [data, q]);

  return (
    <>
      <PageHeader
        title="Audit Logs"
        description="Written by the backend. Entries cannot be edited or deleted from the admin console."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Entries (30 days)" value={data.length} />
        <StatCard label="Actors" value={new Set(data.map((l) => l.actorName)).size} />
        <StatCard label="Entities touched" value={new Set(data.map((l) => l.entity)).size} />
        <StatCard label="Retention" value="24 months" />
      </section>

      <Panel
        title="Activity trail"
        action={
          <Input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search actor, action or record"
            maxLength={80}
            className="w-full sm:w-72"
            aria-label="Search audit logs"
          />
        }
      >
        {logs.isPending ? (
          <Skeleton className="h-80 w-full" />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>When</TableHead>
                  <TableHead>Actor</TableHead>
                  <TableHead>Action</TableHead>
                  <TableHead>Record</TableHead>
                  <TableHead>Previous</TableHead>
                  <TableHead>New</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((l) => (
                  <TableRow key={l.id}>
                    <TableCell className="whitespace-nowrap text-sm">
                      {new Date(l.occurredAt).toLocaleString()}
                    </TableCell>
                    <TableCell>
                      <div className="font-medium">{l.actorName}</div>
                      <div className="text-xs text-muted-foreground">{l.actorRole}</div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">{l.action}</Badge>
                    </TableCell>
                    <TableCell>
                      <div className="text-sm">{l.recordLabel}</div>
                      <div className="text-xs text-muted-foreground">{l.entity}</div>
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">
                      {l.previousValue ?? "—"}
                    </TableCell>
                    <TableCell className="text-xs">{l.newValue ?? "—"}</TableCell>
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
