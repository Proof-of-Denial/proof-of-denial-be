package pod.app.interfaces.anchor.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import pod.app.interfaces.anchor.res.AnchorReceiptRes
import pod.app.interfaces.common.CommonRes

/** 도장(체인 앵커) API 명세. */
interface AnchorControllerInterface {

    /** 장부의 마지막 지문을 체인에 올린다. */
    @PostMapping("")
    fun anchorNow(): CommonRes<AnchorReceiptRes>

    /** 가장 최근에 찍은 도장의 영수증. */
    @GetMapping("/latest")
    fun getLatest(): CommonRes<AnchorReceiptRes>
}
