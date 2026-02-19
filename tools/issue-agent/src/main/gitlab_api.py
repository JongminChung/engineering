from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from urllib import parse


class GitLabApiError(RuntimeError):
    pass


@dataclass(slots=True)
class GitLabProjectRef:
    project_path: str

    @property
    def encoded(self) -> str:
        return parse.quote_plus(self.project_path)


def parse_gitlab_project_path(remote_url: str) -> GitLabProjectRef:
    url = remote_url.strip()
    if url.endswith(".git"):
        url = url[:-4]

    if url.startswith("http://") or url.startswith("https://"):
        parsed = parse.urlparse(url)
        path = parsed.path.strip("/")
        if not path:
            raise GitLabApiError("Cannot parse project path from remote URL")
        return GitLabProjectRef(project_path=path)

    if ":" in url and "@" in url:
        right = url.split(":", maxsplit=1)[1]
        return GitLabProjectRef(project_path=right.strip("/"))

    raise GitLabApiError("Unsupported remote URL format")


def discover_origin2_remote(repo_root: Path) -> str:
    import subprocess

    result = subprocess.run(
        ["git", "config", "--get", "remote.origin2.url"],
        cwd=repo_root,
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise GitLabApiError("remote.origin2.url not found")

    remote = result.stdout.strip()
    if not remote:
        raise GitLabApiError("remote.origin2.url is empty")
    return remote


def discover_repo_root(cwd: Path) -> Path:
    import subprocess

    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        cwd=cwd,
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        return cwd
    resolved = result.stdout.strip()
    if not resolved:
        return cwd
    return Path(resolved)
