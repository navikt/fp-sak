create sequence SEQ_FAGSAK_NOTAT minvalue 1000000 increment by 50;

create table FAGSAK_NOTAT
(
    ID            NUMBER(19) not null constraint PK_FAGSAK_NOTAT primary key,
    FAGSAK_ID     NUMBER(19) not null constraint FK_FAGSAK_NOTAT references FAGSAK,
    OPPRETTET_AV  VARCHAR2(20 char) default 'VL' not null,
    OPPRETTET_TID TIMESTAMP(3) default systimestamp not null,
    AKTIV         VARCHAR2(1 char) default 'J' not null,
    NOTAT         VARCHAR2(4000 char)
);
comment on table FAGSAK_NOTAT is 'Generell notatblokk for saken';
comment on column FAGSAK_NOTAT.ID is 'Primary Key';
comment on column FAGSAK_NOTAT.FAGSAK_ID is 'FK:FAGSAK Fremmednøkkel for kobling til fagsak';
comment on column FAGSAK_NOTAT.NOTAT is 'Tekstlig notat';

create index IDX_FAGSAK_NOTAT_1 on FAGSAK_NOTAT (FAGSAK_ID);
