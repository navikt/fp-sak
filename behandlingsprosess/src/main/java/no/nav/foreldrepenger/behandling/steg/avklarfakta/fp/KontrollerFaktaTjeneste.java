package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaTjenesteInngangsVilkår;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
class KontrollerFaktaTjeneste extends KontrollerFaktaTjenesteInngangsVilkår {

    protected KontrollerFaktaTjeneste() {
        // for CDI proxy
    }

    @Inject
    KontrollerFaktaTjeneste(KontrollerFaktaUtledereTjenesteImpl utlederTjeneste,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        super(utlederTjeneste, behandlingskontrollTjeneste);
    }
}
