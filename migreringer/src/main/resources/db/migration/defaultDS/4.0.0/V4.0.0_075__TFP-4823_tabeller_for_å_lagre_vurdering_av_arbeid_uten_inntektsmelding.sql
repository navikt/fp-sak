CREATE TABLE ARBEIDSFORHOLD_VALG
(
                                          id NUMBER(19,0) NOT NULL,
                                          behandling_id NUMBER(19,0) NOT NULL,
                                          versjon NUMBER(19,0) DEFAULT 0 NOT NULL,
                                          opprettet_av VARCHAR2(20 CHAR) DEFAULT 'VL' NOT NULL,
                                          opprettet_tid TIMESTAMP (3) DEFAULT systimestamp NOT NULL,
                                          endret_av VARCHAR2(20 CHAR),
                                          endret_tid TIMESTAMP (3),
                                          vurdering VARCHAR2(50 CHAR) NOT NULL,
                                          begrunnelse VARCHAR2(4000 CHAR) NOT NULL,
                                          arbeidsgiver_ident VARCHAR2(9 CHAR),
                                          arbeidsforhold_intern_id RAW(16)
);

ALTER TABLE ARBEIDSFORHOLD_VALG ADD CONSTRAINT PK_ARBEIDSFORHOLD_VALG PRIMARY KEY (id);
ALTER TABLE ARBEIDSFORHOLD_VALG ADD CONSTRAINT FK_ARBEIDSFORHOLD_VALG_1 FOREIGN KEY (behandling_id) REFERENCES BEHANDLING(id);

CREATE INDEX IDX_ARBEIDSFORHOLD_VALG_1 ON ARBEIDSFORHOLD_VALG (behandling_id);
CREATE SEQUENCE SEQ_ARBEIDSFORHOLD_VALG MINVALUE 1 START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;

COMMENT ON TABLE ARBEIDSFORHOLD_VALG  IS 'Valget fra saksbehandler om hva som skal gjøres med arbeidsforhold som mangler inntektsmelding';
COMMENT ON COLUMN ARBEIDSFORHOLD_VALG.behandling_id IS 'Behandlingen valget er knyttet til';
COMMENT ON COLUMN ARBEIDSFORHOLD_VALG.vurdering IS 'Kodeverk av saksbehandlers vurdering';
COMMENT ON COLUMN ARBEIDSFORHOLD_VALG.begrunnelse IS 'Saksbehandlers fritekst begrunnelse';
COMMENT ON COLUMN ARBEIDSFORHOLD_VALG.arbeidsgiver_ident IS 'Arbeidsgivers identifikator, vil enten være et organisasjonsnummer eller en aktørId';
COMMENT ON COLUMN ARBEIDSFORHOLD_VALG.arbeidsforhold_intern_id IS 'Arbeidsforhold intern-id';
