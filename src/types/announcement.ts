/**
 * Announcement types.
 *
 * `read` is per-student and comes from a row in `announcement_reads`; absence means unread,
 * which is what drives the unread badge.
 */

export type AnnouncementPriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";

export interface Announcement {
  id: string;
  courseId: string;
  courseTitle?: string | undefined;
  /** Null when addressed to the whole course rather than one cohort. */
  batchId?: string | null | undefined;
  batchName?: string | null | undefined;
  teacherId?: string | null | undefined;
  teacherName?: string | undefined;
  title: string;
  content: string;
  priority?: AnnouncementPriority | string | undefined;
  createdAt?: string | undefined;
  /** Student view only. */
  read?: boolean | null | undefined;
  /** Teacher view only. */
  readCount?: number | null | undefined;
}

export interface CreateAnnouncementPayload {
  courseId: string;
  batchId?: string | undefined;
  title: string;
  content: string;
  priority?: AnnouncementPriority | undefined;
}

export function priorityTone(
  p: string | null | undefined,
): "default" | "secondary" | "destructive" | "outline" {
  switch (p) {
    case "URGENT":
      return "destructive";
    case "HIGH":
      return "default";
    case "LOW":
      return "outline";
    default:
      return "secondary";
  }
}
