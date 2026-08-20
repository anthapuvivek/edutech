import { Link, createFileRoute } from "@tanstack/react-router";
import { useState } from "react";

import { AuthShell } from "@/components/auth/AuthShell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authService } from "@/services/auth.service";

export const Route = createFileRoute("/forgot-password")({
  head: () => ({
    meta: [
      { title: "Reset your Learntrix password" },
      {
        name: "description",
        content: "Request a secure password reset link for your Learntrix learning account.",
      },
      { property: "og:title", content: "Reset your Learntrix password" },
      { property: "og:description", content: "Request a password reset link for your account." },
    ],
  }),
  component: ForgotPasswordPage,
});

function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [state, setState] = useState<"idle" | "loading" | "sent">("idle");

  return (
    <AuthShell
      title="Forgot your password?"
      description="Enter your account email and we'll send a reset link."
    >
      {state === "sent" ? (
        <div className="surface-panel p-5 text-sm">
          <p className="font-medium">Check your inbox</p>
          <p className="mt-1 text-muted-foreground">
            If an account exists for {email}, a reset link is on its way.
          </p>
          <Button variant="outline" className="mt-4 w-full" asChild>
            <Link to="/login">Back to login</Link>
          </Button>
        </div>
      ) : (
        <form
          className="space-y-4"
          onSubmit={async (e) => {
            e.preventDefault();
            setState("loading");
            await authService.requestPasswordReset(email);
            setState("sent");
          }}
        >
          <div className="space-y-2">
            <Label htmlFor="fp-email">Email</Label>
            <Input
              id="fp-email"
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
            />
          </div>
          <Button type="submit" size="lg" className="w-full" disabled={state === "loading"}>
            {state === "loading" ? "Sending…" : "Send reset link"}
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            <Link
              to="/login"
              className="font-medium text-foreground underline-offset-4 hover:underline"
            >
              Back to login
            </Link>
          </p>
        </form>
      )}
    </AuthShell>
  );
}
