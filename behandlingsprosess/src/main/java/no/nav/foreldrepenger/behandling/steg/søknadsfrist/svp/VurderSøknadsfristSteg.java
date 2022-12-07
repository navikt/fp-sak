package no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp;

import static java.util.Collections.singletonList;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;

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

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        if (!Objects.equals(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER, førsteSteg)) {
            uttaksperiodegrenseRepository.ryddUttaksperiodegrense(kontekst.getBehandlingId());
        }
    }
}
