import { createFileRoute } from "@tanstack/react-router";
import { createTestRun, listTestRuns } from "@/server/perf-tests";

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

export const Route = createRoute("/api/perf-tests/$testId/runs")({
  server: {
    handlers: {
      GET: async ({ params }) => {
        const { testId } = params as { testId: string };
        const id = parseId(testId);
        if (!id) {
          return new Response("Invalid test id", { status: 400 });
        }

        const runs = await listTestRuns(id);
        return Response.json(runs);
      },
      POST: async ({ params, request }) => {
        const { testId } = params as { testId: string };
        const id = parseId(testId);
        if (!id) {
          return new Response("Invalid test id", { status: 400 });
        }

        const body = (await request.json()) as Record<string, unknown>;
        const run = await createTestRun({
          testId: id,
          overrides: toRecord(body.overrides),
        });

        return Response.json(run, { status: 201 });
      },
    },
  },
});
