"""생성기 공통 — 원본 로딩, frontmatter 파싱, 생성물 헤더.

의존성을 두지 않는다. PyYAML 없이 표준 라이브러리만 쓴다.
CI 러너마다 파이썬 패키지를 맞추는 비용이 이 파일이 파싱하는 문법의 단순함보다 크다.
그래서 frontmatter 는 `키: 값` 한 줄짜리만 지원한다 — 중첩이 필요해지면
그때 형식을 늘리는 대신 파일을 나눈다.
"""

import json
from dataclasses import dataclass, field
from pathlib import Path

FRONTMATTER_FENCE = "---"

# 생성물임을 알리는 헤더. 형식마다 주석 문법이 달라 세 벌이 필요하다.
HEADER_LINES = (
    "이 파일은 harness/ 에서 생성됩니다. 직접 고치지 마세요.",
    "고칠 곳: {source}",
    "다시 생성: python3 scripts/harness/generate.py",
)


def markdown_header(source: str) -> str:
    body = "\n".join("  " + line.format(source=source) for line in HEADER_LINES)
    return f"<!--\n{body}\n-->\n"


def toml_header(source: str) -> str:
    return "\n".join("# " + line.format(source=source) for line in HEADER_LINES) + "\n"


def json_header(source: str) -> list:
    return [line.format(source=source) for line in HEADER_LINES]


@dataclass
class Document:
    """frontmatter 가 붙은 마크다운 원본 하나."""

    source: str
    name: str
    body: str
    meta: dict = field(default_factory=dict)

    @property
    def description(self) -> str:
        return self.meta.get("description", "")

    def capabilities(self) -> list:
        raw = self.meta.get("capabilities", "")
        return [item.strip() for item in raw.split(",") if item.strip()]

    def tier(self) -> str:
        return self.meta.get("model", "")


def parse_frontmatter(text: str):
    """(meta, body) 를 돌려준다. frontmatter 가 없으면 meta 는 빈 dict."""
    lines = text.splitlines()
    if not lines or lines[0].strip() != FRONTMATTER_FENCE:
        return {}, text

    meta, end = {}, None
    for index in range(1, len(lines)):
        if lines[index].strip() == FRONTMATTER_FENCE:
            end = index
            break
        line = lines[index]
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if ":" not in line:
            raise ValueError(f"frontmatter 형식 오류 — '키: 값' 이 아닙니다: {line!r}")
        key, value = line.split(":", 1)
        meta[key.strip()] = value.strip()

    if end is None:
        raise ValueError("frontmatter 를 닫는 --- 가 없습니다")

    # splitlines() 가 끝 개행을 지우므로 되살린다. 안 그러면 생성 파일이
    # 개행 없이 끝나 diff 마다 "\ No newline at end of file" 이 붙는다.
    body = "\n".join(lines[end + 1 :]).lstrip("\n")
    if body and not body.endswith("\n"):
        body += "\n"
    return meta, body


def render_frontmatter(meta: dict) -> str:
    lines = [FRONTMATTER_FENCE]
    for key, value in meta.items():
        if not value:
            continue
        if isinstance(value, (list, tuple)):
            lines.append(f"{key}:")
            lines.extend(f"  - {item}" for item in value)
        else:
            lines.append(f"{key}: {value}")
    lines.append(FRONTMATTER_FENCE)
    return "\n".join(lines) + "\n"


def _load_documents(directory: Path, pattern: str, root: Path):
    documents = []
    if not directory.exists():
        return documents
    for path in sorted(directory.glob(pattern)):
        text = path.read_text(encoding="utf-8")
        source = path.relative_to(root).as_posix()
        try:
            meta, body = parse_frontmatter(text)
        except ValueError as error:
            raise ValueError(f"{source}: {error}") from error
        name = meta.get("name") or path.stem
        if name == "SKILL":
            name = path.parent.name
        documents.append(Document(source=source, name=name, body=body, meta=meta))
    return documents


@dataclass
class Bundle:
    """생성에 필요한 원본 전부."""

    skills: list
    agents: list
    rules: list
    policies: dict


def load_bundle(root: Path) -> Bundle:
    harness = root / "harness"
    policies_path = harness / "hooks" / "policies" / "permissions.json"
    policies = json.loads(policies_path.read_text(encoding="utf-8"))
    return Bundle(
        skills=_load_documents(harness / "skills", "*/SKILL.md", root),
        agents=_load_documents(harness / "agents", "*.md", root),
        rules=_load_documents(harness / "rules", "*.md", root),
        policies=policies,
    )


class Adapter:
    """하네스별 생성기의 공통 계약."""

    name = ""
    output_root = ""

    def generate(self, bundle: Bundle) -> dict:
        """{저장소 기준 상대경로: 파일 내용} 을 돌려준다."""
        raise NotImplementedError
