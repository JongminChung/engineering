from __future__ import annotations

import unittest

from parser import parse_command


class ParserTest(unittest.TestCase):
    def test_parse_fix(self) -> None:
        parsed = parse_command("@agent fix")
        self.assertIsNotNone(parsed)
        assert parsed is not None
        self.assertEqual(parsed.command, "fix")

    def test_parse_differently(self) -> None:
        parsed = parse_command("@agent differently: retry with narrower scope")
        self.assertIsNotNone(parsed)
        assert parsed is not None
        self.assertEqual(parsed.command, "differently")
        self.assertEqual(parsed.instruction, "retry with narrower scope")

    def test_ignore_invalid(self) -> None:
        self.assertIsNone(parse_command("please run"))


if __name__ == "__main__":
    unittest.main()
