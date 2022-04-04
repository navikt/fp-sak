package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.søknadsfrist.SøktPeriodeTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class SøktPeriodeTjenesteImpl implements SøktPeriodeTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public SøktPeriodeTjenesteImpl(YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    public SøktPeriodeTjenesteImpl() {
        // For CDI
    }

    @Override
    public Optional<LocalDateInterval> finnSøktPeriode(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.getBehandlingId();
        var oppgittFordeling = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId)
            .map(YtelseFordelingAggregat::getOppgittFordeling);
        if (oppgittFordeling.isPresent()) {
            var perioder = oppgittFordeling.get()
                .getOppgittePerioder()
                .stream()
                .filter(periode -> UttakPeriodeType.STØNADSPERIODETYPER.contains(periode.getPeriodeType()))
                .filter(periode -> !periode.isUtsettelse())
                .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
                .collect(Collectors.toList());

            if (!perioder.isEmpty()) {
                var fom = perioder.get(0).getFom();
                var tom = perioder.get(perioder.size() - 1).getTom();
                return Optional.of(new LocalDateInterval(fom, tom));

            }
        }
        return Optional.empty();
    }
}
