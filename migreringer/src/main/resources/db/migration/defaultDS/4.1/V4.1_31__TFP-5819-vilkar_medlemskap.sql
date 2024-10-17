CREATE TABLE VILKAR_MEDLEMSKAP
(
    ID                 NUMBER(19)                              not null
        constraint PK_VILKAR_MEDLEMSKAP primary key,
    VILKAR_RESULTAT_ID NUMBER(19)                              not null
        constraint FK_VILKAR_MEDLEMSKAP_1 references VILKAR_RESULTAT,
    OPPHOR_FOM         DATE,
    OPPHOR_ARSAK       VARCHAR2(100 char) default '-'          not null,
    MEDLEM_FOM         DATE,
    AKTIV              VARCHAR2(1 char)   default 'J'          not null,
    VERSJON            NUMBER(19)         default 0            not null,
    OPPRETTET_AV       VARCHAR2(20 char)                       not null,
    OPPRETTET_TID      TIMESTAMP(3)       default systimestamp not null,
    ENDRET_AV          VARCHAR2(20 char),
    ENDRET_TID         TIMESTAMP(3)
);

create index IDX_VILKAR_MEDLEMSKAP_1 on VILKAR_MEDLEMSKAP (VILKAR_RESULTAT_ID);

COMMENT ON TABLE VILKAR_MEDLEMSKAP IS 'Tabell som inneholder data for medlemskapsvilkåret';
COMMENT ON COLUMN VILKAR_MEDLEMSKAP.ID IS 'Primærnøkkel';
COMMENT ON COLUMN VILKAR_MEDLEMSKAP.VILKAR_RESULTAT_ID IS 'Fremmednøkkel til vilkårsresultatet';
COMMENT ON COLUMN VILKAR_MEDLEMSKAP.OPPHOR_FOM IS 'Opphørsdato for medlemskapet';
COMMENT ON COLUMN VILKAR_MEDLEMSKAP.OPPHOR_ARSAK IS 'Opphørsårsak for medlemskapet';
COMMENT ON COLUMN VILKAR_MEDLEMSKAP.MEDLEM_FOM IS 'Bruker anses som medlem fra og med dato';

create sequence SEQ_VILKAR_MEDLEMSKAP increment by 50 nocache;
