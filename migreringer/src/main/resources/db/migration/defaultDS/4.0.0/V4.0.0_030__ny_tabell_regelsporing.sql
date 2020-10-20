create table REGEL_SPORING_GRUNNLAG
(
    ID               NUMBER(19,0)                                 NOT NULL,
    BEHANDLING_ID    NUMBER(19,0)                                 NOT NULL,
    REGEL_EVALUERING CLOB,
    REGEL_INPUT      CLOB                                         NOT NULL,
    REGEL_TYPE       VARCHAR2(100 CHAR)                           NOT NULL,
    AKTIV            VARCHAR2(1 CHAR) DEFAULT 'J'                 NOT NULL,
    VERSJON          NUMBER(19,0)       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV     VARCHAR2(20 CHAR)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT systimestamp            NOT NULL,
    ENDRET_AV        VARCHAR2(20 CHAR),
    ENDRET_TID       TIMESTAMP(3)
);
create index IDX_RS_GR_01 on REGEL_SPORING_GRUNNLAG (BEHANDLING_ID);
create index IDX_RS_GR_02 on REGEL_SPORING_GRUNNLAG (REGEL_TYPE);
create UNIQUE index PK_REGEL_SPORING_GRUNNLAG on REGEL_SPORING_GRUNNLAG (ID);
alter table REGEL_SPORING_GRUNNLAG add constraint PK_REGEL_SPORING_GRUNNLAG primary key (ID);
alter table REGEL_SPORING_GRUNNLAG add constraint FK_REGEL_SPORING_GRUNNLAG_01 foreign key (BEHANDLING_ID) references BEHANDLING (ID);
create sequence SEQ_REGEL_SPORING_GRUNNLAG increment by 50 minvalue 1000000;

comment on table REGEL_SPORING_GRUNNLAG is 'Tabell som lagrer regelsporinger for beregningsgrunnlag';
comment on column REGEL_SPORING_GRUNNLAG.REGEL_TYPE is 'Hvilken regel det gjelder';
comment on column REGEL_SPORING_GRUNNLAG.REGEL_INPUT is 'Input til regelen';
comment on column REGEL_SPORING_GRUNNLAG.REGEL_EVALUERING is 'Regelevaluering/logging';
comment on column REGEL_SPORING_GRUNNLAG.ID is 'Primary Key';
comment on column REGEL_SPORING_GRUNNLAG.BEHANDLING_ID is 'FK: Referanse til behandling';
comment on column REGEL_SPORING_GRUNNLAG.AKTIV is 'Sier om sporingen er aktiv';

create table REGEL_SPORING_PERIODE
(
    ID               NUMBER(19,0)                                   NOT NULL,
    BEHANDLING_ID    NUMBER(19,0)                                   NOT NULL,
    FOM              DATE                                           NOT NULL,
    TOM              DATE                                           NOT NULL,
    REGEL_EVALUERING CLOB,
    REGEL_INPUT      CLOB                                           NOT NULL,
    REGEL_TYPE       VARCHAR2(100 CHAR)                             NOT NULL,
    AKTIV            VARCHAR2(1 CHAR) DEFAULT 'J'                   NOT NULL,
    VERSJON          NUMBER(19,0)       DEFAULT 0                   NOT NULL,
    OPPRETTET_AV     VARCHAR2(20 CHAR)  DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT systimestamp              NOT NULL,
    ENDRET_AV        VARCHAR2(20 CHAR),
    ENDRET_TID       TIMESTAMP(3)
);
create index IDX_RS_PERIODE_01 on REGEL_SPORING_PERIODE (BEHANDLING_ID);
create index IDX_RS_PERIODE_02 on REGEL_SPORING_PERIODE (REGEL_TYPE);
create UNIQUE index PK_REGEL_SPORING_PERIODE on REGEL_SPORING_PERIODE (ID);
alter table REGEL_SPORING_PERIODE add constraint PK_REGEL_SPORING_PERIODE primary key (ID);
alter table REGEL_SPORING_PERIODE add constraint FK_REGEL_SPORING_PERIODE_01 foreign key (BEHANDLING_ID) references BEHANDLING (ID);
create sequence SEQ_REGEL_SPORING_PERIODE increment by 50 minvalue 1000000;

comment on table REGEL_SPORING_PERIODE is 'Tabell som lagrer regelsporinger for beregningsgrunnlagperioder';
comment on column REGEL_SPORING_PERIODE.BEHANDLING_ID is 'FK: Referanse til behandling';
comment on column REGEL_SPORING_PERIODE.REGEL_TYPE is 'Hvilken regel det gjelder';
comment on column REGEL_SPORING_PERIODE.REGEL_INPUT is 'Input til regelen';
comment on column REGEL_SPORING_PERIODE.REGEL_EVALUERING is 'Regelevaluering/logging';
comment on column REGEL_SPORING_PERIODE.ID is 'Primary Key';
comment on column REGEL_SPORING_PERIODE.FOM is 'Fom-dato for periode som spores';
comment on column REGEL_SPORING_PERIODE.TOM is 'Tom-dato for periode som spores';
comment on column REGEL_SPORING_PERIODE.AKTIV is 'Sier om sporingen er aktiv';
