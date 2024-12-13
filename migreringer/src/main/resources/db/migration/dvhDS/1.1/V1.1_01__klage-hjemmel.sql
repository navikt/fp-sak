alter table BEHANDLING_DVH modify (FAGSAK_ID null);

alter table BEHANDLING_DVH ADD (KLAGE_HJEMMEL VARCHAR2(100 char));
comment on column BEHANDLING_DVH.KLAGE_HJEMMEL is 'Lovhjemmel som klagen gjelder';
