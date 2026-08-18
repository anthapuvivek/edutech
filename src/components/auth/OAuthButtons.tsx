import { Button } from "@/components/ui/button";

export function OAuthButtons() {
  return (
    <div className="mt-6">
      <div className="flex items-center gap-3">
        <span className="h-px flex-1 bg-border" />
        <span className="text-xs uppercase tracking-widest text-muted-foreground">or</span>
        <span className="h-px flex-1 bg-border" />
      </div>
      <div className="mt-6 grid gap-2">
        <Button variant="outline" type="button" className="w-full">
          Continue with Google
        </Button>
        <Button variant="outline" type="button" className="w-full">
          Continue with GitHub
        </Button>
      </div>
    </div>
  );
}
