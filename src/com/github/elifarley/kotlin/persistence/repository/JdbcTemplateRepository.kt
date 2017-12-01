package com.orgecc.persistence.repository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
open class JdbcTemplateRepository @Autowired constructor(dataSource: DataSource) {

    private val jdbcTemplate: JdbcTemplate = JdbcTemplate(dataSource)

    private fun delete(from: String) = jdbcTemplate.update("DELETE FROM $from")

    open fun deleteMyTable() = delete("MY_TABLE")

    open fun execMyProc(val1: Long, val2: Int) = jdbcTemplate.query("exec [my-proc] $val1, $val2", ResultSetExtractor<Int> {
        it.next()
        it.getInt(1)
    })

}
