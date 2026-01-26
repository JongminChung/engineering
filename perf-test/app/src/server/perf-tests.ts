import { desc, eq } from "drizzle-orm";
import { db } from "@/db";
import {
  perfTests,
  type templateCategory,
  testResults,
  type testRunStatus,
  testTemplates,
} from "@/db/schema";
import { createK6Run, renderTemplate, stopK6Run } from "@/server/k6-operator";

export type TemplateCategory = (typeof templateCategory.enumValues)[number];
export type TestRunStatus = (typeof testRunStatus.enumValues)[number];

export type TemplateInput = {
  name: string;
  category: TemplateCategory;
  description?: string;
  scriptTemplate: string;
  defaultConfig?: Record<string, unknown>;
};

export type PerfTestInput = {
  name: string;
  description?: string;
  scriptPath?: string | null;
  templateId?: number | null;
  config?: Record<string, unknown>;
};

export type RunCreateInput = {
  testId: number;
  overrides?: Record<string, unknown>;
};

const ensureRecord = (value: unknown) => {
  if (value && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return {};
};

export const listTemplates = async () => {
  return await db.query.testTemplates.findMany({
    orderBy: [desc(testTemplates.updatedAt)],
  });
};

export const getTemplate = async (id: number) => {
  return await db.query.testTemplates.findFirst({
    where: eq(testTemplates.id, id),
  });
};

export const createTemplate = async (input: TemplateInput) => {
  const [template] = await db
    .insert(testTemplates)
    .values({
      name: input.name,
      category: input.category,
      description: input.description ?? null,
      scriptTemplate: input.scriptTemplate,
      defaultConfig: input.defaultConfig ?? {},
    })
    .returning();

  return template;
};

export const updateTemplate = async (id: number, input: TemplateInput) => {
  const [template] = await db
    .update(testTemplates)
    .set({
      name: input.name,
      category: input.category,
      description: input.description ?? null,
      scriptTemplate: input.scriptTemplate,
      defaultConfig: input.defaultConfig ?? {},
      updatedAt: new Date(),
    })
    .where(eq(testTemplates.id, id))
    .returning();

  return template;
};

export const deleteTemplate = async (id: number) => {
  return await db.delete(testTemplates).where(eq(testTemplates.id, id));
};

export const listPerfTests = async () => {
  return await db
    .select({
      id: perfTests.id,
      name: perfTests.name,
      description: perfTests.description,
      scriptPath: perfTests.scriptPath,
      templateId: perfTests.templateId,
      templateName: testTemplates.name,
      config: perfTests.config,
      createdAt: perfTests.createdAt,
      updatedAt: perfTests.updatedAt,
    })
    .from(perfTests)
    .leftJoin(testTemplates, eq(perfTests.templateId, testTemplates.id))
    .orderBy(desc(perfTests.updatedAt));
};

export const getPerfTest = async (id: number) => {
  return await db.query.perfTests.findFirst({
    where: eq(perfTests.id, id),
  });
};

export const createPerfTest = async (input: PerfTestInput) => {
  const [test] = await db
    .insert(perfTests)
    .values({
      name: input.name,
      description: input.description ?? null,
      scriptPath: input.scriptPath ?? null,
      templateId: input.templateId ?? null,
      config: input.config ?? {},
    })
    .returning();

  return test;
};

export const updatePerfTest = async (id: number, input: PerfTestInput) => {
  const [test] = await db
    .update(perfTests)
    .set({
      name: input.name,
      description: input.description ?? null,
      scriptPath: input.scriptPath ?? null,
      templateId: input.templateId ?? null,
      config: input.config ?? {},
      updatedAt: new Date(),
    })
    .where(eq(perfTests.id, id))
    .returning();

  return test;
};

export const deletePerfTest = async (id: number) => {
  return await db.delete(perfTests).where(eq(perfTests.id, id));
};

export const listTestRuns = async (testId?: number) => {
  const baseQuery = db
    .select({
      id: testResults.id,
      runId: testResults.runId,
      status: testResults.status,
      summary: testResults.summary,
      k6ResourceName: testResults.k6ResourceName,
      startedAt: testResults.startedAt,
      endedAt: testResults.endedAt,
      createdAt: testResults.createdAt,
      testId: perfTests.id,
      testName: perfTests.name,
    })
    .from(testResults)
    .leftJoin(perfTests, eq(testResults.testId, perfTests.id));

  if (testId) {
    return await baseQuery
      .where(eq(testResults.testId, testId))
      .orderBy(desc(testResults.createdAt))
      .limit(50);
  }

  return await baseQuery.orderBy(desc(testResults.createdAt)).limit(50);
};

export const getTestRun = async (runId: string) => {
  return await db.query.testResults.findFirst({
    where: eq(testResults.runId, runId),
  });
};

export const createTestRun = async ({ testId, overrides }: RunCreateInput) => {
  const test = await db.query.perfTests.findFirst({
    where: eq(perfTests.id, testId),
  });

  if (!test) {
    throw new Error("Test not found");
  }

  const template = test.templateId
    ? await db.query.testTemplates.findFirst({
        where: eq(testTemplates.id, test.templateId),
      })
    : null;

  if (!template?.scriptTemplate) {
    throw new Error("Template script is missing");
  }

  const mergedConfig = {
    ...ensureRecord(template.defaultConfig),
    ...ensureRecord(test.config),
    ...ensureRecord(overrides),
  };

  const script = renderTemplate(template.scriptTemplate, mergedConfig);
  const runId = crypto.randomUUID();
  const resourceName = `k6-test-${test.id}-${runId.slice(0, 8)}`;
  const startedAt = new Date();

  const [run] = await db
    .insert(testResults)
    .values({
      testId: test.id,
      runId,
      status: "running",
      k6ResourceName: resourceName,
      startedAt,
    })
    .returning();

  try {
    await createK6Run({ name: resourceName, script, config: mergedConfig });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unknown error";
    await db
      .update(testResults)
      .set({
        status: "failed",
        endedAt: new Date(),
        summary: { error: message },
      })
      .where(eq(testResults.id, run.id));
    throw error;
  }

  return run;
};

export const stopTestRun = async (runId: string) => {
  const run = await db.query.testResults.findFirst({
    where: eq(testResults.runId, runId),
  });

  if (!run) {
    throw new Error("Run not found");
  }

  if (!run.k6ResourceName) {
    throw new Error("Run is missing K6 resource name");
  }

  await stopK6Run({ name: run.k6ResourceName });

  const [updated] = await db
    .update(testResults)
    .set({ status: "canceled", endedAt: new Date() })
    .where(eq(testResults.id, run.id))
    .returning();

  return updated;
};
