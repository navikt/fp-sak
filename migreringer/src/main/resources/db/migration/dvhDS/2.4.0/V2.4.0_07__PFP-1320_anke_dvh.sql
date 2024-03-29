------------------------ ANKE_VURDERING_RESULTAT_DVH -----------------------------------------------------

CREATE TABLE ANKE_VURDERING_RESULTAT_DVH (
    TRANS_ID                              NUMBER(19,0) NOT NULL,
    ANKE_BEHANDLING_ID                    NUMBER(19,0) NOT NULL,
    ANKEVURDERING                         VARCHAR2(100) NOT NULL,
    ANKE_OMGJOER_AARSAK                   VARCHAR2(100),
    ANKE_VURDERING_OMGJOER                VARCHAR2(100 char),
    ER_SUBSIDIART_REALITET_BEH            VARCHAR2(1 CHAR) DEFAULT '0' NOT NULL,
    GJELDER_VEDTAK                        VARCHAR2(1 CHAR) NOT NULL,
    ER_ANKER_IKKE_PART                    VARCHAR2(1 CHAR) NOT NULL,
    ER_FRIST_IKKE_OVERHOLDT               VARCHAR2(1 CHAR) NOT NULL,
    ER_IKKE_KONKRET                       VARCHAR2(1 CHAR) NOT NULL,
    ER_IKKE_SIGNERT                       VARCHAR2(1 CHAR) NOT NULL,
    ER_MERKNADER_MOTTATT                  VARCHAR2(1 CHAR) NOT NULL,
    OPPRETTET_TID                         TIMESTAMP(3) DEFAULT systimestamp NOT NULL,
    TRANS_TID                             TIMESTAMP(3)       NOT NULL,
    FUNKSJONELL_TID                       TIMESTAMP(3)       NOT NULL,
    ENDRET_AV                             VARCHAR2(20 CHAR)
);

ALTER TABLE ANKE_VURDERING_RESULTAT_DVH ADD CONSTRAINT PK_ANKE_VURDERING_RES_DVH PRIMARY KEY ( TRANS_ID ) ;
CREATE SEQUENCE SEQ_ANKE_VURDERING_RES_DVH MINVALUE 1 START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;
GRANT SELECT on SEQ_ANKE_VURDERING_RES_DVH to FPSAK_HIST_SKRIVE_ROLE;
GRANT ALL ON ANKE_VURDERING_RESULTAT_DVH TO FPSAK_HIST_SKRIVE_ROLE;
CREATE INDEX IDX_ANKE_VURDERING_RES_DVH_1 on ANKE_VURDERING_RESULTAT_DVH(ANKE_BEHANDLING_ID);


COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.TRANS_ID IS 'Primærnøkkel';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.ANKE_BEHANDLING_ID IS 'referanse til ankebehandlingen';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.ANKEVURDERING IS 'ankevurdering';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.ANKE_OMGJOER_AARSAK IS 'Årsak til omgjør';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.ANKE_VURDERING_OMGJOER IS 'Type omgjøring av anken';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.GJELDER_VEDTAK IS 'gjelder vedtak';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.ER_ANKER_IKKE_PART IS 'er anker ikke part i saken';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.ER_FRIST_IKKE_OVERHOLDT IS 'er frist ikke overholdt';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.ER_IKKE_KONKRET IS 'Er anke ikke konkret';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.ER_IKKE_SIGNERT IS 'Er anke ikke signert';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.ER_SUBSIDIART_REALITET_BEH IS 'Er subsidiært realitet behandles';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.OPPRETTET_TID IS 'Opprettettidspunkt.';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.TRANS_TID IS 'Timestamp som forteller nå transaksjonen inntraff.';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.FUNKSJONELL_TID IS 'Et tidsstempel når transaksjonen er funksjonelt gyldig fra.';
COMMENT ON COLUMN ANKE_VURDERING_RESULTAT_DVH.ENDRET_AV IS 'Opprettet_av eller endret_av i VL'
