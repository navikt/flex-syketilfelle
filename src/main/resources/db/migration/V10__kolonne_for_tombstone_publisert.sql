ALTER TABLE SYKETILFELLEBIT
    ADD COLUMN tombstone_publistert TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS TOMBSTONE_PUBLISERT_INDEX
    ON SYKETILFELLEBIT (slettet, tombstone_publistert);
