package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import static java.util.Collections.singletonList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class VurderSøknadsfristSteg implements BehandlingSteg {

    private final VurderSøknadsfristTjeneste vurderSøknadsfristTjeneste;

    @Inject
    public VurderSøknadsfristSteg(@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) VurderSøknadsfristTjeneste vurderSøknadsfristTjeneste) {
        this.vurderSøknadsfristTjeneste = vurderSøknadsfristTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();

        // Sjekk søknadsfrist for søknadsperioder
        var søknadfristAksjonspunktDefinisjon = vurderSøknadsfristTjeneste.vurder(behandlingId);

        // Returner eventuelt aksjonspunkt ifm søknadsfrist
        return søknadfristAksjonspunktDefinisjon
            .map(ad -> BehandleStegResultat.utførtMedAksjonspunkter(singletonList(ad)))
            .orElseGet(BehandleStegResultat::utførtUtenAksjonspunkter);
    }

}
