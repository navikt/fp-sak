package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class SøknadsperioderMåKontrolleresAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private AvklarFaktaUttakPerioderTjeneste kontrollerFaktaUttakTjeneste;

    @Inject
    public SøknadsperioderMåKontrolleresAksjonspunktUtleder(AvklarFaktaUttakPerioderTjeneste kontrollerFaktaUttakTjeneste) {
        this.kontrollerFaktaUttakTjeneste = kontrollerFaktaUttakTjeneste;
    }

    SøknadsperioderMåKontrolleresAksjonspunktUtleder() {
        // For CDI
    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.behandlingId();
        var finnesOverlappendePerioder = kontrollerFaktaUttakTjeneste.finnesOverlappendePerioder(behandlingId);
        if (finnesOverlappendePerioder || finnesPeriodeSomMåKontrolleres(input) || ingenPerioder(input)) {
            return List.of(AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER);
        }
        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return true;
    }

    private boolean ingenPerioder(UttakInput input) {
        return hentPerioder(input).getPerioder().isEmpty();
    }

    private boolean finnesPeriodeSomMåKontrolleres(UttakInput input) {
        return hentPerioder(input).getPerioder()
            .stream().anyMatch(kontrollerFaktaPeriode -> !kontrollerFaktaPeriode.erBekreftet());
    }

    private KontrollerFaktaData hentPerioder(UttakInput input) {
        return kontrollerFaktaUttakTjeneste.hentKontrollerFaktaPerioder(input);
    }
}
