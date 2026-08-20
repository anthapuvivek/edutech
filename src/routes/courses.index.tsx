import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { Search, SlidersHorizontal } from "lucide-react";
import { useEffect, useState } from "react";
import { z } from "zod";

import { EmptyState } from "@/components/common/EmptyState";
import { CourseCard, CourseCardSkeleton } from "@/components/courses/CourseCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
} from "@/components/ui/pagination";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Slider } from "@/components/ui/slider";
import { PublicLayout } from "@/layouts/PublicLayout";
import { formatPrice } from "@/lib/format";
import { courseService } from "@/services/course.service";
import type { CourseQuery } from "@/types";

const searchSchema = z.object({
  search: z.string().optional(),
  category: z.string().optional(),
  level: z.enum(["All", "Beginner", "Intermediate", "Advanced"]).optional(),
  maxPrice: z.number().optional(),
  minRating: z.number().optional(),
  sort: z.enum(["popular", "rating", "newest", "price_low", "price_high"]).optional(),
  page: z.number().optional(),
});

export const Route = createFileRoute("/courses/")({
  validateSearch: searchSchema,
  head: () => ({
    meta: [
      { title: "Course Catalogue — Learntrix Technology Courses" },
      {
        name: "description",
        content:
          "Browse Learntrix courses across full stack development, data science, AI, cloud, DevOps and cyber security with filters for level, price and rating.",
      },
      { property: "og:title", content: "Learntrix Course Catalogue" },
      {
        property: "og:description",
        content: "Filter expert-led technology courses by category, level, price and rating.",
      },
    ],
  }),
  component: CourseCataloguePage,
});

const PAGE_SIZE = 9;

function CourseCataloguePage() {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/courses/" });
  const [searchInput, setSearchInput] = useState(search.search ?? "");
  const [priceCeiling, setPriceCeiling] = useState(search.maxPrice ?? 30000);

  useEffect(() => setSearchInput(search.search ?? ""), [search.search]);

  const update = (patch: Partial<z.infer<typeof searchSchema>>) => {
    void navigate({ search: (prev) => ({ ...prev, page: 1, ...patch }), replace: true });
  };

  useEffect(() => {
    const id = setTimeout(() => {
      if ((search.search ?? "") !== searchInput) update({ search: searchInput || undefined });
    }, 350);
    return () => clearTimeout(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchInput]);

  const categories = useQuery({
    queryKey: ["categories"],
    queryFn: () => courseService.categories(),
  });

  const query: CourseQuery = {
    search: search.search,
    category: search.category,
    level: search.level,
    maxPrice: search.maxPrice,
    minRating: search.minRating,
    sort: search.sort ?? "popular",
    page: search.page ?? 1,
    pageSize: PAGE_SIZE,
  };

  const courses = useQuery({
    queryKey: ["courses", query],
    queryFn: () => courseService.list(query),
    placeholderData: keepPreviousData,
  });

  const total = courses.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const currentPage = query.page ?? 1;

  const hasFilters =
    Boolean(search.search) ||
    Boolean(search.category) ||
    (search.level && search.level !== "All") ||
    typeof search.maxPrice === "number" ||
    typeof search.minRating === "number";

  return (
    <PublicLayout>
      <section className="border-b border-border bg-surface py-14">
        <div className="container-page">
          <p className="eyebrow">Course catalogue</p>
          <h1 className="mt-3 text-display text-4xl">Find the right course for your next step</h1>
          <p className="mt-3 max-w-2xl text-muted-foreground">
            {total} course{total === 1 ? "" : "s"} across engineering, data, AI, cloud and security.
          </p>
        </div>
      </section>

      <div className="container-page grid gap-10 py-12 lg:grid-cols-[280px_1fr]">
        <aside aria-label="Course filters" className="surface-panel h-fit p-6 lg:sticky lg:top-24">
          <div className="flex items-center gap-2">
            <SlidersHorizontal className="size-4 text-muted-foreground" aria-hidden />
            <h2 className="text-lg">Filters</h2>
          </div>

          <div className="mt-6 space-y-6">
            <div className="space-y-2">
              <Label htmlFor="course-search">Search</Label>
              <div className="relative">
                <Search
                  className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
                  aria-hidden
                />
                <Input
                  id="course-search"
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="Search courses or skills"
                  className="pl-9"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="filter-category">Category</Label>
              <Select
                value={search.category ?? "All"}
                onValueChange={(value) => update({ category: value === "All" ? undefined : value })}
              >
                <SelectTrigger id="filter-category">
                  <SelectValue placeholder="All categories" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="All">All categories</SelectItem>
                  {(categories.data ?? []).map((c) => (
                    <SelectItem key={c.id} value={c.name}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="filter-level">Difficulty</Label>
              <Select
                value={search.level ?? "All"}
                onValueChange={(value) => update({ level: value as "All" })}
              >
                <SelectTrigger id="filter-level">
                  <SelectValue placeholder="All levels" />
                </SelectTrigger>
                <SelectContent>
                  {["All", "Beginner", "Intermediate", "Advanced"].map((level) => (
                    <SelectItem key={level} value={level}>
                      {level}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-3">
              <Label htmlFor="filter-price">Max price · {formatPrice(priceCeiling)}</Label>
              <Slider
                id="filter-price"
                min={5000}
                max={30000}
                step={1000}
                value={[priceCeiling]}
                onValueChange={([value]) => setPriceCeiling(value ?? 30000)}
                onValueCommit={([value]) => update({ maxPrice: value })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="filter-rating">Minimum rating</Label>
              <Select
                value={search.minRating ? String(search.minRating) : "0"}
                onValueChange={(value) =>
                  update({ minRating: value === "0" ? undefined : Number(value) })
                }
              >
                <SelectTrigger id="filter-rating">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="0">Any rating</SelectItem>
                  <SelectItem value="4.5">4.5 and above</SelectItem>
                  <SelectItem value="4.7">4.7 and above</SelectItem>
                  <SelectItem value="4.8">4.8 and above</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {hasFilters ? (
              <Button
                variant="outline"
                className="w-full"
                onClick={() => {
                  setPriceCeiling(30000);
                  void navigate({ search: {}, replace: true });
                }}
              >
                Clear all filters
              </Button>
            ) : null}
          </div>
        </aside>

        <section aria-label="Course results">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-muted-foreground">
              Showing {courses.data?.items.length ?? 0} of {total} courses
            </p>
            <Select
              value={search.sort ?? "popular"}
              onValueChange={(value) => update({ sort: value as CourseQuery["sort"] })}
            >
              <SelectTrigger className="w-52" aria-label="Sort courses">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="popular">Most popular</SelectItem>
                <SelectItem value="rating">Highest rated</SelectItem>
                <SelectItem value="newest">Recently updated</SelectItem>
                <SelectItem value="price_low">Price: low to high</SelectItem>
                <SelectItem value="price_high">Price: high to low</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {courses.isError ? (
            <div className="mt-8">
              <EmptyState
                title="We couldn't load courses"
                description="Something went wrong while fetching the catalogue."
                action={<Button onClick={() => courses.refetch()}>Try again</Button>}
              />
            </div>
          ) : courses.isPending ? (
            <div className="mt-8 grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <CourseCardSkeleton key={i} />
              ))}
            </div>
          ) : total === 0 ? (
            <div className="mt-8">
              <EmptyState
                title="No courses match these filters"
                description="Try widening your price range or clearing a filter."
                action={
                  <Button
                    variant="outline"
                    onClick={() => void navigate({ search: {}, replace: true })}
                  >
                    Clear filters
                  </Button>
                }
              />
            </div>
          ) : (
            <>
              <div className="mt-8 grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
                {courses.data.items.map((course) => (
                  <CourseCard key={course.id} course={course} />
                ))}
              </div>

              {pageCount > 1 ? (
                <Pagination className="mt-12">
                  <PaginationContent>
                    {Array.from({ length: pageCount }).map((_, i) => (
                      <PaginationItem key={i}>
                        <PaginationLink
                          href="#"
                          isActive={currentPage === i + 1}
                          onClick={(e) => {
                            e.preventDefault();
                            void navigate({ search: (prev) => ({ ...prev, page: i + 1 }) });
                          }}
                        >
                          {i + 1}
                        </PaginationLink>
                      </PaginationItem>
                    ))}
                  </PaginationContent>
                </Pagination>
              ) : null}
            </>
          )}
        </section>
      </div>
    </PublicLayout>
  );
}
