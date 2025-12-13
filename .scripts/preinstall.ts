checkPnpm();

function fail(message: string) {
    console.error(message);
    process.exit(1);
}

function checkPnpm() {
    const execPath = process.env.npm_execpath ?? "";
    const userAgent = process.env.npm_config_user_agent ?? "";

    const isPnpm = execPath.includes("pnpm") || userAgent.startsWith("pnpm/");

    if (!isPnpm) {
        fail(`
❌ This project must be installed using pnpm.

Detected:
  npm_execpath = ${execPath || "(empty)"}
  user_agent   = ${userAgent || "(empty)"}

Fix:
  pnpm install
`);
    }
}
