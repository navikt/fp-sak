create sequence SEQ_SVP_AVKLART_OPPHOLD minvalue 1000000 increment by 50;

create table SVP_AVKLART_OPPHOLD
(
    ID                      NUMBER(19) not null constraint PK_SVP_AVKLART_OPPHOLD primary key,
    SVP_TILRETTELEGGING_ID  NUMBER(19) constraint FK_AVKLARRT_OPPHOLD references SVP_TILRETTELEGGING,
    FOM                     DATE not null,
    TOM                     DATE not null,
    SVP_OPPHOLD_ARSAK           VARCHAR2(25 char) not  null,
    OPPRETTET_AV            VARCHAR2(20 char) default 'VL' not null,
    OPPRETTET_TID           TIMESTAMP(3) default systimestamp not null
);

comment on table SVP_AVKLART_OPPHOLD is 'Avklarte oppholdsperioder for svangerskapspenger';
comment on column SVP_AVKLART_OPPHOLD.ID is 'Primary Key';
comment on column SVP_AVKLART_OPPHOLD.SVP_TILRETTELEGGING_ID is 'FK til SvpTilrettelegging';
comment on column SVP_AVKLART_OPPHOLD.FOM is 'Opphold fra og med dato';
comment on column SVP_AVKLART_OPPHOLD.TOM is 'Opphold til og med dato';
comment on column SVP_AVKLART_OPPHOLD.SVP_OPPHOLD_ARSAK is 'Årsak til oppholdet';

create index IDX_AVKLARRT_OPPHOLD_1 on SVP_AVKLART_OPPHOLD (SVP_TILRETTELEGGING_ID);

