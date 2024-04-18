package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.mappers.fra_entitet_til_modell.FraEntitetTilBehandlingsmodellMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;

@ApplicationScoped
public class BeregningFPSAK implements BeregningAPI {
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    BeregningFPSAK() {
        // CDI
    }

    @Inject
    public BeregningFPSAK(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse) {
        var entitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId());
        return entitet.map(FraEntitetTilBehandlingsmodellMapper::mapBeregningsgrunnlagGrunnlag);
    }

    @Override
    public void beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType stegType) {

    }
}
