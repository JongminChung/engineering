import { createFileRoute } from "@tanstack/react-router";
import { getTestRun, stopTestRun } from "@/server/perf-tests";

const createRoute = createFileRoute as unknown as (
  path: string,
) => ReturnType<typeof createFileRoute>;

export const Route = createRoute("/api/test-runs/$runId")({
  server: {
    handlers: {
      GET: async ({ params }) => {
        const { runId } = params as { runId: string };
        if (!runId) {
          return new Response("Invalid run id", { status: 400 });
        }

        const run = await getTestRun(runId);
        if (!run) {
          return new Response("Run not found", { status: 404 });
        }

        return Response.json(run);
      },
      DELETE: async ({ params }) => {
        const { runId } = params as { runId: string };
        if (!runId) {
          return new Response("Invalid run id", { status: 400 });
        }

        const run = await stopTestRun(runId);
        return Response.json(run);
      },
    },
  },
});
