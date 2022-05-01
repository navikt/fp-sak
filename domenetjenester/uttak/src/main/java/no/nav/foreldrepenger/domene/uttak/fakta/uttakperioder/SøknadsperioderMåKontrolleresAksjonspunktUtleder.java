package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class SøknadsperioderMåKontrolleresAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private static final Logger LOG = LoggerFactory.getLogger(SøknadsperioderMåKontrolleresAksjonspunktUtleder.class);

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
        var periodedata = hentPerioder(input);
        if (ingenPerioder(periodedata)) {
            LOG.info("FAKTA UTTAK behandlingtype {} ingen perioder behandling {}", input.getBehandlingReferanse().behandlingType().getKode(), input.getBehandlingReferanse().behandlingId());
            return List.of(AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER);
        }
        var finnesOverlappendePerioder = kontrollerFaktaUttakTjeneste.finnesOverlappendePerioder(behandlingId);
        if (finnesOverlappendePerioder) {
            LOG.info("FAKTA UTTAK behandlingtype {} overlappende perioder behandling {}", input.getBehandlingReferanse().behandlingType().getKode(), input.getBehandlingReferanse().behandlingId());
            return List.of(AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER);
        }
        if (finnesPeriodeSomMåKontrolleres(periodedata)) {
            var friheten = input.getBehandlingReferanse().skjæringstidspunkt().kreverSammenhengendeUttak() ? "sammenhengende" : "fritt";
            LOG.info("FAKTA UTTAK behandlingtype {} uttak {} kontroller perioder behandling {}",
                input.getBehandlingReferanse().behandlingType().getKode(), friheten, input.getBehandlingReferanse().behandlingId());
            return List.of(AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER);
        }
        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return true;
    }

    private boolean ingenPerioder(KontrollerFaktaData periodedata) {
        return periodedata.getPerioder().isEmpty();
    }

    private boolean finnesPeriodeSomMåKontrolleres(KontrollerFaktaData periodedata) {
        return periodedata.getPerioder()
            .stream().anyMatch(kontrollerFaktaPeriode -> !kontrollerFaktaPeriode.erBekreftet());
    }

    private KontrollerFaktaData hentPerioder(UttakInput input) {
        return kontrollerFaktaUttakTjeneste.hentKontrollerFaktaPerioder(input, true);
    }
}
