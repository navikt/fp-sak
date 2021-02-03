package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;

public class RyddBeregningsgrunnlag {

    private final BehandlingskontrollKontekst kontekst;
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    RyddBeregningsgrunnlag(BeregningsgrunnlagRepository beregningsgrunnlagRepository, BehandlingskontrollKontekst kontekst) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.kontekst = kontekst;
    }

    public void ryddFastsettSkjæringstidspunktVedTilbakeføring() {
        beregningsgrunnlagRepository.deaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId());
    }

    public void gjenopprettOppdatertBeregningsgrunnlag() {
        beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

    public void gjenopprettFastsattBeregningAktivitetBeregningsgrunnlag() {
        boolean bgReaktivert = beregningsgrunnlagRepository
            .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        if (!bgReaktivert) {
            gjenopprettFørsteBeregningsgrunnlag();
        }
    }

    public void ryddForeslåBesteberegningVedTilbakeføring() {
        beregningsgrunnlagRepository
            .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.BESTEBEREGNET);
    }

    public void ryddForeslåBeregningsgrunnlagVedTilbakeføring() {
        beregningsgrunnlagRepository
            .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.FORESLÅTT);
    }

    public void ryddVurderRefusjonBeregningsgrunnlagVedTilbakeføring() {
        beregningsgrunnlagRepository
            .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.VURDERT_REFUSJON);
    }

    private void gjenopprettFørsteBeregningsgrunnlag() {
        beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.OPPRETTET);
    }

    public void ryddFordelBeregningsgrunnlagVedTilbakeføring(boolean harAksjonspunktSomErUtførtIUtgang) {
        if (harAksjonspunktSomErUtførtIUtgang) {
            if (beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.FASTSATT_INN).isPresent()) {
                beregningsgrunnlagRepository
                    .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.FASTSATT_INN);
            } else {
                beregningsgrunnlagRepository
                    .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
            }
        } else {
            beregningsgrunnlagRepository
                .reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
        }
    }
}
