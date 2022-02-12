ALTER TABLE UTTAK_RESULTAT_PERIODE MODIFY (kl_periode_resultat_aarsak null);

-- Tabell for å lagre informasjon om neste stønadsperiode
CREATE SEQUENCE SEQ_GR_NESTESAK MINVALUE 1 INCREMENT BY 50 START WITH 1 NOCACHE  NOCYCLE ;

CREATE TABLE GR_NESTESAK (
    id NUMBER(19,0) NOT NULL
        constraint PK_GR_NESTESAK
            primary key,
    behandling_id NUMBER(19,0) NOT NULL,
    SAKSNUMMER VARCHAR2(19 char) not null,
    STARTDATO DATE not null,
    opprettet_av VARCHAR2(20 CHAR) DEFAULT 'VL' NOT NULL,
    opprettet_tid TIMESTAMP (3) DEFAULT systimestamp NOT NULL,
    endret_av VARCHAR2(20 CHAR),
    endret_tid TIMESTAMP (3),
    aktiv VARCHAR2(1 CHAR) DEFAULT 'J' NOT NULL
);

ALTER TABLE GR_NESTESAK ADD CONSTRAINT FK_GR_NESTESAK_1 FOREIGN KEY (behandling_id) REFERENCES BEHANDLING(id);
ALTER TABLE GR_NESTESAK ADD CONSTRAINT GR_NESTESAK_AKTIV CHECK (aktiv IN ('J', 'N'));

COMMENT ON TABLE GR_NESTESAK IS 'Informasjon om neste stønadsperiode';

COMMENT ON COLUMN GR_NESTESAK.id IS 'PK';
COMMENT ON COLUMN GR_NESTESAK.behandling_id IS 'Foreign key til behandling';
comment on column GR_NESTESAK.saksnummer is 'Saksnummer for neste stønadsperiode';
COMMENT ON COLUMN GR_NESTESAK.startdato IS 'Startdato for neste stønadsperiode';
COMMENT ON COLUMN GR_NESTESAK.aktiv IS 'Om valg er aktivt';

CREATE INDEX IDX_GR_NESTESAK_1 ON GR_NESTESAK (BEHANDLING_ID);
