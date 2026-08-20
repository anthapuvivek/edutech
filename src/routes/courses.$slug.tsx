import { useQuery } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import {
  BadgeCheck,
  BookOpen,
  Clock,
  Globe,
  PlayCircle,
  RefreshCw,
  Star,
  Users,
} from "lucide-react";

import { EmptyState } from "@/components/common/EmptyState";
import { EnquiryForm } from "@/components/courses/EnquiryForm";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { PublicLayout } from "@/layouts/PublicLayout";
import { discountPercent, formatCompact, formatPrice } from "@/lib/format";
import { courseService } from "@/services/course.service";
import type { Course } from "@/types";

export const Route = createFileRoute("/courses/$slug")({
  head: ({ params }) => {
    const readable = params.slug.replace(/-/g, " ");
    return {
      meta: [
        { title: `${readable} — Learntrix Course` },
        {
          name: "description",
          content: `Curriculum, outcomes, instructor profile, reviews and pricing for the Learntrix ${readable} course.`,
        },
        { property: "og:title", content: `${readable} — Learntrix` },
        {
          property: "og:description",
          content: `Explore the curriculum, projects and pricing for the Learntrix ${readable} course.`,
        },
      ],
    };
  },
  component: CourseDetailPage,
});

function buildCurriculum(course: Course) {
  return course.skills.map((skill, index) => ({
    title: `Module ${index + 1} · ${skill}`,
    lessons: [
      { title: `Introduction to ${skill}`, duration: "12:40", preview: index === 0 },
      { title: `${skill} in practice`, duration: "24:15", preview: false },
      { title: `Graded lab: ${skill}`, duration: "45:00", preview: false },
      { title: `Coding problems: ${skill}`, duration: "30:00", preview: false },
    ],
  }));
}

function CourseDetailPage() {
  const { slug } = Route.useParams();
  const query = useQuery({ queryKey: ["course", slug], queryFn: () => courseService.bySlug(slug) });

  if (query.isPending) {
    return (
      <PublicLayout>
        <div className="container-page grid gap-10 py-16 lg:grid-cols-[1.6fr_1fr]">
          <div className="space-y-4">
            <Skeleton className="h-10 w-3/4" />
            <Skeleton className="h-5 w-full" />
            <Skeleton className="h-64 w-full" />
          </div>
          <Skeleton className="h-96 w-full" />
        </div>
      </PublicLayout>
    );
  }

  if (query.isError || !query.data) {
    return (
      <PublicLayout>
        <div className="container-page py-24">
          <EmptyState
            icon={BookOpen}
            title="Course not found"
            description="This course may have been unpublished or the link is incorrect."
            action={
              <Button asChild>
                <Link to="/courses">Back to catalogue</Link>
              </Button>
            }
          />
        </div>
      </PublicLayout>
    );
  }

  const course = query.data;
  const curriculum = buildCurriculum(course);
  const discount = discountPercent(course.price, course.originalPrice);
  const outcomes = [
    `Build production-grade projects using ${course.skills[0]}`,
    "Apply engineering best practices used by senior teams",
    "Solve interview-level problems with confidence",
    "Deploy and monitor your work in a cloud environment",
    "Communicate technical decisions clearly",
    "Prepare a portfolio that stands up to hiring review",
  ];

  return (
    <PublicLayout>
      <section className="bg-ink text-ink-foreground">
        <div className="container-page py-14">
          <nav aria-label="Breadcrumb" className="text-sm text-ink-foreground/60">
            <Link to="/courses" className="hover:text-accent">
              Courses
            </Link>
            <span className="px-2">/</span>
            <span>{course.category}</span>
          </nav>
          <div className="mt-6 max-w-3xl">
            <h1 className="text-display text-4xl sm:text-5xl">{course.title}</h1>
            <p className="mt-4 text-lg text-ink-foreground/70">{course.subtitle}</p>
            <div className="mt-6 flex flex-wrap items-center gap-x-6 gap-y-3 text-sm text-ink-foreground/70">
              <span className="inline-flex items-center gap-1.5 text-accent">
                <Star className="size-4 fill-accent" aria-hidden />
                {course.rating.toFixed(1)}
                <span className="text-ink-foreground/60">
                  ({formatCompact(course.ratingCount)} ratings)
                </span>
              </span>
              <span className="inline-flex items-center gap-1.5">
                <Users className="size-4" aria-hidden /> {formatCompact(course.studentCount)}{" "}
                students
              </span>
              <span className="inline-flex items-center gap-1.5">
                <Clock className="size-4" aria-hidden /> {course.durationHours} hours
              </span>
              <span className="inline-flex items-center gap-1.5">
                <Globe className="size-4" aria-hidden /> {course.language}
              </span>
              <span className="inline-flex items-center gap-1.5">
                <RefreshCw className="size-4" aria-hidden /> Updated{" "}
                {new Date(course.updatedAt).toLocaleDateString("en-GB", {
                  month: "short",
                  year: "numeric",
                })}
              </span>
              <Badge variant="secondary">{course.level}</Badge>
            </div>
            <p className="mt-5 text-sm text-ink-foreground/70">
              Taught by{" "}
              <span className="font-medium text-ink-foreground">{course.instructor.name}</span> ·{" "}
              {course.instructor.title}
            </p>
          </div>
        </div>
      </section>

      <div className="container-page grid gap-12 py-14 lg:grid-cols-[1.6fr_1fr] lg:items-start">
        <div>
          <section aria-labelledby="outcomes-heading" className="surface-panel p-8">
            <h2 id="outcomes-heading" className="text-2xl">
              What you will learn
            </h2>
            <ul className="mt-6 grid gap-3 sm:grid-cols-2">
              {outcomes.map((outcome) => (
                <li key={outcome} className="flex gap-2.5 text-sm">
                  <BadgeCheck className="mt-0.5 size-4 shrink-0 text-success" aria-hidden />
                  {outcome}
                </li>
              ))}
            </ul>
            <div className="mt-8 border-t border-border pt-6">
              <h3 className="text-sm font-semibold">Skills you'll gain</h3>
              <div className="mt-3 flex flex-wrap gap-2">
                {course.skills.map((skill) => (
                  <Badge key={skill} variant="outline">
                    {skill}
                  </Badge>
                ))}
              </div>
            </div>
          </section>

          <section aria-labelledby="curriculum-heading" className="mt-10">
            <h2 id="curriculum-heading" className="text-2xl">
              Curriculum
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              {curriculum.length} modules · {course.lessonCount} lessons · {course.durationHours}{" "}
              hours
            </p>
            <Accordion type="multiple" className="surface-panel mt-5 px-6">
              {curriculum.map((module) => (
                <AccordionItem key={module.title} value={module.title}>
                  <AccordionTrigger className="text-left">{module.title}</AccordionTrigger>
                  <AccordionContent>
                    <ul className="space-y-3">
                      {module.lessons.map((lesson) => (
                        <li
                          key={lesson.title}
                          className="flex items-center justify-between gap-4 text-sm"
                        >
                          <span className="flex items-center gap-2 text-muted-foreground">
                            <PlayCircle className="size-4" aria-hidden />
                            {lesson.title}
                            {lesson.preview ? (
                              <Badge variant="info" className="ml-1">
                                Preview
                              </Badge>
                            ) : null}
                          </span>
                          <span className="tabular-nums text-muted-foreground">
                            {lesson.duration}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </AccordionContent>
                </AccordionItem>
              ))}
            </Accordion>
          </section>

          <section className="mt-12">
            <Tabs defaultValue="about">
              <TabsList>
                <TabsTrigger value="about">About</TabsTrigger>
                <TabsTrigger value="instructor">Instructor</TabsTrigger>
                <TabsTrigger value="reviews">Reviews</TabsTrigger>
                <TabsTrigger value="faq">FAQs</TabsTrigger>
              </TabsList>

              <TabsContent value="about" className="surface-panel mt-5 space-y-6 p-8">
                <div>
                  <h3 className="text-xl">Course description</h3>
                  <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
                    {course.title} is a {course.level.toLowerCase()}-level program covering{" "}
                    {course.skills.join(", ")}. The course pairs {course.lessonCount} structured
                    lessons with graded assignments, capstone projects and a curated coding problem
                    set so that every concept is reinforced through practice.
                  </p>
                </div>
                <div>
                  <h3 className="text-xl">Requirements</h3>
                  <ul className="mt-3 list-disc space-y-1.5 pl-5 text-sm text-muted-foreground">
                    <li>A computer with a stable internet connection</li>
                    <li>Basic programming familiarity is helpful but not mandatory</li>
                    <li>Commitment of 6–8 hours per week</li>
                  </ul>
                </div>
                <div>
                  <h3 className="text-xl">Who this is for</h3>
                  <ul className="mt-3 list-disc space-y-1.5 pl-5 text-sm text-muted-foreground">
                    <li>Students preparing for technology roles</li>
                    <li>Working professionals switching specialisation</li>
                    <li>Engineers deepening expertise in {course.category}</li>
                  </ul>
                </div>
              </TabsContent>

              <TabsContent value="instructor" className="surface-panel mt-5 p-8">
                <h3 className="text-xl">{course.instructor.name}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{course.instructor.title}</p>
                <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
                  {course.instructor.name} has led engineering teams across product companies and
                  teaches with a focus on judgement, trade-offs and production realities rather than
                  syntax recall.
                </p>
                <div className="mt-6 flex gap-8 text-sm">
                  <span>
                    <span className="block text-display text-2xl">
                      {course.instructor.rating?.toFixed(1)}
                    </span>
                    <span className="text-muted-foreground">Instructor rating</span>
                  </span>
                  <span>
                    <span className="block text-display text-2xl">
                      {formatCompact(course.instructor.students ?? 0)}
                    </span>
                    <span className="text-muted-foreground">Students taught</span>
                  </span>
                </div>
              </TabsContent>

              <TabsContent
                value="reviews"
                className="surface-panel mt-5 divide-y divide-border p-8 pt-4"
              >
                {[
                  {
                    name: "Nikhil Verma",
                    body: "Dense, well sequenced and genuinely challenging. The graded labs made the difference.",
                  },
                  {
                    name: "Aisha Rahman",
                    body: "The instructor explains trade-offs rather than just steps. Best course I have taken online.",
                  },
                  {
                    name: "Karthik S",
                    body: "Coding problems tied to each module kept the concepts fresh. Highly recommended.",
                  },
                ].map((review) => (
                  <div key={review.name} className="py-5">
                    <div className="flex items-center gap-2">
                      <p className="font-medium">{review.name}</p>
                      <span className="flex gap-0.5">
                        {Array.from({ length: 5 }).map((_, i) => (
                          <Star key={i} className="size-3.5 fill-accent text-accent" aria-hidden />
                        ))}
                      </span>
                    </div>
                    <p className="mt-2 text-sm text-muted-foreground">{review.body}</p>
                  </div>
                ))}
              </TabsContent>

              <TabsContent value="faq" className="surface-panel mt-5 px-6">
                <Accordion type="single" collapsible>
                  {[
                    {
                      q: "Is there a certificate?",
                      a: "Yes. A verified certificate is issued after all graded assessments are completed.",
                    },
                    {
                      q: "Do I get lifetime access?",
                      a: "Enrolment includes lifetime access to course material and future updates.",
                    },
                    {
                      q: "Is placement support included?",
                      a: "Career-track programs include interview preparation and placement guidance.",
                    },
                  ].map((faq) => (
                    <AccordionItem key={faq.q} value={faq.q}>
                      <AccordionTrigger className="text-left">{faq.q}</AccordionTrigger>
                      <AccordionContent>{faq.a}</AccordionContent>
                    </AccordionItem>
                  ))}
                </Accordion>
              </TabsContent>
            </Tabs>
          </section>

          <section id="enquire" aria-labelledby="enquiry-heading" className="mt-12">
            <h2 id="enquiry-heading" className="text-2xl">
              Enquire about this course
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              An advisor will share the full curriculum, fees and cohort dates.
            </p>
            <div className="mt-5">
              <EnquiryForm courseId={course.id} courseTitle={course.title} />
            </div>
          </section>
        </div>

        {/* Pricing card */}
        <aside className="surface-panel overflow-hidden lg:sticky lg:top-24">
          <img
            src={course.thumbnailUrl}
            alt={`${course.title} cover`}
            loading="lazy"
            width={800}
            height={450}
            className="aspect-16/9 w-full object-cover"
          />
          <div className="p-6">
            <div className="flex items-end gap-3">
              <span className="text-display text-3xl">
                {formatPrice(course.price, course.currency)}
              </span>
              {course.originalPrice ? (
                <span className="pb-1 text-sm text-muted-foreground line-through">
                  {formatPrice(course.originalPrice, course.currency)}
                </span>
              ) : null}
              {discount ? (
                <Badge variant="success" className="mb-1">
                  {discount}% off
                </Badge>
              ) : null}
            </div>
            <div className="mt-5 grid gap-2">
              <Button size="lg">Enrol now</Button>
              <Button size="lg" variant="outline" asChild>
                <a href="#enquire">Enquire</a>
              </Button>
            </div>
            <ul className="mt-6 space-y-3 border-t border-border pt-5 text-sm text-muted-foreground">
              <li className="flex justify-between">
                <span>Lessons</span> <span className="text-foreground">{course.lessonCount}</span>
              </li>
              <li className="flex justify-between">
                <span>Duration</span>{" "}
                <span className="text-foreground">{course.durationHours} hours</span>
              </li>
              <li className="flex justify-between">
                <span>Level</span> <span className="text-foreground">{course.level}</span>
              </li>
              <li className="flex justify-between">
                <span>Certificate</span> <span className="text-foreground">Included</span>
              </li>
              <li className="flex justify-between">
                <span>Access</span> <span className="text-foreground">Lifetime</span>
              </li>
            </ul>
          </div>
        </aside>
      </div>
    </PublicLayout>
  );
}
