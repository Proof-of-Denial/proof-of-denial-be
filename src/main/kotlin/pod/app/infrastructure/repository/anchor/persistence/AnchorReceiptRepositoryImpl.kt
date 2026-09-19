package pod.app.infrastructure.repository.anchor.persistence

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import pod.app.domain.anchor.AnchorReceipt
import pod.app.domain.anchor.AnchorReceiptRepository
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE

/** 한 줄 저장용. 필드 순서는 AnchorReceipt 선언 순서(seq, hash, txHash, …). */
private val lineMapper: JsonMapper = JsonMapper.builder()
    .addModule(kotlinModule())
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()

/** append-only JSONL 파일. 한 줄 = 영수증 1건. */
@Repository
class AnchorReceiptRepositoryImpl(
    @Value("\${anchor.file}") anchorFile: String,
) : AnchorReceiptRepository {

    private val path: Path = Path.of(anchorFile)

    override fun findAll(): List<AnchorReceipt> {
        if (!Files.exists(path)) {
            return emptyList()
        }
        val lines = Files.readAllLines(path)
        val receipts = ArrayList<AnchorReceipt>()
        for (line in lines) {
            if (line.isNotBlank()) {
                receipts.add(lineMapper.readValue<AnchorReceipt>(line))
            }
        }
        return receipts
    }

    override fun save(receipt: AnchorReceipt) {
        val parent = path.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        Files.writeString(path, lineMapper.writeValueAsString(receipt) + "\n", CREATE, APPEND)
    }
}
