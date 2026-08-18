import { Outlet, createFileRoute } from "@tanstack/react-router";
import {
  BarChart3,
  Bell,
  BookOpen,
  CalendarDays,
  Code2,
  FileCheck2,
  GraduationCap,
  LayoutDashboard,
  ListChecks,
  MessageSquare,
  Settings,
  Sparkles,
  Trophy,
  Users,
  Video,
  Wallet,
} from "lucide-react";

import { PortalLayout, type PortalNavItem } from "@/layouts/PortalLayout";

const nav: PortalNavItem[] = [
  { label: "Dashboard", to: "/admin/dashboard", icon: LayoutDashboard },
  { label: "Live Classes & Events", to: "/admin/events", icon: Video },
  { label: "Users", icon: Users },
  { label: "Courses", icon: BookOpen },
  { label: "Batches", icon: CalendarDays },
  { label: "Coding Problems", icon: Code2 },
  { label: "Quizzes", icon: ListChecks },
  { label: "Assignments", icon: FileCheck2 },
  { label: "Leaderboard", icon: Trophy },
  { label: "Gamification", icon: Sparkles },
  { label: "Enquiries", icon: MessageSquare },
  { label: "Payments", icon: Wallet },
  { label: "Certificates", icon: GraduationCap },
  { label: "Analytics", icon: BarChart3 },
  { label: "Notifications", icon: Bell },
  { label: "Settings", icon: Settings },
];

export const Route = createFileRoute("/admin")({
  component: () => (
    <PortalLayout role="admin" nav={nav}>
      <Outlet />
    </PortalLayout>
  ),
});
