from __future__ import annotations

import unittest
from pathlib import Path

from models import NoteContext
from providers.gitlab import InMemoryGitLabAdapter
from workflow import WorkflowEngine


class WorkflowTest(unittest.TestCase):
    def setUp(self) -> None:
        self.adapter = InMemoryGitLabAdapter(root=Path.cwd())
        self.engine = WorkflowEngine(adapter=self.adapter)
        self.states = {}

    def test_issue_plan_only_creates_mr(self) -> None:
        note = NoteContext(
            provider="gitlab",
            scope="issue",
            project_id="p1",
            actor="alice",
            note_id="n1",
            issue_id="101",
            body="@agent fix",
        )

        result = self.engine.handle_note(note, self.states)

        self.assertIn("ok:", result)
        self.assertIn("101", self.states)
        state = self.states["101"]
        self.assertEqual(state.current_stage, "plan")
        self.assertEqual(state.branch, "agent/issue-101")
        first_commit = self.adapter.branches[state.branch][0]
        self.assertIn("issues/101/PLAN.md", first_commit["files"])
        self.assertIn("issues/101/PROGRESS.md", first_commit["files"])
        self.assertIn("issues/101/process.md", first_commit["files"])

    def test_issue_rejects_proceed(self) -> None:
        note = NoteContext(
            provider="gitlab",
            scope="issue",
            project_id="p1",
            actor="alice",
            note_id="n1",
            issue_id="101",
            body="@agent proceed",
        )
        result = self.engine.handle_note(note, self.states)
        self.assertIn("only @agent fix|feat", result)

    def test_mr_proceed_runs_to_completed(self) -> None:
        self.engine.handle_note(
            NoteContext(
                provider="gitlab",
                scope="issue",
                project_id="p1",
                actor="alice",
                note_id="n1",
                issue_id="102",
                body="@agent feat",
            ),
            self.states,
        )

        result = self.engine.handle_note(
            NoteContext(
                provider="gitlab",
                scope="mr",
                project_id="p1",
                actor="alice",
                note_id="n2",
                mr_id="102",
                body="@agent proceed",
            ),
            self.states,
        )

        self.assertEqual(result, "ok: workflow completed")
        self.assertEqual(self.states["102"].current_stage, "completed")

    def test_differently_updates_plan(self) -> None:
        self.engine.handle_note(
            NoteContext(
                provider="gitlab",
                scope="issue",
                project_id="p1",
                actor="alice",
                note_id="n1",
                issue_id="103",
                body="@agent feat",
            ),
            self.states,
        )

        result = self.engine.handle_note(
            NoteContext(
                provider="gitlab",
                scope="mr",
                project_id="p1",
                actor="alice",
                note_id="n2",
                mr_id="103",
                body="@agent differently: split rollout by module",
            ),
            self.states,
        )

        self.assertEqual(result, "ok: differently applied")
        state = self.states["103"]
        self.assertEqual(state.current_stage, "plan")
        self.assertIn("split rollout by module", " ".join(state.decision_log))

    def test_same_issue_reuses_same_mr(self) -> None:
        first = self.engine.handle_note(
            NoteContext(
                provider="gitlab",
                scope="issue",
                project_id="p1",
                actor="alice",
                note_id="n1",
                issue_id="200",
                body="@agent fix",
            ),
            self.states,
        )
        second = self.engine.handle_note(
            NoteContext(
                provider="gitlab",
                scope="issue",
                project_id="p1",
                actor="alice",
                note_id="n2",
                issue_id="200",
                body="@agent fix",
            ),
            self.states,
        )

        self.assertIn("draft mr 200", first)
        self.assertIn("draft mr 200", second)
        self.assertEqual(len(self.states), 1)


if __name__ == "__main__":
    unittest.main()
