DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'cloudsqliamuser')
        THEN
            REVOKE UPDATE ON syketilfellebit FROM cloudsqliamuser;
        END IF;
    END
$$;
