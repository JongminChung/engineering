from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

AGENT_ORDER = ("dev", "review", "qa", "docs")


@dataclass(slots=True)
class SkillSet:
    agent: str
    files: list[Path]


def _skill_roots(repo_root: Path) -> list[Path]:
    return [repo_root / ".agents" / "skills", repo_root / ".codex" / "skills"]


def load_agent_skills(repo_root: Path, agent: str) -> SkillSet:
    files: list[Path] = []
    for root in _skill_roots(repo_root):
        agent_dir = root / agent
        if agent_dir.exists() and agent_dir.is_dir():
            primary = sorted(agent_dir.rglob("SKILL.md"))
            others = sorted(path for path in agent_dir.glob("*.md") if path.name != "SKILL.md")
            files.extend(primary)
            files.extend(others)
            if files:
                break
    return SkillSet(agent=agent, files=files)
