create sequence SEQ_LONNSKOMP_VEDTAK increment by 50 minvalue 1000000;

create table LONNSKOMP_VEDTAK
(
    ID                   NUMBER(19,0)                        not null
        constraint PK_LONNSKOMP_VEDTAK
            primary key,
    SAKID                VARCHAR2(100 CHAR)                        not null,
    AKTOER_ID            VARCHAR2(50 CHAR),
    FNR                  VARCHAR2(50 CHAR),
    ORG_NUMMER           VARCHAR2(100 CHAR)                        not null,
    BELOEP               NUMBER(19,2)                      NOT NULL,
    FOM                  DATE                                not null,
    TOM                  DATE                                not null,
    AKTIV            VARCHAR2(1 CHAR) DEFAULT 'J'                 NOT NULL,
    VERSJON          NUMBER(19,0)       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV     VARCHAR2(20 CHAR)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT systimestamp            NOT NULL,
    ENDRET_AV        VARCHAR2(20 CHAR),
    ENDRET_TID       TIMESTAMP(3)
);
comment on table LONNSKOMP_VEDTAK is 'En tabell med informasjon om Lønnskompensasjon / Koronapenger';
comment on column LONNSKOMP_VEDTAK.ID is 'Primærnøkkel';
comment on column LONNSKOMP_VEDTAK.AKTOER_ID is 'Stønadsmottakeren';
comment on column LONNSKOMP_VEDTAK.FNR is 'Stønadsmottakeren ident';
comment on column LONNSKOMP_VEDTAK.ORG_NUMMER is 'Arbeidsgiver som har permittert';
comment on column LONNSKOMP_VEDTAK.BELOEP is 'Sum utbetalt Stønadsmottakeren';
comment on column LONNSKOMP_VEDTAK.FOM is 'Startdato for ytelsen.';
comment on column LONNSKOMP_VEDTAK.TOM is 'Sluttdato for ytelsen';
comment on column LONNSKOMP_VEDTAK.SAKID is 'Saksnummer i kildesystem';
comment on column LONNSKOMP_VEDTAK.AKTIV is 'Er innslaget aktivt';
create index IDX_LONNSKOMP_VEDTAK_1
    on LONNSKOMP_VEDTAK (SAKID);
create index IDX_LONNSKOMP_VEDTAK_2
    on LONNSKOMP_VEDTAK (AKTOER_ID);
create index IDX_LONNSKOMP_VEDTAK_3
    on LONNSKOMP_VEDTAK (FNR);
create index IDX_LONNSKOMP_VEDTAK_10
    on LONNSKOMP_VEDTAK (AKTIV);


