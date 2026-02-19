from __future__ import annotations

import unittest

from gitlab_api import parse_gitlab_project_path


class GitLabApiTest(unittest.TestCase):
    def test_parse_https_remote(self) -> None:
        ref = parse_gitlab_project_path("https://gitlab.com/jongminchung/engineering.git")
        self.assertEqual(ref.project_path, "jongminchung/engineering")

    def test_parse_ssh_remote(self) -> None:
        ref = parse_gitlab_project_path("git@gitlab.com:jongminchung/engineering.git")
        self.assertEqual(ref.project_path, "jongminchung/engineering")


if __name__ == "__main__":
    unittest.main()
