ALTER TABLE TILRETTELEGGING_FOM ADD OVERSTYRT_UTBETALINGSGRAD NUMBER(5,2);

COMMENT ON COLUMN TILRETTELEGGING_FOM.OVERSTYRT_UTBETALINGSGRAD IS 'Felt for overstyring av utbetalingsgrad. Denne vil brukes istedet for utregnet utbetalingsgrad dersom den er oppgitt.';
