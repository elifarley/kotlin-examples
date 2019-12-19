package com.orgecc.myproj.model

import javax.persistence.*
import java.math.BigDecimal
import java.util.Date

// See https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.stored-procedures

// Insert rows via stored procedure using Spring Data

/*
MS-SQL:

CREATE TABLE dbo.MY_TABLE (
	CREATED datetime NOT NULL CONSTRAINT DF_CREATED DEFAULT CURRENT_TIMESTAMP,
	UPDATED datetime NOT NULL CONSTRAINT DF_UPDATED DEFAULT CURRENT_TIMESTAMP,
	fk_type_id int NOT NULL FOREIGN KEY REFERENCES FK_TYPE(ID),
	date date NOT NULL,
	value numeric(9,4) NOT NULL,
    CONSTRAINT PK_MY_TABLE PRIMARY KEY (CREATED)
);

CREATE PROCEDURE [my_table_upsert]
    @fk_type_id int
  , @date date = CURRENT_TIMESTAMP
  , @value numeric(9,4) = 0
AS begin
SET NOCOUNT ON;
MERGE INTO dbo.MY_TABLE WITH (HOLDLOCK) AS t
USING
  (SELECT @fk_type_id, @date) AS s (fk_type_id, date)
    ON  t.fk_type_id = s.fk_type_id
    and t.date = s.date
WHEN MATCHED THEN
    UPDATE SET value = @value, UPDATED = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (fk_type_id, date, value)
    VALUES (s.fk_type_id, s.date, @value);
    
SET NOCOUNT OFF;
end;
*/

// ------------
// Model
// ------------

@NamedStoredProcedureQueries(
        NamedStoredProcedureQuery(name = "myTableUpsert", procedureName = "my_table_upsert",
                parameters = arrayOf(
                        StoredProcedureParameter(mode = ParameterMode.IN, name = "fk_type_id", type = Integer::class),
                        StoredProcedureParameter(mode = ParameterMode.IN, name = "date", type = Date::class),
                        StoredProcedureParameter(mode = ParameterMode.IN, name = "value", type = BigDecimal::class)
                )
        )
)
@Entity class MyTable (@Id var dummy: Serializable? = null)

// ------------
// Repository
// ------------

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.query.Procedure
import org.springframework.data.repository.query.Param

import java.math.BigDecimal
import java.util.Date

interface MyTableRepository : JpaRepository<MyTable, Serializable> {

    @Procedure(name = "myTableUpsert")
    fun upsert(@Param("fk_type_id") fk_type_id: Integer, @Param("date") date: Date, @Param("value") value: BigDecimal)

    @Query(nativeQuery = true, value = """
select top 1 value from my_table
where my_fk_id=1 and date <= :date
order by date desc
""")
    fun findValueForDate(@Param("date") date: Date): BigDecimal?

}


// ------------
// Service
// ------------

@Service
open class MyTableService
@Autowired
constructor(private val myTableRepository: MyTableRepository) {

    companion object : WithLogging() {}

    @Transactional
    open fun upsert(fk_type_id: Int, valueAtDate: MyParser.ValueAtDate) {

        var date = valueAtDate.date
        date = date.withDayOfMonth(1)
        LOG.info("[indexId: $indexId; $valueAtDate]")
        myTableRepository.upsert(fk_type_id as Integer, java.sql.Date.valueOf(date), valueAtDate.value)
    }

    open fun findValueForDate(date: Date) = myTableRepository.findValueForDate(date) ?: BigDecimal.ZERO!!

}
