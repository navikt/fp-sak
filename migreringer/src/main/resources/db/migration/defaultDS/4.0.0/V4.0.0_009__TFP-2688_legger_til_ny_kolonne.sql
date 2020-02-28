ALTER TABLE SAMMENLIGNINGSGRUNNLAG add AVVIK_PROMILLE NUMBER(27,10);
ALTER TABLE BG_SG_PR_STATUS add AVVIK_PROMILLE NUMBER(27,10);

comment on column SAMMENLIGNINGSGRUNNLAG.AVVIK_PROMILLE IS 'Avviket på sammenligningsgrunnlaget angitt i promille';
comment on column BG_SG_PR_STATUS.AVVIK_PROMILLE IS 'Avviket på sammenligningsgrunnlaget angitt i promille';
