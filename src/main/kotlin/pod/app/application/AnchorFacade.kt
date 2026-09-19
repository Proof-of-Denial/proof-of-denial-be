package pod.app.application

import org.springframework.stereotype.Component
import pod.app.application.dto.AnchorReceiptDto
import pod.app.domain.anchor.AnchorService

@Component
class AnchorFacade(
    private val anchorService: AnchorService,
) {
    fun anchorNow(): AnchorReceiptDto {
        val receipt = anchorService.anchorNow()
        return AnchorReceiptDto.from(receipt)
    }

    fun latest(): AnchorReceiptDto? {
        val receipt = anchorService.latest()
        if (receipt == null) {
            return null
        }
        return AnchorReceiptDto.from(receipt)
    }
}
