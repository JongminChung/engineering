# OpenAPI Generator TS Dual Release

openapi-generator + tsdown로 dual 배포

```text
src/
  gen/
    index.ts // generator가 만들어줌
    api.ts
    configuration.ts
    ...
  index.ts # custom 공개 엔트리
```

**src/index.ts**

```ts
/**
 * 코드에서 이름이 겹칠 수 있음. (대표적으로 Configurationm, AxiosPromise, RequestArgs, 모델명 충돌 등)
 * export *는 “전부 위로 끌어올리기”라서 충돌 나면 빌드 단계에서 깨지거나, 타입만 깨지는 애매한 상태로 갈 수 있음
 * tree-shaking은 “정적 named export”면 잘 되는데, 충돌 회피하려고 namespace/default 객체로 묶기 시작하면 손해 커짐
 *
 * export * from './gen'
 */

// 1) configuration 쪽은 이름 충돌 위험이 커서 alias로 고정하는 편이 안전함
export {Configuration as OpenAPIConfiguration} from './gen/configuration'

// 2) API 클래스/팩토리만 명시적으로 re-export
export {
    // 예시: 생성되는 API 클래스명들로 교체
    PetApi,
    StoreApi,
    UserApi,
} from './gen/api'

// 3) (선택) base에서 공개해도 되는 타입/헬퍼만 선별 export (필요할때만)
export type {
    // 예시: 실제 생성물에 맞게
    AxiosPromise,
    RequestArgs,
} from './gen/base'
```

## tsdown 설정

```ts
// tsdown.config.ts
import {defineConfig} from 'tsdown'

export default defineConfig({
    entry: 'src/index.ts',

    // tsdown이 exports/main/module/types를 산출물 기반으로 맞춰줌
    exports: true,

    // 타입 생성 (tsdown은 dts 생성 지원함)
    dts: true,

    // 실무에서 자주 같이 켜는 것들
    clean: true,
    sourcemap: true,
})
```

**src/models.ts(모델만 모으는 엔트리)**

```ts
// src/models.ts
export * from './gen/model' // 생성 옵션에 따라 경로 다를 수 있음 (model, models 등)
```

## package.json

```json
{
    "name": "@your-scope/your-sdk",
    "version": "0.1.0",
    "private": false,
    "type": "module",
    "files": [
        "dist"
    ],
    "sideEffects": false,
    "scripts": {
        "gen": "openapi-generator-cli generate -g typescript-axios -i openapi.yaml -o src/gen",
        "build": "tsdown",
        "prepublishOnly": "npm run gen && npm run build"
    },
    "devDependencies": {
        "tsdown": "^0.13.4",
        "typescript": "^5.0.0",
        "openapi-generator-cli": "^2.0.0"
    }
}
```

- `type: "module"`로 패키지 기본을 ESM으로 두고
- `tsdown exports: true`로 `import/require` 분기 포함한 exports를 자동 생성하는 흐름임
- Package Exports 공식
  문서: https://tsdown.dev/options/package-exports?utm_source=chatgpt.com#enabling-auto-exports

### exports를 수동으로 고정하고 싶을 때

자동 생성 싫으면 수동으로 “import/require”를 갈라서 적으면 됨
(아래는 dist 산출물이 index.mjs, index.cjs, index.d.mts, index.d.cts로 나온다는 가정임 — 실제
파일명은 네 tsdown 산출물에 맞춰 바꿔야 함)

```json
{
    "type": "module",
    "main": "./dist/index.cjs",
    "module": "./dist/index.mjs",
    "types": "./dist/index.d.mts",
    "exports": {
        ".": {
            "types": {
                "import": "./dist/index.d.mts",
                "require": "./dist/index.d.cts"
            },
            "import": "./dist/index.mjs",
            "require": "./dist/index.cjs"
        },
        "./models": { "...": "..." }
    }
}
```

수동 관리의 문제는 “산출물 파일명/구조 바뀌면 exports 깨짐”이라서, 그래서 tsdown이 exports 자동 갱신 기능을 제공하는
쪽으로 가는 흐름임.


## CI Pack verify

```json
{
  "scripts": {
    "build": "tsdown",
    "pack:verify": "node ./scripts/pack-verify.mjs"
  }
}
```

```js
// scripts/pack-verify.mjs
import { execSync } from "node:child_process";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

function sh(cmd, cwd) {
    execSync(cmd, { stdio: "inherit", cwd });
}

const repoRoot = process.cwd();

// 1) build
sh("npm run build", repoRoot);

// 2) pack
const out = execSync("npm pack", { cwd: repoRoot, encoding: "utf8" }).trim();
const tgz = join(repoRoot, out);

// 3) temp consumer project
const dir = mkdtempSync(join(tmpdir(), "pkg-verify-"));
sh("npm init -y", dir);

// typescript consumer setup
sh("npm i -D typescript", dir);
sh(`npm i "${tgz}"`, dir);

// 4) ESM test
writeFileSync(
    join(dir, "esm.mjs"),
    `
import { PetApi } from "${process.env.PKG_NAME || "@package-scope/package-sdk"}";
console.log("esm ok", typeof PetApi);
`.trim()
);

// 5) CJS test
writeFileSync(
    join(dir, "cjs.cjs"),
    `
const sdk = require("${process.env.PKG_NAME || "@package-scope/package-sdk"}");
console.log("cjs ok", typeof sdk.PetApi);
`.trim()
);

// 6) TS typecheck test
writeFileSync(
    join(dir, "tsconfig.json"),
    JSON.stringify(
        {
            compilerOptions: {
                target: "ES2022",
                module: "NodeNext",
                moduleResolution: "NodeNext",
                strict: true,
                noEmit: true
            }
        },
        null,
        2
    )
);

writeFileSync(
    join(dir, "typecheck.ts"),
    `
import type { Pet } from "${process.env.PKG_NAME || "@package-scope/package-sdk/models"}";
const x: Pet | undefined = undefined;
console.log(x);
`.trim()
);

sh("node esm.mjs", dir);
sh("node cjs.cjs", dir);
sh("npx tsc -p tsconfig.json", dir);
```
