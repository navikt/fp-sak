ALTER TABLE BG_ANDEL_ARBEIDSFORHOLD add saksbehandlet_refusjon_pr_aar NUMBER(19,2);
ALTER TABLE BG_ANDEL_ARBEIDSFORHOLD add fordelt_refusjon_pr_aar NUMBER(19,2);

comment on column BG_ANDEL_ARBEIDSFORHOLD.saksbehandlet_refusjon_pr_aar IS 'Refusjonsbeløp satt som følge av å ha vurdert refusjonskravet og refusjonsbeløpet';
comment on column BG_ANDEL_ARBEIDSFORHOLD.fordelt_refusjon_pr_aar IS 'Refusjonsbeløp satt i henhold til fordelingsregler';
