alter table UTTAK_RESULTAT_PERIODE add MANUELL_BEHANDLING_AARSAK VARCHAR2(100 char) default '-' not null;

alter table UTTAK_RESULTAT_DOK_REGEL modify (MANUELL_BEHANDLING_AARSAK null);

comment on column UTTAK_RESULTAT_PERIODE.MANUELL_BEHANDLING_AARSAK is 'Årsak til manuell behandling';
