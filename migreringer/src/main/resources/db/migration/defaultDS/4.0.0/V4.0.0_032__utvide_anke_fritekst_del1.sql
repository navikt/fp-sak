drop table BEHANDLING_OVERLAPP_INFOTRYGD purge;
drop sequence SEQ_BEH_OVERLAPP_INFOTRYGD;

ALTER TABLE ANKE_VURDERING_RESULTAT ADD (fritekst_til_brev_ny CLOB);
comment on column ANKE_VURDERING_RESULTAT.fritekst_til_brev_ny IS 'Fritekstfelt for tekst i ankebrev';

UPDATE ANKE_VURDERING_RESULTAT SET fritekst_til_brev_ny = fritekst_til_brev;
