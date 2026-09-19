package pod.app.application

import org.springframework.stereotype.Component
import pod.app.application.dto.HeadDto
import pod.app.application.dto.RecordDto
import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Attempt
import pod.app.domain.record.Decision
import pod.app.domain.record.RecordService

@Component
class LedgerFacade(
    private val recordService: RecordService,
) {
    fun addRecord(agent: AgentInfo, attempt: Attempt, decision: Decision, reason: String, rawRequest: String): RecordDto {
        val record = recordService.append(agent, attempt, decision, reason, rawRequest)
        return RecordDto.from(record)
    }

    fun getRecords(): List<RecordDto> {
        return recordService.findAll().map { RecordDto.from(it) }
    }

    fun getRecord(seq: Long): RecordDto? {
        val record = recordService.findBySeq(seq)
        if (record == null) {
            return null
        }
        return RecordDto.from(record)
    }

    fun getHead(): HeadDto? {
        val head = recordService.head()
        if (head == null) {
            return null
        }
        return HeadDto.from(head)
    }
}
