package pod.app.infrastructure.llm

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

/** 도구 인자 직렬화 전용 mapper. 장부에 들어갈 문자열이라 설정을 따로 둔다. */
private val toolArgsMapper: JsonMapper = JsonMapper.builder()
    .addModule(kotlinModule())
    .build()

/**
 * Gemini가 돌려준 도구 인자(Map)를 장부에 적을 JSON 문자열로 만든다.
 * Map.toString()은 {item=계란} 같은 자바 표기라 rawRequest로 쓰면 안 된다.
 */
internal fun toolArgsToJson(args: Map<String, Any?>): String {
    return toolArgsMapper.writeValueAsString(args)
}
