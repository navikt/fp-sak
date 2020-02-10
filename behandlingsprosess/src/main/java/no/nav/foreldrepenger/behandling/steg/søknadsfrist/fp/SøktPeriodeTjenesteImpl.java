package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.søknadsfrist.SøktPeriodeTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class SøktPeriodeTjenesteImpl implements SøktPeriodeTjeneste {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    public SøktPeriodeTjenesteImpl() {
        //For CDI
    }

    @Inject
    public SøktPeriodeTjenesteImpl(BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.ytelsesFordelingRepository = behandlingRepositoryProvider.getYtelsesFordelingRepository();
    }


    @Override
    public Optional<LocalDateInterval> finnSøktPeriode(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.getBehandlingId();
        var oppgittFordeling = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId).map(YtelseFordelingAggregat::getOppgittFordeling).orElse(null);
        if (oppgittFordeling != null) {
            List<OppgittPeriodeEntitet> perioder = oppgittFordeling.getOppgittePerioder().stream()
                .filter(periode -> UttakPeriodeType.STØNADSPERIODETYPER.contains(periode.getPeriodeType()))
                .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
                .collect(Collectors.toList());

            if (!perioder.isEmpty()) {
                return Optional.of(new LocalDateInterval(perioder.get(0).getFom(), perioder.get(perioder.size() - 1).getTom()));
            }
        }
        return Optional.empty();
    }
}
