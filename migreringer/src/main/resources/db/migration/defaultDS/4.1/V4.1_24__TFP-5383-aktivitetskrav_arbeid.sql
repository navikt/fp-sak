create sequence SEQ_GR_AKTIVITETSKRAV_ARBEID
    increment by 50
    nocache;

create sequence SEQ_AKTIVITETSKRAV_ARBEID_PERIODER
    increment by 50
    nocache;

create sequence SEQ_AKTIVITETSKRAV_ARBEID_PERIODE
    increment by 50
    nocache;

create table AKTIVITETSKRAV_ARBEID_PERIODER
(
    OPPRETTET_TID TIMESTAMP(3) default systimestamp not null,
    OPPRETTET_AV VARCHAR2(20 char) default 'VL' not null,
    ID NUMBER(19) not null
        constraint PK_AKTIVITETSKRAV_ARBEID_PERIODER
        primary key
)
    /

comment on table AKTIVITETSKRAV_ARBEID_PERIODER is 'Aggregering av informasjon om aktivitetskrav arbeid'
/

comment on column AKTIVITETSKRAV_ARBEID_PERIODER.ID is 'Primærnøkkel'
/

create table GR_AKTIVITETSKRAV_ARBEID
(
    AKTIV VARCHAR2(1 char) default 'J' not null
        constraint GR_AKTIVITETSKRAV_ARBEID
        check (aktiv IN ('J', 'N')),
    ENDRET_TID TIMESTAMP(3),
    ENDRET_AV VARCHAR2(20 char),
    OPPRETTET_TID TIMESTAMP(3) default systimestamp not null,
    OPPRETTET_AV VARCHAR2(20 char) default 'VL' not null,
    AKTIVITETSKRAV_ARBEID_PERIODER_ID NUMBER(19)
        constraint FK_GR_AKTIVITETSKRAV_ARBEID_2
        references AKTIVITETSKRAV_ARBEID_PERIODER,
    BEHANDLING_ID NUMBER(19) not null
        constraint FK_GR_AKTIVITETSKRAV_ARBEID_1
        references BEHANDLING,
    ID NUMBER(19) not null
        constraint PK_GR_AKTIVITETSKRAV_ARBEID
        primary key,
    PERIODE_FOM DATE not null,
    PERIODE_TOM DATE not null
)
    /

comment on table GR_AKTIVITETSKRAV_ARBEID is 'Behandlingsgrunnlag for aktivitetskrav arbeid'
/

comment on column GR_AKTIVITETSKRAV_ARBEID.AKTIV is 'Om valg er aktivt'
/

comment on column GR_AKTIVITETSKRAV_ARBEID.AKTIVITETSKRAV_ARBEID_PERIODER_ID is 'Foreign key til AKTIVITETSKRAV_ARBEID_PERIODER'
/

comment on column GR_AKTIVITETSKRAV_ARBEID.BEHANDLING_ID is 'Foreign key til behandling'
/

comment on column GR_AKTIVITETSKRAV_ARBEID.ID is 'PK'
/

comment on column GR_AKTIVITETSKRAV_ARBEID.PERIODE_FOM is 'Fra dato vi innhenter informasjon om arbeid'
/

comment on column GR_AKTIVITETSKRAV_ARBEID.PERIODE_TOM is 'Til dato vi innhenter informasjon om arbeid'
/

create index IDX_GR_AKTIVITETSKRAV_ARBEID_1
    on GR_AKTIVITETSKRAV_ARBEID (BEHANDLING_ID)
    /

create index IDX_GR_AKTIVITETSKRAV_ARBEID_2
    on GR_AKTIVITETSKRAV_ARBEID (AKTIVITETSKRAV_ARBEID_PERIODER_ID)
    /

create table AKTIVITETSKRAV_ARBEID_PERIODE
(
    OPPRETTET_TID TIMESTAMP(3) default systimestamp not null,
    OPPRETTET_AV VARCHAR2(20 char) default 'VL' not null,
    AKTIVITETSKRAV_ARBEID_PERIODER_ID NUMBER(19) not null
        constraint FK_AKTIVITETSKRAV_ARBEID_PERIODE_1
        references AKTIVITETSKRAV_ARBEID_PERIODER,
    TOM DATE not null,
    FOM DATE not null,
    ORG_NUMMER VARCHAR2(50 char) not null,
    SUM_STILLINGSPROSENT NUMBER(5,2) not null,
    SUM_PERMISJONSPROSENT NUMBER(5,2) not null,
    ID NUMBER(19) not null
        constraint PK_AKTIVITETSKRAV_ARBEID_PERIODE
        primary key
)
    /

comment on table AKTIVITETSKRAV_ARBEID_PERIODE is 'Perioden vi henter informasjon om arbeidsforholdet ved aktivitetskrav arbeid'
/

comment on column AKTIVITETSKRAV_ARBEID_PERIODE.AKTIVITETSKRAV_ARBEID_PERIODER_ID is 'FK: mot aggregat'
/

comment on column AKTIVITETSKRAV_ARBEID_PERIODE.FOM is 'Første dato vi henter informasjon om arbeidsforholdet for'
/

comment on column AKTIVITETSKRAV_ARBEID_PERIODE.TOM is 'Siste dato vi henter informasjon om arbeidsforholdet for'
/

comment on column AKTIVITETSKRAV_ARBEID_PERIODE.ORG_NUMMER is 'Virksomhetens organisasjonsnummer'
/

comment on column AKTIVITETSKRAV_ARBEID_PERIODE.SUM_STILLINGSPROSENT is 'Sum stillingsprosent registrert for personen i virksomheten i aa-reg i perioden'
/

comment on column AKTIVITETSKRAV_ARBEID_PERIODE.SUM_PERMISJONSPROSENT is 'Sum permisjonsprosent registrert for personen i virksomheten i aa-reg i perioden'
/

comment on column AKTIVITETSKRAV_ARBEID_PERIODE.ID is 'Primærnøkkel'
/

create index IDX_AKTIVITETSKRAV_ARBEID_PERIODE_1
    on AKTIVITETSKRAV_ARBEID_PERIODE (AKTIVITETSKRAV_ARBEID_PERIODER_ID)
    /
