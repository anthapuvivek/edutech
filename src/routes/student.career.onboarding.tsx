import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";

import { PageHeader } from "@/components/portal/StatCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { careerService } from "@/services/career.service";
import type { CareerOnboardingPayload, CareerProfile, WorkMode } from "@/types/career";

export const Route = createFileRoute("/student/career/onboarding")({
  head: () => ({
    meta: [
      { title: "Career Onboarding — Learntrix" },
      {
        name: "description",
        content:
          "Set your target role, locations and companies to personalise career recommendations.",
      },
      { property: "og:title", content: "Career Onboarding — Learntrix" },
      {
        property: "og:description",
        content: "Tell Learntrix your career goals to unlock personalised guidance.",
      },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: CareerOnboardingPage,
});

const steps = [
  "Complete profile",
  "Build resume",
  "Select target role",
  "Complete skill assessment",
  "Start coding practice",
  "Prepare for interviews",
  "Apply for jobs",
];

function CareerOnboardingPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [targetRole, setTargetRole] = useState("Software Engineer");
  const [preferredLocation, setPreferredLocation] = useState("Bengaluru");
  const [experienceLevel, setExperienceLevel] =
    useState<CareerProfile["experienceLevel"]>("Fresher");
  const [workMode, setWorkMode] = useState<WorkMode>("Hybrid");
  const [targetCompanies, setTargetCompanies] = useState("Microsoft, Google, Amazon");
  const [careerGoal, setCareerGoal] = useState("");

  const submit = useMutation({
    mutationFn: (payload: CareerOnboardingPayload) => careerService.completeOnboarding(payload),
    onSuccess: async () => {
      toast.success("Career profile set up. Recommendations are being personalised.");
      await queryClient.invalidateQueries({ queryKey: ["career"] });
      void navigate({ to: "/student/career" });
    },
    onError: () => toast.error("Could not save your preferences. Please try again."),
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    submit.mutate({
      targetRole,
      preferredLocation,
      experienceLevel,
      workMode,
      targetCompanies: targetCompanies
        .split(",")
        .map((c) => c.trim())
        .filter(Boolean),
      careerGoal,
    });
  }

  return (
    <>
      <PageHeader
        title="Welcome to your career journey"
        description="These answers drive job, resume and preparation recommendations. You can change them any time."
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <form onSubmit={handleSubmit} className="surface-panel space-y-5 p-6 lg:col-span-2">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="target-role">Target role</Label>
              <Input
                id="target-role"
                value={targetRole}
                onChange={(e) => setTargetRole(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="location">Preferred location</Label>
              <Input
                id="location"
                value={preferredLocation}
                onChange={(e) => setPreferredLocation(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="experience">Experience level</Label>
              <Select
                value={experienceLevel}
                onValueChange={(v) => setExperienceLevel(v as CareerProfile["experienceLevel"])}
              >
                <SelectTrigger id="experience">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(["Fresher", "0-1 years", "1-3 years", "3+ years"] as const).map((v) => (
                    <SelectItem key={v} value={v}>
                      {v}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="work-mode">Work mode</Label>
              <Select value={workMode} onValueChange={(v) => setWorkMode(v as WorkMode)}>
                <SelectTrigger id="work-mode">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(["Remote", "Hybrid", "Onsite"] as const).map((v) => (
                    <SelectItem key={v} value={v}>
                      {v}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="companies">Target companies</Label>
            <Input
              id="companies"
              value={targetCompanies}
              onChange={(e) => setTargetCompanies(e.target.value)}
              placeholder="Comma separated"
            />
            <p className="text-xs text-muted-foreground">
              Adding a company creates a preparation track. It does not imply a hiring partnership.
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="goal">Career goal</Label>
            <Textarea
              id="goal"
              value={careerGoal}
              onChange={(e) => setCareerGoal(e.target.value)}
              rows={3}
              placeholder="e.g. Land a backend engineering role within six months"
            />
          </div>

          <Button type="submit" disabled={submit.isPending}>
            {submit.isPending ? "Saving…" : "Activate career dashboard"}
          </Button>
        </form>

        <aside className="surface-panel p-6">
          <h2 className="text-display mb-4 text-xl">Your journey</h2>
          <ol className="space-y-3 text-sm">
            {steps.map((step, i) => (
              <li key={step} className="flex gap-3">
                <span className="grid size-6 shrink-0 place-items-center rounded-full bg-muted text-xs font-medium">
                  {i + 1}
                </span>
                <span className="pt-0.5">{step}</span>
              </li>
            ))}
          </ol>
        </aside>
      </div>
    </>
  );
}
