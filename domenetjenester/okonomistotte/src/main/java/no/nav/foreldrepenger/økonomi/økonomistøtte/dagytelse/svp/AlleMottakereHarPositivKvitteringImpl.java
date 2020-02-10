package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.svp;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomi.økonomistøtte.AlleMottakereHarPositivKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.AlleMottakereHarPositivKvitteringDagYtelse;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class AlleMottakereHarPositivKvitteringImpl implements AlleMottakereHarPositivKvittering {

    @Override
    public boolean vurder(Oppdragskontroll oppdragskontroll) {
        return AlleMottakereHarPositivKvitteringDagYtelse.forSvangerskapspenger().vurder(oppdragskontroll);
    }
}
