import { Link } from "@tanstack/react-router";
import { Menu } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { Logo } from "@/components/site/Logo";
import { useAuth } from "@/hooks/useAuth";
import { roleHome } from "@/services/auth.service";

const navItems = [
  { to: "/", label: "Home" },
  { to: "/courses", label: "Courses" },
  { to: "/programs", label: "Programs" },
  { to: "/coding-practice", label: "Coding Practice" },
  { to: "/ai-learning", label: "AI Learning" },
  { to: "/career", label: "Careers" },
  { to: "/about", label: "About" },
  { to: "/contact", label: "Contact" },
] as const;

export function SiteHeader() {
  const [open, setOpen] = useState(false);
  const { user, isAuthenticated } = useAuth();
  const portalHome = user ? roleHome[user.role] : "/login";

  return (
    <header className="sticky top-0 z-50 border-b border-border/80 bg-background/85 backdrop-blur-md">
      <div className="container-page flex h-16 items-center justify-between gap-6">
        <Link to="/" className="shrink-0" aria-label="Learntrix home">
          <Logo />
        </Link>

        <nav aria-label="Primary" className="hidden items-center gap-1 lg:flex">
          {navItems.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              activeOptions={{ exact: item.to === "/" }}
              activeProps={{ className: "text-foreground after:scale-x-100" }}
              className="relative px-3 py-2 text-sm font-medium text-muted-foreground transition-colors after:absolute after:inset-x-3 after:-bottom-0.5 after:h-px after:origin-left after:scale-x-0 after:bg-accent after:transition-transform hover:text-foreground"
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="hidden items-center gap-2 lg:flex">
          {isAuthenticated && user ? (
            <Button size="sm" asChild>
              <Link to={portalHome}>
                {user.role === "student" ? "Student Dashboard" : "Go to portal"}
              </Link>
            </Button>
          ) : (
            <>
              <Button variant="ghost" size="sm" asChild>
                <Link to="/login">Student Login</Link>
              </Button>
              <Button size="sm" asChild>
                <Link to="/register">Get started</Link>
              </Button>
            </>
          )}
        </div>

        <Sheet open={open} onOpenChange={setOpen}>
          <SheetTrigger asChild className="lg:hidden">
            <Button variant="outline" size="icon" aria-label="Open menu">
              <Menu className="size-4" />
            </Button>
          </SheetTrigger>
          <SheetContent side="right" className="w-[86vw] max-w-sm">
            <SheetTitle className="sr-only">Navigation</SheetTitle>
            <div className="flex flex-col gap-1 p-6 pt-14">
              {navItems.map((item) => (
                <Link
                  key={item.to}
                  to={item.to}
                  onClick={() => setOpen(false)}
                  className="rounded-md px-3 py-3 text-base font-medium text-foreground transition-colors hover:bg-muted"
                >
                  {item.label}
                </Link>
              ))}
              <div className="mt-6 flex flex-col gap-2">
                {isAuthenticated ? (
                  <Button asChild onClick={() => setOpen(false)}>
                    <Link to={portalHome}>Student Dashboard</Link>
                  </Button>
                ) : (
                  <>
                    <Button variant="outline" asChild onClick={() => setOpen(false)}>
                      <Link to="/login">Student Login</Link>
                    </Button>
                    <Button asChild onClick={() => setOpen(false)}>
                      <Link to="/register">Get started</Link>
                    </Button>
                  </>
                )}
              </div>
            </div>
          </SheetContent>
        </Sheet>
      </div>
    </header>
  );
}
