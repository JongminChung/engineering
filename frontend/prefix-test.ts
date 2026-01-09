import { existsSync, readFileSync } from "node:fs";
import type { Agent as HttpsAgent } from "node:https";
import { dirname, resolve } from "node:path";
import { Readable } from "node:stream";
import { fileURLToPath } from "node:url";
import {
    DeleteObjectCommand,
    GetObjectCommand,
    HeadObjectCommand,
    ListObjectsV2Command,
    type ListObjectsV2CommandInput,
    PutObjectCommand,
    S3Client,
    type S3ClientConfig,
} from "@aws-sdk/client-s3";
import { config as loadEnv } from "dotenv";

const envPath = resolve(dirname(fileURLToPath(import.meta.url)), ".env");

if (existsSync(envPath)) {
    loadEnv({ path: envPath });
} else {
    loadEnv();
}

type HttpsAgentWithMutableOptions = HttpsAgent & {
    options: {
        ca?: string | Buffer | Array<string | Buffer>;
        rejectUnauthorized?: boolean;
        [key: string]: unknown;
    };
};

type NodeHttpHandlerInternals = {
    config?: {
        httpsAgent?: HttpsAgentWithMutableOptions;
    };
    configProvider?: Promise<{
        httpsAgent?: HttpsAgentWithMutableOptions;
    }>;
};

const TLS_CA_ENV = "S3_CA_CERT";
const TLS_INSECURE_ENV = "S3_ALLOW_INSECURE_TLS";

const resolveHttpsAgent = async (
    client: S3Client,
): Promise<HttpsAgentWithMutableOptions | undefined> => {
    const handler = client.config.requestHandler as NodeHttpHandlerInternals;
    if (!handler) {
        return undefined;
    }

    if (handler.config?.httpsAgent) {
        return handler.config.httpsAgent;
    }

    if (!handler.configProvider) {
        return undefined;
    }

    const resolvedConfig = await handler.configProvider;
    handler.config = resolvedConfig;
    return resolvedConfig?.httpsAgent;
};

const configureClientTls = async (client: S3Client): Promise<void> => {
    const caFile = process.env[TLS_CA_ENV];
    const allowInsecure =
        process.env[TLS_INSECURE_ENV] === "true" ||
        !process.env[TLS_INSECURE_ENV];

    if (!caFile && !allowInsecure) {
        return;
    }

    const httpsAgent = await resolveHttpsAgent(client);
    if (!httpsAgent) {
        throw new Error("S3 클라이언트의 HTTPS 에이전트를 불러오지 못했습니다.");
    }

    if (caFile) {
        if (!existsSync(caFile)) {
            throw new Error(
                `환경변수 ${TLS_CA_ENV} 로 전달한 CA 파일을 찾을 수 없습니다: ${caFile}`,
            );
        }

        httpsAgent.options.ca = readFileSync(caFile);
        httpsAgent.options.rejectUnauthorized = true;
    } else if (allowInsecure) {
        httpsAgent.options.rejectUnauthorized = false;
        console.warn(
            `${TLS_INSECURE_ENV}=true 설정으로 TLS 인증서 검증을 비활성화했습니다. 테스트 용도로만 사용하세요.`,
        );
    }

    httpsAgent.destroy();
};

const streamToString = async (stream: Readable | Blob): Promise<string> => {
    if (stream instanceof Readable) {
        const chunks: Buffer[] = [];
        for await (const chunk of stream) {
            chunks.push(
                Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk as string),
            );
        }
        return Buffer.concat(chunks).toString("utf-8");
    }

    if (typeof (stream as Blob).text === "function") {
        return (stream as Blob).text();
    }

    return "";
};

const {
    S3_ENDPOINT = "https://jamie.object-storage.gov-gabiacloud.com",
    S3_REGION = "ap-northeast-2",
    S3_ACCESS_KEY_ID,
    S3_SECRET_ACCESS_KEY,
    S3_BUCKET = "jamie",
    S3_PREFIX = "pg-cluster/",
} = process.env;

const endpointHost = new URL(S3_ENDPOINT).host;
const bucketEndpoint =
    endpointHost === S3_BUCKET ||
    endpointHost.startsWith(`${S3_BUCKET}.`);

const clientConfig: S3ClientConfig = {
    endpoint: S3_ENDPOINT,
    region: S3_REGION,
    requestChecksumCalculation: "WHEN_REQUIRED",
    ...(bucketEndpoint ? { bucketEndpoint: true } : {}),
};

if (S3_ACCESS_KEY_ID && S3_SECRET_ACCESS_KEY) {
    clientConfig.credentials = {
        accessKeyId: S3_ACCESS_KEY_ID,
        secretAccessKey: S3_SECRET_ACCESS_KEY,
    };
}

const client = new S3Client(clientConfig);
await configureClientTls(client);

const bucketForRequest = bucketEndpoint ? S3_ENDPOINT : S3_BUCKET;
const prefix = S3_PREFIX.endsWith("/") ? S3_PREFIX : `${S3_PREFIX}/`;
const key = `${prefix}codex-test-${Date.now()}.txt`;
const payload = `s3 prefix test ${new Date().toISOString()}`;

await client.send(
    new PutObjectCommand({
        Bucket: bucketForRequest,
        Key: key,
        Body: payload,
        ContentType: "text/plain",
    }),
);

await client.send(
    new HeadObjectCommand({
        Bucket: bucketForRequest,
        Key: key,
    }),
);

const getResult = await client.send(
    new GetObjectCommand({
        Bucket: bucketForRequest,
        Key: key,
    }),
);

const bodyText = getResult.Body
    ? await streamToString(getResult.Body as Readable | Blob)
    : "";

const listParams: ListObjectsV2CommandInput = {
    Bucket: bucketForRequest,
    Prefix: prefix,
};

const listResult = await client.send(new ListObjectsV2Command(listParams));
const listHasKey =
    listResult.Contents?.some((item) => item.Key === key) ?? false;

await client.send(
    new DeleteObjectCommand({
        Bucket: bucketForRequest,
        Key: key,
    }),
);

console.log(
    JSON.stringify(
        {
            bucket: S3_BUCKET,
            prefix,
            key,
            put: "ok",
            head: "ok",
            get: bodyText === payload ? "ok" : "mismatch",
            list: listHasKey ? "ok" : "missing",
            delete: "ok",
        },
        null,
        2,
    ),
);
