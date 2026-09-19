package pod.app.interfaces.record.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pod.app.application.LedgerFacade
import pod.app.interfaces.common.CommonRes
import pod.app.interfaces.exception.ApiException
import pod.app.interfaces.exception.ExceptionCode
import pod.app.interfaces.record.req.AddRecordReq
import pod.app.interfaces.record.res.HeadRes
import pod.app.interfaces.record.res.RecordRes

@RestController
@RequestMapping("/api/v1/ledger")
class RecordController(
    private val ledgerFacade: LedgerFacade,
) : RecordControllerInterface {

    override fun addRecord(req: AddRecordReq): CommonRes<RecordRes> {
        val dto = ledgerFacade.addRecord(
            agent = req.agent.toDomain(),
            attempt = req.attempt.toDomain(),
            decision = req.decision,
            reason = req.reason,
            rawRequest = req.rawRequest,
        )
        return CommonRes.success(RecordRes.from(dto))
    }

    override fun getRecords(): CommonRes<List<RecordRes>> {
        val records = ledgerFacade.getRecords().map { RecordRes.from(it) }
        return CommonRes.success(records)
    }

    override fun getRecord(seq: Long): CommonRes<RecordRes> {
        val dto = ledgerFacade.getRecord(seq)
        if (dto == null) {
            throw ApiException(ExceptionCode.RECORD_NOT_FOUND, "seq=$seq")
        }
        return CommonRes.success(RecordRes.from(dto))
    }

    override fun getHead(): CommonRes<HeadRes> {
        val dto = ledgerFacade.getHead()
        if (dto == null) {
            throw ApiException(ExceptionCode.LEDGER_EMPTY)
        }
        return CommonRes.success(HeadRes.from(dto))
    }
}
