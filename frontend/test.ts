import { existsSync, readFileSync } from "node:fs";
import type { Agent as HttpsAgent } from "node:https";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
    ListBucketsCommand,
    type ListBucketsCommandInput,
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
    // const allowInsecure = process.env[TLS_INSECURE_ENV] === "true";
    const allowInsecure = true;

    if (!caFile && !allowInsecure) {
        return;
    }

    const httpsAgent = await resolveHttpsAgent(client);
    if (!httpsAgent) {
        throw new Error(
            "S3 클라이언트의 HTTPS 에이전트를 불러오지 못했습니다.",
        );
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

const {
    S3_ENDPOINT = "https://jamie.object-storage.gov-gabiacloud.com",
    S3_REGION = "ap-northeast-2",
    S3_ACCESS_KEY_ID,
    S3_SECRET_ACCESS_KEY,
    S3_BUCKET = "jamie",
} = process.env;

console.log(S3_ACCESS_KEY_ID);
console.log(S3_SECRET_ACCESS_KEY);

const endpointHost = new URL(S3_ENDPOINT).host;
const bucketEndpoint =
    endpointHost === S3_BUCKET || endpointHost.startsWith(`${S3_BUCKET}.`);

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

const params: ListBucketsCommandInput = {
    /** input parameters */
};

const command = new ListBucketsCommand(params);

const result = await client.send(command);

console.log(JSON.stringify(result));
