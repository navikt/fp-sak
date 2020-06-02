alter table YF_FORDELING_PERIODE add MOTTATT_DATO DATE;
comment on column YF_FORDELING_PERIODE.MOTTATT_DATO is 'Mottatt dato hentet fra tilhørende søknad';

alter table UTTAK_RESULTAT_PERIODE_SOKNAD add MOTTATT_DATO_2 DATE;
comment on column UTTAK_RESULTAT_PERIODE_SOKNAD.MOTTATT_DATO_2 is 'Mottatt dato hentet fra tilhørende søknad';

