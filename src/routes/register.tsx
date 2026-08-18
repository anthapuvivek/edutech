import { createFileRoute, Link } from "@tanstack/react-router";

import { AuthShell } from "@/components/auth/AuthShell";
import { OAuthButtons } from "@/components/auth/OAuthButtons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/register")({
  head: () => ({
    meta: [
      { title: "Create your Learntrix account" },
      { name: "description", content: "Create a free Learntrix account to start technology courses, coding practice and AI-assisted learning." },
      { property: "og:title", content: "Create your Learntrix account" },
      { property: "og:description", content: "Start courses, coding practice and AI-assisted learning with a free account." },
    ],
  }),
  component: RegisterPage,
});

function RegisterPage() {
  return (
    <AuthShell
      title="Create your account"
      description="Start with free lessons, curated coding problems and an AI tutor."
    >
      <form
        className="space-y-4"
        onSubmit={(e) => {
          e.preventDefault();
        }}
      >
        <div className="space-y-2">
          <Label htmlFor="name">Full name</Label>
          <Input id="name" required autoComplete="name" placeholder="Your name" />
        </div>
        <div className="space-y-2">
          <Label htmlFor="reg-email">Email</Label>
          <Input id="reg-email" type="email" required autoComplete="email" placeholder="you@example.com" />
        </div>
        <div className="space-y-2">
          <Label htmlFor="reg-password">Password</Label>
          <Input id="reg-password" type="password" required autoComplete="new-password" />
          <p className="text-xs text-muted-foreground">Minimum 8 characters with a number and a symbol.</p>
        </div>
        <Button type="submit" className="w-full" size="lg">
          Create account
        </Button>
      </form>

      <OAuthButtons />

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Already registered?{" "}
        <Link to="/login" className="font-medium text-foreground underline-offset-4 hover:underline">
          Log in
        </Link>
      </p>
    </AuthShell>
  );
}
