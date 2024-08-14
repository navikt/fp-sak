CREATE TABLE BG_EKSTERN_KOBLING (
                                ID NUMBER(19) not null constraint PK_BG_EKSTERN_KOBLING  primary key,
                                BEHANDLING_ID NUMBER(19) not null constraint FK_BG_EKSTERN_KOBLING_1 references BEHANDLING,
                                SKJAERINGSTIDSPUNKT DATE,
                                GRUNNBELOEP    NUMBER(12,2),
                                KOBLING_UUID           RAW(16) not null,
                                REGULERINGSBEHOV VARCHAR2(1 char) constraint BG_EKSTERN_KOBLING_REGULERINGSBEHOV check (REGULERINGSBEHOV IN ('J', 'N')),
                                OPPRETTET_TID  TIMESTAMP(3) DEFAULT systimestamp NOT NULL,
                                ENDRET_TID     TIMESTAMP(3),
                                OPPRETTET_AV  VARCHAR2(20 char) default 'VL' not null,
                                ENDRET_AV     VARCHAR2(20 char)
);

create index IDX_BG_EKSTERN_KOBLING on BG_EKSTERN_KOBLING (BEHANDLING_ID);

COMMENT ON TABLE BG_EKSTERN_KOBLING IS 'Tabell som identifiserer et beregningsgrunnlag som ligger lagret i kalkulus';
COMMENT ON COLUMN BG_EKSTERN_KOBLING.ID IS 'Primærnøkkel';
COMMENT ON COLUMN BG_EKSTERN_KOBLING.BEHANDLING_ID IS 'Fremmednøkkel til behandlingen';
COMMENT ON COLUMN BG_EKSTERN_KOBLING.SKJAERINGSTIDSPUNKT IS 'Skjæringstidspunkt for beregningen';
COMMENT ON COLUMN BG_EKSTERN_KOBLING.REGULERINGSBEHOV IS 'Om koblingen kan ha behov for reberegning etter justeriung av grunnbeløp som gjøres hvert år.';
COMMENT ON COLUMN BG_EKSTERN_KOBLING.GRUNNBELOEP IS 'Grunnbeløpet som er brukt i beregningsgrunnlaget';
COMMENT ON COLUMN BG_EKSTERN_KOBLING.KOBLING_UUID IS 'UUID som brukes for å identifisere tilhørende beregningsgrunnlag i kalkulus';

create sequence SEQ_BG_EKSTERN_KOBLING increment by 50 nocache;
