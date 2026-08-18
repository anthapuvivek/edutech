import { Link, createFileRoute } from "@tanstack/react-router";
import {
  Briefcase,
  Building2,
  FileText,
  Handshake,
  MessagesSquare,
  Route as RouteIcon,
  ScanSearch,
  Target,
} from "lucide-react";

import { SectionHeading } from "@/components/common/SectionHeading";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PublicLayout } from "@/layouts/PublicLayout";

export const Route = createFileRoute("/career")({
  head: () => ({
    meta: [
      { title: "Career & Placement Services — Learntrix" },
      {
        name: "description",
        content:
          "Learntrix career support: job portal, interview preparation, resume builder, company preparation tracks, referrals and mock interviews for eligible enrolled students.",
      },
      { property: "og:title", content: "Career & Placement Services — Learntrix" },
      {
        property: "og:description",
        content: "Job portal, resume builder, company preparation and mock interviews for eligible enrolled students.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: PublicCareerPage,
});

const services = [
  { icon: Briefcase, title: "Job & internship portal", body: "Curated openings matched to your course, skills and coding performance." },
  { icon: Building2, title: "Company preparation tracks", body: "Role-wise preparation plans, question banks and coding sets per company." },
  { icon: FileText, title: "Resume builder", body: "ATS-friendly templates recommended from your enrolled programme." },
  { icon: ScanSearch, title: "ATS resume analysis", body: "Keyword, skills and formatting feedback against a target job description." },
  { icon: MessagesSquare, title: "Mock interviews", body: "Technical, HR, behavioural and coding rounds with a scored report." },
  { icon: Handshake, title: "Referral opportunities", body: "Admin-verified referral openings with a transparent request workflow." },
  { icon: RouteIcon, title: "Career roadmaps", body: "Step-by-step paths from your current skills to your target role." },
  { icon: Target, title: "Skill gap analysis", body: "See exactly what to learn, practise and build next." },
];

const preparationCompanies = ["Google", "Microsoft", "Amazon", "Wipro", "Bosch", "Capgemini", "Infosys", "TCS", "Accenture", "Cognizant"];

function PublicCareerPage() {
  return (
    <PublicLayout>
      <section className="border-b border-border bg-muted/30">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:px-8 lg:py-24">
          <Badge variant="outline" className="mb-4">
            Available to eligible enrolled students
          </Badge>
          <h1 className="text-display max-w-3xl text-4xl leading-tight sm:text-5xl">
            Career support that runs from your first lesson to your first offer
          </h1>
          <p className="mt-4 max-w-2xl text-muted-foreground">
            Learntrix career services combine a curated job portal, company preparation tracks, resume and portfolio
            tooling, mock interviews and referral support inside the student portal. We support your preparation — we
            never guarantee employment.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Button asChild size="lg">
              <Link to="/courses">Explore courses</Link>
            </Button>
            <Button asChild size="lg" variant="outline">
              <Link to="/programs">View eligible programs</Link>
            </Button>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:px-8">
        <SectionHeading
          eyebrow="What's included"
          title="A complete career ecosystem"
          description="Every tool below unlocks inside your student portal once your enrolment makes you eligible."
        />
        <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {services.map((s) => (
            <article key={s.title} className="surface-panel p-6">
              <s.icon className="size-5 text-accent" aria-hidden />
              <h3 className="mt-4 font-medium">{s.title}</h3>
              <p className="mt-2 text-sm text-muted-foreground">{s.body}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="border-y border-border bg-muted/30">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:px-8">
          <SectionHeading
            eyebrow="Company preparation"
            title="Prepare for leading technology employers"
            description="These are preparation tracks built from publicly reported hiring patterns. They are not official company partnerships — partner status is shown only where verified."
          />
          <div className="mt-8 flex flex-wrap gap-2">
            {preparationCompanies.map((c) => (
              <Badge key={c} variant="outline" className="px-3 py-1.5 text-sm">
                {c} · Preparation track
              </Badge>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-3xl px-4 py-16 text-center sm:px-6 lg:px-8">
        <h2 className="text-display text-3xl">Ready to start?</h2>
        <p className="mt-3 text-muted-foreground">
          Career services, private job listings and referral opportunities are available to eligible enrolled students
          only. Enrol in a career programme to activate your career dashboard.
        </p>
        <Button asChild size="lg" className="mt-6">
          <Link to="/courses">Explore courses</Link>
        </Button>
      </section>
    </PublicLayout>
  );
}
