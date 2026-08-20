import { Link } from "@tanstack/react-router";
import { Github, Linkedin, Twitter, Youtube } from "lucide-react";

import { Logo } from "@/components/site/Logo";

const columns = [
  {
    heading: "Company",
    links: [
      { label: "About us", to: "/about" },
      { label: "Contact", to: "/contact" },
      { label: "Programs", to: "/programs" },
    ],
  },
  {
    heading: "Learn",
    links: [
      { label: "Course catalogue", to: "/courses" },
      { label: "Coding practice", to: "/coding-practice" },
      { label: "AI learning", to: "/ai-learning" },
    ],
  },
  {
    heading: "Account",
    links: [
      { label: "Log in", to: "/login" },
      { label: "Create account", to: "/register" },
    ],
  },
] as const;

export function SiteFooter() {
  return (
    <footer className="mt-24 bg-ink text-ink-foreground">
      <div className="container-page grid gap-12 py-16 md:grid-cols-2 lg:grid-cols-5">
        <div className="lg:col-span-2">
          <Logo tone="inverse" />
          <p className="mt-4 max-w-sm text-sm leading-relaxed text-ink-foreground/70">
            Learntrix is a premium technology education platform combining structured curricula,
            hands-on engineering practice and AI-assisted learning for serious career growth.
          </p>
          <div className="mt-6 flex gap-3">
            {[Linkedin, Twitter, Github, Youtube].map((Icon, i) => (
              <a
                key={i}
                href="#"
                aria-label="Learntrix social profile"
                className="grid size-9 place-items-center rounded-md border border-ink-foreground/15 text-ink-foreground/70 transition-colors hover:border-accent hover:text-accent"
              >
                <Icon className="size-4" />
              </a>
            ))}
          </div>
        </div>

        {columns.map((col) => (
          <nav key={col.heading} aria-label={col.heading}>
            <h3 className="eyebrow text-ink-foreground/50">{col.heading}</h3>
            <ul className="mt-4 space-y-3">
              {col.links.map((link) => (
                <li key={link.label}>
                  <Link
                    to={link.to}
                    className="text-sm text-ink-foreground/75 transition-colors hover:text-accent"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        ))}
      </div>

      <div className="border-t border-ink-foreground/10">
        <div className="container-page flex flex-col gap-3 py-6 text-xs text-ink-foreground/55 sm:flex-row sm:items-center sm:justify-between">
          <p>© {new Date().getFullYear()} Learntrix. All rights reserved.</p>
          <div className="flex gap-6">
            <a href="#" className="transition-colors hover:text-accent">
              Privacy Policy
            </a>
            <a href="#" className="transition-colors hover:text-accent">
              Terms of Service
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
