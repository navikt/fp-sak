package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaTjenesteInngangsVilkår;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
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
