import { Outlet, createFileRoute } from "@tanstack/react-router";
import {
  Award,
  BarChart3,
  BookOpen,
  Bot,
  Code2,
  FileCheck2,
  GraduationCap,
  LayoutDashboard,
  ListChecks,
  Settings,
  Trophy,
  User,
  Video,
} from "lucide-react";

import { PortalLayout, type PortalNavItem } from "@/layouts/PortalLayout";

const nav: PortalNavItem[] = [
  { label: "Dashboard", to: "/student/dashboard", icon: LayoutDashboard },
  { label: "My Courses", to: "/student/courses", icon: BookOpen },
  { label: "Live Classes", to: "/student/live-classes", icon: Video },
  { label: "Leaderboard", to: "/student/leaderboard", icon: Trophy },
  { label: "Coding Practice", icon: Code2 },
  { label: "Quizzes", icon: ListChecks },
  { label: "Assignments", icon: FileCheck2 },
  { label: "Achievements", icon: Award },
  { label: "AI Tutor", icon: Bot },
  { label: "Certificates", icon: GraduationCap },
  { label: "Analytics", icon: BarChart3 },
  { label: "Profile", icon: User },
  { label: "Settings", icon: Settings },
];

export const Route = createFileRoute("/student")({
  component: () => (
    <PortalLayout role="student" nav={nav}>
      <Outlet />
    </PortalLayout>
  ),
});
