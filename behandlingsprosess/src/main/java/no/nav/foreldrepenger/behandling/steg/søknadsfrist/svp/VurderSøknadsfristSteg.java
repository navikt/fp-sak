package no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;

import static java.util.Collections.singletonList;

@BehandlingStegRef(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class VurderSøknadsfristSteg implements BehandlingSteg {

    private FørsteLovligeUttaksdatoTjeneste førsteLovligeUttaksdatoTjeneste;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    public VurderSøknadsfristSteg() {
        // For CDI
    }

    @Inject
    public VurderSøknadsfristSteg(UttaksperiodegrenseRepository uttaksperiodegrenseRepository,
                                  FørsteLovligeUttaksdatoTjeneste førsteLovligeUttaksdatoTjeneste) {
        this.førsteLovligeUttaksdatoTjeneste = førsteLovligeUttaksdatoTjeneste;
        this.uttaksperiodegrenseRepository = uttaksperiodegrenseRepository;
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
