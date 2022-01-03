package no.nav.helse.flex.syketilfelle.syketilfellebit

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SyketilfellebitRepository : CrudRepository<Syketilfellebit, String> {
    fun existsBySyketilfellebitId(syketilfellebitId: String): Boolean
    fun findBySyketilfellebitId(syketilfellebitId: String): Syketilfellebit?
    @Modifying
    @Query("delete from Syketilfellebit s where s.fnr = :fnr")
    fun deleteByFnr(fnr: String): Long
}
