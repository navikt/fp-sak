CREATE TABLE MEDLEMSKAPSVILKÅR_VURDERING
(
    ID                 NUMBER(19)                              not null
        constraint PK_MEDLEMSKAPSVILKÅR_VURDERING primary key,
    VILKAR_RESULTAT_ID NUMBER(19)                              not null
        constraint FK_MEDLEMSKAPSVILKÅR_VURDERING_1 references VILKAR_RESULTAT,
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

create index IDX_MEDLEMSKAPSVILKÅR_VURDERING_1 on MEDLEMSKAPSVILKÅR_VURDERING (VILKAR_RESULTAT_ID);

COMMENT ON TABLE MEDLEMSKAPSVILKÅR_VURDERING IS 'Tabell som inneholder data fra avklaringer rundt medlemskapsvilkåret';
COMMENT ON COLUMN MEDLEMSKAPSVILKÅR_VURDERING.ID IS 'Primærnøkkel';
COMMENT ON COLUMN MEDLEMSKAPSVILKÅR_VURDERING.VILKAR_RESULTAT_ID IS 'Fremmednøkkel til vilkårsresultatet';
COMMENT ON COLUMN MEDLEMSKAPSVILKÅR_VURDERING.OPPHOR_FOM IS 'Opphørsdato for medlemskapet';
COMMENT ON COLUMN MEDLEMSKAPSVILKÅR_VURDERING.OPPHOR_ARSAK IS 'Opphørsårsak for medlemskapet';
COMMENT ON COLUMN MEDLEMSKAPSVILKÅR_VURDERING.MEDLEM_FOM IS 'Bruker anses som medlem fra og med dato';

create sequence SEQ_MEDLEMSKAPSVILKÅR_VURDERING increment by 50 nocache;
