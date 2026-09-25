import { createFileRoute, redirect } from "@tanstack/react-router";

// The former in-app editor route is intentionally retired. Coding Practice now opens
// external platforms and keeps progress on the batch assignment list.
export const Route = createFileRoute("/student/coding-practice/$problemId")({
  beforeLoad: () => {
    throw redirect({ to: "/student/coding-practice" });
  },
});
