package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndringBasertPåKonsekvenserForYtelsen;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

/**
 * Sjekk om revurdering endrer utfall for SVP.
 */
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class RevurderingEndring extends RevurderingEndringBasertPåKonsekvenserForYtelsen {

    public RevurderingEndring() {
        // for CDI proxy
    }
}
