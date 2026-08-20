import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  ArrowRight,
  Award,
  BrainCircuit,
  Braces,
  Briefcase,
  CheckCircle2,
  GraduationCap,
  LineChart,
  MessagesSquare,
  Quote,
  ShieldCheck,
  Sparkles,
  Star,
  Terminal,
  Trophy,
} from "lucide-react";

import heroImage from "@/assets/hero.jpg";
import { EmptyState } from "@/components/common/EmptyState";
import { SectionHeading } from "@/components/common/SectionHeading";
import { CourseCard, CourseCardSkeleton } from "@/components/courses/CourseCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PublicLayout } from "@/layouts/PublicLayout";
import { courseService } from "@/services/course.service";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Learntrix — Premium Technology Education & Career Programs" },
      {
        name: "description",
        content:
          "Learntrix turns ambitious learners into technology professionals with expert-led courses, hands-on coding practice and AI-powered learning.",
      },
      { property: "og:title", content: "Learntrix — Premium Technology Education" },
      {
        property: "og:description",
        content:
          "Expert-led courses, hands-on coding practice and AI-powered learning built for serious career growth.",
      },
    ],
  }),
  component: HomePage,
});

const whyChooseUs = [
  {
    icon: GraduationCap,
    title: "Industry-focused curriculum",
    body: "Programs designed with senior engineers and refreshed every quarter.",
  },
  {
    icon: Braces,
    title: "Hands-on projects",
    body: "Ship production-style projects reviewed against real engineering standards.",
  },
  {
    icon: Terminal,
    title: "Integrated coding practice",
    body: "Thousands of curated problems mapped directly to your course modules.",
  },
  {
    icon: BrainCircuit,
    title: "AI-powered learning",
    body: "Context-aware tutoring that understands the lesson you are studying.",
  },
  {
    icon: ShieldCheck,
    title: "Expert instructors",
    body: "Practitioners from leading product companies, not career trainers.",
  },
  {
    icon: Briefcase,
    title: "Career support",
    body: "Interview preparation, resume reviews and structured placement guidance.",
  },
  {
    icon: Award,
    title: "Verified certificates",
    body: "Shareable credentials backed by assessed projects and evaluations.",
  },
  {
    icon: LineChart,
    title: "Progress tracking",
    body: "Granular analytics across lessons, quizzes, projects and problem solving.",
  },
];

function HomePage() {
  const featured = useQuery({
    queryKey: ["courses", "featured"],
    queryFn: () => courseService.featured(6),
  });
  const categories = useQuery({
    queryKey: ["categories"],
    queryFn: () => courseService.categories(),
  });
  const testimonials = useQuery({
    queryKey: ["testimonials"],
    queryFn: () => courseService.testimonials(),
  });
  const stats = useQuery({ queryKey: ["stats"], queryFn: () => courseService.stats() });

  return (
    <PublicLayout>
      {/* Hero */}
      <section className="relative overflow-hidden bg-ink text-ink-foreground">
        <div className="container-page grid gap-14 py-20 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:py-28">
          <div>
            <Badge variant="outline" className="border-accent/40 bg-accent/10 text-accent">
              Admissions open · Cohort 2026
            </Badge>
            <h1 className="mt-6 text-display text-4xl sm:text-5xl lg:text-6xl">
              Become the technology professional the industry is hiring for.
            </h1>
            <p className="mt-6 max-w-xl text-base leading-relaxed text-ink-foreground/70 sm:text-lg">
              Learntrix blends a rigorous academic curriculum with hands-on engineering practice,
              integrated coding challenges and an AI tutor that knows exactly what you are learning.
            </p>
            <div className="mt-9 flex flex-wrap gap-3">
              <Button size="lg" variant="accent" asChild>
                <Link to="/courses">
                  Explore courses <ArrowRight className="size-4" />
                </Link>
              </Button>
              <Button size="lg" variant="onDark" asChild>
                <Link to="/register">Start learning</Link>
              </Button>
            </div>
            <ul className="mt-10 flex flex-wrap gap-x-8 gap-y-3 text-sm text-ink-foreground/70">
              {["Career-track programs", "Live mentor support", "Placement guidance"].map(
                (item) => (
                  <li key={item} className="inline-flex items-center gap-2">
                    <CheckCircle2 className="size-4 text-accent" aria-hidden />
                    {item}
                  </li>
                ),
              )}
            </ul>
          </div>

          <div className="relative">
            <div className="overflow-hidden rounded-xl border border-ink-foreground/10 shadow-[var(--shadow-float)]">
              <img
                src={heroImage}
                alt="A learner studying software engineering at Learntrix"
                width={1600}
                height={1200}
                className="size-full object-cover"
              />
            </div>
            <div className="absolute -bottom-6 -left-4 hidden rounded-lg border border-ink-foreground/10 bg-card p-5 text-card-foreground shadow-[var(--shadow-float)] sm:block">
              <p className="eyebrow">Weekly progress</p>
              <p className="mt-2 text-display text-3xl">92%</p>
              <p className="text-xs text-muted-foreground">
                Course completion across active cohorts
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Stats */}
      <section className="border-b border-border bg-surface">
        <div className="container-page grid grid-cols-2 gap-8 py-12 lg:grid-cols-4">
          {(stats.data ?? []).map((stat) => (
            <div key={stat.id}>
              <p className="text-display text-3xl sm:text-4xl">{stat.value}</p>
              <p className="mt-1 text-sm text-muted-foreground">{stat.label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Featured courses */}
      <section className="container-page py-20">
        <SectionHeading
          eyebrow="Featured programs"
          title="Courses built for measurable career outcomes"
          description="Every program pairs structured theory with graded projects, assessments and coding practice."
          action={
            <Button variant="outline" asChild>
              <Link to="/courses">
                Browse catalogue <ArrowRight className="size-4" />
              </Link>
            </Button>
          }
        />
        <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {featured.isPending
            ? Array.from({ length: 6 }).map((_, i) => <CourseCardSkeleton key={i} />)
            : featured.data?.map((course) => <CourseCard key={course.id} course={course} />)}
        </div>
        {featured.isError ? (
          <EmptyState
            title="Courses could not be loaded"
            description="Please refresh the page to try again."
            action={<Button onClick={() => featured.refetch()}>Retry</Button>}
          />
        ) : null}
      </section>

      {/* Categories */}
      <section className="bg-surface py-20">
        <div className="container-page">
          <SectionHeading
            eyebrow="Learning categories"
            title="Choose your discipline"
            description="Twelve specialisations spanning software engineering, data, cloud and security."
            align="center"
          />
          <div className="mt-10 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {(categories.data ?? []).map((category) => (
              <Link
                key={category.id}
                to="/courses"
                search={{ category: category.name }}
                className="surface-panel group flex items-center justify-between gap-4 p-5 transition-colors hover:border-primary/40"
              >
                <div>
                  <p className="font-medium">{category.name}</p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {category.courseCount} courses
                  </p>
                </div>
                <ArrowRight className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-1 group-hover:text-primary" />
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Why choose us */}
      <section className="container-page py-20">
        <SectionHeading
          eyebrow="Why Learntrix"
          title="An academic standard with an engineering culture"
          align="center"
        />
        <div className="mt-12 grid gap-x-10 gap-y-10 sm:grid-cols-2 lg:grid-cols-4">
          {whyChooseUs.map((item) => (
            <div key={item.title}>
              <span className="grid size-10 place-items-center rounded-md bg-primary/8 text-primary">
                <item.icon className="size-5" aria-hidden />
              </span>
              <h3 className="mt-4 text-lg">{item.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{item.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Coding practice + AI learning */}
      <section className="container-page grid gap-6 pb-20 lg:grid-cols-2">
        <div className="surface-panel flex flex-col justify-between gap-8 bg-ink p-10 text-ink-foreground">
          <div>
            <p className="eyebrow text-accent">Coding practice</p>
            <h2 className="mt-3 text-3xl">A practice arena engineered for interviews</h2>
            <ul className="mt-6 space-y-3 text-sm text-ink-foreground/75">
              {[
                "3,000+ curated problems across data structures and systems",
                "Python, Java, C++, JavaScript and C# support",
                "Easy, medium and hard difficulty progression",
                "Weekly contests, leaderboards and streaks",
                "Real-time execution powered by a secure backend runner",
              ].map((line) => (
                <li key={line} className="flex gap-3">
                  <Trophy className="mt-0.5 size-4 shrink-0 text-accent" aria-hidden />
                  {line}
                </li>
              ))}
            </ul>
          </div>
          <Button variant="accent" className="self-start" asChild>
            <Link to="/coding-practice">
              Explore coding practice <ArrowRight className="size-4" />
            </Link>
          </Button>
        </div>

        <div className="surface-panel flex flex-col justify-between gap-8 p-10">
          <div>
            <p className="eyebrow">AI learning</p>
            <h2 className="mt-3 text-3xl">Guidance that adapts to every lesson</h2>
            <div className="mt-6 grid gap-4 sm:grid-cols-2">
              {[
                {
                  icon: Sparkles,
                  title: "AI Tutor",
                  body: "Explains concepts using your current course context.",
                },
                {
                  icon: Terminal,
                  title: "AI Coding Coach",
                  body: "Reviews failed submissions and suggests approaches.",
                },
                {
                  icon: MessagesSquare,
                  title: "Course Assistant",
                  body: "Answers lesson questions with cited material.",
                },
                {
                  icon: LineChart,
                  title: "Personalised paths",
                  body: "Recommends what to study next based on performance.",
                },
              ].map((item) => (
                <div key={item.title} className="rounded-lg border border-border p-4">
                  <item.icon className="size-4 text-primary" aria-hidden />
                  <p className="mt-3 font-medium">{item.title}</p>
                  <p className="mt-1 text-sm text-muted-foreground">{item.body}</p>
                </div>
              ))}
            </div>
          </div>
          <Button variant="outline" className="self-start" asChild>
            <Link to="/ai-learning">
              Discover AI learning <ArrowRight className="size-4" />
            </Link>
          </Button>
        </div>
      </section>

      {/* Career */}
      <section className="bg-surface py-20">
        <div className="container-page grid gap-12 lg:grid-cols-[0.9fr_1.1fr] lg:items-center">
          <SectionHeading
            eyebrow="Career development"
            title="From first lesson to first offer"
            description="Career outcomes are engineered into the curriculum, not offered as an afterthought."
          />
          <div className="grid gap-4 sm:grid-cols-2">
            {[
              {
                title: "Portfolio projects",
                body: "Four graded, production-style builds per career track.",
              },
              {
                title: "Skill development",
                body: "Competency mapping against real hiring requirements.",
              },
              {
                title: "Interview preparation",
                body: "Mock interviews, DSA sprints and system design drills.",
              },
              {
                title: "Resume & profile support",
                body: "Reviews from hiring managers and recruiters.",
              },
            ].map((item) => (
              <div key={item.title} className="surface-panel p-6">
                <h3 className="text-lg">{item.title}</h3>
                <p className="mt-2 text-sm text-muted-foreground">{item.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Testimonials */}
      <section className="container-page py-20">
        <SectionHeading
          eyebrow="Student outcomes"
          title="Trusted by engineers building serious careers"
          align="center"
        />
        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {(testimonials.data ?? []).map((item) => (
            <figure key={item.id} className="surface-panel flex h-full flex-col p-6">
              <Quote className="size-5 text-accent" aria-hidden />
              <blockquote className="mt-4 flex-1 text-sm leading-relaxed text-foreground/90">
                {item.quote}
              </blockquote>
              <figcaption className="mt-6 border-t border-border pt-4">
                <p className="font-medium">{item.name}</p>
                <p className="text-sm text-muted-foreground">
                  {item.role} · {item.company}
                </p>
                <div className="mt-2 flex gap-0.5" aria-label={`${item.rating} out of 5`}>
                  {Array.from({ length: item.rating }).map((_, i) => (
                    <Star key={i} className="size-3.5 fill-accent text-accent" aria-hidden />
                  ))}
                </div>
              </figcaption>
            </figure>
          ))}
        </div>
      </section>

      {/* Final CTA */}
      <section className="container-page pb-4">
        <div className="surface-panel flex flex-col items-center gap-6 bg-ink px-8 py-16 text-center text-ink-foreground">
          <h2 className="max-w-2xl text-3xl sm:text-4xl">
            Your next role starts with a structured plan and daily practice.
          </h2>
          <p className="max-w-xl text-ink-foreground/70">
            Join thousands of learners advancing their careers with Learntrix programs.
          </p>
          <div className="flex flex-wrap justify-center gap-3">
            <Button size="lg" variant="accent" asChild>
              <Link to="/register">Create your account</Link>
            </Button>
            <Button size="lg" variant="onDark" asChild>
              <Link to="/contact">Talk to an advisor</Link>
            </Button>
          </div>
        </div>
      </section>
    </PublicLayout>
  );
}
