package no.nav.foreldrepenger.domene.prosess;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.mappers.fra_entitet_til_modell.FraEntitetTilBehandlingsmodellMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;

import java.util.Optional;

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
    public Optional<BeregningsgrunnlagGrunnlag> hent(Long behandlingId) {
        var entitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        return entitet.map(FraEntitetTilBehandlingsmodellMapper::mapBeregningsgrunnlagGrunnlag);
    }
}
