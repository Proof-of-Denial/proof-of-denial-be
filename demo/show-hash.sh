#!/usr/bin/env bash
# 기록 한 건이 "무엇을 해시했는지" 터미널에서 보여준다. 화면의 검산 패널과 같은 계산.
# 사용: bash demo/show-hash.sh [순번]   (기본 1)
set -euo pipefail
SEQ="${1:-1}"
LEDGER="${LEDGER:-data/ledger.jsonl}"

python3 - "$LEDGER" "$SEQ" <<'PY'
import json, hashlib, sys
path, want = sys.argv[1], int(sys.argv[2])
rec = None
for line in open(path, encoding='utf-8'):
    if line.strip():
        r = json.loads(line)
        if r.get('seq') == want:
            rec = r
            break
if rec is None:
    print(f"{want}번 기록이 없습니다"); raise SystemExit(1)

body = {k: v for k, v in rec.items() if k not in ('hash', 'signature')}
canonical = json.dumps(body, sort_keys=True, separators=(',', ':'), ensure_ascii=False)
digest = hashlib.sha256(canonical.encode('utf-8')).hexdigest()
stored = rec.get('hash', '(없음)')

print(f"\n① 기록 #{want} 에서 hash·signature 두 칸을 뺀다")
print(f"   뺀 것: hash={stored[:16]}…  signature={rec.get('signature','(없음)')[:12]}…")
print(f"\n② 남은 칸을 알파벳순으로 이어 붙인 문자열 ({len(canonical)}자) — 이게 해시한 값")
print("   " + "─" * 72)
for i in range(0, len(canonical), 72):
    print("   " + canonical[i:i+72])
print("   " + "─" * 72)
print("\n③ 대조")
print(f"   지금 계산한 값 : {digest}")
print(f"   기록의 hash    : {stored}")
print("\n   " + ("✓ 같습니다 — 고쳐지지 않았습니다" if digest == stored else "✗ 다릅니다 — 내용이 고쳐졌습니다") + "\n")
raise SystemExit(0 if digest == stored else 1)
PY
