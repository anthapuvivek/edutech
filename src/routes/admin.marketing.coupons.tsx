import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { couponService } from "@/services/coupon.service";
import type { Coupon, CouponStatus, DiscountType } from "@/types/ops";

export const Route = createFileRoute("/admin/marketing/coupons")({
  head: () => ({
    meta: [
      { title: "Coupons & Promotions — Learntrix Admin" },
      {
        name: "description",
        content: "Create, schedule and analyse promotional campaigns across courses and batches.",
      },
      { property: "og:title", content: "Coupons & Promotions — Learntrix Admin" },
      {
        property: "og:description",
        content: "Promotional campaign management with usage and revenue analytics.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminCoupons,
});

const inr = (n: number) => `₹${n.toLocaleString("en-IN")}`;

function AdminCoupons() {
  const qc = useQueryClient();
  const coupons = useQuery({ queryKey: ["admin", "coupons"], queryFn: () => couponService.list() });
  const overview = useQuery({
    queryKey: ["admin", "coupons", "overview"],
    queryFn: () => couponService.overview(),
  });
  const [open, setOpen] = useState(false);

  const invalidate = () => void qc.invalidateQueries({ queryKey: ["admin", "coupons"] });

  const mutate = useMutation({
    mutationFn: ({ id, patch }: { id: string; patch: Partial<Coupon> }) =>
      couponService.update(id, patch),
    onSuccess: () => {
      invalidate();
      toast.success("Coupon updated. The change is queued for the audit log.");
    },
  });

  async function act(coupon: Coupon, action: string) {
    if (action === "duplicate") {
      await couponService.duplicate(coupon.id);
      invalidate();
      toast.success(`${coupon.code} duplicated as a draft.`);
      return;
    }
    if (action === "delete") {
      await couponService.remove(coupon.id);
      invalidate();
      toast.success(`${coupon.code} deleted.`);
      return;
    }
    const status = action as CouponStatus;
    mutate.mutate({ id: coupon.id, patch: { status } });
  }

  const data = coupons.data ?? [];

  return (
    <>
      <PageHeader
        title="Coupons & Promotions"
        description="Campaign configuration only. Coupon validation and final pricing are always computed by the backend at checkout."
        action={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="size-4" /> New coupon
              </Button>
            </DialogTrigger>
            <CouponDialog
              onCreated={() => {
                setOpen(false);
                invalidate();
              }}
            />
          </Dialog>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <StatCard label="Total coupons" value={overview.data?.totalCoupons ?? "—"} />
        <StatCard label="Active coupons" value={overview.data?.activeCoupons ?? "—"} />
        <StatCard label="Total usage" value={overview.data?.totalUsage ?? "—"} />
        <StatCard
          label="Revenue generated"
          value={overview.data ? inr(overview.data.revenueGenerated) : "—"}
        />
        <StatCard
          label="Discount given"
          value={overview.data ? inr(overview.data.discountGiven) : "—"}
        />
        <StatCard
          label="Conversion rate"
          value={overview.data ? `${overview.data.conversionRate}%` : "—"}
        />
      </section>

      <Tabs defaultValue="campaigns">
        <TabsList className="mb-4">
          <TabsTrigger value="campaigns">Campaigns</TabsTrigger>
          <TabsTrigger value="usage">Usage analytics</TabsTrigger>
        </TabsList>

        <TabsContent value="campaigns">
          <Panel
            title="All campaigns"
            description="Draft, schedule, activate, disable or expire promotional codes."
          >
            {coupons.isPending ? (
              <Skeleton className="h-80 w-full" />
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Code</TableHead>
                      <TableHead>Discount</TableHead>
                      <TableHead>Applicability</TableHead>
                      <TableHead>Window</TableHead>
                      <TableHead>Limits</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data.map((c) => (
                      <TableRow key={c.id}>
                        <TableCell>
                          <div className="font-medium">{c.code}</div>
                          <div className="text-xs text-muted-foreground">{c.name}</div>
                        </TableCell>
                        <TableCell>
                          {c.discountType === "percentage"
                            ? `${c.discountValue}%`
                            : inr(c.discountValue)}
                          {c.maximumDiscount ? (
                            <div className="text-xs text-muted-foreground">
                              max {inr(c.maximumDiscount)}
                            </div>
                          ) : null}
                        </TableCell>
                        <TableCell className="text-xs text-muted-foreground">
                          {c.applicableCourses.length
                            ? `${c.applicableCourses.length} course(s)`
                            : "All courses"}
                          <div>
                            {c.applicableBatches.length
                              ? `${c.applicableBatches.length} batch(es)`
                              : "All batches"}
                          </div>
                        </TableCell>
                        <TableCell className="text-xs">
                          {c.startDate}
                          <div className="text-muted-foreground">to {c.expiryDate}</div>
                        </TableCell>
                        <TableCell className="text-xs">
                          {c.usageLimit} total
                          <div className="text-muted-foreground">{c.usagePerStudent} / student</div>
                        </TableCell>
                        <TableCell>
                          <StatusBadge value={c.status} />
                        </TableCell>
                        <TableCell className="text-right">
                          <Select onValueChange={(v) => void act(c, v)}>
                            <SelectTrigger
                              className="ml-auto w-[140px]"
                              aria-label={`Actions for ${c.code}`}
                            >
                              <SelectValue placeholder="Actions" />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="active">Activate</SelectItem>
                              <SelectItem value="disabled">Disable</SelectItem>
                              <SelectItem value="scheduled">Schedule</SelectItem>
                              <SelectItem value="expired">Expire</SelectItem>
                              <SelectItem value="duplicate">Duplicate</SelectItem>
                              <SelectItem value="delete">Delete</SelectItem>
                            </SelectContent>
                          </Select>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </Panel>
        </TabsContent>

        <TabsContent value="usage">
          <Panel
            title="Coupon usage"
            description="Usage, revenue, discount and conversion per campaign."
          >
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Coupon</TableHead>
                    <TableHead className="text-right">Usage</TableHead>
                    <TableHead className="text-right">Revenue</TableHead>
                    <TableHead className="text-right">Discount</TableHead>
                    <TableHead className="text-right">Conversions</TableHead>
                    <TableHead className="text-right">Conversion rate</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(overview.data?.perCoupon ?? []).map((row) => (
                    <TableRow key={row.couponId}>
                      <TableCell className="font-medium">{row.code}</TableCell>
                      <TableCell className="text-right">{row.usage}</TableCell>
                      <TableCell className="text-right">{inr(row.revenue)}</TableCell>
                      <TableCell className="text-right">{inr(row.discountGiven)}</TableCell>
                      <TableCell className="text-right">{row.conversions}</TableCell>
                      <TableCell className="text-right">{row.conversionRate}%</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </Panel>
        </TabsContent>
      </Tabs>
    </>
  );
}

function CouponDialog({ onCreated }: { onCreated: () => void }) {
  const [form, setForm] = useState<Partial<Coupon>>({
    discountType: "percentage",
    status: "draft",
    usageLimit: 100,
    usagePerStudent: 1,
  });

  async function submit() {
    if (!form.code || !form.name) {
      toast.error("Coupon code and name are required.");
      return;
    }
    await couponService.create(form);
    toast.success(`${form.code.toUpperCase()} created.`);
    onCreated();
  }

  return (
    <DialogContent className="max-h-[85vh] max-w-2xl overflow-y-auto">
      <DialogHeader>
        <DialogTitle>Create coupon</DialogTitle>
        <DialogDescription>
          The backend validates and applies these rules at checkout.
        </DialogDescription>
      </DialogHeader>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Coupon code">
          <Input
            value={form.code ?? ""}
            onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
            placeholder="WELCOME20"
          />
        </Field>
        <Field label="Coupon name">
          <Input
            value={form.name ?? ""}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            placeholder="Welcome offer"
          />
        </Field>
        <Field label="Description" className="sm:col-span-2">
          <Textarea
            value={form.description ?? ""}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            rows={2}
          />
        </Field>
        <Field label="Discount type">
          <Select
            value={form.discountType ?? "percentage"}
            onValueChange={(v) => setForm({ ...form, discountType: v as DiscountType })}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="percentage">Percentage</SelectItem>
              <SelectItem value="fixed">Fixed amount</SelectItem>
            </SelectContent>
          </Select>
        </Field>
        <Field label="Discount value">
          <Input
            type="number"
            value={form.discountValue ?? ""}
            onChange={(e) => setForm({ ...form, discountValue: Number(e.target.value) })}
          />
        </Field>
        <Field label="Maximum discount (₹)">
          <Input
            type="number"
            value={form.maximumDiscount ?? ""}
            onChange={(e) => setForm({ ...form, maximumDiscount: Number(e.target.value) })}
          />
        </Field>
        <Field label="Minimum purchase (₹)">
          <Input
            type="number"
            value={form.minimumPurchase ?? ""}
            onChange={(e) => setForm({ ...form, minimumPurchase: Number(e.target.value) })}
          />
        </Field>
        <Field label="Applicable courses (comma separated, blank = all)" className="sm:col-span-2">
          <Input
            onChange={(e) =>
              setForm({
                ...form,
                applicableCourses: e.target.value
                  .split(",")
                  .map((s) => s.trim())
                  .filter(Boolean),
              })
            }
          />
        </Field>
        <Field label="Applicable batches (blank = all)">
          <Input
            onChange={(e) =>
              setForm({
                ...form,
                applicableBatches: e.target.value
                  .split(",")
                  .map((s) => s.trim())
                  .filter(Boolean),
              })
            }
          />
        </Field>
        <Field label="Applicable users (blank = all)">
          <Input
            onChange={(e) =>
              setForm({
                ...form,
                applicableUsers: e.target.value
                  .split(",")
                  .map((s) => s.trim())
                  .filter(Boolean),
              })
            }
          />
        </Field>
        <Field label="Start date">
          <Input
            type="date"
            value={form.startDate ?? ""}
            onChange={(e) => setForm({ ...form, startDate: e.target.value })}
          />
        </Field>
        <Field label="Expiry date">
          <Input
            type="date"
            value={form.expiryDate ?? ""}
            onChange={(e) => setForm({ ...form, expiryDate: e.target.value })}
          />
        </Field>
        <Field label="Usage limit">
          <Input
            type="number"
            value={form.usageLimit ?? ""}
            onChange={(e) => setForm({ ...form, usageLimit: Number(e.target.value) })}
          />
        </Field>
        <Field label="Usage per student">
          <Input
            type="number"
            value={form.usagePerStudent ?? ""}
            onChange={(e) => setForm({ ...form, usagePerStudent: Number(e.target.value) })}
          />
        </Field>
        <Field label="Status">
          <Select
            value={form.status ?? "draft"}
            onValueChange={(v) => setForm({ ...form, status: v as CouponStatus })}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="draft">Draft</SelectItem>
              <SelectItem value="active">Active</SelectItem>
              <SelectItem value="scheduled">Scheduled</SelectItem>
              <SelectItem value="disabled">Disabled</SelectItem>
            </SelectContent>
          </Select>
        </Field>
      </div>

      <DialogFooter>
        <Button onClick={() => void submit()}>Create coupon</Button>
      </DialogFooter>
    </DialogContent>
  );
}

function Field({
  label,
  children,
  className,
}: {
  label: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={className}>
      <Label className="mb-1.5 block text-xs">{label}</Label>
      {children}
    </div>
  );
}
