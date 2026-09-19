package pod.app.infrastructure.chain

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException
import org.xrpl.xrpl4j.client.XrplClient
import org.xrpl.xrpl4j.client.faucet.FaucetClient
import org.xrpl.xrpl4j.client.faucet.FundAccountRequest
import org.xrpl.xrpl4j.crypto.keys.KeyPair
import org.xrpl.xrpl4j.crypto.keys.Passphrase
import org.xrpl.xrpl4j.crypto.keys.Seed
import org.xrpl.xrpl4j.crypto.signing.bc.BcSignatureService
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams
import org.xrpl.xrpl4j.model.client.fees.FeeUtils
import org.xrpl.xrpl4j.model.client.transactions.TransactionRequestParams
import org.xrpl.xrpl4j.model.transactions.AccountSet
import org.xrpl.xrpl4j.model.transactions.Address
import org.xrpl.xrpl4j.model.transactions.Hash256
import org.xrpl.xrpl4j.model.transactions.Memo
import org.xrpl.xrpl4j.model.transactions.MemoWrapper
import pod.app.domain.anchor.ChainAnchor
import pod.app.domain.anchor.ChainWriteResult
import pod.app.interfaces.exception.ApiException
import pod.app.interfaces.exception.ExceptionCode
import java.util.HexFormat

/**
 * xrpl4j로 지문을 XRPL 테스트넷에 올리고 읽는다. 수신자가 필요 없는 AccountSet 트랜잭션에
 * "pod:v1:<seq>:<hash>" 메모만 실어 보낸다 — 온체인에는 지문 64자만 올라가고 기록 내용·개인정보는
 * 절대 올라가지 않는다. 계정은 비밀 문구에서 결정적으로 파생된다.
 */
@Component
class XrplChainAnchor(
    private val xrplClient: XrplClient,
    private val faucetClient: FaucetClient,
    private val keyPair: KeyPair,
) : ChainAnchor {
    private val logger = LoggerFactory.getLogger(XrplChainAnchor::class.java)

    companion object {
        // 테스트넷 계정 최소 유지 잔액보다 넉넉히 잡은 기준. 이보다 적으면 faucet으로 충전한다.
        private const val MIN_BALANCE_DROPS = 20_000_000L
        private const val FUND_WAIT_ATTEMPTS = 10
        private const val FUND_WAIT_MILLIS = 2000L
        private const val TX_LOOKUP_ATTEMPTS = 3
        private const val TX_LOOKUP_WAIT_MILLIS = 2000L
        private const val MEMO_PREFIX = "pod:v1:"

        // 검증 CLI는 Spring 없이 readHead만 쓴다. faucet·서명은 절대 호출되지 않으므로
        // 네트워크 호출 없는 더미 faucetClient·keyPair로 채워 생성자를 그대로 재사용한다.
        private const val READ_ONLY_PASSPHRASE = "pod-verify-cli-readonly-unused"

        fun forReadOnly(rpcUrl: String): XrplChainAnchor {
            val xrplClient = XrplClient(rpcUrl.toHttpUrl())
            val faucetClient = FaucetClient.construct(rpcUrl.toHttpUrl())
            val seed = Seed.ed25519SeedFromPassphrase(Passphrase.of(READ_ONLY_PASSPHRASE))
            val keyPair = seed.deriveKeyPair()
            return XrplChainAnchor(xrplClient, faucetClient, keyPair)
        }
    }

    override fun writeHead(seq: Long, hash: String): ChainWriteResult {
        val address = keyPair.publicKey().deriveAddress()
        ensureFunded(address)

        val accountInfo = xrplClient.accountInfo(AccountInfoRequestParams.of(address))
        val sequence = accountInfo.accountData().sequence()
        val feeResult = xrplClient.fee()
        val fee = FeeUtils.computeNetworkFees(feeResult).recommendedFee()

        val memoText = MEMO_PREFIX + seq + ":" + hash
        val memo = Memo.withPlaintext(memoText).build()
        val memoWrapper = MemoWrapper.builder().memo(memo).build()
        val accountSet = AccountSet.builder()
            .account(address)
            .fee(fee)
            .sequence(sequence)
            .signingPublicKey(keyPair.publicKey())
            .addMemos(memoWrapper)
            .build()

        val signed = BcSignatureService().sign(keyPair.privateKey(), accountSet)
        val submitResult = xrplClient.submit(signed)
        if (submitResult.engineResult() != "tesSUCCESS") {
            throw ApiException(ExceptionCode.ANCHOR_FAILED, submitResult.engineResult())
        }

        val txHash = submitResult.transactionResult().hash().value()
        logger.info("체인에 도장 제출 tx={}", txHash)
        return ChainWriteResult(txHash, address.value())
    }

    override fun readHead(txHash: String): String? {
        val params = TransactionRequestParams.of(Hash256.of(txHash))
        var attempt = 0
        while (attempt < TX_LOOKUP_ATTEMPTS) {
            val head = tryReadHead(params)
            if (head != null) {
                return head
            }
            sleep(TX_LOOKUP_WAIT_MILLIS)
            attempt = attempt + 1
        }
        return null
    }

    /** 검증(validated)되지 않았거나 아직 조회되지 않는 트랜잭션이면 null — 호출자가 재시도한다. */
    private fun tryReadHead(params: TransactionRequestParams): String? {
        try {
            val result = xrplClient.transaction(params, AccountSet::class.java)
            if (!result.validated()) {
                return null
            }
            return extractHead(result.transaction().memos())
        } catch (exception: JsonRpcClientErrorException) {
            return null
        }
    }

    private fun extractHead(memos: List<MemoWrapper>): String? {
        if (memos.isEmpty()) {
            return null
        }
        val memoDataOptional = memos[0].memo().memoData()
        if (memoDataOptional.isEmpty) {
            return null
        }
        val decodedBytes = HexFormat.of().parseHex(memoDataOptional.get())
        val decoded = String(decodedBytes, Charsets.UTF_8)
        val parts = decoded.split(":")
        if (parts.size != 4) {
            return null
        }
        if (parts[0] != "pod" || parts[1] != "v1") {
            return null
        }
        return parts[3]
    }

    private fun ensureFunded(address: Address) {
        val balanceDrops = currentBalanceDrops(address)
        if (balanceDrops != null && balanceDrops >= MIN_BALANCE_DROPS) {
            return
        }
        logger.info("XRPL 계정 충전 필요 — faucet 호출 {}", address.value())
        faucetClient.fundAccount(FundAccountRequest.of(address))

        var attempt = 0
        while (attempt < FUND_WAIT_ATTEMPTS) {
            sleep(FUND_WAIT_MILLIS)
            val fundedBalance = currentBalanceDrops(address)
            if (fundedBalance != null && fundedBalance >= MIN_BALANCE_DROPS) {
                return
            }
            attempt = attempt + 1
        }
        throw ApiException(ExceptionCode.ANCHOR_FAILED, "faucet 충전 후에도 잔액을 확인하지 못했습니다.")
    }

    /** 계정이 아직 없으면(actNotFound 등) null. */
    private fun currentBalanceDrops(address: Address): Long? {
        try {
            val accountInfo = xrplClient.accountInfo(AccountInfoRequestParams.of(address))
            val balanceDrops = accountInfo.accountData().balance().value().toString()
            return balanceDrops.toLong()
        } catch (exception: JsonRpcClientErrorException) {
            logger.info("계정 조회 실패(아직 없을 수 있음): {}", exception.message)
            return null
        }
    }

    private fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
