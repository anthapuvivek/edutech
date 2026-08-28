import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Bookmark, MapPin } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { jobService } from "@/services/job.service";

export const Route = createFileRoute("/student/career/jobs")({
  head: () => ({
    meta: [
      { title: "Job Portal — Learntrix Careers" },
      {
        name: "description",
        content:
          "Search curated jobs and internships matched to your course, skills and coding performance.",
      },
      { property: "og:title", content: "Job Portal — Learntrix Careers" },
      {
        property: "og:description",
        content: "Curated opportunities for eligible enrolled Learntrix students.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: JobPortalPage,
});

function JobPortalPage() {
  const [search, setSearch] = useState("");
  const [type, setType] = useState("all");
  const [workMode, setWorkMode] = useState("all");
  const [sort, setSort] = useState<"relevance" | "newest" | "deadline">("relevance");

  const jobs = useQuery({
    queryKey: ["career", "jobs", { search, type, workMode, sort }],
    queryFn: () => jobService.list({ search, type, workMode, sort }),
    placeholderData: keepPreviousData,
  });

  return (
    <>
      <PageHeader
        title="Job portal"
        description="Eligibility for each opportunity is verified by the placement team before your application is forwarded."
      />

      <div className="surface-panel mb-6 grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-4">
        <Input
          placeholder="Search role, company or skill"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Search jobs"
        />
        <Select value={type} onValueChange={setType}>
          <SelectTrigger aria-label="Job type">
            <SelectValue placeholder="Job type" />
          </SelectTrigger>
          <SelectContent>
            {["all", "Full Time", "Internship", "Part Time", "Contract", "Graduate/Fresher"].map(
              (v) => (
                <SelectItem key={v} value={v}>
                  {v === "all" ? "All job types" : v}
                </SelectItem>
              ),
            )}
          </SelectContent>
        </Select>
        <Select value={workMode} onValueChange={setWorkMode}>
          <SelectTrigger aria-label="Work mode">
            <SelectValue placeholder="Work mode" />
          </SelectTrigger>
          <SelectContent>
            {["all", "Remote", "Hybrid", "Onsite"].map((v) => (
              <SelectItem key={v} value={v}>
                {v === "all" ? "Any work mode" : v}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={sort} onValueChange={(v) => setSort(v as typeof sort)}>
          <SelectTrigger aria-label="Sort jobs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="relevance">Sort · Relevance</SelectItem>
            <SelectItem value="newest">Sort · Newest</SelectItem>
            <SelectItem value="deadline">Sort · Deadline</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {jobs.isLoading ? (
        <div className="grid gap-4 md:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-44 w-full" />
          ))}
        </div>
      ) : !jobs.data?.length ? (
        <EmptyState
          title="No matching opportunities"
          description="Try a different keyword or clear your filters."
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {jobs.data.map((job) => (
            <article key={job.id} className="surface-panel flex flex-col gap-3 p-5">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-display text-xl leading-tight">{job.title}</h2>
                  <p className="text-sm text-muted-foreground">{job.companyName}</p>
                </div>
                <Badge variant="success">Match {job.matchPercent}%</Badge>
              </div>

              <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                <span className="inline-flex items-center gap-1">
                  <MapPin className="size-3.5" aria-hidden /> {job.location}
                </span>
                <span>{job.workMode}</span>
                <span>{job.type}</span>
                <span>{job.experience}</span>
                {job.salaryRange ? <span>{job.salaryRange}</span> : null}
              </p>

              <div className="flex flex-wrap gap-2">
                {(job.skills ?? []).map((s) => (
                  <Badge key={s} variant="outline">
                    {s}
                  </Badge>
                ))}
              </div>

              <p className="text-xs text-muted-foreground">
                Posted {new Date(job.postedAt).toLocaleDateString()} · Closes{" "}
                {new Date(job.deadline).toLocaleDateString()}
              </p>

              <div className="mt-auto flex flex-wrap gap-2">
                <Button
                  size="sm"
                  onClick={() => toast.success(`Application started for ${job.title}`)}
                >
                  Apply now
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => toast.success(job.saved ? "Removed from saved jobs" : "Job saved")}
                >
                  <Bookmark className="size-3.5" aria-hidden /> {job.saved ? "Saved" : "Save"}
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => toast.info("Referral requests open in Phase 3F")}
                >
                  Refer me
                </Button>
              </div>
            </article>
          ))}
        </div>
      )}
    </>
  );
}
