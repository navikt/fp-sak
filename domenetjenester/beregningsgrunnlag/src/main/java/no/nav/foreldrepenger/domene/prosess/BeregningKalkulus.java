package no.nav.foreldrepenger.domene.prosess;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;

import java.util.Optional;

@ApplicationScoped
public class BeregningKalkulus implements BeregningAPI {


    public BeregningKalkulus() {
        // CDI
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(Long behandlingId) {
        throw new IllegalStateException("FEIL: Kaller kalkulus for å hente beregningsgrunnlag, men implementasjonen av denne er ikke ferdigstilt");
    }

}
