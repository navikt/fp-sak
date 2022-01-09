-- Utvide rettighet med oppgitt verdi
ALTER TABLE SO_RETTIGHET ADD MOR_UFORETRYGD VARCHAR2(1 CHAR) DEFAULT 'N' ;
COMMENT ON COLUMN SO_RETTIGHET.MOR_UFORETRYGD IS 'Oppgitt at mor mottar uføretrygd';

-- Tabell for å lagre bekreftet uføretrygd - fra Pesys eller aksjonspunkt
CREATE SEQUENCE SEQ_GR_UFORETRYGD MINVALUE 1 INCREMENT BY 50 START WITH 1 NOCACHE  NOCYCLE ;

CREATE TABLE GR_UFORETRYGD (
    id NUMBER(19,0) NOT NULL
        constraint PK_GR_UFORETRYGD
            primary key,
    behandling_id NUMBER(19,0) NOT NULL,
    aktoer_id VARCHAR2(50 char),
    register_ufore VARCHAR2(1 CHAR),
    uforedato DATE,
    virkningsdato DATE,
    overstyrt_ufore VARCHAR2(1 CHAR),
    opprettet_av VARCHAR2(20 CHAR) DEFAULT 'VL' NOT NULL,
    opprettet_tid TIMESTAMP (3) DEFAULT systimestamp NOT NULL,
    endret_av VARCHAR2(20 CHAR),
    endret_tid TIMESTAMP (3),
    aktiv VARCHAR2(1 CHAR) DEFAULT 'J' NOT NULL
);

ALTER TABLE GR_UFORETRYGD ADD CONSTRAINT FK_GR_UFORETRYGD_1 FOREIGN KEY (behandling_id) REFERENCES BEHANDLING(id);
ALTER TABLE GR_UFORETRYGD ADD CONSTRAINT GR_UFORETRYGD_AKTIV CHECK (aktiv IN ('J', 'N'));

COMMENT ON TABLE GR_UFORETRYGD IS 'Bekreftet grunnlag for 14-14 mor mottar uføretrygd';

COMMENT ON COLUMN GR_UFORETRYGD.id IS 'PK';
COMMENT ON COLUMN GR_UFORETRYGD.behandling_id IS 'Foreign key til behandling';
comment on column GR_UFORETRYGD.AKTOER_ID is 'Aktørid fra PDL';
COMMENT ON COLUMN GR_UFORETRYGD.register_ufore IS 'Registrert uføretrygd i fagsystem';
COMMENT ON COLUMN GR_UFORETRYGD.uforedato IS 'Uføredato';
COMMENT ON COLUMN GR_UFORETRYGD.virkningsdato IS 'Virkningsdato for uføretrygd';
COMMENT ON COLUMN GR_UFORETRYGD.overstyrt_ufore IS 'Saksbehandlervurdering uføretrygd';
COMMENT ON COLUMN GR_UFORETRYGD.aktiv IS 'Om valg er aktivt';

CREATE INDEX IDX_GR_UFORETRYGD_1 ON GR_UFORETRYGD (BEHANDLING_ID);

-- Sikre idempotens ved gjentatt mottak av vedtakshendelse
create sequence SEQ_MOTTATT_VEDTAK increment by 50 START WITH 1 NOCACHE  NOCYCLE ;

create table MOTTATT_VEDTAK (
    ID NUMBER(19) not null
        constraint PK_MOTTATT_VEDTAK
            primary key,
    SAKSNUMMER VARCHAR2(19 char) not null,
    FAGSYSTEM VARCHAR2(19 char) not null,
    YTELSE VARCHAR2(19 char) not null,
    REFERANSE VARCHAR2(100 char),
    OPPRETTET_AV VARCHAR2(20 char) default 'VL' not null,
    OPPRETTET_TID TIMESTAMP(3) default systimestamp not null
);

comment on table MOTTATT_VEDTAK is 'Vedtak som er mottatt og håndert - idempotens ved rekonsumering';

comment on column MOTTATT_VEDTAK.SAKSNUMMER is 'FK: Fagsak.SAKSNUMMER';
comment on column MOTTATT_VEDTAK.FAGSYSTEM is 'Eksternt fagsystem som har fattet mottatt vedtak';
comment on column MOTTATT_VEDTAK.YTELSE is 'Ytelse for mottatt vedtak';
comment on column MOTTATT_VEDTAK.REFERANSE is 'Referanse UUID el til mottatt vedtak';

create index IDX_MOTTATT_VEDTAK_01 on MOTTATT_VEDTAK (REFERANSE);

-- div rydding
DROP SEQUENCE SEQ_ADOPSJON_BARN;
