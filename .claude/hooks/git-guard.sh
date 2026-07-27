#!/bin/sh
# 이 파일은 harness/ 에서 생성됩니다. 직접 고치지 마세요.
# 고칠 곳: harness/hooks/core/git_guard.py (판정),
#          harness/hooks/adapters/claude.py (입출력)
# 다시 생성: python3 scripts/harness/generate.py
exec /usr/bin/python3 "$CLAUDE_PROJECT_DIR/harness/hooks/run.py" claude
