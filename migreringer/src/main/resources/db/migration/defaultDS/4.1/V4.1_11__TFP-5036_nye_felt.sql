ALTER TABLE GR_YTELSES_FORDELING ADD (
    OVERSTYRT_OMSORG VARCHAR2(1 char),
    MIGRERT_DOK VARCHAR2(1 char) default 'N'
);
COMMENT ON COLUMN GR_YTELSES_FORDELING.OVERSTYRT_OMSORG IS 'Vurdering av omsorg for barnet (ved utledet avklaringsbehov)';
COMMENT ON COLUMN GR_YTELSES_FORDELING.MIGRERT_DOK IS 'Er dokumentasjonsperiode og aktivitetskrav migrert';
