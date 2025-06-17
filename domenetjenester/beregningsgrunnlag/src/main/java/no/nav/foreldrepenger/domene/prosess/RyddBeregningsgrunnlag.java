package no.nav.foreldrepenger.domene.prosess;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;

public class RyddBeregningsgrunnlag {

    private final BehandlingskontrollKontekst kontekst;
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    RyddBeregningsgrunnlag(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                           BehandlingskontrollKontekst kontekst) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.kontekst = kontekst;
    }

    public void ryddFastsettSkjæringstidspunktVedTilbakeføring() {
        if (harAktivBeregningIFpsak(kontekst.getBehandlingId())) {
            beregningsgrunnlagRepository.deaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId());
        }
    }

    public void gjenopprettOppdatertBeregningsgrunnlag() {
        if (harAktivBeregningIFpsak(kontekst.getBehandlingId())) {
            beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        }
    }

    public void gjenopprettFastsattBeregningAktivitetBeregningsgrunnlag() {
        if (harAktivBeregningIFpsak(kontekst.getBehandlingId())) {
            var bgReaktivert = beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(
                kontekst.getBehandlingId(), BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
            if (!bgReaktivert) {
                gjenopprettFørsteBeregningsgrunnlag();
            }
        }
    }

    public void ryddForeslåBesteberegningVedTilbakeføring() {
        if (harAktivBeregningIFpsak(kontekst.getBehandlingId())) {
            beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                BeregningsgrunnlagTilstand.BESTEBEREGNET);
        }
    }

    public void ryddForeslåBeregningsgrunnlagVedTilbakeføring() {
        if (harAktivBeregningIFpsak(kontekst.getBehandlingId())) {
            beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                BeregningsgrunnlagTilstand.FORESLÅTT);
        }
    }

    public void ryddFortsettForeslåBeregningsgrunnlagVedTilbakeføring() {
        if (harAktivBeregningIFpsak(kontekst.getBehandlingId())) {
            beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                BeregningsgrunnlagTilstand.FORESLÅTT_2);
        }
    }

    public void ryddVurderVilkårBeregningsgrunnlagVedTilbakeføring() {
        if (harAktivBeregningIFpsak(kontekst.getBehandlingId())) {
            beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                BeregningsgrunnlagTilstand.VURDERT_VILKÅR);
        }
    }

    public void ryddVurderRefusjonBeregningsgrunnlagVedTilbakeføring() {
        if (harAktivBeregningIFpsak(kontekst.getBehandlingId())) {
            beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                BeregningsgrunnlagTilstand.VURDERT_REFUSJON);
        }
    }

    private void gjenopprettFørsteBeregningsgrunnlag() {
        beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
            BeregningsgrunnlagTilstand.OPPRETTET);
    }

    public void ryddFordelBeregningsgrunnlagVedTilbakeføring(boolean harAksjonspunktSomErUtførtIUtgang) {
        if (harAktivBeregningIFpsak(kontekst.getBehandlingId())) {
            if (harAksjonspunktSomErUtførtIUtgang) {
                if (beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                    BeregningsgrunnlagTilstand.FASTSATT_INN).isPresent()) {
                    beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                        BeregningsgrunnlagTilstand.FASTSATT_INN);
                } else {
                    beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                        BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
                }
            } else {
                beregningsgrunnlagRepository.reaktiverBeregningsgrunnlagGrunnlagEntitet(kontekst.getBehandlingId(),
                    BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
            }
        }
    }

    private boolean harAktivBeregningIFpsak(Long behandlingId) {
        return beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId).isPresent();
    }

}
