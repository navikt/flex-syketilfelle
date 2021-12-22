package no.nav.helse.flex.syketilfelle.syketilfellebit

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SyketilfellebitRepository : CrudRepository<Syketilfellebit, String> {
    fun existsBySyketilfellebitId(syketilfellebitId: String): Boolean
    fun findByRessursId(ressursId: String): List<Syketilfellebit>
}
