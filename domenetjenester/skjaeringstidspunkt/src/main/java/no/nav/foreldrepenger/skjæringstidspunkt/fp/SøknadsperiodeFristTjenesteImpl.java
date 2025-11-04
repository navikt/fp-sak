package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.Søknadsfristdatoer;
import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.skjæringstidspunkt.SøknadsperiodeFristTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class SøknadsperiodeFristTjenesteImpl implements SøknadsperiodeFristTjeneste  {

    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private SøknadRepository søknadRepository;

    SøknadsperiodeFristTjenesteImpl() {
        // CDI
    }

    @Inject
    public SøknadsperiodeFristTjenesteImpl(YtelsesFordelingRepository ytelsesFordelingRepository,
                                           SøknadRepository søknadRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.søknadRepository = søknadRepository;
    }

    @Override
    public Søknadsfristdatoer finnSøknadsfrist(Long behandlingId) {
        var søknadMottattDato = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .map(SøknadEntitet::getMottattDato).orElse(null);
        var perioder = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(YtelseFordelingAggregat::getGjeldendeFordeling)
            .map(OppgittFordelingEntitet::getPerioder)
            .map(p -> perioderSkalVurderes(p, søknadMottattDato))
            .orElseGet(List::of);
        var min = perioder.stream().map(OppgittPeriodeEntitet::getFom).min(Comparator.naturalOrder());
        var max = perioder.stream().map(OppgittPeriodeEntitet::getTom).max(Comparator.naturalOrder());
        var periode = min.map(m -> new LocalDateInterval(m, max.orElseThrow())).orElse(null);

        return finnSøknadsfrist(behandlingId, periode);
    }

    private Søknadsfristdatoer finnSøknadsfrist(Long behandlingId, LocalDateInterval søknadsperiode) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandlingId);
        var brukfrist = søknadsperiode != null ? Søknadsfrister.søknadsfristDagytelse(søknadsperiode.getFomDato()) : null;

        var builder = Søknadsfristdatoer.builder()
            .medSøknadGjelderPeriode(søknadsperiode)
            .medUtledetSøknadsfrist(brukfrist);
        søknad.ifPresent(s -> builder.medSøknadMottattDato(s.getMottattDato()));
        søknad.filter(s -> brukfrist != null && s.getMottattDato() != null && s.getMottattDato().isAfter(brukfrist))
            .ifPresent(s -> builder.medDagerOversittetFrist(DAYS.between(brukfrist, s.getMottattDato())));
        return builder.build();
    }

    public static List<OppgittPeriodeEntitet> perioderSkalVurderes(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate søknadMottattDato) {
        return oppgittePerioder.stream()
            .filter(p -> FordelingPeriodeKilde.SØKNAD.equals(p.getPeriodeKilde()))
            .filter(p -> !p.isUtsettelse())
            .filter(p -> periodeSkalVurderes(p, søknadMottattDato))
            .toList();
    }

    private static boolean periodeSkalVurderes(OppgittPeriodeEntitet periode, LocalDate søknadMottattDato) {
        // Mangler tidligst mottatt dato, eller tidligst mottatt er >= søknadMottatt/periodeMottatt
        if (periode.getTidligstMottattDato().isEmpty() || periode.getMottattDato() == null
            || søknadMottattDato != null && periode.getTidligstMottattDato().filter(tmd -> tmd.isBefore(søknadMottattDato)).isEmpty()
            || periode.getTidligstMottattDato().filter(tmd -> tmd.isBefore(periode.getMottattDato())).isEmpty()) {
            return true;
        }
        // Det skal finnes en tidligst mottatt dato og den skal være før mottatt dato. Perioden har vært behandlet i tidligere behandling
        var tidligstedato = periode.getTidligstMottattDato().map(Søknadsfrister::tidligsteDatoDagytelse).orElseThrow();
        // Sjekk perioder med for tidlig fom - de kan ha blitt underkjent i tidligere behandling (frist eller uttaksregler).
        return periode.getFom().isBefore(tidligstedato);
    }

}
