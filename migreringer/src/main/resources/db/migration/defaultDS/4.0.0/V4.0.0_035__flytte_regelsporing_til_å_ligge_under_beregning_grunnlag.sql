DROP SEQUENCE SEQ_REGEL_SPORING_GRUNNLAG;
DROP TABLE REGEL_SPORING_GRUNNLAG;
DROP SEQUENCE SEQ_REGEL_SPORING_PERIODE;
DROP TABLE REGEL_SPORING_PERIODE;

create table BG_REGEL_SPORING_AGGREGAT
(
    ID                              NUMBER(19,0)                                 NOT NULL,
    VERSJON                         NUMBER(19,0)       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV                    VARCHAR2(20 CHAR)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID                   TIMESTAMP(3) DEFAULT systimestamp            NOT NULL,
    ENDRET_AV                       VARCHAR2(20 CHAR),
    ENDRET_TID                      TIMESTAMP(3)
);
create UNIQUE index PK_BG_REGEL_SPORING_AGGREGAT on BG_REGEL_SPORING_AGGREGAT (ID);
alter table BG_REGEL_SPORING_AGGREGAT add constraint PK_BG_REGEL_SPORING_AGGREGAT primary key (ID);
create sequence SEQ_BG_REGEL_SPORING_AGGREGAT increment by 50 minvalue 1000000;

comment on table BG_REGEL_SPORING_AGGREGAT is 'Agregattabell for regelsporinger for beregning';

ALTER TABLE GR_BEREGNINGSGRUNNLAG ADD REGEL_SPORING_AGGREGAT_ID NUMBER(19,0);
create index IDX_GR_BEREGNINGSGRUNNLAG_08 on GR_BEREGNINGSGRUNNLAG (REGEL_SPORING_AGGREGAT_ID);
ALTER TABLE GR_BEREGNINGSGRUNNLAG ADD CONSTRAINT FK_GR_BEREGNINGSGRUNNLAG_07 foreign key (REGEL_SPORING_AGGREGAT_ID)
references BG_REGEL_SPORING_AGGREGAT (ID);

create table BG_REGEL_SPORING_GRUNNLAG
(
    ID                              NUMBER(19,0)                                 NOT NULL,
    REGEL_SPORING_AGGREGAT_ID       NUMBER(19,0)                                 NOT NULL,
    REGEL_EVALUERING                CLOB,
    REGEL_INPUT                     CLOB                                         NOT NULL,
    REGEL_TYPE                      VARCHAR2(100 CHAR)                           NOT NULL,
    VERSJON                         NUMBER(19,0)       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV                    VARCHAR2(20 CHAR)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID                   TIMESTAMP(3) DEFAULT systimestamp            NOT NULL,
    ENDRET_AV                       VARCHAR2(20 CHAR),
    ENDRET_TID                      TIMESTAMP(3)
);
create index IDX_BG_RS_GR_01 on BG_REGEL_SPORING_GRUNNLAG (REGEL_SPORING_AGGREGAT_ID);
create index IDX_BG_RS_GR_02 on BG_REGEL_SPORING_GRUNNLAG (REGEL_TYPE);
create UNIQUE index PK_BG_REGEL_SPORING_GRUNNLAG on BG_REGEL_SPORING_GRUNNLAG (ID);
alter table BG_REGEL_SPORING_GRUNNLAG add constraint PK_BG_RS_GRUNNLAG primary key (ID);
alter table BG_REGEL_SPORING_GRUNNLAG add constraint FK_BG_RS_GRUNNLAG_01 foreign key (REGEL_SPORING_AGGREGAT_ID)
references BG_REGEL_SPORING_AGGREGAT (ID);
create sequence SEQ_BG_REGEL_SPORING_GRUNNLAG increment by 50 minvalue 1000000;

comment on table BG_REGEL_SPORING_GRUNNLAG is 'Tabell som lagrer regelsporinger for beregningsgrunnlag';
comment on column BG_REGEL_SPORING_GRUNNLAG.REGEL_TYPE is 'Hvilken regel det gjelder';
comment on column BG_REGEL_SPORING_GRUNNLAG.REGEL_INPUT is 'Input til regelen';
comment on column BG_REGEL_SPORING_GRUNNLAG.REGEL_EVALUERING is 'Regelevaluering/logging';
comment on column BG_REGEL_SPORING_GRUNNLAG.ID is 'Primary Key';
comment on column BG_REGEL_SPORING_GRUNNLAG.REGEL_SPORING_AGGREGAT_ID is 'FK: Referanse til aggregat';

create table BG_REGEL_SPORING_PERIODE
(
    ID                              NUMBER(19,0)                                   NOT NULL,
    REGEL_SPORING_AGGREGAT_ID       NUMBER(19,0)                                   NOT NULL,
    FOM                             DATE                                           NOT NULL,
    TOM                             DATE                                           NOT NULL,
    REGEL_EVALUERING                CLOB,
    REGEL_INPUT                     CLOB                                           NOT NULL,
    REGEL_TYPE                      VARCHAR2(100 CHAR)                             NOT NULL,
    VERSJON                         NUMBER(19,0)       DEFAULT 0                   NOT NULL,
    OPPRETTET_AV                    VARCHAR2(20 CHAR)  DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID                   TIMESTAMP(3) DEFAULT systimestamp              NOT NULL,
    ENDRET_AV                       VARCHAR2(20 CHAR),
    ENDRET_TID                      TIMESTAMP(3)
);
create index IDX_BG_RS_PERIODE_01 on BG_REGEL_SPORING_PERIODE (REGEL_SPORING_AGGREGAT_ID);
create index IDX_BG_RS_PERIODE_02 on BG_REGEL_SPORING_PERIODE (REGEL_TYPE);
create UNIQUE index PK_BG_REGEL_SPORING_PERIODE on BG_REGEL_SPORING_PERIODE (ID);
alter table BG_REGEL_SPORING_PERIODE add constraint PK_BG_RS_PERIODE primary key (ID);
alter table BG_REGEL_SPORING_PERIODE add constraint FK_BG_RS_01 foreign key (REGEL_SPORING_AGGREGAT_ID)
references BG_REGEL_SPORING_AGGREGAT (ID);
create sequence SEQ_BG_REGEL_SPORING_PERIODE increment by 50 minvalue 1000000;

comment on table BG_REGEL_SPORING_PERIODE is 'Tabell som lagrer regelsporinger for beregningsgrunnlagperioder';
comment on column BG_REGEL_SPORING_PERIODE.REGEL_SPORING_AGGREGAT_ID is 'FK: Referanse til aggregat';
comment on column BG_REGEL_SPORING_PERIODE.REGEL_TYPE is 'Hvilken regel det gjelder';
comment on column BG_REGEL_SPORING_PERIODE.REGEL_INPUT is 'Input til regelen';
comment on column BG_REGEL_SPORING_PERIODE.REGEL_EVALUERING is 'Regelevaluering/logging';
comment on column BG_REGEL_SPORING_PERIODE.ID is 'Primary Key';
comment on column BG_REGEL_SPORING_PERIODE.FOM is 'Fom-dato for periode som spores';
comment on column BG_REGEL_SPORING_PERIODE.TOM is 'Tom-dato for periode som spores';
