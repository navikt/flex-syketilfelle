package no.nav.helse.flex.syketilfelle.syketilfellebit

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SyketilfellebitRepository : CrudRepository<SyketilfellebitDbRecord, String> {
    fun findByFnrIn(fnrs: List<String>): List<SyketilfellebitDbRecord>

    fun findByFnr(fnr: String): List<SyketilfellebitDbRecord>

    fun findByRessursId(ressursId: String): List<SyketilfellebitDbRecord>

    fun findAllByRessursIdIn(ressursIds: List<String>): List<SyketilfellebitDbRecord>

    fun findFirst300ByPublisertOrderByOpprettetAsc(publisert: Boolean): List<SyketilfellebitDbRecord>

    fun findFirst300ByTombstonePublistertIsNullAndSlettetIsNotNullOrderByOpprettetAsc(): List<SyketilfellebitDbRecord>

    @Modifying
    @Query("delete from Syketilfellebit s where s.fnr = :fnr")
    fun deleteByFnr(fnr: String): Long
}
