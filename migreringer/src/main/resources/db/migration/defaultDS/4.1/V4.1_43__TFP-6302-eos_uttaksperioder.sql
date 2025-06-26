CREATE TABLE EOS_UTTAKSPERIODER
(
    ID            NUMBER(19, 0)                          NOT NULL PRIMARY KEY,
    OPPRETTET_AV  VARCHAR2(20 char) default 'VL'         not null,
    OPPRETTET_TID TIMESTAMP(3)      default systimestamp not null
);

CREATE TABLE EOS_UTTAKSPERIODE
(
    ID                    NUMBER(19, 0)                          NOT NULL PRIMARY KEY,
    EOS_UTTAKSPERIODER_ID NUMBER(19, 0)                          NOT NULL,
    FOM                   DATE                                   NOT NULL,
    TOM                   DATE                                   NOT NULL,
    TREKKONTO             VARCHAR2(100 CHAR)                     NOT NULL,
    TREKKDAGER            NUMBER(4, 1)                           NOT NULL,
    OPPRETTET_AV          VARCHAR2(20 char) default 'VL'         not null,
    OPPRETTET_TID         TIMESTAMP(3)      default systimestamp not null,
    CONSTRAINT FK_EOS_UTTAKSPERIODER_1 FOREIGN KEY (EOS_UTTAKSPERIODER_ID) REFERENCES EOS_UTTAKSPERIODER (ID)
);

CREATE TABLE GR_EOS_UTTAK
(
    ID                        NUMBER(19, 0)                                NOT NULL PRIMARY KEY,
    AKTIV                     VARCHAR2(1 char) CHECK (aktiv IN ('J', 'N')) NOT NULL,
    BEHANDLING_ID             NUMBER(19, 0)                                NOT NULL,
    SAKSBEHANDLER_PERIODER_ID NUMBER(19, 0),
    OPPRETTET_AV              VARCHAR2(20 char) default 'VL'               not null,
    OPPRETTET_TID             TIMESTAMP(3)      default systimestamp       not null,
    ENDRET_AV                 VARCHAR2(20 char),
    ENDRET_TID                TIMESTAMP(3),
    CONSTRAINT FK_GR_EOS_UTTAK_1 FOREIGN KEY (SAKSBEHANDLER_PERIODER_ID) REFERENCES EOS_UTTAKSPERIODER (ID)
);

-- Add sequences for ID generation (if needed)
CREATE SEQUENCE SEQ_EOS_UTTAKSPERIODER INCREMENT BY 50;
CREATE SEQUENCE SEQ_EOS_UTTAKSPERIODE INCREMENT BY 50;
CREATE SEQUENCE SEQ_GR_EOS_UTTAK INCREMENT BY 50;

-- Indexes for performance optimization
CREATE INDEX IDX_GR_EOS_UTTAK_1 ON GR_EOS_UTTAK (BEHANDLING_ID);
CREATE INDEX IDX_GR_EOS_UTTAKSPERIODE_1 ON EOS_UTTAKSPERIODE (EOS_UTTAKSPERIODER_ID);

-- Comments for clarity
COMMENT ON TABLE EOS_UTTAKSPERIODER IS 'Tabell for å lagre periodene for annenparts uttak i EØS';
COMMENT ON COLUMN EOS_UTTAKSPERIODER.ID IS 'Unik identifikator for uttaksperioder';

COMMENT ON TABLE EOS_UTTAKSPERIODE IS 'Tabell for å lagre hver enkel period for annenparts uttak i EØS';
COMMENT ON COLUMN EOS_UTTAKSPERIODE.ID IS 'Unik identifikator for uttaksperiode';
COMMENT ON COLUMN EOS_UTTAKSPERIODE.EOS_UTTAKSPERIODER_ID IS 'Referanse til EOS_UTTAKSPERIODER';
COMMENT ON COLUMN EOS_UTTAKSPERIODE.FOM IS 'Fra og med dato';
COMMENT ON COLUMN EOS_UTTAKSPERIODE.TOM IS 'Til og med dato';
COMMENT ON COLUMN EOS_UTTAKSPERIODE.TREKKONTO IS 'Konto som trekkes';
COMMENT ON COLUMN EOS_UTTAKSPERIODE.TREKKDAGER IS 'Antall dager som trekkes';

COMMENT ON TABLE GR_EOS_UTTAK IS 'Tabell for å lagre annenparts uttak i EØS';
COMMENT ON COLUMN GR_EOS_UTTAK.ID IS 'Unik identifikator for GR_EOS_UTTAK';
COMMENT ON COLUMN GR_EOS_UTTAK.ID IS 'Unik identifikator for GR_EOS_UTTAK';
COMMENT ON COLUMN GR_EOS_UTTAK.AKTIV IS 'Om uttaket er aktivt (J/N)';
COMMENT ON COLUMN GR_EOS_UTTAK.BEHANDLING_ID IS 'Referanse til behandling';
COMMENT ON COLUMN GR_EOS_UTTAK.SAKSBEHANDLER_PERIODER_ID IS 'Referanse til saksbehandlerens perioder';
