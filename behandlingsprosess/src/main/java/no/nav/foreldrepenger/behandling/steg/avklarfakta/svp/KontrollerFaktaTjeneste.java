package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaTjenesteInngangsVilkår;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
class KontrollerFaktaTjeneste extends KontrollerFaktaTjenesteInngangsVilkår {

    protected KontrollerFaktaTjeneste() {
        // for CDI proxy
    }

    @Inject
    protected KontrollerFaktaTjeneste(KontrollerFaktaUtledereTjenesteImpl utlederTjeneste,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        super(utlederTjeneste, behandlingskontrollTjeneste);
    }

}
