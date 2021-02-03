package no.nav.foreldrepenger.økonomistøtte.kontantytelse.es;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.AlleMottakereHarPositivKvittering;
import no.nav.foreldrepenger.økonomistøtte.OppdragKvitteringTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("ES")
public class AlleMottakereHarPositivKvitteringEngangsstønad implements AlleMottakereHarPositivKvittering {

    @Override
    public boolean vurder(Oppdragskontroll oppdragskontroll) {
        return oppdragskontroll.getOppdrag110Liste().stream()
            .anyMatch(OppdragKvitteringTjeneste::harPositivKvittering);
    }
}
