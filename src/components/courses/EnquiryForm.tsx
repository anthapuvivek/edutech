import { useMutation } from "@tanstack/react-query";
import { CheckCircle2 } from "lucide-react";
import { useState, type FormEvent } from "react";

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
import { enquiryService } from "@/services/enquiry.service";
import type { EnquiryPayload } from "@/types";

const experienceLevels: EnquiryPayload["experienceLevel"][] = [
  "Fresher",
  "0-2 years",
  "2-5 years",
  "5+ years",
];
const learningModes: EnquiryPayload["learningMode"][] = [
  "Live Online",
  "Self Paced",
  "Hybrid",
  "Corporate",
];

export function EnquiryForm({ courseId, courseTitle }: { courseId: string; courseTitle: string }) {
  const [experienceLevel, setExperienceLevel] =
    useState<EnquiryPayload["experienceLevel"]>("Fresher");
  const [learningMode, setLearningMode] = useState<EnquiryPayload["learningMode"]>("Live Online");

  const mutation = useMutation({
    mutationFn: (payload: EnquiryPayload) => enquiryService.submit(payload),
  });

  const onSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    mutation.mutate({
      name: String(data.get("name") ?? ""),
      email: String(data.get("email") ?? ""),
      phone: String(data.get("phone") ?? ""),
      courseId,
      experienceLevel,
      learningMode,
      message: String(data.get("message") ?? "") || undefined,
    });
  };

  if (mutation.isSuccess) {
    return (
      <div className="surface-panel flex flex-col items-center gap-3 p-10 text-center">
        <CheckCircle2 className="size-8 text-success" aria-hidden />
        <h3 className="text-2xl">Enquiry received</h3>
        <p className="max-w-md text-sm text-muted-foreground">
          Thank you. A Learntrix advisor will contact you within one business day about{" "}
          <span className="font-medium text-foreground">{courseTitle}</span>. Your reference is{" "}
          <span className="font-mono text-foreground">{mutation.data.id}</span>.
        </p>
        <Button variant="outline" className="mt-2" onClick={() => mutation.reset()}>
          Submit another enquiry
        </Button>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} className="surface-panel grid gap-5 p-8 sm:grid-cols-2">
      <div className="space-y-2">
        <Label htmlFor="enq-name">Full name</Label>
        <Input id="enq-name" name="name" required autoComplete="name" placeholder="Your name" />
      </div>
      <div className="space-y-2">
        <Label htmlFor="enq-email">Email</Label>
        <Input
          id="enq-email"
          name="email"
          type="email"
          required
          autoComplete="email"
          placeholder="you@example.com"
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="enq-phone">Phone</Label>
        <Input
          id="enq-phone"
          name="phone"
          required
          autoComplete="tel"
          placeholder="+91 90000 00000"
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="enq-course">Course</Label>
        <Input id="enq-course" value={courseTitle} readOnly aria-readonly className="bg-muted" />
      </div>
      <div className="space-y-2">
        <Label htmlFor="enq-experience">Experience level</Label>
        <Select
          value={experienceLevel}
          onValueChange={(v) => setExperienceLevel(v as EnquiryPayload["experienceLevel"])}
        >
          <SelectTrigger id="enq-experience">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {experienceLevels.map((level) => (
              <SelectItem key={level} value={level}>
                {level}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="space-y-2">
        <Label htmlFor="enq-mode">Preferred learning mode</Label>
        <Select
          value={learningMode}
          onValueChange={(v) => setLearningMode(v as EnquiryPayload["learningMode"])}
        >
          <SelectTrigger id="enq-mode">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {learningModes.map((mode) => (
              <SelectItem key={mode} value={mode}>
                {mode}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="space-y-2 sm:col-span-2">
        <Label htmlFor="enq-message">Message (optional)</Label>
        <Textarea id="enq-message" name="message" rows={4} placeholder="Tell us about your goals" />
      </div>

      {mutation.isError ? (
        <p
          role="alert"
          className="sm:col-span-2 rounded-md border border-destructive/30 bg-destructive/8 p-3 text-sm text-destructive"
        >
          We couldn't submit your enquiry. Please try again.
        </p>
      ) : null}

      <div className="sm:col-span-2">
        <Button type="submit" size="lg" disabled={mutation.isPending}>
          {mutation.isPending ? "Submitting…" : "Submit enquiry"}
        </Button>
      </div>
    </form>
  );
}
