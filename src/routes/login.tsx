import { createFileRoute, Link } from "@tanstack/react-router";

import { AuthShell } from "@/components/auth/AuthShell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { OAuthButtons } from "@/components/auth/OAuthButtons";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Log in to Learntrix" },
      { name: "description", content: "Sign in to your Learntrix account to continue learning, practising and tracking progress." },
      { property: "og:title", content: "Log in to Learntrix" },
      { property: "og:description", content: "Sign in to continue your Learntrix learning journey." },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  return (
    <AuthShell
      title="Welcome back"
      description="Sign in to continue your program, practice streak and AI conversations."
    >
      <form
        className="space-y-4"
        onSubmit={(e) => {
          e.preventDefault();
        }}
      >
        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" required autoComplete="email" placeholder="you@example.com" />
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <Label htmlFor="password">Password</Label>
            <a href="#" className="text-xs text-muted-foreground underline-offset-4 hover:underline">
              Forgot password?
            </a>
          </div>
          <Input id="password" type="password" required autoComplete="current-password" />
        </div>
        <Button type="submit" className="w-full" size="lg">
          Log in
        </Button>
      </form>

      <OAuthButtons />

      <p className="mt-6 text-center text-sm text-muted-foreground">
        New to Learntrix?{" "}
        <Link to="/register" className="font-medium text-foreground underline-offset-4 hover:underline">
          Create an account
        </Link>
      </p>
    </AuthShell>
  );
}
