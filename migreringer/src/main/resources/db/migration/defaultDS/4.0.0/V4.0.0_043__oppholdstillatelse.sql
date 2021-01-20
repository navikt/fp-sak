ALTER TABLE PO_ADRESSE ADD MATRIKKELID VARCHAR2(20 CHAR);
comment on column PO_ADRESSE.MATRIKKELID IS 'MatrikkelId fra FREG';

CREATE SEQUENCE  "SEQ_PO_OPPHOLD"  MINVALUE 1 INCREMENT BY 50 START WITH 1 NOCACHE  NOCYCLE ;

create table PO_OPPHOLD
(
    ID NUMBER(19) not null
        constraint PK_PO_OPPHOLD
            primary key,
    FOM DATE not null,
    TOM DATE not null,
    TILLATELSE VARCHAR2(100 char) default '-' not null,
    OPPRETTET_AV VARCHAR2(20 char) default 'VL' not null,
    OPPRETTET_TID TIMESTAMP(3) default systimestamp not null,
    ENDRET_AV VARCHAR2(20 char),
    ENDRET_TID TIMESTAMP(3),
    PO_INFORMASJON_ID NUMBER(19) not null
        constraint FK_PO_OPPHOLD_2
            references PO_INFORMASJON,
    AKTOER_ID VARCHAR2(50 char)
);

comment on table PO_OPPHOLD is 'Personopplysning - oppholdstillatelse';
comment on column PO_OPPHOLD.ID is 'Primærnøkkel';
comment on column PO_OPPHOLD.FOM is 'Gyldig fom';
comment on column PO_OPPHOLD.TOM is 'Gyldig tom';
comment on column PO_OPPHOLD.PO_INFORMASJON_ID is 'FK: mot grunnlag';
comment on column PO_OPPHOLD.TILLATELSE is 'Opphold type (permanent, midlertidig)';
comment on column PO_OPPHOLD.AKTOER_ID is 'Aktørid fra PDL';
create index IDX_PO_OPPHOLD_1
    on PO_OPPHOLD (PO_INFORMASJON_ID);
create index IDX_PO_OPPHOLD_2
    on PO_OPPHOLD (AKTOER_ID);
