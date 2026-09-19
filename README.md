# Proof of Denial — AI 결제 차단 증명

AI 에이전트가 무단으로 결제를 시도하면 막고, "막았다"는 기록을 아무도 못 고치게 남긴다.
TRUST404 해커톤 트랙 3 (오프체인 의사결정 검증) 제출물.

## 지금 되는 것 (순서 1)

- 기록 장부 서버: 차단 1건 → `seq` + 앞 기록 지문(`prevHash`) + SHA-256 지문 + Ed25519 서명 → `data/ledger.jsonl`에 한 줄
- 검증 CLI: 장부 파일 + 공개키만으로 고침·삭제·서명 위조 탐지. 서버 접속 없음

아직 없는 것: 결제 가드, AI 에이전트, 블록체인 앵커 (순서 2·3).

## 구조 (클린 아키텍처)

```
pod/
├── ProofOfDenialApplication.kt
├── config/            Clock, PrivateKey 빈
└── app/
    ├── domain/        record/(엔티티·RecordRepository·RecordService), verify/(LedgerVerifyService), crypto/(해시·서명 포트)
    ├── application/   LedgerFacade + dto/
    ├── infrastructure/ crypto/(CanonicalJsonHasher, Ed25519*), repository/record/persistence/RecordRepositoryImpl
    └── interfaces/    common/(CommonRes), exception/, record/(controller·req·res), cli/(keygen, verifyLedger)
```

의존 방향은 interfaces → application → domain ← infrastructure. 검증 CLI는 Spring 없이 infrastructure를 직접 조립해 domain 서비스를 부른다.

## 실행

```bash
./gradlew keygen                 # keys/ed25519.private, keys/ed25519.public
./gradlew bootRun                # localhost:8080
bash demo/seed.sh                # 다른 터미널에서. 차단 3건 입력
```

API (응답은 모두 `{"resultType":"SUCCESS"|"FAIL","data":…,"exception":{code,message}?}`)
- `POST /api/v1/ledger/records` — 차단 1건 저장. body: `{agent, attempt, decision, reason, rawRequest}`
- `GET /api/v1/ledger/records`, `GET /api/v1/ledger/records/{seq}`, `GET /api/v1/ledger/head`

## 데모: 빨간불

서버를 끈 상태에서 (Ctrl+C):

```bash
# 정상
./gradlew verifyLedger -q --args="data/ledger.jsonl keys/ed25519.public"
#   ✓ #1 ✓ #2 ✓ #3 → 결과: 진짜, 안 고쳐짐

# 1) 금액을 고친다
cp data/ledger.jsonl /tmp/backup.jsonl
sed -i '' 's/"amount":5980/"amount":980/' data/ledger.jsonl
./gradlew verifyLedger -q --args="data/ledger.jsonl keys/ed25519.public"
#   ✗ #2  HASH_MISMATCH: 내용이 지문과 다름 — 고쳐졌음
cp /tmp/backup.jsonl data/ledger.jsonl

# 2) 한 줄을 지운다
sed -i '' '2d' data/ledger.jsonl
./gradlew verifyLedger -q --args="data/ledger.jsonl keys/ed25519.public"
#   ✗ #3  SEQ_GAP: 2번 없음
#   ✗ #3  PREV_MISMATCH: 앞 지문이 …
cp /tmp/backup.jsonl data/ledger.jsonl

# 3) 마지막 지문을 외부 값과 대조 (블록체인 앵커 자리 — 지금은 파일에서 뽑아 손으로 넣는다)
HEAD_HASH=$(tail -1 data/ledger.jsonl | sed 's/.*"hash":"\([^"]*\)".*/\1/')
./gradlew verifyLedger -q --args="data/ledger.jsonl keys/ed25519.public --expect-head $HEAD_HASH"
#   결과: 진짜, 안 고쳐짐
./gradlew verifyLedger -q --args="data/ledger.jsonl keys/ed25519.public --expect-head ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
#   ✗  HEAD_MISMATCH: 마지막 지문이 블록체인에 적힌 값과 다름
```

`sed -i ''`는 macOS 문법. Linux는 `sed -i`.

## 기록 한 줄

```json
{"seq":2,"prevHash":"a91f…","at":"2026-09-20T14:03:11+09:00",
 "agent":{"provider":"anthropic","model":"claude","requestId":"req_002","sessionId":"sess_demo"},
 "attempt":{"tool":"pay","item":"계란 30구","amount":5980,"currency":"KRW","merchant":"마트A"},
 "decision":"BLOCKED","reason":"NO_PAYMENT_PERMISSION","rawRequest":"{…}",
 "hash":"e7b2…","signature":"…"}
```

- `hash` = SHA-256( `hash`·`signature`를 뺀 나머지를 **필드 알파벳순**(중첩 포함)·공백 없이 직렬화한 JSON ) — 파일에 적힌 순서와 다르다. 지문 계산 시에만 정렬한다
- `signature` = Ed25519( `hash` 문자열 ) — 서버 개인키
- 첫 기록의 `prevHash`는 `0` 64개

## 테스트

```bash
./gradlew test
```

## 이번 해커톤 기간에 새로 구현한 것

전부. 기존 코드 없음. 코드 작성에 Claude Code를 사용했고, 설계·검증 시나리오는 팀이 정했다.
