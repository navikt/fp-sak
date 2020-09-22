package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.søknadsfrist.SøknadsfristPeriode;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class VurderSøknadsfristOppdatererTjenesteSVP extends VurderSøknadsfristOppdatererTjeneste {

    @Inject
    public VurderSøknadsfristOppdatererTjenesteSVP(@FagsakYtelseTypeRef("SVP") SøknadsfristPeriode søknadsfristPeriode,
                                                   HistorikkTjenesteAdapter historikkAdapter,
                                                   BehandlingRepositoryProvider repositoryProvider) {
        super(søknadsfristPeriode, historikkAdapter, repositoryProvider);
    }

    VurderSøknadsfristOppdatererTjenesteSVP() {
        //CDI
    }

    @Override
    protected void lagreYtelseSpesifikkeData(Long behandlingId, Uttaksperiodegrense uttaksperiodegrense) {
        //Alt som trengs er lagret i uttaksperiodegrense
    }
}
