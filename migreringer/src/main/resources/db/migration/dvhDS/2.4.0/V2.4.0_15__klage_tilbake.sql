-- klage ref til tilbakekreving
alter table KLAGE_FORMKRAV_DVH ADD PAAKLAGD_TILBAKEKREVING_UUID RAW(16);
COMMENT ON COLUMN KLAGE_FORMKRAV_DVH.PAAKLAGD_TILBAKEKREVING_UUID IS 'UUID for påklagd tilbakekrevingsbehandling';

-- anke dato oversendt Trygderetten
ALTER TABLE ANKE_VURDERING_RESULTAT_DVH ADD TR_OVERSENDT_DATO DATE;
comment on column ANKE_VURDERING_RESULTAT_DVH.TR_OVERSENDT_DATO IS 'Dato anken er sendt til Trygderetten';
