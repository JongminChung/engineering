from __future__ import annotations

import re

from .models import ParsedCommand

_COMMAND_RE = re.compile(r"^@agent\s+(fix|feat|proceed|stop)\b", re.IGNORECASE)
_DIFFERENTLY_RE = re.compile(r"^@agent\s+differently\s*:\s*(.+)$", re.IGNORECASE | re.DOTALL)


def parse_command(note_body: str) -> ParsedCommand | None:
    body = note_body.strip()

    differently_match = _DIFFERENTLY_RE.match(body)
    if differently_match:
        instruction = differently_match.group(1).strip()

        if not instruction:
            return None

        return ParsedCommand(command="differently", instruction=instruction)

    match = _COMMAND_RE.match(body)
    if not match:
        return None

    command = match.group(1).lower()
    return ParsedCommand(command=command)
