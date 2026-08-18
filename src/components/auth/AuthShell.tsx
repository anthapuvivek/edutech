import { Link } from "@tanstack/react-router";
import type { ReactNode } from "react";

import { Logo } from "@/components/site/Logo";

export function AuthShell({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="hidden flex-col justify-between bg-ink p-12 text-ink-foreground lg:flex">
        <Link to="/" aria-label="Learntrix home">
          <Logo tone="inverse" />
        </Link>
        <div>
          <h2 className="max-w-md text-display text-4xl">
            Structured learning, deliberate practice, measurable progress.
          </h2>
          <p className="mt-4 max-w-sm text-ink-foreground/65">
            Join learners advancing into engineering, data and AI roles with Learntrix programs.
          </p>
        </div>
        <p className="text-xs text-ink-foreground/45">© {new Date().getFullYear()} Learntrix</p>
      </div>

      <div className="flex items-center justify-center px-6 py-14">
        <div className="w-full max-w-sm">
          <div className="lg:hidden">
            <Link to="/" aria-label="Learntrix home">
              <Logo />
            </Link>
          </div>
          <h1 className="mt-8 text-display text-3xl lg:mt-0">{title}</h1>
          <p className="mt-2 text-sm text-muted-foreground">{description}</p>
          <div className="mt-8">{children}</div>
        </div>
      </div>
    </div>
  );
}
