package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_FAKTA_UTTAK)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class KontrollerFaktaUttakSteg implements UttakSteg {

    KontrollerFaktaUttakSteg() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
