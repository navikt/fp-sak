package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.validering.Valideringsfeil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeLagreDto;

class AvklarFaktaUttakValidator {

    static final String KREV_MINST_EN_SØKNADSPERIODE = "Påkrevd minst en søknadsperiode";
    static final String OVERLAPPENDE_PERIODER = "Periodene må ikke overlappe";
    static final String GRADERING = "Gradert periode må ha arbeidsgiver";
    static final String PERIODE_FØR_FØRSTE_UTTAKSDATO = "Kan ikke ha perioder før første uttaksdato";

    private AvklarFaktaUttakValidator() {
        // skal ikke lages instans
    }

    static void validerOpplysninger(List<BekreftetOppgittPeriodeDto> bekreftedePerioder, Optional<LocalDate> førsteUttaksdato) {
        var funnetFeil = Stream.of(validerSøknadsperioder(bekreftedePerioder, førsteUttaksdato))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        if (!funnetFeil.isEmpty()) {
            throw new Valideringsfeil(funnetFeil);
        }
    }

    static Optional<FeltFeilDto> validerSøknadsperioder(List<BekreftetOppgittPeriodeDto> bekreftedePerioder, Optional<LocalDate> førsteUttaksdato) {
        var feltnavn = "søknadsperioder";

        if (ingenSøknadsperiode(bekreftedePerioder)) {
            return Optional.of(new FeltFeilDto(feltnavn, KREV_MINST_EN_SØKNADSPERIODE));
        }
        if (overlappendePerioder(getBekreftetPerioder(bekreftedePerioder))) {
            return Optional.of(new FeltFeilDto(feltnavn, OVERLAPPENDE_PERIODER));
        }
        if (førsteUttaksdato.isPresent() && periodeFørFørsteUttaksdato(getBekreftetPerioder(bekreftedePerioder), førsteUttaksdato.get())) {
            return Optional.of(new FeltFeilDto(feltnavn, PERIODE_FØR_FØRSTE_UTTAKSDATO));
        }
        if (validerGradering(bekreftedePerioder)) {
            return Optional.of(new FeltFeilDto(feltnavn, GRADERING));
        }
        return Optional.empty();

    }

    private static boolean periodeFørFørsteUttaksdato(List<KontrollerFaktaPeriodeLagreDto> bekreftetPerioder,
                                                      LocalDate førsteUttaksdato) {
        var bekreftetFørsteUttaksdato = sortert(bekreftetPerioder).get(0).getFom();
        return bekreftetFørsteUttaksdato.isBefore(førsteUttaksdato);
    }

    private static List<KontrollerFaktaPeriodeLagreDto> sortert(List<KontrollerFaktaPeriodeLagreDto> bekreftetPerioder) {
        return bekreftetPerioder.stream().sorted(Comparator.comparing(KontrollerFaktaPeriodeLagreDto::getFom)).collect(Collectors.toList());
    }

    private static boolean validerGradering(List<BekreftetOppgittPeriodeDto> perioder) {
        for (var periodeDto : perioder) {
            var bekreftetPeriode = periodeDto.getBekreftetPeriode();
            if (erGradering(bekreftetPeriode)) {
                if (bekreftetPeriode.getGraderingAktivitetType() == GraderingAktivitetType.ARBEID && bekreftetPeriode.getArbeidsgiver() == null) {
                    return true;
                }
                if (bekreftetPeriode.getGraderingAktivitetType() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean erGradering(KontrollerFaktaPeriodeLagreDto bekreftetPeriode) {
        return bekreftetPeriode.getArbeidstidsprosent() != null;
    }

    private static boolean ingenSøknadsperiode(List<BekreftetOppgittPeriodeDto> perioder) {
        return perioder == null || perioder.isEmpty();
    }

    private static boolean perioderOverlapper(KontrollerFaktaPeriodeLagreDto p1, KontrollerFaktaPeriodeLagreDto p2) {
        if (p2.getFom() == null || p2.getTom() == null || p1.getFom() == null || p1.getTom() == null) {
            return false;
        }

        var p1BegynnerFørst = p1.getFom().isBefore(p2.getTom());
        var begynnerFørst = p1BegynnerFørst ? p1 : p2;
        var begynnerSist = p1BegynnerFørst ? p2 : p1;
        return begynnerFørst.getTom().isAfter(begynnerSist.getFom());
    }

    private static boolean overlappendePerioder(List<KontrollerFaktaPeriodeLagreDto> perioder) {
        for (var i = 0; i < perioder.size(); i++) {
            var periode = perioder.get(i);

            for (var y = i + 1; y < perioder.size(); y++) {
                if (perioderOverlapper(periode, perioder.get(y))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<KontrollerFaktaPeriodeLagreDto> getBekreftetPerioder(List<BekreftetOppgittPeriodeDto> bekreftedePerioder) {
        return bekreftedePerioder.stream()
            .map(BekreftetOppgittPeriodeDto::getBekreftetPeriode)
            .collect(Collectors.toList());
    }
}
