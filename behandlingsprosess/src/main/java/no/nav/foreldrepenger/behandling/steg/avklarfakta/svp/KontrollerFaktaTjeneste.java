package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaTjenesteInngangsVilkår;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
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
