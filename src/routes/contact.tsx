import { useMutation } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { CheckCircle2, Mail, MapPin, Phone } from "lucide-react";
import type { FormEvent } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { PublicLayout } from "@/layouts/PublicLayout";
import { enquiryService } from "@/services/enquiry.service";

export const Route = createFileRoute("/contact")({
  head: () => ({
    meta: [
      { title: "Contact Learntrix — Talk to a Learning Advisor" },
      {
        name: "description",
        content:
          "Speak with a Learntrix advisor about course selection, career tracks, corporate training or admissions for the next cohort.",
      },
      { property: "og:title", content: "Contact Learntrix" },
      {
        property: "og:description",
        content: "Talk to a learning advisor about courses, career tracks and corporate training.",
      },
    ],
  }),
  component: ContactPage,
});

function ContactPage() {
  const mutation = useMutation({
    mutationFn: (payload: { name: string; email: string; phone: string; message: string }) =>
      enquiryService.submit({
        ...payload,
        courseId: "general",
        experienceLevel: "Fresher",
        learningMode: "Live Online",
      }),
  });

  const onSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    mutation.mutate({
      name: String(data.get("name") ?? ""),
      email: String(data.get("email") ?? ""),
      phone: String(data.get("phone") ?? ""),
      message: String(data.get("message") ?? ""),
    });
  };

  return (
    <PublicLayout>
      <section className="container-page grid gap-14 py-20 lg:grid-cols-[0.9fr_1.1fr]">
        <div>
          <p className="eyebrow">Contact</p>
          <h1 className="mt-3 text-display text-4xl">Talk to a learning advisor</h1>
          <p className="mt-4 max-w-md text-muted-foreground">
            Tell us where you are in your career and what you want to build. We will recommend a
            track, share the curriculum and outline the cohort schedule.
          </p>
          <ul className="mt-10 space-y-5 text-sm">
            <li className="flex gap-3">
              <Mail className="size-5 text-primary" aria-hidden />
              <span>
                <span className="block font-medium">Email</span>
                <span className="text-muted-foreground">admissions@learntrix.com</span>
              </span>
            </li>
            <li className="flex gap-3">
              <Phone className="size-5 text-primary" aria-hidden />
              <span>
                <span className="block font-medium">Phone</span>
                <span className="text-muted-foreground">+91 80 4567 8900</span>
              </span>
            </li>
            <li className="flex gap-3">
              <MapPin className="size-5 text-primary" aria-hidden />
              <span>
                <span className="block font-medium">Campus</span>
                <span className="text-muted-foreground">Learntrix Learning Centre, Bengaluru</span>
              </span>
            </li>
          </ul>
        </div>

        {mutation.isSuccess ? (
          <div className="surface-panel flex flex-col items-center justify-center gap-3 p-12 text-center">
            <CheckCircle2 className="size-8 text-success" aria-hidden />
            <h2 className="text-2xl">Message sent</h2>
            <p className="max-w-sm text-sm text-muted-foreground">
              An advisor will respond within one business day. Reference{" "}
              <span className="font-mono text-foreground">{mutation.data.id}</span>.
            </p>
            <Button variant="outline" className="mt-2" onClick={() => mutation.reset()}>
              Send another message
            </Button>
          </div>
        ) : (
          <form onSubmit={onSubmit} className="surface-panel grid gap-5 p-8 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="c-name">Full name</Label>
              <Input id="c-name" name="name" required autoComplete="name" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="c-email">Email</Label>
              <Input id="c-email" name="email" type="email" required autoComplete="email" />
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="c-phone">Phone</Label>
              <Input id="c-phone" name="phone" required autoComplete="tel" />
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="c-message">How can we help?</Label>
              <Textarea id="c-message" name="message" rows={5} required />
            </div>
            {mutation.isError ? (
              <p
                role="alert"
                className="sm:col-span-2 rounded-md border border-destructive/30 bg-destructive/8 p-3 text-sm text-destructive"
              >
                Your message could not be sent. Please try again.
              </p>
            ) : null}
            <div className="sm:col-span-2">
              <Button type="submit" size="lg" disabled={mutation.isPending}>
                {mutation.isPending ? "Sending…" : "Send message"}
              </Button>
            </div>
          </form>
        )}
      </section>
    </PublicLayout>
  );
}
