create table HISTORIKKINNSLAG2
(
    ID            NUMBER(19)                             not null
        constraint PK_HISTORIKKINNSLAG2
            primary key,
    FAGSAK_ID     NUMBER(19)                             not null
        constraint FK_HISTORIKKINNSLAG2_1 references FAGSAK,
    BEHANDLING_ID NUMBER(19)
        constraint FK_HISTORIKKINNSLAG2_2 references BEHANDLING,
    AKTOER        VARCHAR2(100 char)                     not null,
    SKJERMLENKE   VARCHAR2(100 char),
    TITTEL        VARCHAR2(1000 char),
    OPPRETTET_AV  VARCHAR2(20 char) default 'VL'         not null,
    OPPRETTET_TID TIMESTAMP(3)      default systimestamp not null,
    ENDRET_AV     VARCHAR2(20 char),
    ENDRET_TID    TIMESTAMP(3)
);

comment on table HISTORIKKINNSLAG2 is 'Historikk over hendelser i saken';
comment on column HISTORIKKINNSLAG2.ID is 'PK';
comment on column HISTORIKKINNSLAG2.FAGSAK_ID is 'FK fagsak';
comment on column HISTORIKKINNSLAG2.BEHANDLING_ID is 'FK behandling';
comment on column HISTORIKKINNSLAG2.AKTOER is 'Hvilken aktoer';
comment on column HISTORIKKINNSLAG2.SKJERMLENKE is 'Skjermlenke til endring i saken';
comment on column HISTORIKKINNSLAG2.TITTEL is 'Tittel';

create index IDX_HISTORIKKINNSLAG2_01
    on HISTORIKKINNSLAG2 (BEHANDLING_ID);

create index IDX_HISTORIKKINNSLAG2_02
    on HISTORIKKINNSLAG2 (FAGSAK_ID);

create sequence SEQ_HISTORIKKINNSLAG2
    minvalue 1000000
    increment by 50
    nocache;

create table HISTORIKKINNSLAG2_TEKSTLINJE
(
    ID                  NUMBER(19)                             not null
        constraint PK_HISTORIKKINNSLAG2_TEKSTLINJE
            primary key,
    HISTORIKKINNSLAG_ID NUMBER(19)                             not null
        constraint FK_HISTORIKKINNSLAG2_TEKSTLINJE_1 references HISTORIKKINNSLAG2,
    TEKST               VARCHAR2(4000 char),
    REKKEFOELGE_INDEKS  VARCHAR2(100 char)                     not null,
    OPPRETTET_AV        VARCHAR2(20 char) default 'VL'         not null,
    OPPRETTET_TID       TIMESTAMP(3)      default systimestamp not null,
    ENDRET_AV           VARCHAR2(20 char),
    ENDRET_TID          TIMESTAMP(3)
);

comment on table HISTORIKKINNSLAG2_TEKSTLINJE is 'Tekstlinjer i historikkinnslag';
comment on column HISTORIKKINNSLAG2_TEKSTLINJE.ID is 'PK';
comment on column HISTORIKKINNSLAG2_TEKSTLINJE.TEKST is 'Innholdet. Forklarer hva som har skjedd i saken';
comment on column HISTORIKKINNSLAG2_TEKSTLINJE.REKKEFOELGE_INDEKS is 'Rekkefølger på tekstlinje i historikkinnslaget';

create index IDX_HISTORIKKINNSLAG2_TEKSTLINJE_01
    on HISTORIKKINNSLAG2_TEKSTLINJE (HISTORIKKINNSLAG_ID);

create sequence SEQ_HISTORIKKINNSLAG2_TEKSTLINJE
    minvalue 1000000
    increment by 50
    nocache;
