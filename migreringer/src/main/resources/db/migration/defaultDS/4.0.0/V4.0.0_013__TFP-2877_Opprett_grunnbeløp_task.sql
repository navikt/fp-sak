INSERT INTO PROSESS_TASK_TYPE (
KODE,NAVN,
FEIL_MAKS_FORSOEK,
FEIL_SEK_MELLOM_FORSOEK,
FEILHANDTERING_ALGORITME,
BESKRIVELSE,
CRON_EXPRESSION)
VALUES ('beregning.opprettGrunnbeløp','Migrering av grunnbeløp',1,30,'DEFAULT','Setter grunnbeløp i beregning der dette ikke er satt','');
