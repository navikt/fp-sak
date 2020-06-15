alter table YF_FORDELING_PERIODE add MOTTATT_DATO_TEMP DATE;
comment on column YF_FORDELING_PERIODE.MOTTATT_DATO_TEMP is 'temporær kolonne som skal brukes til utleding av mottatt dato. Legger utledet dato her først for å sammenligne';

