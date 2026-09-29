ALTER TABLE MOTTATT_DOKUMENT set unused (DOKUMENT_KATEGORI);
alter table FAGSAK_PROSESS_TASK modify (FAGSAK_ID null);
alter table FAGSAK_PROSESS_TASK add (SAKSNUMMER VARCHAR2(50 char));

comment on column FAGSAK_PROSESS_TASK.SAKSNUMMER is 'Saksnummer for kobling til fagsak';
