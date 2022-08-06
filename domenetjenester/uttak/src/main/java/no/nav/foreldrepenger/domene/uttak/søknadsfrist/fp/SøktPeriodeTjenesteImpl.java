package no.nav.foreldrepenger.domene.uttak.søknadsfrist.fp;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.søknadsfrist.SøktPeriodeTjeneste;
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
        var behandlingId = ref.behandlingId();
        var oppgittePerioder = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId)
            .map(YtelseFordelingAggregat::getOppgittFordeling)
            .map(OppgittFordelingEntitet::getOppgittePerioder).orElse(List.of()).stream()
            .filter(periode -> UttakPeriodeType.STØNADSPERIODETYPER.contains(periode.getPeriodeType()))
            .filter(periode -> !periode.isUtsettelse())
            .collect(Collectors.toList());

        if (!oppgittePerioder.isEmpty()) {
            var fom = oppgittePerioder.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder());
            var tom = oppgittePerioder.stream().map(OppgittPeriodeEntitet::getTom).max(Comparator.naturalOrder());
            return fom.map(dato -> new LocalDateInterval(dato, tom.orElseThrow()));
        }
        return Optional.empty();
    }
}
