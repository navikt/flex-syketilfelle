package no.nav.helse.flex.syketilfelle.syketilfellebit

import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Timestamp

@Transactional
@Repository
class SyketilfellebitBatchInsertDAO(
    private val jdbcTemplate: JdbcTemplate
) {

    fun batchInsert(biter: List<Syketilfellebit>): IntArray {

        val sql = """
INSERT INTO  "syketilfellebit" 
    ("fnr",
    "fom",
    "inntruffet",
    "korrigerer_sendt_soknad",
    "opprettet",
    "orgnummer",
    "ressurs_id",
    "syketilfellebit_id",
    "tags",
    "tom")
VALUES
  (?,?,?,?,?,?,?,?,?,?) ON CONFLICT ON CONSTRAINT syketilfellebit_syketilfellebit_id_key DO NOTHING;"""

        return jdbcTemplate.batchUpdate(
            sql,
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    ps.setString(1, biter[i].fnr)
                    ps.setDate(2, Date.valueOf(biter[i].fom))
                    ps.setTimestamp(3, Timestamp.from(biter[i].inntruffet.toInstant()))
                    ps.setString(4, biter[i].korrigererSendtSoknad)
                    ps.setTimestamp(5, Timestamp.from(biter[i].opprettet.toInstant()))
                    ps.setString(6, biter[i].orgnummer)
                    ps.setString(7, biter[i].ressursId)
                    ps.setString(8, biter[i].syketilfellebitId)
                    ps.setString(9, biter[i].tags)
                    ps.setDate(10, Date.valueOf(biter[i].tom))
                }

                override fun getBatchSize() = biter.size
            }
        )
    }
}
