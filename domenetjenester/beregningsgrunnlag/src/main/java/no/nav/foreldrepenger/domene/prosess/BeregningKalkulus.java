package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;

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
