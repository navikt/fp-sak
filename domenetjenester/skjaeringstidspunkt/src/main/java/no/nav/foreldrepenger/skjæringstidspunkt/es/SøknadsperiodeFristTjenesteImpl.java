package no.nav.foreldrepenger.skjæringstidspunkt.es;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.Søknadsfristdatoer;
import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.skjæringstidspunkt.SøknadsperiodeFristTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class SøknadsperiodeFristTjenesteImpl implements SøknadsperiodeFristTjeneste  {

    private static final EnumSet<DayOfWeek> WEEKEND = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private FamilieHendelseRepository familieGrunnlagRepository;
    private SøknadRepository søknadRepository;

    SøknadsperiodeFristTjenesteImpl() {
        // CDI
    }

    @Inject
    public SøknadsperiodeFristTjenesteImpl(BehandlingRepositoryProvider repositoryProvider) {
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
    }

    @Override
    public Søknadsfristdatoer finnSøknadsfrist(Long behandlingId) {
        var intervall = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
            .map(fhdato -> new LocalDateInterval(fhdato, fhdato))
            .orElse(null);

        return finnSøknadsfrist(behandlingId, intervall);
    }

    private Søknadsfristdatoer finnSøknadsfrist(Long behandlingId, LocalDateInterval søknadsperiode) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandlingId);
        var fristFraHendelse = søknadsperiode != null ? Søknadsfrister.søknadsfristEngangsbeløp(søknadsperiode.getFomDato()) : null;
        var brukfrist = fristFraHendelse == null || søknad.isEmpty() || søknad.filter(SøknadEntitet::getElektroniskRegistrert)
            .isPresent() ? fristFraHendelse : fristFraHendelse.plusDays(antallDagerTotaltNårTellerVirkedager(fristFraHendelse, 2));

        var builder = Søknadsfristdatoer.builder()
            .medSøknadGjelderPeriode(søknadsperiode)
            .medUtledetSøknadsfrist(brukfrist);
        søknad.ifPresent(s -> builder.medSøknadMottattDato(s.getMottattDato()));
        søknad.filter(s -> brukfrist != null && s.getMottattDato().isAfter(brukfrist))
            .ifPresent(s -> builder.medDagerOversittetFrist(DAYS.between(brukfrist, s.getMottattDato())));
        return builder.build();
    }

    private int antallDagerTotaltNårTellerVirkedager(LocalDate dato, int antallVirkedager) {
        var result = dato;
        var addedDays = 0;
        while (addedDays < antallVirkedager) {
            result = result.plusDays(1);
            if (!WEEKEND.contains(result.getDayOfWeek())) {
                ++addedDays;
            }
        }

        return Period.between(dato, result).getDays();
    }

}
