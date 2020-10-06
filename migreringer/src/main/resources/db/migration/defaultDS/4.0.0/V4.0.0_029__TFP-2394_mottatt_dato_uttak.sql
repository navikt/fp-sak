alter table UTTAK_RESULTAT_PERIODE_SOKNAD add MOTTATT_DATO_3 DATE;
comment on column UTTAK_RESULTAT_PERIODE_SOKNAD.MOTTATT_DATO_3 is 'Mottatt dato hentet fra tilhørende søknad';

