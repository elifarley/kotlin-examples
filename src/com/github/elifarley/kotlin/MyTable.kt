package com.orgecc.myproj.model

import javax.persistence.*
import java.math.BigDecimal
import java.util.Date

// Insert rows via stored procedure

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

@NamedStoredProcedureQueries(
        NamedStoredProcedureQuery(name = "myTableUpsert", procedureName = "my_table_upsert",
                parameters = arrayOf(
                        StoredProcedureParameter(mode = ParameterMode.IN, name = "fk_type_id", type = Integer::class),
                        StoredProcedureParameter(mode = ParameterMode.IN, name = "date", type = Date::class),
                        StoredProcedureParameter(mode = ParameterMode.IN, name = "value", type = BigDecimal::class)
                )
        )
)
@Entity
@Table(name = "MY_TABLE")
class MyTable {

    @Id
    @Column(name = "CREATED", updatable = false, insertable = false, nullable = false, columnDefinition = "DATETIME")
    @GeneratedValue(strategy = GenerationType.AUTO)
    var created: Date? = null

}

// ------------
// Repository
// ------------

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.query.Procedure
import org.springframework.data.repository.query.Param

import java.math.BigDecimal
import java.util.Date

interface MyTableRepository : JpaRepository<MyTable, Date> {

    @Procedure(name = "myTableUpsert")
    fun upsert(@Param("fk_type_id") fk_type_id: Integer, @Param("date") date: Date, @Param("value") value: BigDecimal)

}
