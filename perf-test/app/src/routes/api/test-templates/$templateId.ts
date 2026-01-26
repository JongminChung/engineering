import { createFileRoute } from "@tanstack/react-router";
import { templateCategory } from "@/db/schema";
import {
  deleteTemplate,
  getTemplate,
  type TemplateCategory,
  updateTemplate,
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

const parseId = (value: string) => {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? parsed : null;
};

export const Route = createRoute("/api/test-templates/$templateId")({
  server: {
    handlers: {
      GET: async ({ params }) => {
        const { templateId } = params as { templateId: string };
        const id = parseId(templateId);
        if (!id) {
          return new Response("Invalid template id", { status: 400 });
        }

        const template = await getTemplate(id);
        if (!template) {
          return new Response("Template not found", { status: 404 });
        }

        return Response.json(template);
      },
      PUT: async ({ params, request }) => {
        const { templateId } = params as { templateId: string };
        const id = parseId(templateId);
        if (!id) {
          return new Response("Invalid template id", { status: 400 });
        }

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

        const template = await updateTemplate(id, {
          name,
          category,
          description,
          scriptTemplate,
          defaultConfig: toRecord(body.defaultConfig),
        });

        return Response.json(template);
      },
      DELETE: async ({ params }) => {
        const { templateId } = params as { templateId: string };
        const id = parseId(templateId);
        if (!id) {
          return new Response("Invalid template id", { status: 400 });
        }

        await deleteTemplate(id);
        return new Response(null, { status: 204 });
      },
    },
  },
});
