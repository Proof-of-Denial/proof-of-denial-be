# Proof of Denial — AI 결제 차단 증명

AI 에이전트가 무단으로 결제를 시도하면 막고, "막았다"는 기록을 아무도 못 고치게 남긴다.
TRUST404 해커톤 트랙 3 (오프체인 의사결정 검증) 제출물.

## 지금 되는 것

**순서 1 — 장부 + 검증**
- 기록 장부 서버: 차단 1건 → `seq` + 앞 기록 지문(`prevHash`) + SHA-256 지문 + Ed25519 서명 → `data/ledger.jsonl`에 한 줄
- 검증 CLI: 장부 파일 + 공개키만으로 고침·삭제·서명 위조 탐지. 서버 접속 없음

**순서 2 — 결제 가드 + AI 에이전트**
- `POST /api/v1/agent/chat {"message":"계란 찾아줘"}` → Claude가 `search_product` → `pay` 순으로 도구를 부른다
- `pay`는 결제 가드(`guard.*` 규칙: 권한 → 한도 → 등록 가게)를 거치고, 판정이 BLOCKED든 ALLOWED든 장부에 남는다
- 에이전트는 "찾으면 바로 결제"하도록 시스템 프롬프트로 공격적으로 배치했다 — 무단 결제 시도를 재현하기 위한 설정

**순서 3 — 블록체인 도장 (XRPL 테스트넷 앵커)**
- `POST /api/v1/anchor` — 장부의 마지막 지문을 XRPL 테스트넷 트랜잭션(`AccountSet` + 메모)에 적어 올린다. 온체인에는 지문 64자만 올라간다 — 기록 내용·개인정보는 절대 올라가지 않는다
- `GET /api/v1/anchor/latest` — 마지막으로 찍은 도장 조회
- 검증 CLI `--anchor-tx <txHash>`: 서버·장부 파일 없이 그 트랜잭션 메모만 읽어 지문을 대조한다 (아래 "데모: 초록불")

## 평가 기준 ↔ 어디서 확인하나

| 기준 | 무엇으로 | 확인 방법 |
|---|---|---|
| 불변성 | 줄마다 SHA-256 + Ed25519 서명, 앞 줄 해시를 품는 체인, 요약 해시(맨 끝 줄 해시)를 XRPL에 앵커 | 금액 한 자리 고치면 `HASH_MISMATCH`. 해시·서명을 전부 다시 계산해도 요약 해시가 앵커 값과 달라져 `HEAD_MISMATCH` (아래 "데모: 빨간불") |
| 완전성 | `seq` 연속 검사 + 체인 + 앵커에 적힌 순번 | 가운데 줄 삭제 → `SEQ_GAP` + `PREV_MISMATCH`. 끝 줄 누락 → 앵커와 불일치 `HEAD_MISMATCH` |
| 독립 검증 | Spring 없는 CLI. 입력은 장부 파일·공개키·XRPL 공개 RPC뿐. 우리 서버엔 요청을 보내지 않음 | 서버 끄고 `verifyLedger --anchor-tx` (아래 "데모: 초록불"). 브라우저 화면도 서버 말을 안 믿고 직접 재계산 (아래 "시연 화면") |
| 부인 방지 | 거절 주체: 우리 Ed25519 서명 / 요청자: AI 회사가 발급한 `requestId` + 요청 원문 `rawRequest` | 아래 "기록 한 줄". AI가 자기 요청에 직접 서명하는 건 AI 회사 협조가 필요해 다음 단계 |

## 구조 (클린 아키텍처)

```
pod/
├── ProofOfDenialApplication.kt
├── config/            Clock, PrivateKey, Claude, Gemini, XRPL 빈
└── app/
    ├── domain/        record/, verify/(LedgerVerifyService), crypto/(해시·서명 포트),
    │                  product/, guard/(결제 가드 규칙), agent/(AgentTools·AgentStep 포트), anchor/(ChainAnchor 포트)
    ├── application/   LedgerFacade, AgentFacade, AnchorFacade + dto/
    ├── infrastructure/ crypto/(CanonicalJsonHasher, Ed25519*), llm/(ClaudeChatModel, GeminiChatModel), chain/(XrplChainAnchor),
    │                  repository/record, repository/product, repository/anchor
    └── interfaces/    common/(CommonRes), exception/, cli/(keygen, verifyLedger),
                       record/(controller·req·res), agent/(controller·req·res), anchor/(controller·res)
```

의존 방향은 interfaces → application → domain ← infrastructure. 검증 CLI는 Spring 없이 infrastructure를 직접 조립해 domain 서비스를 부른다.

## 실행

AI 에이전트를 돌리려면 API 키가 필요하다 (없어도 장부 API·검증 CLI는 동작한다). 기본은 Claude, `AGENT_PROVIDER=gemini`로 바꾸면 Gemini가 대신 돈다:

서버 실행에는 XRPL 테스트넷 계정의 비밀 문구도 필요합니다. 최초 한 번 생성해 안전하게 보관하고, 같은 계정을 사용하려면 동일한 값을 설정하세요.

```bash
export XRPL_PASSPHRASE="$(openssl rand -hex 32)"
export POD_ANTHROPIC_API_KEY=sk-ant-api03-...   # ANTHROPIC_API_KEY 가 아니라 POD_ 접두사 (셸의 다른 토큰과 충돌 방지)
# 또는
export AGENT_PROVIDER=gemini
export GEMINI_API_KEY=AIza...
```

장부의 `agent.provider` 필드에 `anthropic` / `google`이 그대로 남으므로, 어느 회사 AI가 무단 결제를 시도했는지가 증거에 남는다 — 이게 이 프로젝트의 요점과 맞물린다.

```bash
./gradlew keygen                 # keys/ed25519.private, keys/ed25519.public
./gradlew bootRun                # localhost:8080
bash demo/seed.sh                # 다른 터미널에서. 차단 3건 입력
curl -s -X POST localhost:8080/api/v1/agent/chat -H 'Content-Type: application/json' -d '{"message":"계란 찾아줘"}'
```

API (응답은 모두 `{"resultType":"SUCCESS"|"FAIL","data":…,"exception":{code,message}?}`)
- `POST /api/v1/ledger/records` — 차단 1건 저장. body: `{agent, attempt, decision, reason, rawRequest}`
- `GET /api/v1/ledger/records`, `GET /api/v1/ledger/records/{seq}`, `GET /api/v1/ledger/head`
- `POST /api/v1/agent/chat` — 에이전트 한 판. body: `{message}`. 응답 `steps[]`의 `kind`는 `ASSISTANT` / `TOOL_CALL` / `TOOL_RESULT`
- `POST /api/v1/anchor` — 장부 마지막 지문을 XRPL 테스트넷에 도장 찍고 영수증 반환. `GET /api/v1/anchor/latest` — 마지막 영수증 (없으면 404 `ANCHOR_NOT_FOUND`)

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

# 3) 마지막 지문을 외부 값과 대조 (실제로는 블록체인 앵커로 대체한다 — 아래 "데모: 초록불" 참고)
HEAD_HASH=$(tail -1 data/ledger.jsonl | sed 's/.*"hash":"\([^"]*\)".*/\1/')
./gradlew verifyLedger -q --args="data/ledger.jsonl keys/ed25519.public --expect-head $HEAD_HASH"
#   결과: 진짜, 안 고쳐짐
./gradlew verifyLedger -q --args="data/ledger.jsonl keys/ed25519.public --expect-head ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
#   ✗  HEAD_MISMATCH: 마지막 지문이 블록체인에 적힌 값과 다름
```

`sed -i ''`는 macOS 문법. Linux는 `sed -i`.

`./gradlew verifyLedger`로 돌리면 셸 종료 코드는 항상 0이다 (Gradle 태스크 실패를 데모 화면에서 감추려고 무시하도록 설정했다).
검증 결과는 오직 위 출력의 `결과:` 줄로 본다. CLI 자체의 종료 코드(정상 0 / 문제 있음 1 / 사용법·파일 오류 2)는
Gradle 없이 직접 실행할 때만 그대로 나온다. 배포 서버처럼 JAR만 있는 환경에서는 이렇게 부른다
(Spring Boot fat JAR라 `-cp`에 클래스 이름만 주면 안 되고, Boot 런처에 `loader.main`으로 넘긴다):

```bash
./gradlew bootJar   # → build/libs/proof-of-denial-0.0.1-SNAPSHOT.jar
java -cp build/libs/proof-of-denial-0.0.1-SNAPSHOT.jar \
  -Dloader.main=pod.app.interfaces.cli.VerifyCliKt \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  data/ledger.jsonl keys/ed25519.public --anchor-tx <txHash>
echo $?   # 0 정상 / 1 문제 있음 / 2 사용법·파일 오류
```

## 데모: 초록불

빨간불 데모의 3번을 실제 블록체인으로 대체한 것 — `--expect-head`를 손으로 넣는 대신 XRPL 테스트넷
트랜잭션 메모에서 지문을 읽어와 대조한다. **CLI는 이때도 우리 서버에 접속하지 않는다.** 접속하는 건
XRPL 공개 RPC뿐이다 — 그게 "제3자가 독립적으로 검증한다"는 것의 의미다.

실행 데이터와 비밀키는 저장소에 포함하지 않습니다. 먼저 위 실행 절차대로 키를 생성하고 서버를 실행한 뒤, 시연 기록과 앵커를 만드세요:

```bash
bash demo/seed.sh
curl -s -X POST localhost:8080/api/v1/anchor
# 응답의 data.txHash를 아래 값으로 설정
TX_HASH="<방금 생성된 트랜잭션 해시>"
./gradlew verifyLedger -q --args="data/ledger.jsonl keys/ed25519.public --anchor-tx $TX_HASH"
```

검증 CLI는 서버 종료 후에도 로컬 장부와 공개키, XRPL 트랜잭션으로 검증할 수 있습니다.
장부에 기록을 추가했다면 새 앵커를 생성하고 해당 트랜잭션 해시를 사용하세요.

아래 트랜잭션은 과거 시연의 참고 자료이며, 새로 생성한 로컬 장부와 일치하지 않습니다.

실제로 찍힌 도장을 익스플로러에서 확인: https://testnet.xrpl.org/transactions/56ECC6987765C507025C4B934284E365029349CB4F973E36F48D658C78C039A6

메모: `pod:v1:3:73bc9a3904522cea9575cccebffeab2cf4e28838ff52005dbec0a83d1c2617ce` (과거 시연 장부 3건의 마지막 지문).

XRPL 계정은 환경변수 `XRPL_PASSPHRASE`에서 결정적으로 파생됩니다 — 같은 문구는 항상 같은 계정.
기본 비밀 문구는 없으며, 누락하거나 공백으로 설정하면 서버가 시작되지 않습니다. 새 계정은 첫 앵커 때 faucet이 자동으로 충전합니다.

XRPL 테스트넷 호스트가 TLS 핸드셰이크에서 중간 인증서를 안 보내는 문제가 있어 `com.sun.security.enableAIAcaIssuers=true`를 켠다.
이 옵션은 빠진 인증서를 AIA로 받아와 체인을 완성할 뿐 검증을 끄지 않는다. 서버와 CLI의 `main()`에서 직접 설정하므로
IDE·JAR 실행에서도 동작하고, Gradle 태스크(`bootRun`, `verifyLedger`)에도 같은 `jvmArgs`가 걸려 있다.

## 시연 화면

서버 실행 후(`./gradlew bootRun`) `http://localhost:8080/` 를 열면 AI에게 말 걸기 → 장부 표 → 블록체인 도장이
한 화면에 나온다. 정적 파일 하나(`src/main/resources/static/index.html`)이고 빌드 단계가 없다 — Spring Boot가
그대로 서빙한다.

화면도 서버 말을 믿지 않는다:
- 페이지를 열 때마다 브라우저가 **모든 줄의 해시를 Web Crypto로 다시 계산**해 기록의 `hash`·`prevHash`·순번과 대조한다. 멀쩡한 줄은 초록 선, 깨진 줄은 빨간 배경 + 이유(`내용이 해시와 다름`, `앞 해시가 어긋남`, `순번 N 없음`)
- 기록을 누르면 ① 뺀 칸(`hash`·`signature`) ② 해시에 넣은 문자열 원문 ③ 브라우저 계산값 vs 기록값 — 세 단계로 펼쳐진다
- 도장 박스는 순번으로 상황을 구분한다: 도장 뒤에 새 기록이 생김(노랑, 다시 찍으면 됨) / 같은 순번인데 요약 해시가 다름(빨강, 조작) / 장부가 도장보다 짧음(빨강, 누락)

장부 파일(`data/ledger.jsonl`)을 편집기로 직접 고치고 새로고침하면 그 자리에서 빨개진다 — 시연은 그렇게 한다.

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
- `agent.requestId` = AI 회사 API 응답의 id (Anthropic `msg_…`, Gemini `responseId`). 우리가 만들 수 없는 값이라 저쪽 로그와 대조하는 열쇠 — 요청자 측 부인 방지. `signature`는 우리 개인키 — 거절 주체 측 부인 방지

## 테스트

```bash
./gradlew test
```

49개, 전부 통과. 규칙마다 코드를 일부러 망가뜨려 테스트가 실제로 실패하는지 확인했다 — 예: "키를 가진 내부자가 고치고 다시 해시·서명해도 다음 기록의 `PREV_MISMATCH`로 걸린다"(`LedgerVerifyServiceTest`).

## 이번 해커톤 기간에 새로 구현한 것

전부. 기존 코드 없음. 코드 작성에 Claude Code를 사용했고, 설계·검증 시나리오는 팀이 정했다.
