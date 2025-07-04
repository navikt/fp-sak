package no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp;

import static java.util.Collections.singletonList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class VurderSøknadsfristSteg implements BehandlingSteg {

    private FørsteLovligeUttaksdatoTjeneste førsteLovligeUttaksdatoTjeneste;

    public VurderSøknadsfristSteg() {
        // For CDI
    }

    @Inject
    public VurderSøknadsfristSteg(FørsteLovligeUttaksdatoTjeneste førsteLovligeUttaksdatoTjeneste) {
        this.førsteLovligeUttaksdatoTjeneste = førsteLovligeUttaksdatoTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var trengerAvklaring = førsteLovligeUttaksdatoTjeneste.vurder(kontekst.getBehandlingId());

        // Returner eventuelt aksjonspunkt ifm søknadsfrist
        return trengerAvklaring
            .map(ad -> BehandleStegResultat.utførtMedAksjonspunkter(singletonList(ad)))
            .orElseGet(BehandleStegResultat::utførtUtenAksjonspunkter);
    }

}
