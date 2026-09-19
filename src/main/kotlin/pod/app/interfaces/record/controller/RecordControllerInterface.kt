package pod.app.interfaces.record.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import pod.app.interfaces.common.CommonRes
import pod.app.interfaces.record.req.AddRecordReq
import pod.app.interfaces.record.res.HeadRes
import pod.app.interfaces.record.res.RecordRes

/** 장부 API 명세. (Swagger는 나중에 springdoc 붙일 때 여기에 어노테이션을 단다.) */
interface RecordControllerInterface {

    /** 결제 가드가 차단할 때 호출한다. */
    @PostMapping("/records")
    fun addRecord(@Valid @RequestBody req: AddRecordReq): CommonRes<RecordRes>

    @GetMapping("/records")
    fun getRecords(): CommonRes<List<RecordRes>>

    @GetMapping("/records/{seq}")
    fun getRecord(@PathVariable seq: Long): CommonRes<RecordRes>

    /** 마지막 지문. 나중에 블록체인에 올릴 값. */
    @GetMapping("/head")
    fun getHead(): CommonRes<HeadRes>
}
