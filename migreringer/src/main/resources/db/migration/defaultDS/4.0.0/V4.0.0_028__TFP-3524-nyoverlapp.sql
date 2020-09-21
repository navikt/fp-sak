create sequence SEQ_OVERLAPP_VEDTAK increment by 50 nocache;

create table OVERLAPP_VEDTAK (
    ID NUMBER(19) not null
        constraint PK_OVERLAPP_VEDTAK
            primary key,
    SAKSNUMMER VARCHAR2(19 char) not null,
    BEHANDLING_ID NUMBER(19) not null,
    HENDELSE VARCHAR2(30 char) not null,
    FAGSYSTEM VARCHAR2(19 char) not null,
    YTELSE VARCHAR2(19 char) not null,
    REFERANSE VARCHAR2(40 char),
    FOM DATE not null,
    TOM DATE not null,
    UTBETALINGSPROSENT NUMBER(19) not null,
    VERSJON NUMBER(19) default 0 not null,
    OPPRETTET_AV VARCHAR2(20 char) default 'VL' not null,
    OPPRETTET_TID TIMESTAMP(3) default systimestamp not null,
    ENDRET_AV VARCHAR2(20 char),
    ENDRET_TID TIMESTAMP(3)
);

comment on table OVERLAPP_VEDTAK is 'Behandlinger identifisert med overlappende ytelse i Infotrygd under iverksetting';

comment on column OVERLAPP_VEDTAK.SAKSNUMMER is 'FK: Fagsak.SAKSNUMMER';
comment on column OVERLAPP_VEDTAK.BEHANDLING_ID is 'FK: Behandling';
comment on column OVERLAPP_VEDTAK.HENDELSE is 'Opphav til logging - VEDTAK, AVSTEMMING';
comment on column OVERLAPP_VEDTAK.FAGSYSTEM is 'Eksternt fagsystem som har fattet overlappende vedtak';
comment on column OVERLAPP_VEDTAK.YTELSE is 'Ytelse for overlappende vedtak';
comment on column OVERLAPP_VEDTAK.REFERANSE is 'Referanse til overlappende vedtak';
comment on column OVERLAPP_VEDTAK.FOM is 'Første dato med overlapp';
comment on column OVERLAPP_VEDTAK.TOM is 'Siste dato med overlapp';
comment on column OVERLAPP_VEDTAK.UTBETALINGSPROSENT is 'Sum av utbetalingsprosent for perioden';

create index IDX_OVERLAPP_VEDTAK_01 on OVERLAPP_VEDTAK (BEHANDLING_ID);
create index IDX_OVERLAPP_VEDTAK_02 on OVERLAPP_VEDTAK (SAKSNUMMER);
