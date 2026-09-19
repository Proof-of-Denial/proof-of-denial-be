package pod.infrastructure.crypto

import pod.domain.model.HashBody
import pod.domain.port.RecordHasher
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.security.MessageDigest

/** 지문 계산 전용 직렬화 설정. 필드·맵 키 모두 알파벳순, 공백 없음. */
private val canonicalMapper: JsonMapper = JsonMapper.builder()
    .addModule(kotlinModule())
    .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    .build()

/**
 * HashBody → canonical JSON → SHA-256 hex.
 * 서버가 기록을 만들 때와 검증 CLI가 다시 계산할 때 반드시 이 클래스를 쓴다.
 */
class CanonicalJsonHasher : RecordHasher {

    override fun hashOf(body: HashBody): String = sha256Hex(canonicalJson(body))

    /**
     * POJO(Kotlin data class)를 그대로 직렬화하면 생성자 순서가 유지되고 SORT_PROPERTIES_ALPHABETICALLY가
     * 먹지 않는다(Kotlin 모듈이 creator 프로퍼티에 명시적 순서를 부여하기 때문). 그래서 먼저 순수
     * Map/List/원시값 구조로 변환한 뒤 그걸 다시 직렬화한다 — 이 경로는 ORDER_MAP_ENTRIES_BY_KEYS가
     * 모든 중첩 단계에서 실제로 키를 알파벳순 정렬해 준다.
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
