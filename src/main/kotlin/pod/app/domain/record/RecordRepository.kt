package pod.app.domain.record

/** 장부. 붙이기만 하고 고치거나 지우는 기능은 일부러 없다. */
interface RecordRepository {
    fun findAll(): List<Record>
    fun save(record: Record)
}
