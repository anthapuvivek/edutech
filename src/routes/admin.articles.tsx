import { useQuery } from "@tanstack/react-query";
import { createFileRoute } from "@tanstack/react-router";
import { Plus } from "lucide-react";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { Panel, StatusBadge } from "@/components/portal/AdminBits";
import { PageHeader, StatCard } from "@/components/portal/StatCard";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { adminService } from "@/services/admin.service";

export const Route = createFileRoute("/admin/articles")({
  head: () => ({
    meta: [
      { title: "Articles — Learntrix Admin" },
      { name: "description", content: "Author, review and publish articles. Teacher submissions flow through admin review before going live." },
      { property: "og:title", content: "Articles — Learntrix Admin" },
      { property: "og:description", content: "Editorial workflow for the Learntrix knowledge library." },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminArticles,
});

const ALL = "all";

function AdminArticles() {
  const articles = useQuery({ queryKey: ["admin", "articles"], queryFn: () => adminService.articles() });
  const [status, setStatus] = useState(ALL);
  const [open, setOpen] = useState(false);
  const data = articles.data ?? [];
  const rows = useMemo(() => data.filter((a) => status === ALL || a.status === status), [data, status]);

  return (
    <>
      <PageHeader
        title="Articles"
        description="Draft → In review → Published. Only admins can publish; teachers submit for review and students read."
        action={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="size-4" aria-hidden /> New article
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
              <DialogHeader>
                <DialogTitle>New article</DialogTitle>
                <DialogDescription>Content, categorisation and SEO metadata for the public knowledge library.</DialogDescription>
              </DialogHeader>
              <form
                id="new-article"
                className="grid gap-4"
                onSubmit={(e) => {
                  e.preventDefault();
                  const title = String(new FormData(e.currentTarget).get("title") ?? "").trim();
                  if (!title) {
                    toast.error("Title is required.");
                    return;
                  }
                  void adminService.saveArticle({ title }).then(() => {
                    toast.success("Article saved as draft.");
                    setOpen(false);
                  });
                }}
              >
                <div className="grid gap-4 sm:grid-cols-2">
                  {[
                    { id: "title", label: "Title" },
                    { id: "slug", label: "Slug" },
                    { id: "category", label: "Category" },
                    { id: "tags", label: "Tags (comma separated)" },
                    { id: "seoTitle", label: "SEO title" },
                    { id: "publishDate", label: "Publish date", type: "date" },
                  ].map((f) => (
                    <div key={f.id} className="space-y-1.5">
                      <Label htmlFor={f.id}>{f.label}</Label>
                      <Input id={f.id} name={f.id} type={f.type ?? "text"} maxLength={160} />
                    </div>
                  ))}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="excerpt">Excerpt</Label>
                  <Textarea id="excerpt" name="excerpt" rows={2} maxLength={300} />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="body">Content</Label>
                  <Textarea id="body" name="body" rows={10} maxLength={20000} placeholder="Markdown supported — headings, lists, code blocks, links." />
                </div>
              </form>
              <DialogFooter>
                <Button type="submit" form="new-article">
                  Save draft
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        }
      />

      <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Articles" value={data.length} />
        <StatCard label="Awaiting review" value={data.filter((a) => a.status === "In Review").length} />
        <StatCard label="Published" value={data.filter((a) => a.status === "Published").length} />
        <StatCard label="Total views" value={data.reduce((s, a) => s + a.views, 0).toLocaleString()} />
      </section>

      <Panel
        title="Editorial queue"
        action={
          <Select value={status} onValueChange={setStatus}>
            <SelectTrigger className="w-40" aria-label="Filter by status">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All statuses</SelectItem>
              {["Draft", "In Review", "Published", "Scheduled", "Archived"].map((s) => (
                <SelectItem key={s} value={s}>
                  {s}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        }
      >
        {articles.isPending ? (
          <Skeleton className="h-72 w-full" />
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Title</TableHead>
                  <TableHead>Author</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Publish date</TableHead>
                  <TableHead className="text-right">Views</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((a) => (
                  <TableRow key={a.id}>
                    <TableCell>
                      <div className="font-medium">{a.title}</div>
                      <div className="text-xs text-muted-foreground">/{a.slug}</div>
                    </TableCell>
                    <TableCell>
                      {a.authorName}
                      <div className="text-xs text-muted-foreground capitalize">{a.authorRole}</div>
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        <Badge variant="outline">{a.category}</Badge>
                        {a.tags.slice(0, 2).map((t) => (
                          <Badge key={t} variant="outline">
                            {t}
                          </Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell className="text-sm">{a.publishDate}</TableCell>
                    <TableCell className="text-right">{a.views.toLocaleString()}</TableCell>
                    <TableCell>
                      <StatusBadge value={a.status} />
                    </TableCell>
                    <TableCell className="space-x-2 text-right">
                      <Button size="sm" variant="outline" onClick={() => toast.info("Opening the editor.")}>
                        Edit
                      </Button>
                      <Button size="sm" onClick={() => toast.success(a.status === "Published" ? "Article unpublished." : "Article published.")}>
                        {a.status === "Published" ? "Unpublish" : "Publish"}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Panel>
    </>
  );
}
