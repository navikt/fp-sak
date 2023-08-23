package no.nav.foreldrepenger.behandling.steg.medlemskap.svp;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandling.steg.medlemskap.KontrollerFaktaLøpendeMedlemskapSteg;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class KontrollerFaktaLøpendeMedlemskapStegImpl implements KontrollerFaktaLøpendeMedlemskapSteg {

    KontrollerFaktaLøpendeMedlemskapStegImpl() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // kan utvides ved behov for sjekk av løpende medlemskap.
        // er tomt nå fordi startpunktutlederen peker på KOFAK_LOP_MEDL for uttak.
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
