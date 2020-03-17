ALTER TABLE AKSJONSPUNKT_DVH ADD "FRIST_TID" TIMESTAMP (3);
ALTER TABLE AKSJONSPUNKT_DVH ADD "VENT_AARSAK" VARCHAR2(100 CHAR);

COMMENT ON COLUMN "AKSJONSPUNKT_DVH"."FRIST_TID" IS 'Behandling blir automatisk gjenopptatt etter dette tidspunktet';
COMMENT ON COLUMN "AKSJONSPUNKT_DVH"."VENT_AARSAK" IS 'Årsak for at behandling er satt på vent';

CREATE TABLE AKSJONSPUNKT_DEF_DVH (
                AKSJONSPUNKT_DEF  VARCHAR2(50 char) not null,
                AKSJONSPUNKT_TYPE varchar2(50 char) not null,
                AKSJONSPUNKT_NAVN varchar2(500 char),
                opprettet_tid     TIMESTAMP(3) DEFAULT systimestamp NOT NULL,
                endret_tid        TIMESTAMP(3),
                CONSTRAINT PK_AKSJONSPUNKT_DEF PRIMARY KEY (AKSJONSPUNKT_DEF)
);
GRANT ALL ON AKSJONSPUNKT_DEF_DVH TO FPSAK_HIST_SKRIVE_ROLE;

COMMENT ON TABLE AKSJONSPUNKT_DEF_DVH  IS 'En tabell over aksjonspunktkoder som autogenereres fra FPSAK.';
COMMENT ON COLUMN AKSJONSPUNKT_DEF_DVH.AKSJONSPUNKT_DEF IS 'Primærnøkkel Aksjonspunkt_def i Aksjonspunkt_DVH';
COMMENT ON COLUMN AKSJONSPUNKT_DEF_DVH.AKSJONSPUNKT_TYPE IS 'Type aksjonspunkt';
COMMENT ON COLUMN AKSJONSPUNKT_DEF_DVH.AKSJONSPUNKT_NAVN IS 'Beskrivende tittel for aksjonspunkt';
COMMENT ON COLUMN AKSJONSPUNKT_DEF_DVH.opprettet_tid IS 'Opprettet tidspunkt';
COMMENT ON COLUMN AKSJONSPUNKT_DEF_DVH.endret_tid IS 'Tidspunkt ved endring av steg eller navn';
