drop index IDX_OPPGAVE_BEH_KOB_BEH_ID;
drop index IDX_OPPGAVE_BEHANDLING_KOBLI_6;
alter table OPPGAVE_BEHANDLING_KOBLING drop constraint PK_OPPGAVE_BEHANDLING_KOBLING;
drop table OPPGAVE_BEHANDLING_KOBLING;
drop sequence SEQ_OPPGAVE_BEHANDLING_KOBLING;

create sequence SEQ_FAGSAK_EGENSKAP minvalue 1000000 increment by 50;

create table FAGSAK_EGENSKAP
(
    ID            NUMBER(19) not null constraint PK_FAGSAK_EGENSKAP primary key,
    FAGSAK_ID     NUMBER(19) not null constraint FK_FAGSAK_EGENSKAP references FAGSAK,
    OPPRETTET_AV  VARCHAR2(20 char) default 'VL' not null,
    OPPRETTET_TID TIMESTAMP(3) default systimestamp not null,
    ENDRET_AV     VARCHAR2(20 char),
    ENDRET_TID    TIMESTAMP(3),
    AKTIV         VARCHAR2(1 char) default 'J' not null,
    EGENSKAP_KEY  VARCHAR2(50 CHAR) not null,
    EGENSKAP_VALUE VARCHAR2(200 char)
);
comment on table FAGSAK_EGENSKAP is 'Generell propertytabell for glissen merking av sak for søk, etc';
comment on column FAGSAK_EGENSKAP.ID is 'Primary Key';
comment on column FAGSAK_EGENSKAP.FAGSAK_ID is 'FK:FAGSAK Fremmednøkkel for kobling til fagsak';
comment on column FAGSAK_EGENSKAP.EGENSKAP_KEY is 'Nøkkel som er en kodeliste';
comment on column FAGSAK_EGENSKAP.EGENSKAP_VALUE is 'Verdi som er en kodeverdi';

create index IDX_FAGSAK_EGENSKAP_1 on FAGSAK_EGENSKAP (FAGSAK_ID);
