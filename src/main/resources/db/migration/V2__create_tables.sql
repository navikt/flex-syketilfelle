CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE SYKETILFELLEBIT
(
    ID                      VARCHAR(36) DEFAULT UUID_GENERATE_V4() PRIMARY KEY,
    SYKETILFELLEBIT_ID      VARCHAR(36)              NOT NULL UNIQUE,
    FNR                     VARCHAR(11)              NOT NULL,
    OPPRETTET               TIMESTAMP WITH TIME ZONE NOT NULL,
    INNTRUFFET              TIMESTAMP WITH TIME ZONE NOT NULL,
    ORGNUMMER               VARCHAR(9)               NULL,
    TAGS                    VARCHAR(100)             NOT NULL,
    RESSURS_ID              VARCHAR(36)              NOT NULL,
    KORRIGERER_SENDT_SOKNAD VARCHAR(36)              NULL,
    FOM                     DATE                     NULL,
    TOM                     DATE                     NULL
);


CREATE INDEX BIT_FNR_INDEX
    ON SYKETILFELLEBIT (FNR);
