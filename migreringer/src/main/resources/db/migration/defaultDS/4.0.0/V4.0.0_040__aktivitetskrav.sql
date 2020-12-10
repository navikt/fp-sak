create table YF_AKTIVITETSKRAV_PERIODER
(
    ID            NUMBER(19, 0)                          NOT NULL,
    OPPRETTET_AV  VARCHAR2(20 CHAR) DEFAULT 'VL'         NOT NULL,
    OPPRETTET_TID TIMESTAMP(3)      DEFAULT systimestamp NOT NULL,
    ENDRET_AV     VARCHAR2(20 CHAR),
    ENDRET_TID    TIMESTAMP(3)
);

alter table YF_AKTIVITETSKRAV_PERIODER
    add constraint PK_YF_AKTIVITETSKRAV_PERIODER primary key ("ID");
create sequence SEQ_YF_AKTIVITETSKRAV_PERIODER minvalue 1 increment by 50 start with 1 nocache nocycle;
comment on table YF_AKTIVITETSKRAV_PERIODER is 'Koblingstabell for perioder med aktivitetskrav';
comment on column YF_AKTIVITETSKRAV_PERIODER.ID IS 'PK';

ALTER TABLE GR_YTELSES_FORDELING
    add opprinnelige_aktkrav_per_id NUMBER(19, 0);

CREATE INDEX IDX_GR_YTELSES_FORDELING_16 ON GR_YTELSES_FORDELING (opprinnelige_aktkrav_per_id);
ALTER TABLE GR_YTELSES_FORDELING
    ADD CONSTRAINT FK_YF_DOKUMENTASJON_PERIODE_13 FOREIGN KEY (opprinnelige_aktkrav_per_id)
        REFERENCES YF_AKTIVITETSKRAV_PERIODER (ID) ENABLE;
comment on column GR_YTELSES_FORDELING.opprinnelige_aktkrav_per_id IS 'FK: Perioder fra forrige behandling der det er avklart mors aktivitet. Mtp aktivitetskravet.';

ALTER TABLE GR_YTELSES_FORDELING
    add saksbehandlede_aktkrav_per_id NUMBER(19, 0);
CREATE INDEX IDX_GR_YTELSES_FORDELING_17 ON GR_YTELSES_FORDELING (saksbehandlede_aktkrav_per_id);
ALTER TABLE GR_YTELSES_FORDELING
    ADD CONSTRAINT FK_YF_DOKUMENTASJON_PERIODE_14 FOREIGN KEY (saksbehandlede_aktkrav_per_id)
        REFERENCES YF_AKTIVITETSKRAV_PERIODER (ID) ENABLE;
comment on column GR_YTELSES_FORDELING.saksbehandlede_aktkrav_per_id IS 'FK: Perioder der saksbehandler har avklart mors aktivitet. Mtp aktivitetskravet';

create table YF_AKTIVITETSKRAV_PERIODE
(
    ID                            NUMBER(19, 0)                          NOT NULL,
    YF_AKTIVITETSKRAV_PERIODER_ID NUMBER(19, 0)                          NOT NULL,
    FOM                           DATE                                   NOT NULL,
    TOM                           DATE                                   NOT NULL,
    AVKLARING                     VARCHAR2(50 char)                      NOT NULL,
    BEGRUNNELSE                   VARCHAR2(4000 char)                    NOT NULL,
    OPPRETTET_AV                  VARCHAR2(20 CHAR) DEFAULT 'VL'         NOT NULL,
    OPPRETTET_TID                 TIMESTAMP(3)      DEFAULT systimestamp NOT NULL,
    ENDRET_AV                     VARCHAR2(20 CHAR),
    ENDRET_TID                    TIMESTAMP(3)
);

alter table YF_AKTIVITETSKRAV_PERIODE
    add constraint PK_YF_AKTIVITETSKRAV_PERIODE primary key ("ID");
create sequence SEQ_YF_AKTIVITETSKRAV_PERIODE minvalue 1 increment by 50 start with 1 nocache nocycle;
alter table YF_AKTIVITETSKRAV_PERIODE
    add constraint FK_YF_AKTIVITETSKRAV_PERIODE_1
        foreign key (YF_AKTIVITETSKRAV_PERIODER_ID) references YF_AKTIVITETSKRAV_PERIODER (ID);

CREATE INDEX IDX_YF_AKTKRAV_PERIODE_1 ON YF_AKTIVITETSKRAV_PERIODE (YF_AKTIVITETSKRAV_PERIODER_ID);

comment on table YF_AKTIVITETSKRAV_PERIODE is 'Avklaring av dokumentasjon mtp aktivitetskravet';
comment on column YF_AKTIVITETSKRAV_PERIODE.ID IS 'PK';
comment on column YF_AKTIVITETSKRAV_PERIODE.YF_AKTIVITETSKRAV_PERIODER_ID IS 'FK';
comment on column YF_AKTIVITETSKRAV_PERIODE.AVKLARING IS 'Resultat av avklaring om mor er i aktivitet';
comment on column YF_AKTIVITETSKRAV_PERIODE.BEGRUNNELSE IS 'Begrunnelse for resultat av avklaring';
