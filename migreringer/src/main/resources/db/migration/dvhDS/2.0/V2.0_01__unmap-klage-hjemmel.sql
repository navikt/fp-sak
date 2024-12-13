alter table BEHANDLING_DVH set unused (RELATERT_TIL, ENDRET_AV);

alter table BEHANDLING_DVH modify (BEHANDLING_ID null);
alter table BEHANDLING_DVH modify (FAGSAK_ID null);

alter table BEHANDLING_DVH ADD (KLAGE_HJEMMEL VARCHAR2(100 char));
comment on column BEHANDLING_DVH.BEHANDLING_DVH is 'Lovhjemmel som klagen gjelder';
