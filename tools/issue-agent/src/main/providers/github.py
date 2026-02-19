from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass(slots=True)
class GitHubAdapterSkeleton:
    root: Path

    def repo_root(self) -> Path:
        return self.root
