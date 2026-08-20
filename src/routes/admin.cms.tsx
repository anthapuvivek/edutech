import { createFileRoute } from "@tanstack/react-router";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";

export const Route = createFileRoute("/admin/cms")({
  head: () => ({
    meta: [
      { title: "Website Content — Learntrix Admin" },
      {
        name: "description",
        content:
          "Manage homepage banners, highlights, testimonials, FAQs and public site copy without a code change.",
      },
      { property: "og:title", content: "Website Content — Learntrix Admin" },
      {
        property: "og:description",
        content: "Content management for the public Learntrix website.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminCms,
});

const banners = [
  { id: "b1", title: "Admissions open · Full Stack Cohort 12", cta: "Explore program", live: true },
  { id: "b2", title: "Free Gen AI demo class this Saturday", cta: "Reserve a seat", live: true },
  { id: "b3", title: "Placement report 2025", cta: "Download report", live: false },
];

const testimonials = [
  { id: "t1", name: "Ishita Nair", role: "SDE at Microsoft", approved: true },
  { id: "t2", name: "Rohan Gupta", role: "Data Analyst at Wipro", approved: true },
  { id: "t3", name: "Sneha Reddy", role: "Backend Engineer at Zoho", approved: false },
];

function AdminCms() {
  return (
    <>
      <PageHeader
        title="Website Content"
        description="Everything on the public site that changes often is editable here."
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Banners"
          value={banners.length}
          hint={`${banners.filter((b) => b.live).length} live`}
        />
        <StatCard label="Testimonials" value={testimonials.length} />
        <StatCard label="Pending approval" value={testimonials.filter((t) => !t.approved).length} />
        <StatCard label="FAQ entries" value={18} />
      </section>

      <Tabs defaultValue="banners">
        <TabsList className="mb-4">
          <TabsTrigger value="banners">Banners</TabsTrigger>
          <TabsTrigger value="testimonials">Testimonials</TabsTrigger>
          <TabsTrigger value="copy">Homepage copy</TabsTrigger>
        </TabsList>

        <TabsContent value="banners">
          <Panel title="Homepage banners">
            <ul className="divide-y divide-border">
              {banners.map((b) => (
                <li key={b.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                  <div>
                    <p className="text-sm font-medium">{b.title}</p>
                    <p className="text-xs text-muted-foreground">CTA · {b.cta}</p>
                  </div>
                  <label className="flex items-center gap-2 text-xs">
                    Live
                    <Switch
                      defaultChecked={b.live}
                      onCheckedChange={() => toast.success("Banner visibility updated.")}
                      aria-label={`Live for ${b.title}`}
                    />
                  </label>
                </li>
              ))}
            </ul>
          </Panel>
        </TabsContent>

        <TabsContent value="testimonials">
          <Panel title="Testimonials">
            <ul className="divide-y divide-border">
              {testimonials.map((t) => (
                <li key={t.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                  <div>
                    <p className="text-sm font-medium">{t.name}</p>
                    <p className="text-xs text-muted-foreground">{t.role}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={t.approved ? "success" : "outline"}>
                      {t.approved ? "Approved" : "Pending"}
                    </Badge>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => toast.success("Testimonial updated.")}
                    >
                      {t.approved ? "Unpublish" : "Approve"}
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          </Panel>
        </TabsContent>

        <TabsContent value="copy">
          <Panel title="Homepage copy">
            <form
              className="grid gap-4"
              onSubmit={(e) => {
                e.preventDefault();
                toast.success("Homepage copy saved.");
              }}
            >
              <div className="space-y-1.5">
                <Label htmlFor="heroTitle">Hero title</Label>
                <Input
                  id="heroTitle"
                  defaultValue="Learn with rigour. Get placed with confidence."
                  maxLength={120}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="heroSubtitle">Hero subtitle</Label>
                <Textarea
                  id="heroSubtitle"
                  rows={3}
                  maxLength={400}
                  defaultValue="Industry-grade programs, live mentorship and a placement ecosystem built for outcomes."
                />
              </div>
              <div>
                <Button type="submit">Save copy</Button>
              </div>
            </form>
          </Panel>
        </TabsContent>
      </Tabs>
    </>
  );
}
