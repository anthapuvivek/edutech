import { createFileRoute } from "@tanstack/react-router";
import { FileText, Plus } from "lucide-react";
import { toast } from "sonner";

import { Panel } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";

export const Route = createFileRoute("/admin/career/resume-templates")({
  head: () => ({
    meta: [
      { title: "Resume Templates — Learntrix Admin" },
      { name: "description", content: "Manage ATS-friendly resume templates and map them to courses, roles and experience levels." },
      { property: "og:title", content: "Resume Templates — Learntrix Admin" },
      { property: "og:description", content: "Resume template library and course mapping." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminResumeTemplates,
});

const templates = [
  { id: "t1", name: "Fresher — Software Engineer", level: "Fresher", courses: ["Full Stack Engineering Program"], atsScore: 96, active: true, uses: 1240 },
  { id: "t2", name: "Data Science Practitioner", level: "0–2 years", courses: ["Generative AI & LLM Systems", "Applied Machine Learning"], atsScore: 93, active: true, uses: 618 },
  { id: "t3", name: "Experienced Backend Engineer", level: "3+ years", courses: ["Full Stack Engineering Program"], atsScore: 91, active: false, uses: 210 },
];

function AdminResumeTemplates() {
  return (
    <>
      <PageHeader
        title="Resume Templates"
        description="Templates are ATS-checked by the backend and offered to students based on their course and experience level."
        action={
          <Button onClick={() => toast.info("Templates are uploaded through the template editor.")}>
            <Plus className="size-4" aria-hidden /> Add template
          </Button>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Templates" value={templates.length} icon={FileText} />
        <StatCard label="Active" value={templates.filter((t) => t.active).length} />
        <StatCard label="Resumes generated" value={templates.reduce((s, t) => s + t.uses, 0).toLocaleString()} />
        <StatCard label="Average ATS score" value={`${Math.round(templates.reduce((s, t) => s + t.atsScore, 0) / templates.length)}%`} />
      </section>

      <div className="grid gap-4 md:grid-cols-3">
        {templates.map((t) => (
          <Panel key={t.id} title={t.name} description={`${t.level} · ${t.uses.toLocaleString()} resumes generated`}>
            <div className="flex flex-wrap gap-1.5">
              {t.courses.map((c) => (
                <Badge key={c} variant="outline">
                  {c}
                </Badge>
              ))}
            </div>
            <div className="mt-4 flex items-center justify-between">
              <Badge variant="success">ATS {t.atsScore}%</Badge>
              <label className="flex items-center gap-2 text-xs">
                Active
                <Switch defaultChecked={t.active} onCheckedChange={() => toast.success(`${t.name} updated.`)} aria-label={`Active for ${t.name}`} />
              </label>
            </div>
          </Panel>
        ))}
      </div>
    </>
  );
}
