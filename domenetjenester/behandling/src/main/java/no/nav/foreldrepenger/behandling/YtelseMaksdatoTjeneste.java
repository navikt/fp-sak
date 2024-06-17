package no.nav.foreldrepenger.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

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
import java.util.stream.Collectors;

@ApplicationScoped
public class YtelseMaksdatoTjeneste {

    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private FpUttakRepository fpUttakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    YtelseMaksdatoTjeneste() {
        // CDI
    }

    @Inject
    public YtelseMaksdatoTjeneste(RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                  FpUttakRepository fpUttakRepository,
                                  FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.fpUttakRepository = fpUttakRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
    }

    public Optional<LocalDate> beregnMorsMaksdato(Saksnummer saksnummer, RelasjonsRolleType rolleType) {
        if (RelasjonsRolleType.MORA.equals(rolleType)) {
            return Optional.empty();
        }

        var apBehandling = annenpartsGjeldendeVedtatteBehandling(saksnummer);
        var uttakResultat = apBehandling.flatMap(this::annenpartsUttak);
        if (uttakResultat.isPresent()) {
            var gjeldenePerioder = uttakResultat.get().getGjeldendePerioder();

            var perArbeidsforhold = finnPerioderPerArbeidsforhold(gjeldenePerioder.getPerioder());
            LocalDate maksdato = null;
            for (var entry : perArbeidsforhold.entrySet()) {
                var perioder = entry.getValue();
                var sisteUttaksdag = finnMorsSisteUttaksdag(perioder);
                if (sisteUttaksdag.isEmpty()) {
                    return Optional.empty();
                }
                var tilgjengeligeStønadsdager = beregnTilgjengeligeStønadsdager(perioder, apBehandling.get());
                var tmpMaksdato = plusVirkedager(sisteUttaksdag.get(), tilgjengeligeStønadsdager);
                if (maksdato == null || maksdato.isBefore(tmpMaksdato)) {
                    maksdato = tmpMaksdato;
                }
            }
            return Optional.ofNullable(maksdato);
        }
        return Optional.empty();
    }

    private Optional<Behandling> annenpartsGjeldendeVedtatteBehandling(Saksnummer saksnummer) {
        return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(saksnummer);
    }

    private Optional<UttakResultatEntitet> annenpartsUttak(Behandling behandling) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
    }

    private Optional<LocalDate> finnMorsSisteUttaksdag(List<UttakResultatPeriodeAktivitetEntitet> perioder) {
        return perioder.stream()
            .filter(a -> a.getTrekkdager().merEnn0() || a.getPeriode().isInnvilget())
            .max(Comparator.comparing(UttakResultatPeriodeAktivitetEntitet::getTom))
            .map(UttakResultatPeriodeAktivitetEntitet::getTom);
    }

    private Map<UttakAktivitetEntitet, List<UttakResultatPeriodeAktivitetEntitet>> finnPerioderPerArbeidsforhold(List<UttakResultatPeriodeEntitet> perioder) {
        return perioder.stream()
            .map(UttakResultatPeriodeEntitet::getAktiviteter)
            .flatMap(Collection::stream)
            .collect(Collectors.groupingBy(UttakResultatPeriodeAktivitetEntitet::getUttakAktivitet));
    }

    private int beregnTilgjengeligeStønadsdager(List<UttakResultatPeriodeAktivitetEntitet> perioder, Behandling behandling) {
        var stønadsdagerUtregning = getKontoUtregning(behandling);
        if (!stønadsdagerUtregning.isEmpty()) {
            var tilgjengeligMødrekvote = rundOpp(beregnTilgjengeligeDagerFor(StønadskontoType.MØDREKVOTE, perioder, stønadsdagerUtregning));
            var tilgjengeligFellesperiode = rundOpp(beregnTilgjengeligeDagerFor(StønadskontoType.FELLESPERIODE, perioder, stønadsdagerUtregning));
            return tilgjengeligFellesperiode + tilgjengeligMødrekvote;
        }
        return 0;
    }

    private Map<StønadskontoType, Integer> getKontoUtregning(Behandling behandling) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())
            .map(UttakResultatEntitet::getStønadskontoberegning)
            .map(Stønadskontoberegning::getStønadskontoutregning)
            .or(() -> fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsakId())
                .flatMap(FagsakRelasjon::getStønadskontoberegning)
                .map(Stønadskontoberegning::getStønadskontoutregning))
            .orElseGet(Map::of);
    }

    private int rundOpp(BigDecimal tilgjengeligeDager) {
        return tilgjengeligeDager.setScale(0, RoundingMode.UP).intValue();
    }

    private BigDecimal beregnTilgjengeligeDagerFor(StønadskontoType stønadskontoType,
                                                   List<UttakResultatPeriodeAktivitetEntitet> aktiviteter,
                                                   Map<StønadskontoType, Integer> stønadskontoer) {
        var optionalStønadskonto = stønadskontoer.getOrDefault(stønadskontoType, 0);
        if (optionalStønadskonto > 0) {
            var brukteDager = brukteDager(stønadskontoType, aktiviteter);

            var tilgjengeligeDager = BigDecimal.valueOf(optionalStønadskonto).subtract(brukteDager);
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
        return justertDatoForHelg.plusDays(((long) uker * dagerPrUke + dager) - (long) padBefore);
    }
}
