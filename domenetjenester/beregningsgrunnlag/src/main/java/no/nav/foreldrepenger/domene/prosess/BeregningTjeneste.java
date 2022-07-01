package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;

@ApplicationScoped
public class BeregningTjeneste {
    private BeregningFPSAK fpsakBeregner;
    private BeregningKalkulus kalkulusBeregner;
    private boolean skalKalleKalkulus;

    BeregningTjeneste() {
        // CDI
    }

    @Inject
    public BeregningTjeneste(BeregningFPSAK fpsakBeregner,
                             BeregningKalkulus kalkulusBeregner) {
        this.fpsakBeregner = fpsakBeregner;
        this.kalkulusBeregner = kalkulusBeregner;
        this.skalKalleKalkulus = false;
    }

    public Optional<BeregningsgrunnlagGrunnlag> hent(Long behandlingId) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.hent(behandlingId);
        } else {
            return fpsakBeregner.hent(behandlingId);
        }
    }
}
