ALTER TABLE BEHANDLING_DOKUMENT ADD overstyrt_brev_utgangspunkt_html CLOB;
COMMENT ON COLUMN BEHANDLING_DOKUMENT.overstyrt_brev_utgangspunkt_html is 'Utgangspunktet for overstyringen av vedtaksbrevet';
