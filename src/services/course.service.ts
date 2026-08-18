import { env } from "@/lib/env";
import { mockCategories, mockCourses, mockStats, mockTestimonials } from "@/mock/courses";
import { apiRequest, mockDelay } from "@/services/api-client";
import type { Category, Course, CourseQuery, Paginated, PlatformStat, Testimonial } from "@/types";

function filterAndSort(query: CourseQuery): Course[] {
  const { search, category, level, maxPrice, minRating, sort = "popular" } = query;
  let items = [...mockCourses];

  if (search) {
    const q = search.toLowerCase();
    items = items.filter(
      (c) =>
        c.title.toLowerCase().includes(q) ||
        c.subtitle.toLowerCase().includes(q) ||
        c.category.toLowerCase().includes(q) ||
        c.skills.some((s) => s.toLowerCase().includes(q)),
    );
  }
  if (category && category !== "All") items = items.filter((c) => c.category === category);
  if (level && level !== "All") items = items.filter((c) => c.level === level);
  if (typeof maxPrice === "number") items = items.filter((c) => c.price <= maxPrice);
  if (typeof minRating === "number") items = items.filter((c) => c.rating >= minRating);

  const sorters: Record<string, (a: Course, b: Course) => number> = {
    popular: (a, b) => b.studentCount - a.studentCount,
    rating: (a, b) => b.rating - a.rating,
    newest: (a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt),
    price_low: (a, b) => a.price - b.price,
    price_high: (a, b) => b.price - a.price,
  };
  return items.sort(sorters[sort] ?? sorters["popular"]!);
}

export const courseService = {
  async list(query: CourseQuery = {}): Promise<Paginated<Course>> {
    const page = query.page ?? 1;
    const pageSize = query.pageSize ?? 9;

    if (!env.useMocks) {
      return apiRequest<Paginated<Course>>("/courses", { query: { ...query, page, pageSize } });
    }

    const filtered = filterAndSort(query);
    return mockDelay({
      items: filtered.slice((page - 1) * pageSize, page * pageSize),
      page,
      pageSize,
      total: filtered.length,
    });
  },

  async featured(limit = 6): Promise<Course[]> {
    if (!env.useMocks) return apiRequest<Course[]>("/courses/featured", { query: { limit } });
    return mockDelay(filterAndSort({ sort: "popular" }).slice(0, limit));
  },

  async bySlug(slug: string): Promise<Course | null> {
    if (!env.useMocks) return apiRequest<Course | null>(`/courses/${slug}`);
    return mockDelay(mockCourses.find((c) => c.slug === slug) ?? null);
  },

  async categories(): Promise<Category[]> {
    if (!env.useMocks) return apiRequest<Category[]>("/categories");
    return mockDelay(mockCategories);
  },

  async testimonials(): Promise<Testimonial[]> {
    if (!env.useMocks) return apiRequest<Testimonial[]>("/testimonials");
    return mockDelay(mockTestimonials);
  },

  async stats(): Promise<PlatformStat[]> {
    if (!env.useMocks) return apiRequest<PlatformStat[]>("/stats");
    return mockDelay(mockStats);
  },
};
