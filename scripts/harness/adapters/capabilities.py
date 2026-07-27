"""공통 capability·모델 등급을 하네스별 이름으로 옮기는 표.

원본(harness/agents/*.md)은 하네스 도구 이름을 쓰지 않는다. `Read`, `Grep` 은
Claude Code 의 이름이고 다른 하네스에는 없거나 다르게 불린다. 원본에는
"이 에이전트가 무엇을 할 수 있어야 하는가"만 적고, 번역은 여기서 한다.

지원하지 않는 capability 는 조용히 버리지 않고 생성 시 경고로 알린다.
읽기 전용이어야 할 리뷰어에게 쓰기 권한이 붙는 것보다, 권한이 빠져서
동작을 못 하는 쪽이 눈에 띄기 때문이다.
"""

CAPABILITIES = ("read", "search", "edit", "write", "shell")

# capability → 하네스별 도구 이름 목록
TOOL_NAMES = {
    "claude": {
        "read": ("Read",),
        "search": ("Grep", "Glob"),
        "edit": ("Edit",),
        "write": ("Write",),
        "shell": ("Bash",),
    },
    # ⚠️ codex·antigravity 의 도구 이름은 미검증이다. 형식이 확인되면 여기만 고친다.
    "codex": {
        "read": ("read_file",),
        "search": ("grep", "glob"),
        "edit": ("apply_patch",),
        "write": ("write_file",),
        "shell": ("shell",),
    },
    "antigravity": {
        "read": ("read_file",),
        "search": ("search",),
        "edit": ("edit_file",),
        "write": ("write_file",),
        "shell": ("run_command",),
    },
}

# 모델 등급 별칭 → 하네스별 모델 이름
# 원본에는 fast / balanced / deep 만 쓴다. 특정 모델명을 원본에 박으면
# 모델이 바뀔 때마다 모든 에이전트 파일을 고쳐야 한다.
MODEL_TIERS = {
    "claude": {"fast": "haiku", "balanced": "sonnet", "deep": "opus"},
    "codex": {"fast": "gpt-5-mini", "balanced": "gpt-5", "deep": "gpt-5-codex"},
    "antigravity": {"fast": "gemini-flash", "balanced": "gemini-pro", "deep": "gemini-pro"},
}


def tools_for(harness: str, capabilities, warn=None) -> list:
    table = TOOL_NAMES.get(harness, {})
    names = []
    for capability in capabilities:
        if capability not in CAPABILITIES:
            if warn:
                warn(f"모르는 capability '{capability}' — CAPABILITIES 에 추가하세요")
            continue
        mapped = table.get(capability)
        if not mapped:
            if warn:
                warn(f"{harness} 는 capability '{capability}' 를 지원하지 않습니다")
            continue
        for tool in mapped:
            if tool not in names:
                names.append(tool)
    return names


def model_for(harness: str, tier: str, warn=None) -> str:
    if not tier:
        return ""
    table = MODEL_TIERS.get(harness, {})
    model = table.get(tier)
    if not model and warn:
        warn(f"{harness} 에 모델 등급 '{tier}' 매핑이 없습니다")
    return model or ""
