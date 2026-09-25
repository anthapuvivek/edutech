import { cpp } from "@codemirror/lang-cpp";
import { go } from "@codemirror/lang-go";
import { java } from "@codemirror/lang-java";
import { javascript } from "@codemirror/lang-javascript";
import { python } from "@codemirror/lang-python";
import { oneDark } from "@codemirror/theme-one-dark";
import CodeMirror from "@uiw/react-codemirror";
import { useEffect, useState } from "react";

/**
 * Source editor for the coding practice pages.
 *
 * CodeMirror touches `document` when it builds its view, so it cannot run during SSR.
 * Until the component has mounted on the client we render a plain textarea with the same
 * value and handler: the page is usable immediately and through any hydration hiccup,
 * then upgrades in place. That is why the fallback is a working editor rather than a
 * spinner — a student should never be blocked from typing.
 */

function extensionsFor(language: string) {
  switch (language) {
    case "python":
      return [python()];
    case "cpp":
    case "c":
      return [cpp()];
    case "go":
      return [go()];
    case "javascript":
      return [javascript()];
    case "typescript":
      return [javascript({ typescript: true })];
    // C# has no first-party CodeMirror 6 mode here; plain text still edits fine.
    case "csharp":
      return [];
    case "java":
    default:
      return [java()];
  }
}

export function CodeEditor({
  value,
  onChange,
  language = "java",
  height = "420px",
  readOnly = false,
}: {
  value: string;
  onChange: (next: string) => void;
  language?: string;
  height?: string;
  readOnly?: boolean;
}) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  if (!mounted) {
    return (
      <textarea
        value={value}
        onChange={(e) => onChange(e.target.value)}
        readOnly={readOnly}
        spellCheck={false}
        style={{ height }}
        className="w-full resize-y rounded-md border bg-zinc-900 p-3 font-mono text-sm text-zinc-100 outline-none"
        aria-label="Source code"
      />
    );
  }

  return (
    <div className="overflow-hidden rounded-md border">
      <CodeMirror
        value={value}
        height={height}
        theme={oneDark}
        extensions={extensionsFor(language)}
        editable={!readOnly}
        onChange={onChange}
        basicSetup={{
          lineNumbers: true,
          highlightActiveLine: true,
          bracketMatching: true,
          autocompletion: false,
          foldGutter: false,
        }}
      />
    </div>
  );
}
