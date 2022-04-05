package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class VurderSøknadsfristOppdatererTjenesteSVP extends VurderSøknadsfristOppdatererTjeneste {

    @Inject
    public VurderSøknadsfristOppdatererTjenesteSVP(HistorikkTjenesteAdapter historikkAdapter,
                                                   BehandlingRepositoryProvider repositoryProvider) {
        super(historikkAdapter, repositoryProvider);
    }

    VurderSøknadsfristOppdatererTjenesteSVP() {
        //CDI
    }

    @Override
    protected void lagreYtelseSpesifikkeData(Long behandlingId, Uttaksperiodegrense uttaksperiodegrense) {
        //Alt som trengs er lagret i uttaksperiodegrense
    }
}
