import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { toast } from "sonner";

import { AuthShell } from "@/components/auth/AuthShell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authService } from "@/services/auth.service";

export const Route = createFileRoute("/reset-password")({
  head: () => ({
    meta: [
      { title: "Set a new Learntrix password" },
      {
        name: "description",
        content: "Choose a new password for your Learntrix account and get back to learning.",
      },
      { property: "og:title", content: "Set a new Learntrix password" },
      { property: "og:description", content: "Choose a new password for your account." },
    ],
  }),
  component: ResetPasswordPage,
});

function ResetPasswordPage() {
  const navigate = useNavigate();
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);

  return (
    <AuthShell
      title="Set your password"
      description="Choose a strong password to activate your account and start using Learntrix."
    >
      <form
        className="space-y-4"
        onSubmit={async (e) => {
          e.preventDefault();
          if (password !== confirm) {
            toast.error("Passwords do not match.");
            return;
          }

          const searchParams = typeof window !== "undefined" ? new URLSearchParams(window.location.search) : null;
          const token = searchParams?.get("token") || "mock-token";

          setLoading(true);
          try {
            await authService.resetPassword(token, password);
            toast.success("Password created and account activated! Please log in.");
            void navigate({ to: "/login" });
          } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : "Unable to set password. Link may be invalid or expired.";
            toast.error(msg);
          } finally {
            setLoading(false);
          }
        }}
      >
        <div className="space-y-2">
          <Label htmlFor="rp-password">New password</Label>
          <Input
            id="rp-password"
            type="password"
            required
            minLength={8}
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="rp-confirm">Confirm password</Label>
          <Input
            id="rp-confirm"
            type="password"
            required
            minLength={8}
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
          />
        </div>
        <Button type="submit" size="lg" className="w-full" disabled={loading}>
          {loading ? "Updating…" : "Update password"}
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
    </AuthShell>
  );
}
