create sequence SEQ_BEHANDLING_DVH
    increment by 50
    nocache;

create table BEHANDLING_DVH
(
    TRANS_ID                 NUMBER(19)         not null
        constraint PK_BEHANDLING_DVH
            primary key,
    TRANS_TID                TIMESTAMP(3)       not null,
    BEHANDLING_ID            NUMBER(19)         not null,
    FAGSAK_ID                NUMBER(19)         not null,
    FUNKSJONELL_TID          TIMESTAMP(3)       not null,
    BEHANDLING_RESULTAT_TYPE VARCHAR2(100 char),
    BEHANDLING_TYPE          VARCHAR2(100 char) not null,
    BEHANDLING_STATUS        VARCHAR2(100 char) not null,
    BEHANDLENDE_ENHET        VARCHAR2(10 char),
    UTLANDSTILSNITT          VARCHAR2(100 char),
    ENDRET_AV                VARCHAR2(20 char),
    ANSVARLIG_BESLUTTER      VARCHAR2(100 char),
    ANSVARLIG_SAKSBEHANDLER  VARCHAR2(100 char),
    RELATERT_TIL             NUMBER(19),
    FOERSTE_STOENADSDAG      DATE,
    UUID                     RAW(16),
    PAPIR_SOKNAD             VARCHAR2(1 char),
    BEHANDLING_METODE        VARCHAR2(100 char),
    REVURDERING_AARSAK       VARCHAR2(100 char),
    MOTTATT_TID              TIMESTAMP(3),
    REGISTRERT_TID           TIMESTAMP(3),
    KAN_BEHANDLES_TID        TIMESTAMP(3),
    FERDIG_BEHANDLET_TID     TIMESTAMP(3),
    FORVENTET_OPPSTART_TID   DATE,
    VILKAR_IKKE_OPPFYLT      VARCHAR2(100 char),
    VEDTAK_RESULTAT_TYPE     VARCHAR2(100 char),
    VEDTAK_TID               TIMESTAMP(3),
    UTBETALT_TID             DATE,
    FAMILIE_HENDELSE_TYPE    VARCHAR2(100 char),
    SAKSNUMMER               VARCHAR2(100 char),
    AKTOER_ID                VARCHAR2(50 char),
    YTELSE_TYPE              VARCHAR2(100 char),
    RELATERT_TIL_UUID        RAW(16),
    OMGJOERING_AARSAK        VARCHAR2(100 char),
    RELATERT_TIL_FAGSYSTEM   VARCHAR2(20 char) default 'FPSAK'
);

comment on table BEHANDLING_DVH is 'En transaksjonstabell med alle endringer på behandlingen.';

comment on column BEHANDLING_DVH.TRANS_ID is 'Primær nøkkel for behandling transaksjoner';

comment on column BEHANDLING_DVH.TRANS_TID is 'Timestamp som forteller nå transaksjonen inntraff. ';

comment on column BEHANDLING_DVH.BEHANDLING_ID is 'Id til Behandling';

comment on column BEHANDLING_DVH.FAGSAK_ID is 'Id til Fagsak';

comment on column BEHANDLING_DVH.FUNKSJONELL_TID is 'Et tidsstempel når transaksjonen er funksjonelt gyldig fra.';

comment on column BEHANDLING_DVH.BEHANDLING_RESULTAT_TYPE is 'Behandlingsresultat type.';

comment on column BEHANDLING_DVH.BEHANDLING_TYPE is 'Behandlingstype';

comment on column BEHANDLING_DVH.BEHANDLING_STATUS is 'Behandlingsstatus';

comment on column BEHANDLING_DVH.BEHANDLENDE_ENHET is 'Enheten som sitter på behandlinge på dette tidspunktet.';

comment on column BEHANDLING_DVH.UTLANDSTILSNITT is 'Kodeverk: UTLAND, NASJONAL. ';

comment on column BEHANDLING_DVH.ENDRET_AV is 'Opprettet_av eller endret_av i VL';

comment on column BEHANDLING_DVH.ANSVARLIG_BESLUTTER is 'Ansvarlige saksbehandler.';

comment on column BEHANDLING_DVH.ANSVARLIG_SAKSBEHANDLER is 'Ansvarlig besluttningstager';

comment on column BEHANDLING_DVH.RELATERT_TIL is 'Behandling ID til forrige behandling';

comment on column BEHANDLING_DVH.FOERSTE_STOENADSDAG is 'Dato for uttak av rettighet';

comment on column BEHANDLING_DVH.UUID is 'UUID for behandling';

comment on column BEHANDLING_DVH.PAPIR_SOKNAD is 'Flagg for behandling: 1-papir,0-digital';

comment on column BEHANDLING_DVH.BEHANDLING_METODE is 'Utledet behandlingsmetode';

comment on column BEHANDLING_DVH.REVURDERING_AARSAK is 'Hovedårsak for revurdering';

comment on column BEHANDLING_DVH.MOTTATT_TID is 'Søknad eller klage mottatt';

comment on column BEHANDLING_DVH.REGISTRERT_TID is 'Behandling registrert';

comment on column BEHANDLING_DVH.KAN_BEHANDLES_TID is 'Behandling kan ikke begynne før';

comment on column BEHANDLING_DVH.FERDIG_BEHANDLET_TID is 'Behandling avsluttet';

comment on column BEHANDLING_DVH.FORVENTET_OPPSTART_TID is 'Første uttaksdato fra søknad';

comment on column BEHANDLING_DVH.VILKAR_IKKE_OPPFYLT is 'Vilkår som ikke er oppfylt';

comment on column BEHANDLING_DVH.VEDTAK_RESULTAT_TYPE is 'Vedtaksresultat';

comment on column BEHANDLING_DVH.VEDTAK_TID is 'Tidspunkt da vedtak ble fattet';

comment on column BEHANDLING_DVH.UTBETALT_TID is 'Dato for første utbetaling';

comment on column BEHANDLING_DVH.FAMILIE_HENDELSE_TYPE is 'Omstendighetene som saken gjelder';

comment on column BEHANDLING_DVH.SAKSNUMMER is 'Saksnummer behandlingen tilhører';

comment on column BEHANDLING_DVH.AKTOER_ID is 'AktørId for bruker saken gjelder';

comment on column BEHANDLING_DVH.YTELSE_TYPE is 'Ytelsen saken gjelder';

comment on column BEHANDLING_DVH.RELATERT_TIL_UUID is 'Relatert til behandling med UUID';

comment on column BEHANDLING_DVH.OMGJOERING_AARSAK is 'Årsak til omgjøring av klage eller anke';

comment on column BEHANDLING_DVH.RELATERT_TIL_FAGSYSTEM is 'Relatert til behandling i fagsystem';
