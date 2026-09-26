import { Outlet, createFileRoute } from "@tanstack/react-router";
import {
  BarChart3,
  BookOpen,
  Code2,
  FileCheck2,
  LayoutDashboard,
  ListChecks,
  Megaphone,
  TrendingUp,
  User,
  Users,
  Video,
  Sparkles,
} from "lucide-react";

import { PortalLayout, type PortalNavItem } from "@/layouts/PortalLayout";

const nav: PortalNavItem[] = [
  { label: "Dashboard", to: "/teacher/dashboard", icon: LayoutDashboard },
  { label: "Students", to: "/teacher/students", icon: Users },
  { label: "My Courses", icon: BookOpen },
  { label: "Batches", to: "/teacher/batches", icon: Users },
  { label: "Live Classes", to: "/teacher/live-classes", icon: Video },
  { label: "Recorded Classes", to: "/teacher/recordings", icon: Video },
  { label: "Assignments", to: "/teacher/assignments", icon: FileCheck2 },
  { label: "Quizzes", to: "/teacher/quizzes", icon: ListChecks },
  { label: "Coding Problems", to: "/teacher/coding-problems", icon: Code2 },
  { label: "AI Assistant", to: "/teacher/ai-assistant", icon: Sparkles },
  { label: "Progress", to: "/teacher/progress", icon: TrendingUp },
  { label: "Analytics", to: "/teacher/analytics", icon: BarChart3 },
  { label: "Announcements", to: "/teacher/announcements", icon: Megaphone },
  { label: "Profile", icon: User },
];

export const Route = createFileRoute("/teacher")({
  component: () => (
    <PortalLayout role="teacher" nav={nav}>
      <Outlet />
    </PortalLayout>
  ),
});
