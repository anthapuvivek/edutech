import { createFileRoute } from "@tanstack/react-router";
import { TeacherCodingProblems } from "./teacher.coding-problems";

// Admins use the same management surface; backend RBAC broadens the data scope.
export const Route = createFileRoute("/admin/coding-problems")({
  component: TeacherCodingProblems,
});
