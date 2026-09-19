package pod.app.interfaces.anchor.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pod.app.application.AnchorFacade
import pod.app.interfaces.anchor.res.AnchorReceiptRes
import pod.app.interfaces.common.CommonRes
import pod.app.interfaces.exception.ApiException
import pod.app.interfaces.exception.ExceptionCode

@RestController
@RequestMapping("/api/v1/anchor")
class AnchorController(
    private val anchorFacade: AnchorFacade,
) : AnchorControllerInterface {

    override fun anchorNow(): CommonRes<AnchorReceiptRes> {
        val dto = anchorFacade.anchorNow()
        return CommonRes.success(AnchorReceiptRes.from(dto))
    }

    override fun getLatest(): CommonRes<AnchorReceiptRes> {
        val dto = anchorFacade.latest()
        if (dto == null) {
            throw ApiException(ExceptionCode.ANCHOR_NOT_FOUND)
        }
        return CommonRes.success(AnchorReceiptRes.from(dto))
    }
}
