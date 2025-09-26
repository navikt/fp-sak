
alter table FH_ADOPSJON add VILKAAR_HJEMMEL VARCHAR2(100 char) default '-' not null;

comment on column FH_ADOPSJON.VILKAAR_HJEMMEL is 'Hjemmel brukt ved vurdering av vilkår for adopsjon';
