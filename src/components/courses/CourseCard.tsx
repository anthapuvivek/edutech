import { Link } from "@tanstack/react-router";
import { Clock, Star, Users } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { discountPercent, formatCompact, formatPrice } from "@/lib/format";
import type { Course } from "@/types";

export function CourseCard({ course }: { course: Course }) {
  const discount = discountPercent(course.price, course.originalPrice);

  return (
    <article className="group surface-panel flex h-full flex-col overflow-hidden transition-shadow duration-300 hover:shadow-[var(--shadow-elevated)]">
      <Link
        to="/courses/$slug"
        params={{ slug: course.slug }}
        className="relative block aspect-16/9 overflow-hidden bg-muted focus-visible:outline-2 focus-visible:outline-ring"
      >
        <img
          src={course.thumbnailUrl}
          alt={`${course.title} course cover`}
          loading="lazy"
          width={800}
          height={450}
          className="size-full object-cover transition-transform duration-500 group-hover:scale-[1.03]"
        />
        {course.badges?.length ? (
          <div className="absolute left-3 top-3 flex gap-1.5">
            {course.badges.map((badge) => (
              <Badge key={badge} variant={badge === "New" ? "secondary" : "default"}>
                {badge}
              </Badge>
            ))}
          </div>
        ) : null}
      </Link>

      <div className="flex flex-1 flex-col p-5">
        <p className="eyebrow">{course.category}</p>
        <h3 className="mt-2 text-lg leading-snug">
          <Link
            to="/courses/$slug"
            params={{ slug: course.slug }}
            className="transition-colors hover:text-primary"
          >
            {course.title}
          </Link>
        </h3>
        <p className="mt-1.5 line-clamp-2 text-sm text-muted-foreground">{course.subtitle}</p>
        {course.instructor ? (
          <p className="mt-3 text-sm text-muted-foreground">{course.instructor.name}</p>
        ) : null}

        <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-muted-foreground">
          <span className="inline-flex items-center gap-1 font-medium text-foreground">
            <Star className="size-3.5 fill-accent text-accent" aria-hidden />
            {course.rating.toFixed(1)}
            <span className="font-normal text-muted-foreground">
              ({formatCompact(course.ratingCount)})
            </span>
          </span>
          <span className="inline-flex items-center gap-1">
            <Users className="size-3.5" aria-hidden /> {formatCompact(course.studentCount)}
          </span>
          <span className="inline-flex items-center gap-1">
            <Clock className="size-3.5" aria-hidden /> {course.durationHours}h
          </span>
          <span>{course.level}</span>
        </div>

        <div className="mt-5 flex items-end justify-between border-t border-border pt-4">
          <div>
            <span className="text-display text-xl">
              {formatPrice(course.price, course.currency)}
            </span>
            {course.originalPrice ? (
              <span className="ml-2 text-sm text-muted-foreground line-through">
                {formatPrice(course.originalPrice, course.currency)}
              </span>
            ) : null}
          </div>
          {discount ? (
            <Badge variant="outline" className="border-success/40 text-success">
              {discount}% off
            </Badge>
          ) : null}
        </div>
      </div>
    </article>
  );
}

export function CourseCardSkeleton() {
  return (
    <div className="surface-panel overflow-hidden">
      <Skeleton className="aspect-16/9 w-full rounded-none" />
      <div className="space-y-3 p-5">
        <Skeleton className="h-3 w-24" />
        <Skeleton className="h-5 w-4/5" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-8 w-full" />
      </div>
    </div>
  );
}
