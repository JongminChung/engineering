#!/usr/bin/env python3
"""
Idempotent OpenClaw bootstrap for CentOS/Rocky 9 + Podman Compose.

This script automates the runbook in docs/openclaw/DEPLOYMENT_RUNBOOK.md.

Mattermost installation is out of scope.
"""

from __future__ import annotations

import argparse
import re
import shlex
import subprocess
import sys
from pathlib import Path

CMD_SEQ = 0


def step(title: str) -> None:
    print(f"\n=== {title} ===")


def run(cmd: str, check: bool = True, capture: bool = False) -> subprocess.CompletedProcess[str]:
    global CMD_SEQ
    CMD_SEQ += 1
    print(f"[cmd {CMD_SEQ:02d}] $ {cmd}")
    return subprocess.run(
        cmd,
        shell=True,
        check=check,
        text=True,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.STDOUT if capture else None,
    )


def command_exists(name: str) -> bool:
    return subprocess.run(
        f"command -v {shlex.quote(name)} >/dev/null 2>&1", shell=True
    ).returncode == 0


def ensure_commands() -> None:
    required = ["podman", "dnf", "git", "openssl", "jq", "ss"]
    missing = [cmd for cmd in required if not command_exists(cmd)]
    if missing:
        raise RuntimeError(f"Missing required commands: {', '.join(missing)}")


def sync_repo(repo_dir: Path) -> None:
    if (repo_dir / ".git").exists():
        run(f"cd {shlex.quote(str(repo_dir))} && git fetch --all --prune")
        run(f"cd {shlex.quote(str(repo_dir))} && git pull --ff-only")
    else:
        repo_dir.parent.mkdir(parents=True, exist_ok=True)
        run(
            "git clone https://github.com/openclaw/openclaw.git "
            + shlex.quote(str(repo_dir))
        )
    run(f"cd {shlex.quote(str(repo_dir))} && git rev-parse --short HEAD")


def read_token_from_env(env_path: Path) -> str | None:
    if not env_path.exists():
        return None
    token_re = re.compile(r"^OPENCLAW_GATEWAY_TOKEN=(.+)$")
    for line in env_path.read_text(encoding="utf-8").splitlines():
        m = token_re.match(line.strip())
        if m:
            return m.group(1)
    return None


def generate_token() -> str:
    out = run("openssl rand -hex 32", capture=True)
    return out.stdout.strip()


def write_env(env_path: Path, state_dir: Path, token: str, image: str) -> None:
    workspace_dir = state_dir / "workspace"
    content = "\n".join(
        [
            f"OPENCLAW_CONFIG_DIR={state_dir}",
            f"OPENCLAW_WORKSPACE_DIR={workspace_dir}",
            "OPENCLAW_GATEWAY_PORT=127.0.0.1:18789",
            "OPENCLAW_BRIDGE_PORT=127.0.0.1:18790",
            "OPENCLAW_GATEWAY_BIND=loopback",
            f"OPENCLAW_GATEWAY_TOKEN={token}",
            f"OPENCLAW_IMAGE={image}",
            "",
        ]
    )
    env_path.write_text(content, encoding="utf-8")


def ensure_gateway_mode_local(config_path: Path) -> None:
    if not config_path.exists():
        raise RuntimeError(f"Missing OpenClaw config: {config_path}")
    cmd = f"jq -e '.gateway.mode == \"local\"' {shlex.quote(str(config_path))} >/dev/null"
    run(cmd)


def step_prepare_compose() -> None:
    step("Prepare pure podman compose mode")
    run("dnf -y install podman-compose")
    run("dnf -y remove podman-docker || true")
    run("rm -f /usr/local/bin/podman-compose")
    run("mkdir -p /etc/containers/containers.conf.d")
    run(
        "cat > /etc/containers/containers.conf.d/99-compose-provider.conf <<'EOF'\n"
        "[engine]\n"
        "compose_providers = [\"/usr/bin/podman-compose\"]\n"
        "EOF"
    )
    run(
        "if grep -q '^export PODMAN_COMPOSE_PROVIDER=' /etc/profile; then "
        "sed -i 's|^export PODMAN_COMPOSE_PROVIDER=.*$|export PODMAN_COMPOSE_PROVIDER=/usr/bin/podman-compose|' /etc/profile; "
        "else echo 'export PODMAN_COMPOSE_PROVIDER=/usr/bin/podman-compose' >> /etc/profile; fi"
    )
    run("podman --version")
    run("podman compose version")
    run("command -v podman-compose")


def step_sync_repository(repo_dir: Path) -> None:
    step("Sync repository")
    sync_repo(repo_dir)


def step_configure_state_env(repo_dir: Path, state_dir: Path, image: str) -> None:
    step("Configure state and .env")
    workspace_dir = state_dir / "workspace"
    env_path = repo_dir / ".env"
    workspace_dir.mkdir(parents=True, exist_ok=True)
    token = read_token_from_env(env_path) or generate_token()
    write_env(env_path, state_dir, token, image)


def step_build_start_gateway(repo_dir: Path, image: str, skip_build: bool) -> None:
    step("Build image and start gateway")
    if not skip_build:
        run(
            f"cd {shlex.quote(str(repo_dir))} && "
            f"podman build -t {shlex.quote(image)} -f Dockerfile ."
        )
    run(f"cd {shlex.quote(str(repo_dir))} && podman compose up -d openclaw-gateway")


def step_onboard_fix(repo_dir: Path, config_path: Path, skip_onboard_fix: bool) -> None:
    step("Apply non-interactive onboarding fix")
    if not skip_onboard_fix:
        onboard_cmd = (
            f"cd {shlex.quote(str(repo_dir))} && "
            "source .env && "
            "podman compose run --rm openclaw-cli onboard "
            "--accept-risk "
            "--non-interactive "
            "--flow quickstart "
            "--gateway-bind loopback "
            "--gateway-auth token "
            '"--gateway-token $OPENCLAW_GATEWAY_TOKEN" '
            "--tailscale off "
            "--skip-channels "
            "--skip-skills "
            "--skip-ui "
            "--no-install-daemon "
            "--auth-choice skip || true"
        )
        run(onboard_cmd)
    ensure_gateway_mode_local(config_path)


def step_normalize_plugin(repo_dir: Path, state_dir: Path) -> None:
    step("Normalize mattermost plugin source")
    run(f"rm -rf {shlex.quote(str(state_dir / 'extensions' / 'mattermost'))}")
    run(
        f"cd {shlex.quote(str(repo_dir))} && "
        "podman compose exec openclaw-gateway test -f /app/extensions/mattermost/index.ts"
    )


def step_verify_runtime(repo_dir: Path) -> None:
    step("Verify runtime status")
    run(f"cd {shlex.quote(str(repo_dir))} && podman compose ps")
    run(f"cd {shlex.quote(str(repo_dir))} && podman compose logs --tail=80 openclaw-gateway")
    run(f"cd {shlex.quote(str(repo_dir))} && podman compose run --rm openclaw-cli dashboard --no-open")
    run(f"cd {shlex.quote(str(repo_dir))} && podman compose run --rm openclaw-cli models status || true")
    run("ss -lntp | egrep '18789|18790'")


def main() -> int:
    parser = argparse.ArgumentParser(description="Bootstrap OpenClaw idempotently")
    parser.add_argument("--repo-dir", default="/root/openclaw", help="OpenClaw repo path")
    parser.add_argument(
        "--state-dir", default="/root/.openclaw", help="OpenClaw state directory"
    )
    parser.add_argument(
        "--image", default="openclaw:local", help="Container image tag to build/use"
    )
    parser.add_argument(
        "--skip-build", action="store_true", help="Skip podman build step"
    )
    parser.add_argument(
        "--skip-onboard-fix",
        action="store_true",
        help="Skip non-interactive onboard fix for gateway.mode=local",
    )
    parser.add_argument(
        "--only-step",
        choices=[
            "compose",
            "repo",
            "env",
            "build",
            "onboard-fix",
            "plugin",
            "verify",
            "all",
        ],
        default="all",
        help="Run only one step (default: all)",
    )
    args = parser.parse_args()

    repo_dir = Path(args.repo_dir)
    state_dir = Path(args.state_dir)
    config_path = state_dir / "openclaw.json"

    try:
        step("Preflight")
        ensure_commands()

        if args.only_step in ("compose", "all"):
            step_prepare_compose()
        if args.only_step in ("repo", "all"):
            step_sync_repository(repo_dir)
        if args.only_step in ("env", "all"):
            step_configure_state_env(repo_dir, state_dir, args.image)
        if args.only_step in ("build", "all"):
            step_build_start_gateway(repo_dir, args.image, args.skip_build)
        if args.only_step in ("onboard-fix", "all"):
            step_onboard_fix(repo_dir, config_path, args.skip_onboard_fix)
        if args.only_step in ("plugin", "all"):
            step_normalize_plugin(repo_dir, state_dir)
        if args.only_step in ("verify", "all"):
            step_verify_runtime(repo_dir)

        step("Next manual step")
        print("[OAuth]")
        print(f"cd {repo_dir} && podman compose run --rm openclaw-cli onboard --no-install-daemon")
        print("Choose auth: openai-codex, then finish OAuth in browser.")

        print("\n[Local access]")
        print("ssh -N -L 18789:127.0.0.1:18789 openclaw")
        print("Open: http://localhost:18789/")

        return 0
    except Exception as exc:  # pylint: disable=broad-except
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
