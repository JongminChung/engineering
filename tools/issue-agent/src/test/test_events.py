from __future__ import annotations

import unittest

from events import parse_gitlab_note_payload


class EventsTest(unittest.TestCase):
    def test_parse_issue_note_payload(self) -> None:
        payload = {
            "object_kind": "note",
            "object_attributes": {
                "id": 11,
                "note": "@agent fix",
                "noteable_type": "Issue",
            },
            "project": {"id": 1},
            "user": {"username": "alice"},
            "issue": {"iid": 100},
        }

        parsed = parse_gitlab_note_payload(payload)
        self.assertIsNotNone(parsed)
        assert parsed is not None
        self.assertEqual(parsed.scope, "issue")
        self.assertEqual(parsed.issue_id, "100")

    def test_parse_mr_note_payload(self) -> None:
        payload = {
            "object_kind": "note",
            "object_attributes": {
                "id": 12,
                "note": "@agent proceed",
                "noteable_type": "MergeRequest",
            },
            "project": {"id": 1},
            "user": {"username": "alice"},
            "merge_request": {"iid": 22},
        }

        parsed = parse_gitlab_note_payload(payload)
        self.assertIsNotNone(parsed)
        assert parsed is not None
        self.assertEqual(parsed.scope, "mr")
        self.assertEqual(parsed.mr_id, "22")


if __name__ == "__main__":
    unittest.main()
