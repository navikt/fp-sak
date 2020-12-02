package no.nav.foreldrepenger.behandling;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class YtelseMaksdatoTjeneste {

    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;

    YtelseMaksdatoTjeneste() {
        // CDI
    }

    @Inject
    public YtelseMaksdatoTjeneste(BehandlingRepositoryProvider repositoryProvider,
            RelatertBehandlingTjeneste relatertBehandlingTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
    }

    public Optional<LocalDate> beregnMorsMaksdato(Saksnummer saksnummer, RelasjonsRolleType rolleType) {
        if (RelasjonsRolleType.MORA.equals(rolleType)) {
            return Optional.empty();
        }

        Optional<UttakResultatEntitet> uttakResultat = annenpartsUttak(saksnummer);
        if (uttakResultat.isPresent()) {
            UttakResultatPerioderEntitet gjeldenePerioder = uttakResultat.get().getGjeldendePerioder();

            Map<UttakAktivitetEntitet, List<UttakResultatPeriodeAktivitetEntitet>> perArbeidsforhold = finnPerioderPerArbeidsforhold(
                    gjeldenePerioder.getPerioder());
            LocalDate maksdato = null;
            for (Map.Entry<UttakAktivitetEntitet, List<UttakResultatPeriodeAktivitetEntitet>> entry : perArbeidsforhold.entrySet()) {
                List<UttakResultatPeriodeAktivitetEntitet> perioder = entry.getValue();
                Optional<LocalDate> sisteUttaksdag = finnMorsSisteUttaksdag(perioder);
                if (sisteUttaksdag.isEmpty()) {
                    return Optional.empty();
                }
                int tilgjengeligeStønadsdager = beregnTilgjengeligeStønadsdager(perioder, saksnummer);
                LocalDate tmpMaksdato = plusVirkedager(sisteUttaksdag.get(), tilgjengeligeStønadsdager);
                if ((maksdato == null) || maksdato.isBefore(tmpMaksdato)) {
                    maksdato = tmpMaksdato;
                }
            }
            return Optional.ofNullable(maksdato);
        }
        return Optional.empty();
    }

    private Optional<UttakResultatEntitet> annenpartsUttak(Saksnummer saksnummer) {
        var annenPartsGjeldendeVedtattBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(saksnummer);
        if (annenPartsGjeldendeVedtattBehandling.isEmpty()) {
            return Optional.empty();
        }

        return repositoryProvider.getFpUttakRepository().hentUttakResultatHvisEksisterer(annenPartsGjeldendeVedtattBehandling.get().getId());
    }

    // TODO PK-48734 Her trengs det litt refaktorering
    public Optional<LocalDate> beregnMaksdatoForeldrepenger(BehandlingReferanse ref) {
        Behandling behandling = repositoryProvider.getBehandlingRepository().hentBehandling(ref.getBehandlingId());
        Optional<UttakResultatEntitet> uttakResultat = annenpartsUttak(ref.getSaksnummer());
        if (uttakResultat.isPresent()) {
            UttakResultatPerioderEntitet gjeldenePerioder = uttakResultat.get().getGjeldendePerioder();

            Map<UttakAktivitetEntitet, List<UttakResultatPeriodeAktivitetEntitet>> perArbeidsforhold = finnPerioderPerArbeidsforhold(
                    gjeldenePerioder.getPerioder());
            LocalDate maksdato = null;
            for (Map.Entry<UttakAktivitetEntitet, List<UttakResultatPeriodeAktivitetEntitet>> entry : perArbeidsforhold.entrySet()) {
                List<UttakResultatPeriodeAktivitetEntitet> perioder = entry.getValue();
                Optional<LocalDate> sisteUttaksdag = finnMorsSisteUttaksdag(perioder);
                if (!sisteUttaksdag.isPresent()) {
                    return Optional.empty();
                }
                int tilgjengeligeStønadsdager = beregnTilgjengeligeStønadsdagerForeldrepenger(perioder, behandling.getFagsak());
                LocalDate tmpMaksdato = plusVirkedager(sisteUttaksdag.get(), tilgjengeligeStønadsdager);
                if ((maksdato == null) || maksdato.isBefore(tmpMaksdato)) {
                    maksdato = tmpMaksdato;
                }
            }
            return Optional.ofNullable(maksdato);
        }
        return Optional.empty();
    }

    private Optional<LocalDate> finnMorsSisteUttaksdag(List<UttakResultatPeriodeAktivitetEntitet> perioder) {
        return perioder.stream()
                .filter(a -> a.getTrekkdager().merEnn0() || a.getPeriode().isInnvilget())
                .max(Comparator.comparing(UttakResultatPeriodeAktivitetEntitet::getTom))
                .map(UttakResultatPeriodeAktivitetEntitet::getTom);
    }

    private Map<UttakAktivitetEntitet, List<UttakResultatPeriodeAktivitetEntitet>> finnPerioderPerArbeidsforhold(
            List<UttakResultatPeriodeEntitet> perioder) {
        return perioder.stream()
                .map(UttakResultatPeriodeEntitet::getAktiviteter)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(UttakResultatPeriodeAktivitetEntitet::getUttakAktivitet));
    }

    private int beregnTilgjengeligeStønadsdager(List<UttakResultatPeriodeAktivitetEntitet> perioder, Saksnummer saksnummer) {
        FagsakRelasjon fagsakRelasjon = repositoryProvider.getFagsakRelasjonRepository().finnRelasjonFor(saksnummer);
        Optional<Stønadskontoberegning> optionalStønadskontoberegning = fagsakRelasjon.getGjeldendeStønadskontoberegning();
        if (optionalStønadskontoberegning.isPresent()) {
            Set<Stønadskonto> stønadskontoer = optionalStønadskontoberegning.get().getStønadskontoer();
            int tilgjengeligMødrekvote = rundOpp(beregnTilgjengeligeDagerFor(StønadskontoType.MØDREKVOTE, perioder, stønadskontoer));
            int tilgjengeligFellesperiode = rundOpp(beregnTilgjengeligeDagerFor(StønadskontoType.FELLESPERIODE, perioder, stønadskontoer));
            return tilgjengeligFellesperiode + tilgjengeligMødrekvote;
        }
        return 0;
    }

    private int beregnTilgjengeligeStønadsdagerForeldrepenger(List<UttakResultatPeriodeAktivitetEntitet> perioder, Fagsak fagsak) {
        FagsakRelasjon fagsakRelasjon = repositoryProvider.getFagsakRelasjonRepository().finnRelasjonFor(fagsak);
        Optional<Stønadskontoberegning> optionalStønadskontoberegning = fagsakRelasjon.getGjeldendeStønadskontoberegning();
        if (optionalStønadskontoberegning.isPresent()) {
            Set<Stønadskonto> stønadskontoer = optionalStønadskontoberegning.get().getStønadskontoer();
            BigDecimal tilgjengeligeDager = beregnTilgjengeligeDagerFor(StønadskontoType.FORELDREPENGER, perioder, stønadskontoer);
            return rundOpp(tilgjengeligeDager);
        }
        return 0;
    }

    private int rundOpp(BigDecimal tilgjengeligeDager) {
        return tilgjengeligeDager.setScale(0, RoundingMode.UP).intValue();
    }

    private BigDecimal beregnTilgjengeligeDagerFor(StønadskontoType stønadskontoType, List<UttakResultatPeriodeAktivitetEntitet> aktiviteter,
            Set<Stønadskonto> stønadskontoer) {
        Optional<Stønadskonto> optionalStønadskonto = stønadskontoer.stream().filter(s -> stønadskontoType.equals(s.getStønadskontoType()))
                .findFirst();
        if (optionalStønadskonto.isPresent()) {
            BigDecimal brukteDager = brukteDager(stønadskontoType, aktiviteter);

            BigDecimal tilgjengeligeDager = BigDecimal.valueOf(optionalStønadskonto.get().getMaxDager()).subtract(brukteDager);
            return tilgjengeligeDager.compareTo(BigDecimal.ZERO) > 0 ? tilgjengeligeDager : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal brukteDager(StønadskontoType stønadskontoType, List<UttakResultatPeriodeAktivitetEntitet> aktiviteter) {
        BigDecimal sum = BigDecimal.ZERO;
        for (UttakResultatPeriodeAktivitetEntitet aktivitet : aktiviteter) {
            if (Objects.equals(aktivitet.getTrekkonto(), stønadskontoType)) {
                sum = sum.add(aktivitet.getTrekkdager().decimalValue());
            }
        }
        return sum;
    }

    private static LocalDate plusVirkedager(LocalDate fom, int virkedager) {
        int virkedager_pr_uke = 5;
        int dager_pr_uke = 7;
        LocalDate justertDatoForHelg = fom;
        if (fom.getDayOfWeek().equals(DayOfWeek.SATURDAY) || fom.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            justertDatoForHelg = fom.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
        }
        int padBefore = justertDatoForHelg.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();

        int paddedVirkedager = virkedager + padBefore;

        int uker = paddedVirkedager / virkedager_pr_uke;
        int dager = paddedVirkedager % virkedager_pr_uke;
        return justertDatoForHelg.plusDays(((uker * dager_pr_uke) + dager) - (long) padBefore);
    }
}
