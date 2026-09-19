package pod.app.interfaces.cli

import pod.app.domain.verify.LedgerVerifyService
import pod.app.domain.verify.Problem
import pod.app.infrastructure.chain.XrplChainAnchor
import pod.app.infrastructure.crypto.CanonicalJsonHasher
import pod.app.infrastructure.crypto.Ed25519Keys
import pod.app.infrastructure.crypto.Ed25519SignatureVerifier
import pod.app.infrastructure.repository.record.persistence.RecordRepositoryImpl
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

// application.yml의 xrpl.rpc-url과 같은 값. CLI는 Spring/yml을 로드하지 않으므로 여기 그대로 박아둔다.
private const val XRPL_RPC_URL = "https://s.altnet.rippletest.net:51234/"

/**
 * 사용: ./gradlew verifyLedger --args="data/ledger.jsonl keys/ed25519.public [--seq N] [--expect-head HASH] [--anchor-tx TXHASH]"
 * 서버에 접속하지 않는다. 장부·공개키 파일과(옵션) XRPL 공개 RPC만 본다. Spring 없이 infrastructure를 직접 조립한다.
 * --anchor-tx는 그 트랜잭션 메모에서 지문을 읽어와 expectHead로 쓴다(제3자가 서버 없이 독립 검증하는 경로).
 *
 * 이 파일은 프로젝트에서 유일하게 println을 쓴다: 아래 출력은 로그가 아니라 검증 도구의 결과물(stdout)이라
 * 타임스탬프·로거명 없이 그대로 보여야 한다. (심사 데모 화면 · 파이프 연결)
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        println("사용법: verifyLedger --args=\"<ledger.jsonl> <ed25519.public> [--seq N] [--expect-head HASH]\"")
        exitProcess(2)
    }
    val ledgerPath = Path.of(args[0])
    val publicKeyPath = Path.of(args[1])
    if (!Files.exists(ledgerPath)) {
        println("장부 파일 없음: $ledgerPath")
        exitProcess(2)
    }
    if (!Files.exists(publicKeyPath)) {
        println("공개키 파일 없음: $publicKeyPath")
        exitProcess(2)
    }
    val focusSeq = optionValue(args, "--seq")?.toLongOrNull()
    val expectHeadOption = optionValue(args, "--expect-head")
    val anchorTxOption = optionValue(args, "--anchor-tx")

    // 장부는 사람이 손으로 고칠 수 있는 파일이라, 형식이 깨진 줄이 있어도 스택트레이스 없이 문제로 보고한다.
    val records = try {
        RecordRepositoryImpl(ledgerPath.toString()).findAll()
    } catch (e: Exception) {
        println("장부 줄을 읽을 수 없음 — 형식이 깨졌습니다: ${e.message?.lineSequence()?.firstOrNull()}")
        println("결과: 문제 1건")
        exitProcess(1)
    }

    // --expect-head가 같이 오면 그게 우선이고, --anchor-tx만 있을 때만 체인에서 읽는다.
    var expectHead = expectHeadOption
    var anchorStampLine: String? = null
    if (expectHeadOption == null && anchorTxOption != null) {
        val headFromChain = readHeadFromChain(anchorTxOption)
        if (headFromChain == null) {
            exitProcess(1)
        }
        expectHead = headFromChain
        anchorStampLine = "블록체인 도장: $anchorTxOption → " + headFromChain.take(12) + "…"
    }

    val publicKey = Ed25519Keys.decodePublic(Files.readString(publicKeyPath))
    val verifyService = LedgerVerifyService(CanonicalJsonHasher(), Ed25519SignatureVerifier(publicKey))
    val result = verifyService.verify(records, expectHead)

    println("장부: $ledgerPath")
    val headPreview = if (result.head == null) "-" else result.head.take(12) + "…"
    println("기록 ${result.count}건 · 마지막 지문 $headPreview")
    if (anchorStampLine != null) {
        println(anchorStampLine)
    }
    println()
    // problems는 기록 순서대로 쌓인다. seq로 다시 필터링하면 같은 seq가 두 번(삭제 후 중복) 나올 때
    // 모든 문제가 두 줄 모두에 겹쳐 붙으므로, 앞에서부터 하나씩 소비해 제자리에만 붙인다.
    val remaining = result.problems.filter { it.seq != null }.toMutableList()
    for (record in records) {
        val problemsOfThisRecord = mutableListOf<Problem>()
        while (remaining.isNotEmpty() && remaining.first().seq == record.seq) {
            problemsOfThisRecord.add(remaining.removeFirst())
        }
        val mark = if (problemsOfThisRecord.isEmpty()) "✓" else "✗"
        val focus = if (record.seq == focusSeq) "  ◀" else ""
        val amount = "%,d".format(record.attempt.amount)
        println("$mark  #${record.seq}  ${record.attempt.item} $amount${record.attempt.currency}  ${record.decision}  ${record.at}$focus")
        for (problem in problemsOfThisRecord) {
            println("      ${problem.kind}: ${problem.detail}")
        }
    }
    for (problem in result.problems.filter { it.seq == null }) {
        println("✗  ${problem.kind}: ${problem.detail}")
    }
    println()
    println(if (result.ok) "결과: 진짜, 안 고쳐짐" else "결과: 문제 ${result.problems.size}건")
    exitProcess(if (result.ok) 0 else 1)
}

private fun optionValue(args: Array<String>, name: String): String? {
    val index = args.indexOf(name)
    if (index < 0 || index + 1 >= args.size) {
        return null
    }
    return args[index + 1]
}

/**
 * XRPL 공개 RPC에서 트랜잭션 메모의 지문을 읽어온다. 실패하면 스택트레이스 없이 한 줄로 알리고 null을 돌려준다
 * — 호출자는 그걸 보고 exitProcess(1)로 끝낸다.
 */
private fun readHeadFromChain(txHash: String): String? {
    try {
        val chainAnchor = XrplChainAnchor.forReadOnly(XRPL_RPC_URL)
        val head = chainAnchor.readHead(txHash)
        if (head == null) {
            println("체인에서 지문을 찾지 못했습니다(트랜잭션 메모가 없거나 형식이 다릅니다): $txHash")
            println("결과: 문제 1건")
            return null
        }
        return head
    } catch (exception: Exception) {
        println("체인 조회 실패: ${exception.message}")
        println("결과: 문제 1건")
        return null
    }
}
