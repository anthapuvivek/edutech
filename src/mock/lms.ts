import { mockCourses } from "@/mock/courses";
import type {
  ActivityItem,
  AdminStats,
  AuthUser,
  Enrollment,
  LeaderboardEntry,
  PlatformEvent,
  StudentProfile,
  StudentStats,
  TeacherStats,
  TeacherStudentRow,
} from "@/types/lms";

export const mockAccounts: Array<AuthUser & { password: string }> = [
  {
    id: "u-student",
    studentId: "LTX-2026-0412",
    name: "Aarav Sharma",
    email: "student@learntrix.com",
    password: "password",
    role: "student",
    status: "active",
    createdAt: "2026-01-12T09:00:00.000Z",
  },
  {
    id: "u-teacher",
    name: "Durga Prasad",
    email: "teacher@learntrix.com",
    password: "password",
    role: "teacher",
    status: "active",
    createdAt: "2025-08-02T09:00:00.000Z",
  },
  {
    id: "u-admin",
    name: "Nisha Verma",
    email: "admin@learntrix.com",
    password: "password",
    role: "admin",
    status: "active",
    createdAt: "2025-04-19T09:00:00.000Z",
  },
];

export const mockStudentProfile: StudentProfile = {
  id: "u-student",
  studentId: "LTX-2026-0412",
  name: "Aarav Sharma",
  level: "Advanced",
  nextLevel: "Expert",
  points: 8450,
  pointsToNextLevel: 1550,
  nextLevelThreshold: 10000,
  rank: 12,
  streakDays: 17,
};

export const mockStudentStats: StudentStats = {
  totalCourses: 5,
  activeCourses: 3,
  completedCourses: 2,
  learningHours: 184,
  problemsSolved: 126,
  quizAverage: 87,
  points: 8450,
  rank: 12,
};

export const mockEnrollments: Enrollment[] = mockCourses.slice(0, 5).map((course, index) => {
  const statuses = ["active", "active", "active", "completed", "payment_pending"] as const;
  const progress = [68, 42, 91, 100, 0][index] ?? 0;
  const total = 50;
  return {
    id: `enr-${index + 1}`,
    courseId: course.id,
    courseSlug: course.slug,
    courseTitle: course.title,
    instructorName: course.instructor.name,
    thumbnailUrl: course.thumbnailUrl,
    status: statuses[index]!,
    progressPercent: progress,
    lessonsCompleted: Math.round((progress / 100) * total),
    lessonsTotal: total,
    lastLessonTitle: progress ? `Module ${Math.ceil(progress / 25)} · Recap` : undefined,
    nextLessonTitle: progress < 100 ? `Module ${Math.ceil(progress / 25) + 1} · Deep dive` : undefined,
    enrolledAt: "2026-05-02T09:00:00.000Z",
  };
});

export const mockActivity: ActivityItem[] = [
  { id: "a1", kind: "lesson", title: "Completed Python Lesson 12", points: 10, occurredAt: "2026-08-18T08:15:00.000Z" },
  { id: "a2", kind: "coding", title: "Solved Two Sum", points: 10, occurredAt: "2026-08-18T06:40:00.000Z" },
  { id: "a3", kind: "class", title: "Attended Live Class · Spring Boot APIs", points: 20, occurredAt: "2026-08-17T13:00:00.000Z" },
  { id: "a4", kind: "quiz", title: "Scored 90% in Module 4 Quiz", points: 50, occurredAt: "2026-08-17T10:20:00.000Z" },
  { id: "a5", kind: "achievement", title: "Unlocked badge · 7 Day Streak", points: 25, occurredAt: "2026-08-16T18:05:00.000Z" },
];

export const mockEvents: PlatformEvent[] = [
  {
    id: "ev1",
    title: "DATA SCIENCE with Gen AI",
    courseTitle: "Generative AI & LLM Systems",
    trainerName: "Team of Experts",
    date: "2026-08-19",
    startTime: "11:00",
    endTime: "12:00",
    platform: "Google Meet",
    meetingUrl: "https://meet.google.com/demo-link",
    type: "Demo Class",
    visibility: "public",
    published: true,
    status: "Upcoming",
    registrations: 184,
  },
  {
    id: "ev2",
    title: "AI & Machine Learning Webinar",
    courseTitle: "Applied Machine Learning",
    trainerName: "Meera Krishnan",
    date: "2026-08-25",
    startTime: "19:00",
    endTime: "20:30",
    platform: "Zoom",
    type: "Webinar",
    visibility: "public",
    published: true,
    status: "Upcoming",
    registrations: 312,
  },
  {
    id: "ev3",
    title: "Full Stack Java · Module 5 Class",
    courseTitle: "Full Stack Engineering Program",
    trainerName: "Durga Prasad",
    date: "2026-08-18",
    startTime: "09:00",
    endTime: "10:30",
    platform: "Google Meet",
    meetingUrl: "https://meet.google.com/private-link",
    type: "Regular Class",
    visibility: "restricted",
    published: true,
    status: "Live Now",
    registrations: 150,
  },
  {
    id: "ev4",
    title: "Placement Readiness Workshop",
    courseTitle: "Career Services",
    trainerName: "Nisha Verma",
    date: "2026-08-12",
    startTime: "17:00",
    endTime: "18:30",
    platform: "Microsoft Teams",
    type: "Workshop",
    visibility: "public",
    published: false,
    status: "Completed",
    registrations: 96,
  },
];

export const mockTeacherStats: TeacherStats = {
  totalStudents: 428,
  activeStudents: 361,
  courses: 4,
  averageCompletion: 72,
  averageQuizScore: 81,
  problemsSolved: 5842,
  engagement: 86,
  upcomingClasses: 6,
};

const studentNames = [
  "Aarav Sharma",
  "Ishita Nair",
  "Rohan Gupta",
  "Sneha Reddy",
  "Karthik Iyer",
  "Priya Menon",
  "Aditya Rao",
  "Fatima Khan",
];

export const mockTeacherStudents: TeacherStudentRow[] = studentNames.map((name, i) => ({
  id: `st-${i + 1}`,
  name,
  email: `${name.split(" ")[0]!.toLowerCase()}@example.com`,
  courseTitle: mockCourses[i % 3]!.title,
  progressPercent: [68, 91, 44, 77, 25, 88, 59, 12][i]!,
  lessonsCompleted: [34, 46, 22, 39, 12, 44, 30, 6][i]!,
  quizScore: [87, 94, 61, 82, 48, 90, 74, 39][i]!,
  assignments: ["6/8", "8/8", "3/8", "7/8", "2/8", "8/8", "5/8", "1/8"][i]!,
  problemsSolved: [126, 210, 48, 143, 19, 188, 92, 7][i]!,
  points: [8450, 12450, 3120, 9180, 1240, 11980, 6040, 480][i]!,
  rank: [12, 1, 84, 9, 172, 2, 38, 240][i]!,
  lastActive: ["2h ago", "20m ago", "5d ago", "1h ago", "12d ago", "35m ago", "1d ago", "22d ago"][i]!,
  status: (["active", "active", "at_risk", "active", "inactive", "active", "active", "inactive"] as const)[i]!,
}));

export const mockAdminStats: AdminStats = {
  students: 12480,
  teachers: 46,
  courses: 128,
  batches: 34,
  liveEvents: 12,
  openEnquiries: 87,
  revenueThisMonth: 4820000,
  activeBatches: 21,
};

export const mockLeaderboard: LeaderboardEntry[] = mockTeacherStudents
  .slice()
  .sort((a, b) => b.points - a.points)
  .map((s, i) => ({
    rank: i + 1,
    studentId: s.id,
    name: s.name,
    points: s.points,
    problemsSolved: s.problemsSolved,
    quizScore: s.quizScore,
    attendance: 78 + ((i * 3) % 20),
    streak: [17, 34, 2, 11, 0, 26, 8, 0][i] ?? 0,
    level: ["Elite", "Expert", "Learner", "Advanced", "Beginner", "Expert", "Intermediate", "Beginner"][i] ?? "Learner",
    isCurrentUser: s.name === "Aarav Sharma",
  }));
