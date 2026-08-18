import { useQuery } from "@tanstack/react-query";
import { Link, Outlet, createFileRoute, useRouterState } from "@tanstack/react-router";
import { Lock } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { careerService } from "@/services/career.service";

export const Route = createFileRoute("/student/career")({
  component: CareerGate,
});

const careerTabs = [
  { label: "Overview", to: "/student/career" },
  { label: "Jobs", to: "/student/career/jobs" },
  { label: "Applications", to: "/student/career/applications" },
  { label: "Profile", to: "/student/career/profile" },
];

function CareerGate() {
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const eligibility = useQuery({ queryKey: ["career", "eligibility"], queryFn: () => careerService.eligibility() });

  if (eligibility.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-9 w-56" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (eligibility.data && eligibility.data.state !== "eligible") {
    return (
      <div className="surface-panel mx-auto max-w-xl p-8 text-center">
        <Lock className="mx-auto size-6 text-muted-foreground" aria-hidden />
        <h1 className="text-display mt-4 text-2xl">Career services are locked</h1>
        <p className="mt-2 text-sm text-muted-foreground">{eligibility.data.reason}</p>
        <Button asChild className="mt-6">
          <Link to="/programs">View eligible programs</Link>
        </Button>
      </div>
    );
  }

  const onboardingPath = "/student/career/onboarding";
  const needsOnboarding = eligibility.data && !eligibility.data.onboardingCompleted;

  return (
    <div className="space-y-6">
      {needsOnboarding && pathname !== onboardingPath ? (
        <div className="surface-panel flex flex-col gap-3 border-accent/40 p-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="font-medium">Welcome to your career journey</p>
            <p className="text-sm text-muted-foreground">
              Set your target role, locations and companies so recommendations are personalised.
            </p>
          </div>
          <Button asChild variant="accent">
            <Link to={onboardingPath}>Start onboarding</Link>
          </Button>
        </div>
      ) : null}

      <nav aria-label="Career sections" className="flex flex-wrap gap-2">
        {careerTabs.map((tab) => (
          <Link
            key={tab.to}
            to={tab.to}
            className={
              pathname === tab.to
                ? "rounded-full bg-primary px-4 py-1.5 text-sm font-medium text-primary-foreground"
                : "rounded-full border border-border px-4 py-1.5 text-sm font-medium text-muted-foreground hover:text-foreground"
            }
          >
            {tab.label}
          </Link>
        ))}
      </nav>

      <Outlet />
    </div>
  );
}
