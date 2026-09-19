#!/usr/bin/env bash
# 차단 3건을 장부에 넣는다. 서버가 localhost:8080에 떠 있어야 한다.
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"

post() {
  curl -s -X POST "$BASE/api/v1/ledger/records" -H 'Content-Type: application/json' -d "$1"
  echo
}

post '{"agent":{"provider":"anthropic","model":"claude","requestId":"req_001","sessionId":"sess_demo"},"attempt":{"tool":"pay","item":"우유 1L","amount":3200,"currency":"KRW","merchant":"마트A"},"decision":"BLOCKED","reason":"NO_PAYMENT_PERMISSION","rawRequest":"{\"tool\":\"pay\",\"item\":\"우유 1L\",\"amount\":3200,\"merchant\":\"마트A\"}"}'
post '{"agent":{"provider":"anthropic","model":"claude","requestId":"req_002","sessionId":"sess_demo"},"attempt":{"tool":"pay","item":"계란 30구","amount":5980,"currency":"KRW","merchant":"마트A"},"decision":"BLOCKED","reason":"NO_PAYMENT_PERMISSION","rawRequest":"{\"tool\":\"pay\",\"item\":\"계란 30구\",\"amount\":5980,\"merchant\":\"마트A\"}"}'
post '{"agent":{"provider":"anthropic","model":"claude","requestId":"req_003","sessionId":"sess_demo"},"attempt":{"tool":"pay","item":"식빵","amount":2500,"currency":"KRW","merchant":"마트A"},"decision":"BLOCKED","reason":"NO_PAYMENT_PERMISSION","rawRequest":"{\"tool\":\"pay\",\"item\":\"식빵\",\"amount\":2500,\"merchant\":\"마트A\"}"}'

echo "--- head ---"
curl -s "$BASE/api/v1/ledger/head"; echo
