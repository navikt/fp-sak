alter table KLAGE_FORMKRAV ADD MOTTATT_DATO DATE;
COMMENT ON COLUMN KLAGE_FORMKRAV.MOTTATT_DATO IS 'Registrert verdi dersom annen enn i mottatt dokument';
