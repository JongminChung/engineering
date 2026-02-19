from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from skills import load_agent_skills


class SkillsLoaderTest(unittest.TestCase):
    def test_loads_agents_skills_first(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            agents_dev = root / ".agents" / "skills" / "dev"
            codex_dev = root / ".codex" / "skills" / "dev"
            agents_dev.mkdir(parents=True)
            codex_dev.mkdir(parents=True)
            (agents_dev / "SKILL.md").write_text("agents", encoding="utf-8")
            (codex_dev / "SKILL.md").write_text("codex", encoding="utf-8")

            loaded = load_agent_skills(root, "dev")
            self.assertEqual(len(loaded.files), 1)
            self.assertTrue(str(loaded.files[0]).startswith(str(agents_dev)))


if __name__ == "__main__":
    unittest.main()
