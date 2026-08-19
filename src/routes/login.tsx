import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { toast } from "sonner";

import { AuthShell } from "@/components/auth/AuthShell";
import { OAuthButtons } from "@/components/auth/OAuthButtons";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/useAuth";
import { roleHome } from "@/services/auth.service";

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
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(true);
  const [loading, setLoading] = useState(false);

  return (
    <AuthShell
      title="Welcome back"
      description="Sign in to continue your program, practice streak and AI conversations."
    >
      <form
        className="space-y-4"
        onSubmit={async (e) => {
          e.preventDefault();
          setLoading(true);
          try {
            const user = await login({ email, password, remember });
            toast.success(`Welcome back, ${user.name.split(" ")[0]}`);
            void navigate({ to: roleHome[user.role], replace: true });
          } catch (error) {
            toast.error(error instanceof Error ? error.message : "Unable to sign in.");
          } finally {
            setLoading(false);
          }
        }}
      >
        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            type="email"
            required
            autoComplete="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <Label htmlFor="password">Password</Label>
            <Link
              to="/forgot-password"
              className="text-xs text-muted-foreground underline-offset-4 hover:underline"
            >
              Forgot password?
            </Link>
          </div>
          <Input
            id="password"
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <div className="flex items-center gap-2">
          <Checkbox id="remember" checked={remember} onCheckedChange={(v) => setRemember(v === true)} />
          <Label htmlFor="remember" className="text-sm font-normal text-muted-foreground">
            Remember me for 30 days
          </Label>
        </div>
        <Button type="submit" className="w-full" size="lg" disabled={loading}>
          {loading ? "Signing in…" : "Log in"}
        </Button>
      </form>

      <div className="mt-4 rounded-md border border-border bg-muted/40 p-3 text-xs text-muted-foreground">
        <p className="font-medium text-foreground">Demo accounts (mock auth)</p>
        <p>student@learntrix.com · teacher@learntrix.com · admin@learntrix.com</p>
        <p>mentor@learntrix.com · placement@learntrix.com</p>
        <p>Password: password</p>
      </div>

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
