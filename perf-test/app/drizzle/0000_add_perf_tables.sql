CREATE TYPE "test_template_category" AS ENUM (
  'api',
  'websocket',
  'grpc',
  'db',
  'composite'
);

CREATE TYPE "test_run_status" AS ENUM (
  'queued',
  'running',
  'succeeded',
  'failed',
  'canceled',
  'timeout'
);

CREATE TABLE "test_templates" (
  "id" serial PRIMARY KEY,
  "name" text NOT NULL,
  "category" "test_template_category" NOT NULL,
  "description" text,
  "script_template" text NOT NULL,
  "default_config" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "created_at" timestamp NOT NULL DEFAULT now(),
  "updated_at" timestamp NOT NULL DEFAULT now()
);

CREATE TABLE "perf_tests" (
  "id" serial PRIMARY KEY,
  "name" text NOT NULL,
  "description" text,
  "script_path" text,
  "template_id" integer REFERENCES "test_templates"("id"),
  "config" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "created_at" timestamp NOT NULL DEFAULT now(),
  "updated_at" timestamp NOT NULL DEFAULT now()
);

CREATE TABLE "test_results" (
  "id" serial PRIMARY KEY,
  "test_id" integer NOT NULL REFERENCES "perf_tests"("id"),
  "run_id" text NOT NULL,
  "status" "test_run_status" NOT NULL DEFAULT 'queued',
  "summary" jsonb,
  "k6_resource_name" text,
  "start_time" timestamp,
  "end_time" timestamp,
  "created_at" timestamp NOT NULL DEFAULT now()
);

CREATE INDEX "perf_tests_template_id_idx" ON "perf_tests" ("template_id");
CREATE INDEX "test_results_test_id_idx" ON "test_results" ("test_id");
CREATE INDEX "test_results_run_id_idx" ON "test_results" ("run_id");
