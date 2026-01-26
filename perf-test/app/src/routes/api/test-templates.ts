import { createFileRoute } from "@tanstack/react-router";
import { templateCategory } from "@/db/schema";
import {
  createTemplate,
  listTemplates,
  type TemplateCategory,
} from "@/server/perf-tests";

const createRoute = createFileRoute as unknown as (
  path: string,
) => ReturnType<typeof createFileRoute>;

const toRecord = (value: unknown) =>
  value && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};

const isTemplateCategory = (value: string): value is TemplateCategory =>
  templateCategory.enumValues.includes(value as TemplateCategory);

export const Route = createRoute("/api/test-templates")({
  server: {
    handlers: {
      GET: async () => {
        const templates = await listTemplates();
        return Response.json(templates);
      },
      POST: async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        const name = typeof body.name === "string" ? body.name : "";
        const category = typeof body.category === "string" ? body.category : "";
        const description =
          typeof body.description === "string" ? body.description : undefined;
        const scriptTemplate =
          typeof body.scriptTemplate === "string" ? body.scriptTemplate : "";

        if (!name || !scriptTemplate || !isTemplateCategory(category)) {
          return new Response("Invalid template payload", { status: 400 });
        }

        const template = await createTemplate({
          name,
          category,
          description,
          scriptTemplate,
          defaultConfig: toRecord(body.defaultConfig),
        });

        return Response.json(template, { status: 201 });
      },
    },
  },
});
