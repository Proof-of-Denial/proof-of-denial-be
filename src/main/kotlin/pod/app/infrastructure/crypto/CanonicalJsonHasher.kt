package pod.app.infrastructure.crypto

import org.springframework.stereotype.Component
import pod.app.domain.crypto.RecordHasher
import pod.app.domain.record.HashBody
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.security.MessageDigest

/** 지문 계산 전용 직렬화 설정. 맵 키 알파벳순, 공백 없음. */
private val canonicalMapper: JsonMapper = JsonMapper.builder()
    .addModule(kotlinModule())
    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    .build()

/**
 * HashBody → canonical JSON → SHA-256 hex.
 * 서버가 기록을 만들 때와 검증 CLI가 다시 계산할 때 반드시 이 클래스를 쓴다.
 */
@Component
class CanonicalJsonHasher : RecordHasher {

    override fun hashOf(body: HashBody): String {
        return sha256Hex(canonicalJson(body))
    }

    /**
     * Kotlin data class를 그대로 직렬화하면 생성자 순서가 유지된다(Kotlin 모듈이 creator 프로퍼티에
     * 명시적 순서를 주기 때문에 SORT_PROPERTIES_ALPHABETICALLY가 먹지 않는다). 그래서 먼저 순수
     * Map/List/원시값 구조로 바꾼 뒤 다시 직렬화한다 — 이 경로는 ORDER_MAP_ENTRIES_BY_KEYS가
     * 모든 중첩 단계에서 키를 알파벳순으로 정렬해 준다.
     */
    fun canonicalJson(value: Any): String {
        val plainStructure = canonicalMapper.convertValue(value, Any::class.java)
        return canonicalMapper.writeValueAsString(plainStructure)
    }

    fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
