create table BEHANDLING_DOKUMENT (
    ID number (19,0) not null,
    BEHANDLING_ID number (19,0) not null,
    OVERSTYRT_BREV_OVERSKRIFT varchar2 (200 char),
    OVERSTYRT_BREV_FRITEKST clob,
    VEDTAK_FRITEKST clob,
    OPPRETTET_AV varchar2 (20 char) default 'VL' not null,
    OPPRETTET_TID timestamp default systimestamp not null,
    ENDRET_AV varchar2 (20 char),
    ENDRET_TID timestamp
);
alter table BEHANDLING_DOKUMENT add constraint PK_BEHANDLING_DOKUMENT primary key ("ID");
create sequence SEQ_BEHANDLING_DOKUMENT minvalue 1 increment by 50 start with 1 nocache nocycle;
alter table BEHANDLING_DOKUMENT add constraint FK_BEHANDLING_DOK_BEHANDLING foreign key (BEHANDLING_ID) references BEHANDLING (ID);
alter table BEHANDLING_DOKUMENT add constraint CHK_UNIQUE_BEH_DOK_1 unique (BEHANDLING_ID)
    using index (create unique index UIDX_BEHANDLING_DOKUMENT_1 on BEHANDLING_DOKUMENT (BEHANDLING_ID));

comment on table BEHANDLING_DOKUMENT is 'Dokument-informasjon relatert til en behandling';
comment on column BEHANDLING_DOKUMENT.ID IS 'Primary key';
comment on column BEHANDLING_DOKUMENT.BEHANDLING_ID IS 'FK: BEHANDLING';
comment on column BEHANDLING_DOKUMENT.OVERSTYRT_BREV_OVERSKRIFT IS 'Hovedoverskrift i fritekstbrev når saksbehandler overstyrer brevet';
comment on column BEHANDLING_DOKUMENT.OVERSTYRT_BREV_FRITEKST IS 'Friteksten i fritekstbrev når saksbehandler overstyrer brevet';
comment on column BEHANDLING_DOKUMENT.VEDTAK_FRITEKST IS 'Fritekst til søker som inneholder begrunnelse/vurderinger som er gjort i vedtaket';


create table BEHANDLING_DOKUMENT_BESTILT (
    ID number (19,0) not null,
    BEHANDLING_DOKUMENT_ID number (19,0) not null,
    DOKUMENT_MAL_TYPE varchar2 (7 char) not null,
    OPPRETTET_AV varchar2 (20 char) default 'VL' not null,
    OPPRETTET_TID timestamp default systimestamp not null,
    ENDRET_AV varchar2 (20 char),
    ENDRET_TID timestamp
);
alter table BEHANDLING_DOKUMENT_BESTILT add constraint PK_BEHANDLING_DOK_BESTILT primary key ("ID");
create sequence SEQ_BEHANDLING_DOK_BESTILT minvalue 1 increment by 50 start with 1 nocache nocycle;
alter table BEHANDLING_DOKUMENT_BESTILT add constraint FK_BEHANDLING_DOK_BESTILT foreign key (BEHANDLING_DOKUMENT_ID) references BEHANDLING_DOKUMENT (ID);
create index IDX_BEHANDLING_DOK_BESTILT_1 on BEHANDLING_DOKUMENT_BESTILT (BEHANDLING_DOKUMENT_ID);

comment on table BEHANDLING_DOKUMENT_BESTILT is 'Tabellen inneholder ett innslag pr dokument som er bestilt fra Fpformidling';
comment on column BEHANDLING_DOKUMENT_BESTILT.ID IS 'Primary key';
comment on column BEHANDLING_DOKUMENT_BESTILT.BEHANDLING_DOKUMENT_ID IS 'FK: BEHANDLING_DOKUMENT, som er koblingstabellen mot BEHANDLING';
comment on column BEHANDLING_DOKUMENT_BESTILT.DOKUMENT_MAL_TYPE IS 'Koden for dokument-malen som er bestilt';
