import { createFileRoute, useRouter } from "@tanstack/react-router";
import { type CSSProperties, type FormEvent, useMemo, useState } from "react";
import {
  listPerfTests,
  listTemplates,
  listTestRuns,
  type TemplateCategory,
} from "@/server/perf-tests";

type DashboardData = {
  templates: Awaited<ReturnType<typeof listTemplates>>;
  tests: Awaited<ReturnType<typeof listPerfTests>>;
  runs: Awaited<ReturnType<typeof listTestRuns>>;
};

const createRoute = createFileRoute as unknown as (
  path: string,
) => ReturnType<typeof createFileRoute>;

const templateCategories: TemplateCategory[] = [
  "api",
  "websocket",
  "grpc",
  "db",
  "composite",
];

const templateLabels: Record<TemplateCategory, string> = {
  api: "API Load",
  websocket: "WebSocket",
  grpc: "gRPC",
  db: "DB Query",
  composite: "Composite",
};

const toNumber = (value: FormDataEntryValue | null) => {
  if (typeof value !== "string" || !value.trim()) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
};

const toRecord = (value: unknown) =>
  value && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};

export const Route = createRoute("/perf-tests")({
  loader: async () => {
    const [templates, tests, runs] = await Promise.all([
      listTemplates(),
      listPerfTests(),
      listTestRuns(),
    ]);

    return { templates, tests, runs } as DashboardData;
  },
  component: PerfTestsPage,
});

function PerfTestsPage() {
  const router = useRouter();
  const { templates, tests, runs } = Route.useLoaderData() as DashboardData;
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(
    templates[0]?.id ?? null,
  );
  const [templateError, setTemplateError] = useState<string | null>(null);
  const [testError, setTestError] = useState<string | null>(null);
  const [runError, setRunError] = useState<string | null>(null);

  const selectedTemplate = useMemo(
    () =>
      templates.find((template) => template.id === selectedTemplateId) ?? null,
    [selectedTemplateId, templates],
  );

  const lastRunByTest = useMemo(() => {
    const map = new Map<number, (typeof runs)[number]>();
    for (const run of runs) {
      if (!run.testId) {
        continue;
      }
      if (!map.has(run.testId)) {
        map.set(run.testId, run);
      }
    }
    return map;
  }, [runs]);

  const templateDefaults = toRecord(selectedTemplate?.defaultConfig);
  const defaults = {
    targetUrl:
      typeof templateDefaults.targetUrl === "string"
        ? templateDefaults.targetUrl
        : "",
    vus:
      typeof templateDefaults.vus === "number"
        ? templateDefaults.vus
        : typeof templateDefaults.vus === "string"
          ? templateDefaults.vus
          : "",
    duration:
      typeof templateDefaults.duration === "string"
        ? templateDefaults.duration
        : "",
    thresholds:
      typeof templateDefaults.thresholds === "string"
        ? templateDefaults.thresholds
        : "",
    parallelism:
      typeof templateDefaults.parallelism === "number"
        ? templateDefaults.parallelism
        : typeof templateDefaults.parallelism === "string"
          ? templateDefaults.parallelism
          : "",
  };

  const handleCreateTemplate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setTemplateError(null);

    const formData = new FormData(event.currentTarget);
    const name = String(formData.get("templateName") ?? "").trim();
    const category = String(formData.get("templateCategory") ?? "api");
    const description = String(
      formData.get("templateDescription") ?? "",
    ).trim();
    const scriptTemplate = String(formData.get("scriptTemplate") ?? "").trim();
    const defaultConfigInput = String(
      formData.get("defaultConfig") ?? "",
    ).trim();

    let defaultConfig = {} as Record<string, unknown>;

    if (defaultConfigInput) {
      try {
        defaultConfig = JSON.parse(defaultConfigInput) as Record<
          string,
          unknown
        >;
      } catch {
        setTemplateError("Default config must be valid JSON.");
        return;
      }
    }

    if (!name || !scriptTemplate) {
      setTemplateError("Template name and script are required.");
      return;
    }

    try {
      const response = await fetch("/api/test-templates", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          name,
          category: category as TemplateCategory,
          description: description || undefined,
          scriptTemplate,
          defaultConfig,
        }),
      });

      if (!response.ok) {
        setTemplateError("Failed to create template.");
        return;
      }

      const template =
        (await response.json()) as DashboardData["templates"][number];
      setSelectedTemplateId(template.id);
      event.currentTarget.reset();
      router.invalidate();
    } catch {
      setTemplateError("Failed to create template.");
    }
  };

  const handleCreateTest = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setTestError(null);

    const formData = new FormData(event.currentTarget);
    const name = String(formData.get("testName") ?? "").trim();
    const description = String(formData.get("testDescription") ?? "").trim();
    const scriptPath = String(formData.get("scriptPath") ?? "").trim();
    const targetUrl = String(formData.get("targetUrl") ?? "").trim();
    const duration = String(formData.get("duration") ?? "").trim();
    const thresholds = String(formData.get("thresholds") ?? "").trim();
    const vus = toNumber(formData.get("vus"));
    const parallelism = toNumber(formData.get("parallelism"));

    if (!name) {
      setTestError("Test name is required.");
      return;
    }

    const config = {
      ...(targetUrl ? { targetUrl } : {}),
      ...(duration ? { duration } : {}),
      ...(thresholds ? { thresholds } : {}),
      ...(vus !== undefined ? { vus } : {}),
      ...(parallelism !== undefined ? { parallelism } : {}),
    };

    try {
      const response = await fetch("/api/perf-tests", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          name,
          description: description || undefined,
          scriptPath: scriptPath || undefined,
          templateId: selectedTemplate?.id ?? undefined,
          config,
        }),
      });

      if (!response.ok) {
        setTestError("Failed to create test.");
        return;
      }

      event.currentTarget.reset();
      router.invalidate();
    } catch {
      setTestError("Failed to create test.");
    }
  };

  const handleRun = async (testId: number) => {
    setRunError(null);
    try {
      const response = await fetch(`/api/perf-tests/${testId}/runs`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({}),
      });

      if (!response.ok) {
        setRunError("Failed to start run.");
        return;
      }
      router.invalidate();
    } catch {
      setRunError("Failed to start run.");
    }
  };

  const handleStop = async (runId: string) => {
    setRunError(null);
    try {
      const response = await fetch(`/api/test-runs/${runId}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        setRunError("Failed to stop run.");
        return;
      }
      router.invalidate();
    } catch {
      setRunError("Failed to stop run.");
    }
  };

  return (
    <div
      className="relative min-h-screen overflow-hidden text-slate-900"
      style={
        {
          background:
            "radial-gradient(circle at top, rgba(198, 224, 212, 0.6), transparent 55%), radial-gradient(circle at 20% 20%, rgba(254, 221, 196, 0.45), transparent 45%), linear-gradient(135deg, #f7f4ef 0%, #e7f1f0 100%)",
          color: "#1c2431",
          "--panel": "rgba(255, 255, 255, 0.82)",
          "--panel-border": "rgba(148, 163, 184, 0.35)",
          "--accent": "#ea7f58",
          "--accent-strong": "#c65d3c",
          "--ink-soft": "#4b5563",
        } as CSSProperties
      }
    >
      <div className="absolute inset-0 opacity-70">
        <div className="absolute -top-24 right-0 h-72 w-72 rounded-full bg-amber-200/40 blur-3xl"></div>
        <div className="absolute bottom-0 left-10 h-80 w-80 rounded-full bg-emerald-200/50 blur-3xl"></div>
      </div>

      <div className="relative mx-auto max-w-6xl px-6 py-14 space-y-12">
        <header className="space-y-4 animate-rise">
          <p className="uppercase tracking-[0.3em] text-xs font-semibold text-amber-700">
            K6 Operator Control Room
          </p>
          <h1
            className="text-4xl md:text-5xl font-semibold"
            style={{ fontFamily: '"Newsreader", serif' }}
          >
            Performance Test Workspace
          </h1>
          <p
            className="text-base md:text-lg"
            style={{ color: "var(--ink-soft)" }}
          >
            Assemble reusable templates, launch tests, and track run history
            without leaving the UI.
          </p>
        </header>

        <section className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] animate-rise">
          <div
            className="rounded-3xl border p-6 shadow-xl"
            style={{
              background: "var(--panel)",
              borderColor: "var(--panel-border)",
            }}
          >
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-2xl font-semibold">Template Library</h2>
                <p className="text-sm" style={{ color: "var(--ink-soft)" }}>
                  Pick a template to prefill new tests.
                </p>
              </div>
              <span
                className="rounded-full px-3 py-1 text-xs font-semibold"
                style={{
                  backgroundColor: "rgba(234, 127, 88, 0.15)",
                  color: "var(--accent-strong)",
                }}
              >
                {templates.length} ready
              </span>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              {templates.length === 0 && (
                <div className="col-span-full rounded-2xl border border-dashed p-6 text-sm text-slate-500">
                  No templates yet. Create one using the panel on the right.
                </div>
              )}
              {templates.map((template) => (
                <button
                  key={template.id}
                  type="button"
                  onClick={() => setSelectedTemplateId(template.id)}
                  className={`rounded-2xl border p-4 text-left transition-all hover:-translate-y-1 ${
                    selectedTemplateId === template.id
                      ? "shadow-lg"
                      : "shadow-sm"
                  }`}
                  style={{
                    background: "rgba(255, 255, 255, 0.88)",
                    borderColor:
                      selectedTemplateId === template.id
                        ? "var(--accent)"
                        : "var(--panel-border)",
                  }}
                >
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-semibold text-slate-700">
                      {template.name}
                    </span>
                    <span
                      className="rounded-full px-2 py-1 text-[11px] font-semibold"
                      style={{
                        backgroundColor: "rgba(71, 85, 105, 0.12)",
                        color: "#475569",
                      }}
                    >
                      {templateLabels[template.category as TemplateCategory] ??
                        template.category}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 min-h-[32px]">
                    {template.description || "Template description is not set."}
                  </p>
                  <div className="mt-4 rounded-lg bg-slate-900/90 px-3 py-2 text-[11px] text-emerald-200">
                    {template.scriptTemplate.slice(0, 80)}
                    {template.scriptTemplate.length > 80 ? "..." : ""}
                  </div>
                </button>
              ))}
            </div>
          </div>

          <form
            onSubmit={handleCreateTemplate}
            className="rounded-3xl border p-6 shadow-xl flex flex-col gap-4"
            style={{
              background: "var(--panel)",
              borderColor: "var(--panel-border)",
            }}
          >
            <div>
              <h2 className="text-2xl font-semibold">Create Template</h2>
              <p className="text-sm" style={{ color: "var(--ink-soft)" }}>
                Define a reusable script and default config.
              </p>
            </div>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Name
              <input
                name="templateName"
                className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                style={{ borderColor: "var(--panel-border)" }}
                placeholder="Checkout peak"
              />
            </label>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Category
              <select
                name="templateCategory"
                className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                style={{ borderColor: "var(--panel-border)" }}
                defaultValue="api"
              >
                {templateCategories.map((category) => (
                  <option key={category} value={category}>
                    {templateLabels[category]}
                  </option>
                ))}
              </select>
            </label>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Description
              <input
                name="templateDescription"
                className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                style={{ borderColor: "var(--panel-border)" }}
                placeholder="High-traffic checkout flow"
              />
            </label>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Script Template
              <textarea
                name="scriptTemplate"
                rows={6}
                className="mt-2 w-full rounded-xl border px-4 py-2 text-xs font-mono"
                style={{ borderColor: "var(--panel-border)" }}
                placeholder={`import http from "k6/http";\nexport default function () {\n  http.get("${"$"}{TARGET_URL}");\n}`}
              />
            </label>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Default Config (JSON)
              <textarea
                name="defaultConfig"
                rows={4}
                className="mt-2 w-full rounded-xl border px-4 py-2 text-xs font-mono"
                style={{ borderColor: "var(--panel-border)" }}
                placeholder='{"targetUrl":"https://api.example.com","vus":50,"duration":"30s"}'
              />
            </label>
            {templateError && (
              <p className="text-xs text-rose-600">{templateError}</p>
            )}
            <button
              type="submit"
              className="mt-auto rounded-xl px-4 py-2 text-sm font-semibold text-white transition"
              style={{ background: "var(--accent)" }}
            >
              Save Template
            </button>
          </form>
        </section>

        <section
          className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr] animate-rise"
          style={{ animationDelay: "0.1s" }}
        >
          <form
            onSubmit={handleCreateTest}
            key={selectedTemplate?.id ?? "new"}
            className="rounded-3xl border p-6 shadow-xl grid gap-4"
            style={{
              background: "var(--panel)",
              borderColor: "var(--panel-border)",
            }}
          >
            <div>
              <h2 className="text-2xl font-semibold">Test Builder</h2>
              <p className="text-sm" style={{ color: "var(--ink-soft)" }}>
                {selectedTemplate
                  ? `Prefilled from ${selectedTemplate.name}.`
                  : "Create a test with your own settings."}
              </p>
            </div>
            <div className="grid gap-3 md:grid-cols-2">
              <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Name
                <input
                  name="testName"
                  className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                  style={{ borderColor: "var(--panel-border)" }}
                  placeholder="Checkout baseline"
                />
              </label>
              <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Description
                <input
                  name="testDescription"
                  className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                  style={{ borderColor: "var(--panel-border)" }}
                  placeholder="Daily smoke run"
                />
              </label>
            </div>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Script Path (optional)
              <input
                name="scriptPath"
                className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                style={{ borderColor: "var(--panel-border)" }}
                placeholder="/scripts/checkout/script.js"
              />
            </label>
            <div className="grid gap-3 md:grid-cols-2">
              <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Target URL
                <input
                  name="targetUrl"
                  defaultValue={String(defaults.targetUrl)}
                  className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                  style={{ borderColor: "var(--panel-border)" }}
                  placeholder="https://api.example.com"
                />
              </label>
              <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Duration
                <input
                  name="duration"
                  defaultValue={String(defaults.duration)}
                  className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                  style={{ borderColor: "var(--panel-border)" }}
                  placeholder="30s"
                />
              </label>
              <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                VUs
                <input
                  name="vus"
                  type="number"
                  defaultValue={String(defaults.vus)}
                  className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                  style={{ borderColor: "var(--panel-border)" }}
                  placeholder="50"
                />
              </label>
              <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                Parallelism
                <input
                  name="parallelism"
                  type="number"
                  defaultValue={String(defaults.parallelism)}
                  className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                  style={{ borderColor: "var(--panel-border)" }}
                  placeholder="1"
                />
              </label>
            </div>
            <label className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              Thresholds
              <input
                name="thresholds"
                defaultValue={String(defaults.thresholds)}
                className="mt-2 w-full rounded-xl border px-4 py-2 text-sm"
                style={{ borderColor: "var(--panel-border)" }}
                placeholder="http_req_duration<500"
              />
            </label>
            {testError && <p className="text-xs text-rose-600">{testError}</p>}
            <button
              type="submit"
              className="rounded-xl px-4 py-2 text-sm font-semibold text-white transition"
              style={{ background: "var(--accent)" }}
            >
              Save Test
            </button>
          </form>

          <div
            className="rounded-3xl border p-6 shadow-xl flex flex-col gap-4"
            style={{
              background: "var(--panel)",
              borderColor: "var(--panel-border)",
            }}
          >
            <div>
              <h2 className="text-2xl font-semibold">Active Tests</h2>
              <p className="text-sm" style={{ color: "var(--ink-soft)" }}>
                Trigger runs straight from the list.
              </p>
            </div>
            {tests.length === 0 && (
              <div className="rounded-2xl border border-dashed p-6 text-sm text-slate-500">
                No tests saved yet.
              </div>
            )}
            <div className="space-y-4">
              {tests.map((test) => {
                const lastRun = test.id
                  ? lastRunByTest.get(test.id)
                  : undefined;
                const isRunning = lastRun?.status === "running";
                return (
                  <div
                    key={test.id}
                    className="rounded-2xl border px-4 py-3"
                    style={{
                      background: "rgba(255, 255, 255, 0.88)",
                      borderColor: "var(--panel-border)",
                    }}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-800">
                          {test.name}
                        </p>
                        <p className="text-xs text-slate-500">
                          {test.description || "No description"}
                        </p>
                        <p className="text-[11px] text-slate-400 mt-2">
                          Template: {test.templateName || "Custom"}
                        </p>
                      </div>
                      <div className="flex flex-col items-end gap-2">
                        <span
                          className="rounded-full px-2 py-1 text-[11px] font-semibold"
                          style={{
                            backgroundColor: isRunning
                              ? "rgba(16, 185, 129, 0.15)"
                              : "rgba(148, 163, 184, 0.2)",
                            color: isRunning ? "#047857" : "#64748b",
                          }}
                        >
                          {lastRun?.status ?? "idle"}
                        </span>
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => handleRun(test.id)}
                            className="rounded-lg px-3 py-1 text-xs font-semibold text-white"
                            style={{ background: "#0f766e" }}
                          >
                            Run
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              lastRun?.runId && handleStop(lastRun.runId)
                            }
                            disabled={!lastRun?.runId || !isRunning}
                            className="rounded-lg px-3 py-1 text-xs font-semibold text-white disabled:opacity-40"
                            style={{ background: "#b45309" }}
                          >
                            Stop
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
            {runError && <p className="text-xs text-rose-600">{runError}</p>}
          </div>
        </section>

        <section
          className="rounded-3xl border p-6 shadow-xl animate-rise"
          style={{
            background: "var(--panel)",
            borderColor: "var(--panel-border)",
            animationDelay: "0.2s",
          }}
        >
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-semibold">Run History</h2>
              <p className="text-sm" style={{ color: "var(--ink-soft)" }}>
                Latest 50 runs across all tests.
              </p>
            </div>
            <span className="text-xs font-semibold text-slate-500">
              {runs.length} tracked
            </span>
          </div>
          <div className="space-y-3">
            {runs.length === 0 && (
              <div className="rounded-2xl border border-dashed p-6 text-sm text-slate-500">
                No runs yet.
              </div>
            )}
            {runs.map((run) => (
              <div
                key={run.id}
                className="rounded-2xl border px-4 py-3 text-sm"
                style={{
                  background: "rgba(255, 255, 255, 0.88)",
                  borderColor: "var(--panel-border)",
                }}
              >
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-800">
                      {run.testName || "Unknown Test"}
                    </p>
                    <p className="text-xs text-slate-500">
                      Run ID: {run.runId}
                    </p>
                  </div>
                  <div className="flex items-center gap-4">
                    <span
                      className="rounded-full px-2 py-1 text-[11px] font-semibold"
                      style={{
                        backgroundColor:
                          run.status === "running"
                            ? "rgba(16, 185, 129, 0.15)"
                            : run.status === "failed"
                              ? "rgba(239, 68, 68, 0.15)"
                              : "rgba(148, 163, 184, 0.2)",
                        color:
                          run.status === "running"
                            ? "#047857"
                            : run.status === "failed"
                              ? "#b91c1c"
                              : "#64748b",
                      }}
                    >
                      {run.status}
                    </span>
                    <span className="text-xs text-slate-500">
                      {run.startedAt
                        ? new Date(run.startedAt).toLocaleString()
                        : "Pending"}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
