import { createFileRoute } from "@tanstack/react-router";
import {
  deletePerfTest,
  getPerfTest,
  type PerfTestInput,
  updatePerfTest,
} from "@/server/perf-tests";

const createRoute = createFileRoute as unknown as (
  path: string,
) => ReturnType<typeof createFileRoute>;

const toRecord = (value: unknown) =>
  value && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};

const parseId = (value: string) => {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
};

export const Route = createRoute("/api/perf-tests/$testId")({
  server: {
    handlers: {
      GET: async ({ params }) => {
        const { testId } = params as { testId: string };
        const id = parseId(testId);
        if (!id) {
          return new Response("Invalid test id", { status: 400 });
        }

        const test = await getPerfTest(id);
        if (!test) {
          return new Response("Test not found", { status: 404 });
        }

        return Response.json(test);
      },
      PUT: async ({ params, request }) => {
        const { testId } = params as { testId: string };
        const id = parseId(testId);
        if (!id) {
          return new Response("Invalid test id", { status: 400 });
        }

        const body = (await request.json()) as Record<string, unknown>;
        const name = typeof body.name === "string" ? body.name : "";
        const description =
          typeof body.description === "string" ? body.description : undefined;
        const scriptPath =
          typeof body.scriptPath === "string" ? body.scriptPath : undefined;
        const templateId =
          typeof body.templateId === "number"
            ? body.templateId
            : typeof body.templateId === "string"
              ? Number(body.templateId)
              : undefined;

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

        const test = await updatePerfTest(id, input);
        return Response.json(test);
      },
      DELETE: async ({ params }) => {
        const { testId } = params as { testId: string };
        const id = parseId(testId);
        if (!id) {
          return new Response("Invalid test id", { status: 400 });
        }

        await deletePerfTest(id);
        return new Response(null, { status: 204 });
      },
    },
  },
});
