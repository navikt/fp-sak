CREATE TABLE KODEVERDI_NAVN (
        KODEVERK       VARCHAR2(200 char) NOT NULL,
        KODE           VARCHAR2(200 char) NOT NULL,
        NAVN           VARCHAR2(500 char),
        OPPRETTET_TID  TIMESTAMP(3) DEFAULT systimestamp NOT NULL,
        ENDRET_TID     TIMESTAMP(3),
        CONSTRAINT PK_KODEVERDI_NAVN PRIMARY KEY (KODEVERK, KODE)
);

COMMENT ON TABLE KODEVERDI_NAVN  IS 'En tabell over kodeverdier med navn (join i uttrekk)  - autogenereres fra FPSAK';
COMMENT ON COLUMN KODEVERDI_NAVN.KODEVERK IS 'Primærnøkkel Kodeverk';
COMMENT ON COLUMN KODEVERDI_NAVN.KODE IS 'Primærnøkkel aktuell kode';
COMMENT ON COLUMN KODEVERDI_NAVN.NAVN IS 'Beskrivende tittel for kodeverdi';
COMMENT ON COLUMN KODEVERDI_NAVN.OPPRETTET_TID IS 'Opprettet tidspunkt';
COMMENT ON COLUMN KODEVERDI_NAVN.ENDRET_TID IS 'Tidspunkt ved endring av steg eller navn';
