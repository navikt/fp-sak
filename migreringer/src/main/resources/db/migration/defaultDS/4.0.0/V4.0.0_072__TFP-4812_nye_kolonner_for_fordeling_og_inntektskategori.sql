alter table BG_PR_STATUS_OG_ANDEL add manuelt_fordelt_pr_aar NUMBER(19,2);
alter table BG_PR_STATUS_OG_ANDEL add inntektskategori_manuell_fordeling VARCHAR2(100 CHAR);
alter table BG_PR_STATUS_OG_ANDEL add inntektskategori_fordeling VARCHAR2(100 CHAR);
alter table BG_ANDEL_ARBEIDSFORHOLD add manuelt_fordelt_refusjon_pr_aar NUMBER(19,2);

comment on column BG_PR_STATUS_OG_ANDEL.manuelt_fordelt_pr_aar is 'Manuelt fordelt beregningsgrunnlag.';
comment on column BG_PR_STATUS_OG_ANDEL.inntektskategori_manuell_fordeling is 'Inntektskategori satt ved manuell fordeling.';
comment on column BG_PR_STATUS_OG_ANDEL.inntektskategori_fordeling is 'Inntektskategori satt ved automatisk fordeling.';
comment on column BG_ANDEL_ARBEIDSFORHOLD.manuelt_fordelt_refusjon_pr_aar is 'Refusjonsbeløp satt av saksbehandler i fordeling.';
