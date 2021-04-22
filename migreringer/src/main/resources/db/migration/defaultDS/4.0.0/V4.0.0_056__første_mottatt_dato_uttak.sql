alter table YF_FORDELING_PERIODE add TIDLIGST_MOTTATT_DATO DATE;
comment on column YF_FORDELING_PERIODE.TIDLIGST_MOTTATT_DATO is 'Dato for når søknad om periode tidligst ble mottatt. Skiller seg fra mottatt_dato ved flere søknader om samme periode';

alter table UTTAK_RESULTAT_PERIODE_SOKNAD add TIDLIGST_MOTTATT_DATO DATE;
comment on column UTTAK_RESULTAT_PERIODE_SOKNAD.TIDLIGST_MOTTATT_DATO is 'Dato for når søknad om periode tidligst ble mottatt. Skiller seg fra mottatt_dato ved flere søknader om samme periode';

