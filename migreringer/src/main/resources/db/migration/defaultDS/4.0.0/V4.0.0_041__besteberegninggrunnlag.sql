CREATE TABLE BG_BESTEBEREGNINGGRUNNLAG (
        ID                              NUMBER(19, 0)                       NOT NULL,
        VERSJON                         NUMBER(19,0)       DEFAULT 0        NOT NULL,
        OPPRETTET_AV                    VARCHAR2(20 CHAR)  DEFAULT 'VL'     NOT NULL,
        OPPRETTET_TID                   TIMESTAMP(3) DEFAULT systimestamp   NOT NULL,
        ENDRET_AV                       VARCHAR2(20 CHAR),
        ENDRET_TID                      TIMESTAMP(3),
        BEREGNINGSGRUNNLAG_ID           NUMBER(19, 0)                       NOT NULL ENABLE
);
create sequence SEQ_BG_BESTEBEREGNINGGRUNNLAG minvalue 1 increment by 50 start with 1 nocache nocycle;
alter table BG_BESTEBEREGNINGGRUNNLAG add constraint PK_BG_BESTEBEREGNINGGRUNNLAG primary key (ID);
create index IDX_BG_BESTEBEREGNING_1 on BG_BESTEBEREGNINGGRUNNLAG (BEREGNINGSGRUNNLAG_ID);
ALTER TABLE BG_BESTEBEREGNINGGRUNNLAG ADD CONSTRAINT FK_BG_BESTEBEREGNINGGRUNNLAG FOREIGN KEY ("BEREGNINGSGRUNNLAG_ID")
    REFERENCES BEREGNINGSGRUNNLAG (ID);

COMMENT ON TABLE BG_BESTEBEREGNINGGRUNNLAG  IS 'Grunnlag for vurdering av besteberegning';
COMMENT ON COLUMN BG_BESTEBEREGNINGGRUNNLAG.ID IS 'PK';
COMMENT ON COLUMN BG_BESTEBEREGNINGGRUNNLAG.BEREGNINGSGRUNNLAG_ID IS 'FK til beregningsgrunnlag';

CREATE TABLE BG_BESTEBEREGNING_MAANED (
       ID                              NUMBER(19, 0)                       NOT NULL,
       VERSJON                         NUMBER(19,0)       DEFAULT 0        NOT NULL,
       OPPRETTET_AV                    VARCHAR2(20 CHAR)  DEFAULT 'VL'     NOT NULL,
       OPPRETTET_TID                   TIMESTAMP(3) DEFAULT systimestamp   NOT NULL,
       ENDRET_AV                       VARCHAR2(20 CHAR),
       ENDRET_TID                      TIMESTAMP(3),
       BESTEBEREGNINGGRUNNLAG_ID       NUMBER(19, 0)                       NOT NULL,
       FOM                             DATE                                NOT NULL,
       TOM                             DATE                                NOT NULL
);
alter table BG_BESTEBEREGNING_MAANED add constraint PK_BG_BESTEBEREGNING_MAANED primary key (ID);
create sequence SEQ_BESTEBEREGNING_MAANED minvalue 1 increment by 50 start with 1 nocache nocycle;
alter table BG_BESTEBEREGNING_MAANED add constraint FK_BG_BESTEBEREGNING_MAANED foreign key (BESTEBEREGNINGGRUNNLAG_ID) references BG_BESTEBEREGNINGGRUNNLAG(ID);
create index IDX_BG_BESTEBEREGNING_MAANED_1 on BG_BESTEBEREGNING_MAANED (BESTEBEREGNINGGRUNNLAG_ID);

COMMENT ON TABLE BG_BESTEBEREGNING_MAANED  IS 'Aggregat for inntekter pr måned for månedene brukt til beregning av besteberegning';
COMMENT ON COLUMN BG_BESTEBEREGNING_MAANED.ID IS 'PK';
COMMENT ON COLUMN BG_BESTEBEREGNING_MAANED.BESTEBEREGNINGGRUNNLAG_ID IS 'FK til besteberegninggrunnlag';
COMMENT ON COLUMN BG_BESTEBEREGNING_MAANED.FOM IS 'Første dato i måned';
COMMENT ON COLUMN BG_BESTEBEREGNING_MAANED.TOM IS 'Siste dato i måned';

CREATE TABLE BG_BESTEBEREGNING_INNTEKT (
      ID                              NUMBER(19, 0)                         NOT NULL,
      VERSJON                         NUMBER(19,0)       DEFAULT 0          NOT NULL,
      OPPRETTET_AV                    VARCHAR2(20 CHAR)  DEFAULT 'VL'       NOT NULL,
      OPPRETTET_TID                   TIMESTAMP(3) DEFAULT systimestamp     NOT NULL,
      ENDRET_AV                       VARCHAR2(20 CHAR),
      ENDRET_TID                      TIMESTAMP(3),
      BESTEBEREGNING_MAANED_ID        NUMBER(19, 0)                         NOT NULL,
      ARBEIDSGIVER_AKTOR_ID           VARCHAR2(100 CHAR),
      ARBEIDSGIVER_ORGNR              VARCHAR2(100 CHAR),
      ARBEIDSFORHOLD_INTERN_ID        RAW(16),
      OPPTJENING_AKTIVITET_TYPE       VARCHAR2(100 CHAR) DEFAULT '-'        NOT NULL,
      INNTEKT                         NUMBER(19,2)                          NOT NULL
);
alter table BG_BESTEBEREGNING_INNTEKT add constraint PK_BG_BESTEBEREGNING_INNTEKT primary key (ID);
create sequence SEQ_BESTEBEREGNING_INNTEKT minvalue 1 increment by 50 start with 1 nocache nocycle;
alter table BG_BESTEBEREGNING_INNTEKT add constraint FK_BG_BESTEBEREGNING_INNTEKT foreign key (BESTEBEREGNING_MAANED_ID) references BG_BESTEBEREGNING_MAANED(ID);
create index IDX_BESTEBEREGNING_INNTEKT_1 on BG_BESTEBEREGNING_INNTEKT (BESTEBEREGNING_MAANED_ID);

COMMENT ON TABLE BG_BESTEBEREGNING_INNTEKT  IS 'Inntekt for en aktivitet i en måned.';
COMMENT ON COLUMN BG_BESTEBEREGNING_INNTEKT.ID IS 'PK';
COMMENT ON COLUMN BG_BESTEBEREGNING_INNTEKT.BESTEBEREGNING_MAANED_ID IS 'FK til månedsaggregat';
COMMENT ON COLUMN BG_BESTEBEREGNING_INNTEKT.ARBEIDSGIVER_AKTOR_ID IS 'Arbeidsgiver aktør id';
COMMENT ON COLUMN BG_BESTEBEREGNING_INNTEKT.ARBEIDSGIVER_ORGNR IS 'Arbeidsgiver organisasjonsnummer';
COMMENT ON COLUMN BG_BESTEBEREGNING_INNTEKT.ARBEIDSFORHOLD_INTERN_ID IS 'Arbeidsforhold intern-id';
COMMENT ON COLUMN BG_BESTEBEREGNING_INNTEKT.OPPTJENING_AKTIVITET_TYPE IS 'Opptjeningaktivitettype';
COMMENT ON COLUMN BG_BESTEBEREGNING_INNTEKT.INNTEKT IS 'Inntekt i måned';
