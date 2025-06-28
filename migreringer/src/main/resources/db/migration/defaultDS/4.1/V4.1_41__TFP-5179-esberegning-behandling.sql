ALTER TABLE BR_LEGACY_ES_BEREGNING MODIFY(BEREGNING_RESULTAT_ID NULL);

alter table BR_LEGACY_ES_BEREGNING ADD (
    BEHANDLING_ID NUMBER(19)
        constraint FK_BEREGNING_2 references BEHANDLING,
    AKTIV VARCHAR2(1 char) default 'J' not null
        constraint CHK_AKTIV_ESBEREGN check (aktiv IN ('J', 'N')));

comment on column BR_LEGACY_ES_BEREGNING.AKTIV is 'Om beregning er aktiv';
comment on column BR_LEGACY_ES_BEREGNING.BEHANDLING_ID is 'FK behandling';

create index IDX_BEREGNING_2
    on BR_LEGACY_ES_BEREGNING (BEHANDLING_ID);
