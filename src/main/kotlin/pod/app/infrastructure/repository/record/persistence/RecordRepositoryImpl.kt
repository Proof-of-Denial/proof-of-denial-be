package pod.app.infrastructure.repository.record.persistence

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import pod.app.domain.record.Record
import pod.app.domain.record.RecordRepository
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE

/** 한 줄 저장용. 필드 순서는 Record 선언 순서(seq, prevHash, at, …). 지문 계산은 이 mapper를 쓰지 않는다. */
private val lineMapper: JsonMapper = JsonMapper.builder()
    .addModule(kotlinModule())
    .build()

/**
 * append-only JSONL 파일. 한 줄 = 기록 1건.
 * 데모에서 이 파일을 텍스트 편집기로 고치거나 줄을 지워 변조를 재현한다.
 */
@Repository
class RecordRepositoryImpl(
    @Value("\${ledger.file}") ledgerFile: String,
) : RecordRepository {

    private val path: Path = Path.of(ledgerFile)

    override fun findAll(): List<Record> {
        if (!Files.exists(path)) return emptyList()
        return Files.readAllLines(path)
            .filter { it.isNotBlank() }
            .map { lineMapper.readValue<Record>(it) }
    }

    override fun save(record: Record) {
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, lineMapper.writeValueAsString(record) + "\n", CREATE, APPEND)
    }
}
