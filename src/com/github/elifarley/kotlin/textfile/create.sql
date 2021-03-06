CREATE TABLE REPORT_LOG (
	CREATED datetime2(0) NOT NULL CONSTRAINT DF_RL_CREATED DEFAULT sysdatetime(),
	REPORT_NAME varchar(60) NOT NULL,
	LAST_SEQUENCE_NUMBER INT NOT NULL, -- -1: skip report; -2 or less: error line
	LAST_DATE_PARAM DATE NOT NULL,
	MD5 BINARY(16) NOT NULL,
	ELAPSED_MILLIS INT NOT NULL,
	ITEM_COUNT INT NOT NULL,
	ITEMS_PER_SECOND AS CASE WHEN ELAPSED_MILLIS = 0 OR ITEM_COUNT = 0 THEN NULL ELSE 1000.0 * ITEM_COUNT / ELAPSED_MILLIS END,
	MD5_HEX as lower(convert(char(32), MD5, 2)),
	CONSTRAINT PK_REPORT_LOG PRIMARY KEY (CREATED DESC),
	CONSTRAINT U_RL_NAME_CREATED UNIQUE (REPORT_NAME, CREATED DESC),
	CONSTRAINT CH_RL_LSN CHECK (LAST_SEQUENCE_NUMBER BETWEEN -999 and 9999999),
	CONSTRAINT CH_RL_ELAPSED_MILLIS CHECK (ELAPSED_MILLIS >= 0),
	CONSTRAINT CH_RL_ITEM_COUNT CHECK (ITEM_COUNT BETWEEN -999 and 9999999)
)
CREATE NONCLUSTERED INDEX I_RL_MD5 ON REPORT_LOG (MD5_HEX)
