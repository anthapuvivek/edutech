import { Outlet, createFileRoute } from "@tanstack/react-router";
import {
  Award,
  BarChart3,
  BookOpen,
  Bot,
  Briefcase,
  Building2,
  ClipboardList,
  Code2,
  FileCheck2,
  FileText,
  GraduationCap,
  Handshake,
  LayoutDashboard,
  ListChecks,
  Settings,
  Target,
  Trophy,
  User,
  Video,
} from "lucide-react";

import { PortalLayout, type PortalNavItem } from "@/layouts/PortalLayout";

const nav: PortalNavItem[] = [
  { label: "Dashboard", to: "/student/dashboard", icon: LayoutDashboard },
  { label: "My Courses", to: "/student/courses", icon: BookOpen },
  { label: "Live Classes", to: "/student/live-classes", icon: Video },
  { label: "Recorded Classes", to: "/student/recordings", icon: Video },
  { label: "Leaderboard", to: "/student/leaderboard", icon: Trophy },
  { label: "Coding Practice", to: "/student/coding-practice", icon: Code2 },
  { label: "Quizzes", to: "/student/quizzes", icon: ListChecks },
  { label: "Assignments", icon: FileCheck2 },
  { label: "Achievements", icon: Award },
  { label: "Career", to: "/student/career", icon: Briefcase },
  { label: "Jobs", to: "/student/career/jobs", icon: Building2 },
  { label: "Applications", to: "/student/career/applications", icon: ClipboardList },
  { label: "Career Profile", to: "/student/career/profile", icon: Target },
  { label: "Resume Builder", icon: FileText },
  { label: "Referrals", icon: Handshake },
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
