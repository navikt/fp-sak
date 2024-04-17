package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
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

    public Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse) {
        if (skalKalleKalkulus) {
            return kalkulusBeregner.hent(referanse);
        } else {
            return fpsakBeregner.hent(referanse);
        }
    }
}
