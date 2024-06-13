ALTER TABLE BR_FERIEPENGER_PR_AAR MODIFY(BEREGNINGSRESULTAT_ANDEL_ID NULL);

alter table BR_FERIEPENGER_PR_AAR ADD (
    AKTIVITET_STATUS VARCHAR2(100 char),
    BRUKER_ER_MOTTAKER VARCHAR2(1 char) default 'N' not null
        constraint CHK_FERIE_BRUKER_MOTTAKER
        check (bruker_er_mottaker IN ('J', 'N')),
    ARBEIDSGIVER_AKTOR_ID VARCHAR2(100 char),
    ARBEIDSGIVER_ORGNR VARCHAR2(100 char),
    ARBEIDSFORHOLD_INTERN_ID RAW(16));

comment on column BR_FERIEPENGER_PR_AAR.AKTIVITET_STATUS is 'Aktivitetstatus for andelen feriepengene gjelder';
comment on column BR_FERIEPENGER_PR_AAR.BRUKER_ER_MOTTAKER is 'Angir om bruker eller arbeidsgiver er mottaker';
comment on column BR_FERIEPENGER_PR_AAR.ARBEIDSGIVER_AKTOR_ID is 'Arbeidsgivers aktørid dersom privat/forenklet';
comment on column BR_FERIEPENGER_PR_AAR.ARBEIDSGIVER_ORGNR is 'Arbeidsgivers organisasjonsnummer dersom virksomheter';
comment on column BR_FERIEPENGER_PR_AAR.ARBEIDSFORHOLD_INTERN_ID is 'Globalt unikt arbeidsforhold id generert for arbeidsgiver/arbeidsforhold';

ALTER TABLE BR_FERIEPENGER MODIFY(BEREGNINGSRESULTAT_FP_ID NULL);

ALTER TABLE BR_RESULTAT_BEHANDLING ADD BEREGNINGSRESULTAT_FERIEPENGER_ID NUMBER(19)
        constraint FK_BR_RESULTAT_BEHANDLING_4
        references BR_FERIEPENGER;

comment on column BR_RESULTAT_BEHANDLING.BEREGNINGSRESULTAT_FERIEPENGER_ID is 'Beregnet feriepenger for aktiv tilkjent ytelse';

create index IDX_FERIE_BRRES on BR_RESULTAT_BEHANDLING (BEREGNINGSRESULTAT_FERIEPENGER_ID);
