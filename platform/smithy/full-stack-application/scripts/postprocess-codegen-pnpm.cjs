#!/usr/bin/env node

const fs = require("node:fs");
const path = require("node:path");

const root = process.cwd();
const targets = [
    "smithy/build/smithy/source/typescript-ssdk-codegen/package.json",
    "smithy/build/smithy/source/typescript-client-codegen/package.json",
];

const replacements = {
    build: "concurrently 'pnpm run build:cjs' 'pnpm run build:es' 'pnpm run build:types'",
    prepack: "pnpm run clean && pnpm run build",
};

let updated = 0;

for (const relPath of targets) {
    const filePath = path.join(root, relPath);
    if (!fs.existsSync(filePath)) {
        continue;
    }

    const raw = fs.readFileSync(filePath, "utf8");
    const json = JSON.parse(raw);
    json.scripts = json.scripts || {};

    let changed = false;
    for (const [key, value] of Object.entries(replacements)) {
        if (json.scripts[key] !== value) {
            json.scripts[key] = value;
            changed = true;
        }
    }

    if (changed) {
        fs.writeFileSync(
            filePath,
            `${JSON.stringify(json, null, 2)}\n`,
            "utf8",
        );
        updated += 1;
    }
}

if (updated === 0) {
    console.log("postprocess-codegen-pnpm: no changes needed.");
} else {
    console.log(
        `postprocess-codegen-pnpm: updated ${updated} package.json file(s).`,
    );
}
