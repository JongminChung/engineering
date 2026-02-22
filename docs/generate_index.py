from pathlib import Path


ROOT = Path(__file__).resolve().parent
README = ROOT / "README.md"


def _is_markdown(path: Path) -> bool:
    return path.is_file() and path.suffix.lower() == ".md"


def _list_dirs() -> list[Path]:
    return sorted(
        [
            path
            for path in ROOT.iterdir()
            if path.is_dir() and not path.name.startswith(".")
        ],
        key=lambda path: path.name.casefold(),
    )


def _list_root_markdown() -> list[Path]:
    excluded = {"README.md", "AGENTS.md"}
    files = [
        path
        for path in ROOT.iterdir()
        if _is_markdown(path) and path.name not in excluded
    ]
    return sorted(files, key=lambda path: path.name.casefold())


def _list_markdown_recursive(directory: Path) -> list[Path]:
    files = [
        path
        for path in directory.rglob("*.md")
        if path.is_file() and path.name != "AGENTS.md"
    ]
    return sorted(files, key=lambda path: path.relative_to(ROOT).as_posix().casefold())


def _render() -> str:
    lines: list[str] = []
    lines.append("# 문서 인덱스")
    lines.append("")
    lines.append(
        "docs/의 문서를 빠르게 찾기 위한 인덱스입니다. 폴더는 주제별 모음, 루트는 공통 문서입니다."
    )
    lines.append("")

    directories = _list_dirs()
    if not directories:
        lines.append("## 폴더")
        lines.append("- (없음)")
        lines.append("")
    else:
        for directory in directories:
            lines.append(f"## {directory.name}")
            docs = _list_markdown_recursive(directory)
            if not docs:
                lines.append("- (없음)")
            else:
                for doc in docs:
                    relative_path = doc.relative_to(ROOT).as_posix()
                    lines.append(f"- [{relative_path}]({relative_path})")
            lines.append("")

    lines.append("## 루트 문서")
    root_docs = _list_root_markdown()
    if not root_docs:
        lines.append("- (없음)")
    else:
        for doc in root_docs:
            lines.append(f"- [{doc.name}]({doc.name})")
    lines.append("")

    lines.append("## 메타")
    agents = ROOT / "AGENTS.md"
    if agents.exists():
        lines.append("- [AGENTS.md](AGENTS.md)")
    else:
        lines.append("- (없음)")
    lines.append("")

    lines.append("## 갱신 방법")
    lines.append("```bash")
    lines.append("uv run python docs/generate_index.py")
    lines.append("```")
    lines.append("")

    return "\n".join(lines)


def main() -> None:
    print("문서 인덱스 생성 시작")
    output = _render()
    README.write_text(output, encoding="utf-8")
    print(f"문서 인덱스 생성 완료: {README}")


if __name__ == "__main__":
    main()
