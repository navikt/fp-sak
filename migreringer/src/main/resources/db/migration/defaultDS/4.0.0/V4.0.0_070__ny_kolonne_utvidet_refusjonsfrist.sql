alter table BG_REFUSJON_OVERSTYRING add er_frist_utvidet VARCHAR2(1 CHAR);

comment on column BG_REFUSJON_OVERSTYRING.er_frist_utvidet is 'Vurdering fra saksbehandler om refusjonskrav for arbeidsforhold skal ha utvidet gyldighet';
