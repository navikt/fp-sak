ALTER TABLE SO_RETTIGHET ADD ANNEN_FORELDER_RETT_EOS VARCHAR2(1 CHAR) DEFAULT 'N' ;
COMMENT ON COLUMN SO_RETTIGHET.ANNEN_FORELDER_RETT_EOS IS 'Oppgitt at annen forelder har opptjent rett i EØS-land';

ALTER TABLE GR_YTELSES_FORDELING RENAME COLUMN mor_stonad_eos_id to ANNEN_FORELDER_RETT_EOS_ID;
