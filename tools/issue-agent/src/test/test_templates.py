from __future__ import annotations

import unittest

from templates import PlanContext, render_plan_markdown


class TemplatesTest(unittest.TestCase):
    def test_plan_contains_required_sections(self) -> None:
        markdown = render_plan_markdown(
            PlanContext(issue_id="10", mode="fix", branch="agent/issue-10"),
        )
        self.assertIn("## 배경", markdown)
        self.assertIn("## 목표", markdown)
        self.assertIn("## 명령 규칙", markdown)
        self.assertIn("## PLAN/PROGRESS 정책", markdown)
        self.assertIn("## 완료 조건 (Acceptance Criteria)", markdown)
        self.assertIn("## 비범위 (Out of Scope)", markdown)


if __name__ == "__main__":
    unittest.main()
