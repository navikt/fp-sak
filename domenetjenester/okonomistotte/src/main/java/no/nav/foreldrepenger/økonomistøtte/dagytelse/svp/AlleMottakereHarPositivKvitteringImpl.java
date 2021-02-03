package no.nav.foreldrepenger.økonomistøtte.dagytelse.svp;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.AlleMottakereHarPositivKvittering;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.AlleMottakereHarPositivKvitteringDagYtelse;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class AlleMottakereHarPositivKvitteringImpl implements AlleMottakereHarPositivKvittering {

    @Override
    public boolean vurder(Oppdragskontroll oppdragskontroll) {
        return AlleMottakereHarPositivKvitteringDagYtelse.forSvangerskapspenger().vurder(oppdragskontroll);
    }
}
