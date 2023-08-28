ALTER TABLE TILRETTELEGGING_FOM add KILDE VARCHAR2(50 CHAR);
COMMENT ON COLUMN TILRETTELEGGING_FOM.KILDE IS 'Gir informasjon om hvordan tilrettegging-fra-datoen ble opprettet eller endret';
