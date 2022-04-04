package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.PerioderUtenHelgUtil;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fakta.FaktaUttakAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class FørsteUttaksdatoAksjonspunktUtleder implements FaktaUttakAksjonspunktUtleder {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    FørsteUttaksdatoAksjonspunktUtleder() {
        // For CDI
    }

    @Inject
    public FørsteUttaksdatoAksjonspunktUtleder(UttakRepositoryProvider repositoryProvider) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    }

    @Override
    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ref = input.getBehandlingReferanse();

        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(ref.getBehandlingId());
        var avklartFUD = ytelseFordelingAggregat.getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato).orElse(null);
        if (avklartFUD != null) {
            var behovForAvklaring = !førsteSøknadsperiode(ytelseFordelingAggregat)
                .map(oppgittPeriode -> PerioderUtenHelgUtil.datoerLikeNårHelgIgnoreres(oppgittPeriode.getFom(), avklartFUD))
                .orElse(false);
            if (behovForAvklaring) {
                return List.of(AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO);
            }
        }
        return List.of();
    }

    @Override
    public boolean skalBrukesVedOppdateringAvYtelseFordeling() {
        return true;
    }

    private Optional<OppgittPeriodeEntitet> førsteSøknadsperiode(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder()
            .stream()
            .min(Comparator.comparing(OppgittPeriodeEntitet::getFom));
    }
}
