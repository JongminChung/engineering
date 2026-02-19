from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from models import NoteContext
from providers.local_fs import LocalFsAdapter
from workflow import WorkflowEngine


class LocalFsWorkflowTest(unittest.TestCase):
    def test_plan_and_proceed_write_issue_files(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            adapter = LocalFsAdapter(root=root)
            engine = WorkflowEngine(adapter=adapter)
            states = {}

            plan_result = engine.handle_note(
                NoteContext(
                    provider="gitlab",
                    scope="issue",
                    project_id="local",
                    actor="tester",
                    note_id="n1",
                    issue_id="10",
                    body="@agent fix",
                ),
                states,
            )
            self.assertIn("ok:", plan_result)
            self.assertTrue((root / "issues/10/PLAN.md").exists())
            self.assertTrue((root / "issues/10/PROGRESS.md").exists())
            self.assertTrue((root / "issues/10/process.md").exists())

            run_result = engine.handle_note(
                NoteContext(
                    provider="gitlab",
                    scope="mr",
                    project_id="local",
                    actor="tester",
                    note_id="n2",
                    mr_id="10",
                    body="@agent proceed",
                ),
                states,
            )
            self.assertEqual(run_result, "ok: workflow completed")
            progress = (root / "issues/10/PROGRESS.md").read_text(encoding="utf-8")
            self.assertIn("- completed", progress)


if __name__ == "__main__":
    unittest.main()
