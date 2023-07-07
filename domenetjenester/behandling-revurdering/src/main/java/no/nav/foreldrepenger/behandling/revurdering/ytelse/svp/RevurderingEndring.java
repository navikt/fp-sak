package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndringBasertPåKonsekvenserForYtelsen;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Sjekk om revurdering endrer utfall for SVP.
 */
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class RevurderingEndring extends RevurderingEndringBasertPåKonsekvenserForYtelsen {

    public RevurderingEndring() {
        // for CDI proxy
    }
}
