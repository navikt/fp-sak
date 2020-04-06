CREATE TABLE BG_REFUSJON_PERIODE (
                id NUMBER(19,0) NOT NULL,
                BG_REFUSJON_OVERSTYRING_ID NUMBER(19,0) NOT NULL,
                arbeidsforhold_intern_id  RAW(16),
                fom DATE not null,
                OPPRETTET_AV varchar2 (20 char) default 'VL' not null,
                OPPRETTET_TID timestamp default systimestamp not null,
                ENDRET_AV varchar2 (20 char),
	            versjon NUMBER(19,0) DEFAULT 0 NOT NULL,
                ENDRET_TID timestamp
);
alter table BG_REFUSJON_PERIODE add constraint PK_BG_REFUSJON_PERIODE primary key (ID);
create sequence SEQ_BG_REFUSJON_PERIODE minvalue 1 increment by 50 start with 1 nocache nocycle;
alter table BG_REFUSJON_PERIODE add constraint FK_BG_REFUSJON_PERIODE foreign key (BG_REFUSJON_OVERSTYRING_ID) references BG_REFUSJON_OVERSTYRING (ID);
create index IDX_BG_REFUSJON_PERIODE_1 on BG_REFUSJON_PERIODE (BG_REFUSJON_OVERSTYRING_ID);

COMMENT ON TABLE BG_REFUSJON_PERIODE  IS 'Tabell som holder på hvilke refusjonskrav som skal gjelde fra hvilken dato gitt arbeidsgiver og arbeidsforhold';
COMMENT ON COLUMN BG_REFUSJON_PERIODE.arbeidsforhold_intern_id  IS 'Globalt unikt arbeidsforhold id generert for arbeidsgiver/arbeidsforhold. I motsetning til arbeidsforhold_ekstern_id som holder arbeidsgivers referanse';
COMMENT ON COLUMN BG_REFUSJON_PERIODE.fom IS 'Fra og med datoen refusjon skal tas med i beregningen';
COMMENT ON COLUMN BG_REFUSJON_PERIODE.BG_REFUSJON_OVERSTYRING_ID IS 'Foreign key til tabell BG_REFUSJON_OVERSTYRING';

alter table BG_REFUSJON_OVERSTYRING modify (fom null);
