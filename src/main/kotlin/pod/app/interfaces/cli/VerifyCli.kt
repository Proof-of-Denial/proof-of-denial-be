package pod.app.interfaces.cli

import pod.app.domain.verify.LedgerVerifyService
import pod.app.infrastructure.crypto.CanonicalJsonHasher
import pod.app.infrastructure.crypto.Ed25519Keys
import pod.app.infrastructure.crypto.Ed25519SignatureVerifier
import pod.app.infrastructure.repository.record.persistence.RecordRepositoryImpl
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

/**
 * 사용: ./gradlew verifyLedger --args="data/ledger.jsonl keys/ed25519.public [--seq N] [--expect-head HASH]"
 * 서버에 접속하지 않는다. 파일 두 개만 읽는다. Spring 없이 infrastructure를 직접 조립한다.
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
    val expectHead = optionValue(args, "--expect-head")

    val records = RecordRepositoryImpl(ledgerPath.toString()).findAll()
    val publicKey = Ed25519Keys.decodePublic(Files.readString(publicKeyPath))
    val verifyService = LedgerVerifyService(CanonicalJsonHasher(), Ed25519SignatureVerifier(publicKey))
    val result = verifyService.verify(records, expectHead)

    println("장부: $ledgerPath")
    val headPreview = if (result.head == null) "-" else result.head.take(12) + "…"
    println("기록 ${result.count}건 · 마지막 지문 $headPreview")
    println()
    for (record in records) {
        val problemsOfThisRecord = result.problems.filter { it.seq == record.seq }
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
    if (index < 0 || index + 1 >= args.size) return null
    return args[index + 1]
}
