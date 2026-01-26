type K6OperatorConfig = {
  apiUrl: string;
  namespace: string;
  group: string;
  version: string;
  plural: string;
  kind: string;
  token?: string;
};

export type K6RunRequest = {
  name: string;
  namespace?: string;
  script: string;
  config?: Record<string, unknown>;
};

const DEFAULT_NAMESPACE = "monitoring";

const getK6OperatorConfig = (): K6OperatorConfig => {
  const apiUrl = process.env.K6_OPERATOR_API_URL;

  if (!apiUrl) {
    throw new Error("K6_OPERATOR_API_URL is not set");
  }

  return {
    apiUrl: apiUrl.replace(/\/$/, ""),
    namespace: process.env.K6_OPERATOR_NAMESPACE ?? DEFAULT_NAMESPACE,
    group: process.env.K6_OPERATOR_GROUP ?? "k6.io",
    version: process.env.K6_OPERATOR_VERSION ?? "v1alpha1",
    plural: process.env.K6_OPERATOR_PLURAL ?? "k6s",
    kind: process.env.K6_OPERATOR_KIND ?? "K6",
    token: process.env.K6_OPERATOR_TOKEN,
  };
};

const resolveNumber = (value: unknown, fallback: number) => {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return fallback;
};

export const renderTemplate = (
  template: string,
  config: Record<string, unknown>,
) => {
  const variables: Record<string, string> = {};

  for (const [key, value] of Object.entries(config)) {
    if (value === undefined || value === null) {
      continue;
    }
    const normalized = key
      .replace(/([a-z])([A-Z])/g, "$1_$2")
      .replace(/-/g, "_")
      .toUpperCase();
    variables[normalized] = String(value);
  }

  return template.replace(/\$\{([A-Z0-9_]+)\}/g, (match, key) => {
    if (Object.hasOwn(variables, key)) {
      return variables[key];
    }
    return match;
  });
};

export const createK6Run = async ({
  name,
  namespace,
  script,
  config,
}: K6RunRequest) => {
  const operator = getK6OperatorConfig();
  const targetNamespace = namespace ?? operator.namespace;
  const parallelism = resolveNumber(config?.parallelism, 1);
  const args =
    typeof config?.arguments === "string" ? config.arguments : undefined;

  const manifest: Record<string, unknown> = {
    apiVersion: `${operator.group}/${operator.version}`,
    kind: operator.kind,
    metadata: {
      name,
      namespace: targetNamespace,
    },
    spec: {
      parallelism,
      script: {
        inline: script,
      },
    },
  };

  if (args) {
    (manifest.spec as Record<string, unknown>).arguments = args;
  }

  const url = `${operator.apiUrl}/apis/${operator.group}/${operator.version}/namespaces/${targetNamespace}/${operator.plural}`;

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(operator.token ? { Authorization: `Bearer ${operator.token}` } : {}),
    },
    body: JSON.stringify(manifest),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      `Failed to create K6 run (${response.status} ${response.statusText}): ${message}`,
    );
  }

  return { name };
};

export const stopK6Run = async ({
  name,
  namespace,
}: {
  name: string;
  namespace?: string;
}) => {
  const operator = getK6OperatorConfig();
  const targetNamespace = namespace ?? operator.namespace;
  const url = `${operator.apiUrl}/apis/${operator.group}/${operator.version}/namespaces/${targetNamespace}/${operator.plural}/${name}`;

  const response = await fetch(url, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      ...(operator.token ? { Authorization: `Bearer ${operator.token}` } : {}),
    },
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(
      `Failed to stop K6 run (${response.status} ${response.statusText}): ${message}`,
    );
  }

  return { name };
};
