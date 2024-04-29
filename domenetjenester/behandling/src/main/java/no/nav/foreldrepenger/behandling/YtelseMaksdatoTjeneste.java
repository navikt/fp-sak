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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class YtelseMaksdatoTjeneste {

    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private FpUttakRepository fpUttakRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    YtelseMaksdatoTjeneste() {
        // CDI
    }

    @Inject
    public YtelseMaksdatoTjeneste(RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                  FpUttakRepository fpUttakRepository,
                                  BehandlingRepository behandlingRepository,
                                  FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.fpUttakRepository = fpUttakRepository;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
    }

    public Optional<LocalDate> beregnMorsMaksdato(Saksnummer saksnummer, RelasjonsRolleType rolleType) {
        if (RelasjonsRolleType.MORA.equals(rolleType)) {
            return Optional.empty();
        }

        var uttakResultat = annenpartsUttak(saksnummer);
        if (uttakResultat.isPresent()) {
            var gjeldenePerioder = uttakResultat.get().getGjeldendePerioder();

            var perArbeidsforhold = finnPerioderPerArbeidsforhold(
                    gjeldenePerioder.getPerioder());
            LocalDate maksdato = null;
            for (var entry : perArbeidsforhold.entrySet()) {
                var perioder = entry.getValue();
                var sisteUttaksdag = finnMorsSisteUttaksdag(perioder);
                if (sisteUttaksdag.isEmpty()) {
                    return Optional.empty();
                }
                var tilgjengeligeStønadsdager = beregnTilgjengeligeStønadsdager(perioder, saksnummer);
                var tmpMaksdato = plusVirkedager(sisteUttaksdag.get(), tilgjengeligeStønadsdager);
                if (maksdato == null || maksdato.isBefore(tmpMaksdato)) {
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

        return fpUttakRepository.hentUttakResultatHvisEksisterer(annenPartsGjeldendeVedtattBehandling.get().getId());
    }

    // TODO PK-48734 Her trengs det litt refaktorering
    public Optional<LocalDate> beregnMaksdatoForeldrepenger(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        var uttakResultat = annenpartsUttak(ref.saksnummer());
        if (uttakResultat.isPresent()) {
            var gjeldenePerioder = uttakResultat.get().getGjeldendePerioder();

            var perArbeidsforhold = finnPerioderPerArbeidsforhold(
                    gjeldenePerioder.getPerioder());
            LocalDate maksdato = null;
            for (var entry : perArbeidsforhold.entrySet()) {
                var perioder = entry.getValue();
                var sisteUttaksdag = finnMorsSisteUttaksdag(perioder);
                if (sisteUttaksdag.isEmpty()) {
                    return Optional.empty();
                }
                var tilgjengeligeStønadsdager = beregnTilgjengeligeStønadsdagerForeldrepenger(perioder, behandling.getFagsak());
                var tmpMaksdato = plusVirkedager(sisteUttaksdag.get(), tilgjengeligeStønadsdager);
                if (maksdato == null || maksdato.isBefore(tmpMaksdato)) {
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
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(saksnummer);
        var optionalStønadskontoberegning = fagsakRelasjon.getGjeldendeStønadskontoberegning();
        if (optionalStønadskontoberegning.isPresent()) {
            var stønadskontoer = optionalStønadskontoberegning.get().getStønadskontoer();
            var tilgjengeligMødrekvote = rundOpp(beregnTilgjengeligeDagerFor(StønadskontoType.MØDREKVOTE, perioder, stønadskontoer));
            var tilgjengeligFellesperiode = rundOpp(beregnTilgjengeligeDagerFor(StønadskontoType.FELLESPERIODE, perioder, stønadskontoer));
            return tilgjengeligFellesperiode + tilgjengeligMødrekvote;
        }
        return 0;
    }

    private int beregnTilgjengeligeStønadsdagerForeldrepenger(List<UttakResultatPeriodeAktivitetEntitet> perioder, Fagsak fagsak) {
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(fagsak);
        var optionalStønadskontoberegning = fagsakRelasjon.getGjeldendeStønadskontoberegning();
        if (optionalStønadskontoberegning.isPresent()) {
            var stønadskontoer = optionalStønadskontoberegning.get().getStønadskontoer();
            var tilgjengeligeDager = beregnTilgjengeligeDagerFor(StønadskontoType.FORELDREPENGER, perioder, stønadskontoer);
            return rundOpp(tilgjengeligeDager);
        }
        return 0;
    }

    private int rundOpp(BigDecimal tilgjengeligeDager) {
        return tilgjengeligeDager.setScale(0, RoundingMode.UP).intValue();
    }

    private BigDecimal beregnTilgjengeligeDagerFor(StønadskontoType stønadskontoType, List<UttakResultatPeriodeAktivitetEntitet> aktiviteter,
            Set<Stønadskonto> stønadskontoer) {
        var optionalStønadskonto = stønadskontoer.stream().filter(s -> stønadskontoType.equals(s.getStønadskontoType()))
                .findFirst();
        if (optionalStønadskonto.isPresent()) {
            var brukteDager = brukteDager(stønadskontoType, aktiviteter);

            var tilgjengeligeDager = BigDecimal.valueOf(optionalStønadskonto.get().getMaxDager()).subtract(brukteDager);
            return tilgjengeligeDager.compareTo(BigDecimal.ZERO) > 0 ? tilgjengeligeDager : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal brukteDager(StønadskontoType stønadskontoType, List<UttakResultatPeriodeAktivitetEntitet> aktiviteter) {
        var sum = BigDecimal.ZERO;
        for (var aktivitet : aktiviteter) {
            if (Objects.equals(aktivitet.getTrekkonto(), stønadskontoType.toUttakPeriodeType())) {
                sum = sum.add(aktivitet.getTrekkdager().decimalValue());
            }
        }
        return sum;
    }

    private static LocalDate plusVirkedager(LocalDate fom, int virkedager) {
        var virkedagerPrUke = 5;
        var dagerPrUke = 7;
        var justertDatoForHelg = fom;
        if (fom.getDayOfWeek().equals(DayOfWeek.SATURDAY) || fom.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            justertDatoForHelg = fom.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
        }
        var padBefore = justertDatoForHelg.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();

        var paddedVirkedager = virkedager + padBefore;

        var uker = paddedVirkedager / virkedagerPrUke;
        var dager = paddedVirkedager % virkedagerPrUke;
        return justertDatoForHelg.plusDays((uker * dagerPrUke + dager) - (long) padBefore);
    }
}
