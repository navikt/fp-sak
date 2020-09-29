alter table BEHANDLING add KONTO_BEREGNING_ID NUMBER(19,0);

comment on column BEHANDLING.KONTO_BEREGNING_ID IS 'FK: Fremmednøkkel til kontoberegning for fagsakrelasjon ved tidspunkt for endring i denne behandlingen';
