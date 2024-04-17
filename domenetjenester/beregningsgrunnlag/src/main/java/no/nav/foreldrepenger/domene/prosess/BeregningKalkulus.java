package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;

@ApplicationScoped
public class BeregningKalkulus implements BeregningAPI {

    private KalkulusKlient klient;

    @Inject
    public BeregningKalkulus(KalkulusKlient klient) {
        this.klient = klient;
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse) {
        throw new IllegalStateException("FEIL: Kaller kalkulus for å hente beregningsgrunnlag, men implementasjonen av denne er ikke ferdigstilt");
    }

    @Override
    public void beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType stegType) {

    }

}
