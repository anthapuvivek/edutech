import { Outlet, createFileRoute } from "@tanstack/react-router";
import {
  Award,
  BarChart3,
  Bell,
  BookOpen,
  Briefcase,
  Building2,
  CalendarCheck,
  CalendarDays,
  ClipboardList,
  FileText,
  GraduationCap,
  Handshake,
  LayoutDashboard,
  LayoutTemplate,
  Library,
  ListChecks,
  Mail,
  MessageSquare,
  Megaphone,
  ScrollText,
  Settings,
  ShieldCheck,
  Sparkles,
  Target,
  TrendingUp,
  Users,
  Video,
  Wallet,
} from "lucide-react";

import { PortalLayout, type PortalNavItem } from "@/layouts/PortalLayout";

const nav: PortalNavItem[] = [
  { label: "Dashboard", to: "/admin/dashboard", icon: LayoutDashboard },

  { section: "Operations", label: "Students", to: "/admin/students", icon: Users },
  { label: "Teachers", to: "/admin/teachers", icon: GraduationCap },
  { label: "Courses", icon: BookOpen },
  { label: "Batches", to: "/admin/batches", icon: CalendarDays },
  { label: "Attendance", to: "/admin/attendance", icon: CalendarCheck },
  { label: "Learning & Performance", to: "/admin/performance", icon: TrendingUp },

  { section: "Career", label: "Jobs", to: "/admin/career/jobs", icon: Briefcase, indent: true },
  { label: "Companies", to: "/admin/career/companies", icon: Building2, indent: true },
  { label: "Applications", to: "/admin/career/applications", icon: ClipboardList, indent: true },
  { label: "Referrals", to: "/admin/career/referrals", icon: Handshake, indent: true },
  { label: "Interview Preparation", to: "/admin/career/preparation", icon: Target, indent: true },
  {
    label: "Resume Templates",
    to: "/admin/career/resume-templates",
    icon: LayoutTemplate,
    indent: true,
  },
  { label: "Career Resources", to: "/admin/career/resources", icon: Library, indent: true },
  { label: "Eligibility Rules", to: "/admin/career/eligibility", icon: ShieldCheck, indent: true },
  { label: "Placement", icon: Award, indent: true },

  { section: "Growth", label: "Leads / CRM", to: "/admin/crm", icon: Handshake },
  { label: "Coupons", to: "/admin/marketing/coupons", icon: Wallet },
  { label: "Support Desk", to: "/admin/support", icon: MessageSquare },

  { section: "Content", label: "Articles", to: "/admin/articles", icon: FileText, indent: true },
  { label: "CMS", to: "/admin/cms", icon: Sparkles, indent: true },
  { label: "Testimonials", icon: MessageSquare, indent: true },
  { label: "FAQs", icon: ListChecks, indent: true },
  { label: "Announcements", icon: Megaphone, indent: true },

  {
    section: "Events",
    label: "Live Classes & Events",
    to: "/admin/events",
    icon: Video,
    indent: true,
  },
  {
    label: "Recorded Classes",
    to: "/admin/recordings",
    icon: Video,
    indent: true,
  },

  {
    section: "Communication",
    label: "WhatsApp",
    to: "/admin/communication",
    icon: MessageSquare,
    indent: true,
  },
  { label: "Notifications", to: "/admin/notifications", icon: Bell, indent: true },
  { label: "Email", icon: Mail, indent: true },

  { section: "Platform", label: "Payments", icon: Wallet },
  { label: "Certificates", icon: GraduationCap },
  { label: "Analytics", to: "/admin/analytics", icon: BarChart3 },
  { label: "Audit Logs", to: "/admin/audit-logs", icon: ScrollText },
  { label: "Settings", to: "/admin/settings", icon: Settings },
];

export const Route = createFileRoute("/admin")({
  component: () => (
    <PortalLayout role="admin" nav={nav}>
      <Outlet />
    </PortalLayout>
  ),
});
