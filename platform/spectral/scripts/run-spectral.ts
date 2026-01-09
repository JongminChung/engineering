import { readFile } from "node:fs/promises";
import * as path from "node:path";
import spectralCore, {
    type ISpectralDiagnostic,
} from "@stoplight/spectral-core";
import spectralParsers from "@stoplight/spectral-parsers";
import { httpAndFileResolver } from "@stoplight/spectral-ref-resolver";
import { ruleset } from "../ruleset.ts";

const inputPath = process.argv[2] ?? "./petstore.yaml";
const absolutePath = path.resolve(process.cwd(), inputPath);

const { Document, Spectral } =
    spectralCore as typeof import("@stoplight/spectral-core");
const { Yaml } =
    spectralParsers as typeof import("@stoplight/spectral-parsers");

const spectral = new Spectral({ resolver: httpAndFileResolver });

spectral.setRuleset(ruleset);

const source = await readFile(absolutePath, "utf8");
const document = new Document(source, Yaml, absolutePath);

const results = await spectral.run(document);

if (results.length === 0) {
    console.log("No issues found.");
    process.exit(0);
}

const severityLabel = (severity: number) => {
    switch (severity) {
        case 0:
            return "error";
        case 1:
            return "warn";
        case 2:
            return "info";
        case 3:
            return "hint";
        default:
            return "unknown";
    }
};

for (const result of results as ISpectralDiagnostic[]) {
    const location = result.path?.length ? result.path.join(".") : "(root)";
    console.log(
        `[${severityLabel(result.severity)}] ${result.code}: ${result.message}`,
    );
    console.log(`  at: ${result.source}#/${location}`);
}

process.exit(results.some((result) => result.severity === 0) ? 1 : 0);
