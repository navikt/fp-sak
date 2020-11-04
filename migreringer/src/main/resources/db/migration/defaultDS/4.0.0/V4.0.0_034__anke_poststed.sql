-- ny tabell for poststed
create table poststed (
    poststednummer  varchar2(16 char) not null
        constraint pk_poststed
            primary key,
    poststednavn    varchar2(256 char) not null,
    gyldigfom       DATE DEFAULT SYSDATE NOT NULL,
    gyldigtom       DATE DEFAULT TO_DATE('31.12.9999','DD.MM.YYYY') NOT NULL,
    OPPRETTET_AV    VARCHAR2(20 CHAR)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID   TIMESTAMP(3) DEFAULT systimestamp            NOT NULL,
    ENDRET_AV       VARCHAR2(20 CHAR),
    ENDRET_TID      TIMESTAMP(3)
);

comment on table poststed is 'Tabell for sentralt kodeverk Postnummer';
comment on column poststed.poststednummer is 'Postnummer';
comment on column poststed.poststednavn is 'Poststed';
comment on column poststed.gyldigFom is 'Gyldig fra dato';
comment on column poststed.gyldigTom is 'Gyldig til dato';

-- ubrukte kolonner
alter table KLAGE_VURDERING_RESULTAT drop column KLAGE_AVVIST_AARSAK;
alter table KLAGE_VURDERING_RESULTAT drop column VEDTAKSDATO_PAKLAGD_BEHANDLING;

-- endringer anke for Trygderettsresultat
ALTER TABLE ANKE_VURDERING_RESULTAT DROP COLUMN fritekst_til_brev;
ALTER TABLE ANKE_VURDERING_RESULTAT RENAME COLUMN fritekst_til_brev_ny TO fritekst_til_brev;

ALTER TABLE ANKE_VURDERING_RESULTAT ADD TR_VURDERING VARCHAR2(100 CHAR);
ALTER TABLE ANKE_VURDERING_RESULTAT ADD TR_VURDERING_OMGJOER VARCHAR2(100 CHAR);
ALTER TABLE ANKE_VURDERING_RESULTAT ADD TR_OMGJOER_AARSAK VARCHAR2(100 CHAR);

comment on column ANKE_VURDERING_RESULTAT.TR_VURDERING IS 'Trygderettens vurdering';
comment on column ANKE_VURDERING_RESULTAT.TR_VURDERING_OMGJOER IS 'Trygderettens omgjøring';
comment on column ANKE_VURDERING_RESULTAT.TR_OMGJOER_AARSAK IS 'Trygderettens årsak til omgjøring';

update ANKE_VURDERING_RESULTAT set TR_VURDERING = '-' where TR_VURDERING is null;
update ANKE_VURDERING_RESULTAT set TR_VURDERING_OMGJOER = '-' where TR_VURDERING_OMGJOER is null;
update ANKE_VURDERING_RESULTAT set TR_OMGJOER_AARSAK = '-' where TR_OMGJOER_AARSAK is null;

