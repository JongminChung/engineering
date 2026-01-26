import { sql } from "drizzle-orm";
import {
  integer,
  jsonb,
  pgEnum,
  pgTable,
  serial,
  text,
  timestamp,
} from "drizzle-orm/pg-core";

export const templateCategory = pgEnum("test_template_category", [
  "api",
  "websocket",
  "grpc",
  "db",
  "composite",
]);

export const testRunStatus = pgEnum("test_run_status", [
  "queued",
  "running",
  "succeeded",
  "failed",
  "canceled",
  "timeout",
]);

export const testTemplates = pgTable("test_templates", {
  id: serial().primaryKey(),
  name: text().notNull(),
  category: templateCategory("category").notNull(),
  description: text(),
  scriptTemplate: text("script_template").notNull(),
  defaultConfig: jsonb("default_config")
    .$type<Record<string, unknown>>()
    .notNull()
    .default(sql`'{}'::jsonb`),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

export const perfTests = pgTable("perf_tests", {
  id: serial().primaryKey(),
  name: text().notNull(),
  description: text(),
  scriptPath: text("script_path"),
  templateId: integer("template_id").references(() => testTemplates.id),
  config: jsonb("config")
    .$type<Record<string, unknown>>()
    .notNull()
    .default(sql`'{}'::jsonb`),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

export const testResults = pgTable("test_results", {
  id: serial().primaryKey(),
  testId: integer("test_id")
    .notNull()
    .references(() => perfTests.id),
  runId: text("run_id").notNull(),
  status: testRunStatus("status").notNull().default("queued"),
  summary: jsonb("summary").$type<Record<string, unknown>>(),
  k6ResourceName: text("k6_resource_name"),
  startedAt: timestamp("start_time"),
  endedAt: timestamp("end_time"),
  createdAt: timestamp("created_at").notNull().defaultNow(),
});

export const todos = pgTable("todos", {
  id: serial().primaryKey(),
  title: text().notNull(),
  createdAt: timestamp("created_at").defaultNow(),
});
