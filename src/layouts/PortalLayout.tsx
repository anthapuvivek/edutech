import { Link, useNavigate, useRouterState } from "@tanstack/react-router";
import { Bell, LogOut, Menu } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { toast } from "sonner";

import { Logo } from "@/components/site/Logo";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/hooks/useAuth";
import { canAccessPath, landingFor } from "@/lib/access-control";
import { cn } from "@/lib/utils";
import type { Role } from "@/types/lms";
import type { PlatformRole } from "@/types/ops";


export interface PortalNavItem {
  label: string;
  to?: string | undefined;
  icon: LucideIcon;
  /** Renders a small heading above this item — used to group large sidebars. */
  section?: string | undefined;
  /** Indents the item under its section (sub-navigation). */
  indent?: boolean | undefined;
}

const roleLabel: Record<Role, string> = {
  student: "Student Portal",
  teacher: "Trainer Portal",
  admin: "Admin Console",
  super_admin: "Admin Console",
  mentor: "Mentor Portal",
  placement_officer: "Placement Office",
  support_agent: "Support Desk",
  counsellor: "Counsellor Desk",
};

export function PortalLayout({
  role,
  nav,
  children,
}: {
  /** Default portal identity; the signed-in user's own role always wins. */
  role: Role;
  nav: PortalNavItem[];
  children: ReactNode;
}) {
  const { user, isReady, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  const activeRole = (user?.role ?? role) as PlatformRole;
  const allowed = isReady && isAuthenticated && canAccessPath(activeRole, pathname);

  useEffect(() => {
    if (!isReady) return;
    if (!isAuthenticated) {
      void navigate({ to: "/login", replace: true });
      return;
    }
    // Direct URL access to a page this role has no permission for is bounced to
    // the role's own landing page — the same check the API re-runs server side.
    if (!canAccessPath(activeRole, pathname)) {
      toast.error("You do not have access to that area.");
      void navigate({ to: landingFor(activeRole), replace: true });
    }
  }, [isReady, isAuthenticated, activeRole, pathname, navigate]);

  if (!allowed) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-muted/30 px-6">
        <div className="w-full max-w-md space-y-3">
          <Skeleton className="h-8 w-40" />
          <Skeleton className="h-32 w-full" />
        </div>
      </div>
    );
  }

  const visibleNav = filterNav(nav, activeRole);

  const sidebar = (
    <nav
      aria-label={roleLabel[activeRole]}
      className="flex flex-1 flex-col gap-0.5 overflow-y-auto px-3 pb-4"
    >

      {visibleNav.map((item) => (
        <div key={item.label} className="contents">
          {item.section ? (
            <p className="mt-4 px-3 pb-1 text-[10px] font-semibold uppercase tracking-[0.18em] text-ink-foreground/35">
              {item.section}
            </p>
          ) : null}
          {item.to ? (
            <Link
              to={item.to}
              onClick={() => setOpen(false)}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-ink-foreground/65 transition-colors hover:bg-ink-foreground/10 hover:text-ink-foreground",
                item.indent && "pl-8",
                pathname === item.to && "bg-ink-foreground/12 text-ink-foreground",
              )}
            >
              <item.icon className="size-4 shrink-0" aria-hidden />
              {item.label}
            </Link>
          ) : (
            <span
              className={cn(
                "flex items-center justify-between gap-3 rounded-md px-3 py-2 text-sm font-medium text-ink-foreground/35",
                item.indent && "pl-8",
              )}
              title="Available in an upcoming phase"
            >
              <span className="flex items-center gap-3">
                <item.icon className="size-4 shrink-0" aria-hidden />
                {item.label}
              </span>
              <span className="text-[10px] uppercase tracking-wide">soon</span>
            </span>
          )}
        </div>
      ))}
    </nav>
  );

  const initials = (user?.name ?? "U")
    .split(" ")
    .map((p) => p[0])
    .slice(0, 2)
    .join("");

  return (
    <div className="flex min-h-screen bg-muted/30">
      <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col gap-6 border-r border-ink-foreground/10 bg-ink py-6 text-ink-foreground lg:flex">
        <div className="px-6">
          <Link to="/" aria-label="Learntrix home">
            <Logo tone="inverse" />
          </Link>
          <p className="mt-2 text-xs uppercase tracking-[0.18em] text-ink-foreground/45">
            {roleLabel[activeRole]}
          </p>
        </div>
        {sidebar}
        <div className="px-3">
          <Button
            variant="ghost"
            className="w-full justify-start text-ink-foreground/70 hover:text-ink-foreground"
            onClick={handleSignOut}
          >
            <LogOut className="size-4" aria-hidden /> Sign out
          </Button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-40 flex h-16 items-center justify-between gap-3 border-b border-border bg-background/90 px-4 backdrop-blur-md sm:px-6">
          <div className="flex items-center gap-3">
            <Sheet open={open} onOpenChange={setOpen}>
              <SheetTrigger asChild className="lg:hidden">
                <Button variant="outline" size="icon" aria-label="Open portal menu">
                  <Menu className="size-4" />
                </Button>
              </SheetTrigger>
              <SheetContent side="left" className="w-72 border-none bg-ink p-0 text-ink-foreground">
                <SheetTitle className="sr-only">{roleLabel[activeRole]} navigation</SheetTitle>
                <div className="flex h-full flex-col gap-6 py-6">
                  <div className="px-6 pt-6">
                    <Logo tone="inverse" />
                  </div>
                  {sidebar}
                </div>
              </SheetContent>
            </Sheet>
            <Badge variant="outline" className="hidden sm:inline-flex">
              {roleLabel[activeRole]}
            </Badge>
          </div>

          <div className="flex items-center gap-2">
            <Button variant="ghost" size="icon" aria-label="Notifications">
              <Bell className="size-4" />
            </Button>
            <div className="flex items-center gap-2">
              <span className="grid size-8 place-items-center rounded-full bg-muted text-xs font-medium">
                {initials}
              </span>
              <span className="hidden text-sm font-medium sm:inline">{user?.name}</span>
            </div>
            <Button variant="outline" size="sm" className="lg:hidden" onClick={handleSignOut}>
              Sign out
            </Button>
          </div>
        </header>

        <main id="main" className="flex-1 px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
          {children}
        </main>
      </div>
    </div>
  );

  async function handleSignOut() {
    await logout();
    void navigate({ to: "/login", replace: true });
  }
}

/**
 * Navigation reflects permissions: unauthorized destinations are removed, and a
 * section heading is dropped when nothing under it survives the filter.
 */
function filterNav(nav: PortalNavItem[], role: PlatformRole): PortalNavItem[] {
  return nav.filter((item) => (item.to ? canAccessPath(role, item.to) : false));
}
