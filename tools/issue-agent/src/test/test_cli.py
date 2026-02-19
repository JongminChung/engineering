from __future__ import annotations

import os
import subprocess
import unittest
from pathlib import Path


class CliTest(unittest.TestCase):

    def test_bootstrap_requires_token(self) -> None:
        module_root = Path(__file__).resolve().parents[2]
        env = dict(os.environ)
        env.pop("GITLAB_TOKEN", None)
        env.pop("GITLAB_PRIVATE_TOKEN", None)
        run = subprocess.run(
            [
                "uv",
                "run",
                "python",
                "-m",
                "issue_agent.cli",
                "bootstrap-origin2",
                "--title",
                "t",
                "--description",
                "d",
                "--mode",
                "fix",
                "--no-dotenv",
            ],
            cwd=str(module_root),
            env=env,
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertNotEqual(run.returncode, 0)
        self.assertIn("GITLAB_TOKEN", run.stderr)


if __name__ == "__main__":
    unittest.main()
