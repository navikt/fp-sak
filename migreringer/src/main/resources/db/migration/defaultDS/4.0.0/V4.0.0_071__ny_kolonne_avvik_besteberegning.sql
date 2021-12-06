alter table BG_BESTEBEREGNINGGRUNNLAG add avvik_belop NUMBER(19,2);

comment on column BG_BESTEBEREGNINGGRUNNLAG.avvik_belop is 'Hvor mye avviker beregningen mellom første og tredje ledd';
