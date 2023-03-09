drop table OPPGAVE_BEHANDLING_KOBLING;
drop sequence SEQ_OPPGAVE_BEHANDLING_KOBLING;

alter table OVERLAPP_VEDTAK add UTBETALINGSGRAD_FOR NUMBER(19);
COMMENT ON COLUMN OVERLAPP_VEDTAK.UTBETALINGSGRAD_FOR IS 'Utbetalingsgrad egen ytelse';

create sequence SEQ_FAGSAK_EGENSKAP
    minvalue 1000000
    increment by 50
    nocache;

create table FAGSAK_EGENSKAP
(
    ID NUMBER(19) not null
        constraint PK_FAGSAK_EGENSKAP
            primary key,
    FAGSAK_ID NUMBER(19) not null
        constraint FK_FAGSAK_EGENSKAP
            references FAGSAK,
    OPPRETTET_AV VARCHAR2(20 char) default 'VL' not null,
    OPPRETTET_TID TIMESTAMP(3) default systimestamp not null,
    ENDRET_AV VARCHAR2(20 char),
    ENDRET_TID TIMESTAMP(3),
    AKTIV VARCHAR2(1 char) default 'J' not null
        constraint CHK_AKTIV16
            check (aktiv IN ('J', 'N')),
    EGENSKAP_KEY VARCHAR2(50 CHAR),
    EGENSKAP_VALUE VARCHAR2(200 char)
);
comment on table FAGSAK_EGENSKAP is 'Generell propertytabell for marking av sak for søk, etc';
comment on column FAGSAK_EGENSKAP.ID is 'Primary Key';
comment on column FAGSAK_EGENSKAP.FAGSAK_ID is 'FK:FAGSAK Fremmednøkkel for kobling til fagsak';
comment on column FAGSAK_EGENSKAP.EGENSKAP_KEY is 'Nøkkel som er en kodelist';
comment on column FAGSAK_EGENSKAP.EGENSKAP_VALUE is 'Verdi som er en kodeverdi';

create index IDX_FAGSAK_EGENSKAP_1
    on FAGSAK_EGENSKAP (FAGSAK_ID);

