export function Logo({ tone = "default" }: { tone?: "default" | "inverse" }) {
  return (
    <span className="flex items-center gap-2.5">
      <span
        aria-hidden
        className={
          tone === "inverse"
            ? "grid size-8 place-items-center rounded-md bg-accent text-accent-foreground"
            : "grid size-8 place-items-center rounded-md bg-primary text-primary-foreground"
        }
      >
        <span className="text-display text-[17px] leading-none">L</span>
      </span>
      <span
        className={
          tone === "inverse"
            ? "text-display text-xl text-ink-foreground"
            : "text-display text-xl text-foreground"
        }
      >
        Learntrix
      </span>
    </span>
  );
}
