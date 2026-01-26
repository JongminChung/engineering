import { createFileRoute } from "@tanstack/react-router";
import {
  createPerfTest,
  listPerfTests,
  type PerfTestInput,
} from "@/server/perf-tests";

const createRoute = createFileRoute as unknown as (
  path: string,
) => ReturnType<typeof createFileRoute>;

const toRecord = (value: unknown) =>
  value && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};

const parseId = (value: unknown) => {
  if (typeof value === "number" && Number.isInteger(value)) {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isInteger(parsed) ? parsed : null;
  }
  return null;
};

export const Route = createRoute("/api/perf-tests")({
  server: {
    handlers: {
      GET: async () => {
        const tests = await listPerfTests();
        return Response.json(tests);
      },
      POST: async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        const name = typeof body.name === "string" ? body.name : "";
        const description =
          typeof body.description === "string" ? body.description : undefined;
        const scriptPath =
          typeof body.scriptPath === "string" ? body.scriptPath : undefined;
        const templateId = parseId(body.templateId);

        if (!name) {
          return new Response("Invalid test payload", { status: 400 });
        }

        const input: PerfTestInput = {
          name,
          description,
          scriptPath,
          templateId: templateId ?? undefined,
          config: toRecord(body.config),
        };

        const test = await createPerfTest(input);
        return Response.json(test, { status: 201 });
      },
    },
  },
});
