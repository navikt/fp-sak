create table HISTORIKKINNSLAG2
(
    ID             NUMBER(19)                             not null
        constraint PK_HISTORIKKINNSLAG2
            primary key,
    FAGSAK_ID      NUMBER(19)                             not null
        constraint FK_HISTORIKKINNSLAG2_01 references FAGSAK,
    BEHANDLING_ID  NUMBER(19)
        constraint FK_HISTORIKKINNSLAG2_02 references BEHANDLING,
    AKTOER         VARCHAR2(100 char)                     not null,
    SKJERMLENKE    VARCHAR2(100 char),
    TITTEL         VARCHAR2(1000 char),
    OPPRETTET_AV   VARCHAR2(20 char) default 'VL'         not null,
    OPPRETTET_TID  TIMESTAMP(3)      default systimestamp not null,
    MIGRERT_FRA_ID NUMBER(19)
        constraint FK_HISTORIKKINNSLAG2_03 references HISTORIKKINNSLAG
);

comment on table HISTORIKKINNSLAG2 is 'Historikk over hendelser i saken';
comment on column HISTORIKKINNSLAG2.ID is 'PK';
comment on column HISTORIKKINNSLAG2.FAGSAK_ID is 'FK fagsak';
comment on column HISTORIKKINNSLAG2.BEHANDLING_ID is 'FK behandling';
comment on column HISTORIKKINNSLAG2.AKTOER is 'Hvilken aktoer';
comment on column HISTORIKKINNSLAG2.SKJERMLENKE is 'Skjermlenke til endring i saken';
comment on column HISTORIKKINNSLAG2.TITTEL is 'Tittel';
comment on column HISTORIKKINNSLAG2.MIGRERT_FRA_ID is 'Temp kolonne til bruk i migrering. Kan fjernes etter migrering fra tidligere tabell er utfort';

create index IDX_HISTORIKKINNSLAG2_01
    on HISTORIKKINNSLAG2 (BEHANDLING_ID);

create index IDX_HISTORIKKINNSLAG2_02
    on HISTORIKKINNSLAG2 (FAGSAK_ID);

create index IDX_HISTORIKKINNSLAG2_03
    on HISTORIKKINNSLAG2 (MIGRERT_FRA_ID);

create sequence SEQ_HISTORIKKINNSLAG2
    minvalue 1000000
    increment by 50
    nocache;

create table HISTORIKKINNSLAG2_LINJE
(
    ID                  NUMBER(19)                             not null
        constraint PK_HISTORIKKINNSLAG2_LINJE
            primary key,
    HISTORIKKINNSLAG_ID NUMBER(19)                             not null
        constraint FK_HISTORIKKINNSLAG2_LINJE_1 references HISTORIKKINNSLAG2,
    TYPE                VARCHAR2(100 char)                     not null,
    TEKST               VARCHAR2(4000 char),
    SEKVENS_NR          NUMBER(5)                              not null,
    OPPRETTET_AV        VARCHAR2(20 char) default 'VL'         not null,
    OPPRETTET_TID       TIMESTAMP(3)      default systimestamp not null
);

comment on table HISTORIKKINNSLAG2_LINJE is 'Linjer i historikkinnslag';
comment on column HISTORIKKINNSLAG2_LINJE.ID is 'PK';
comment on column HISTORIKKINNSLAG2_LINJE.TEKST is 'Innholdet. Forklarer hva som har skjedd i saken';
comment on column HISTORIKKINNSLAG2_LINJE.SEKVENS_NR is 'Rekkefølge på linjer innad historikkinnslaget';
comment on column HISTORIKKINNSLAG2_LINJE.TYPE is 'Type linje';

create index IDX_HISTORIKKINNSLAG2_LINJE_01
    on HISTORIKKINNSLAG2_LINJE (HISTORIKKINNSLAG_ID);

create sequence SEQ_HISTORIKKINNSLAG2_LINJE
    minvalue 1000000
    increment by 50
    nocache;

create table HISTORIKKINNSLAG2_DOK_LINK
(
    ID                  NUMBER(19)                             not null
        constraint PK_HISTORIKKINNSLAG2_DOK_LINK
            primary key,
    LINK_TEKST          VARCHAR2(100 char)                     not null,
    HISTORIKKINNSLAG_ID NUMBER(19)                             not null
        constraint FK_HISTORIKKINNSLAG2_DOK_LINK_01
            references HISTORIKKINNSLAG2,
    JOURNALPOST_ID      VARCHAR2(100 char),
    DOKUMENT_ID         VARCHAR2(100 char),
    OPPRETTET_AV        VARCHAR2(20 char) default 'VL'         not null,
    OPPRETTET_TID       TIMESTAMP(3)      default systimestamp not null
);

create sequence SEQ_HISTORIKKINNSLAG2_DOK_LINK
    minvalue 1000000
    increment by 50
    nocache;

comment on table HISTORIKKINNSLAG2_DOK_LINK is 'Kobling fra historikkinnslag til aktuell dokumentasjon';

comment on column HISTORIKKINNSLAG2_DOK_LINK.ID is 'Primary Key';

comment on column HISTORIKKINNSLAG2_DOK_LINK.LINK_TEKST is 'Tekst som vises for link til dokumentet';

comment on column HISTORIKKINNSLAG2_DOK_LINK.HISTORIKKINNSLAG_ID is 'FK:HISTORIKKINNSLAG2 Fremmednøkkel til riktig innslag i historikktabellen';

comment on column HISTORIKKINNSLAG2_DOK_LINK.JOURNALPOST_ID is 'Journalpost id';

comment on column HISTORIKKINNSLAG2_DOK_LINK.DOKUMENT_ID is 'Dokument id';

create index IDX_HISTINNSLAG2_DOK_LINK_01
    on HISTORIKKINNSLAG2_DOK_LINK (HISTORIKKINNSLAG_ID)
