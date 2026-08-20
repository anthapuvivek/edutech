import { Link, createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";

import { AuthShell } from "@/components/auth/AuthShell";
import { Button } from "@/components/ui/button";
import { authService } from "@/services/auth.service";

export const Route = createFileRoute("/verify-email")({
  head: () => ({
    meta: [
      { title: "Verify your Learntrix email" },
      {
        name: "description",
        content: "Confirm your email address to activate your Learntrix learning account.",
      },
      { property: "og:title", content: "Verify your Learntrix email" },
      {
        property: "og:description",
        content: "Confirm your email address to activate your account.",
      },
    ],
  }),
  component: VerifyEmailPage,
});

function VerifyEmailPage() {
  const [state, setState] = useState<"verifying" | "done">("verifying");

  useEffect(() => {
    let active = true;
    void authService.verifyEmail("mock-token").then(() => {
      if (active) setState("done");
    });
    return () => {
      active = false;
    };
  }, []);

  return (
    <AuthShell
      title={state === "verifying" ? "Verifying your email" : "Email verified"}
      description={
        state === "verifying"
          ? "Hold on while we confirm your verification link."
          : "Your account is active. You can now sign in and start learning."
      }
    >
      <Button
        size="lg"
        className="w-full"
        disabled={state === "verifying"}
        asChild={state === "done"}
      >
        {state === "done" ? <Link to="/login">Continue to login</Link> : <span>Verifying…</span>}
      </Button>
    </AuthShell>
  );
}
