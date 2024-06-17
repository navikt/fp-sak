package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

class EndringerBareEtterEndringsdatoValideringTest {

    @Test
    void endring_av_periode_før_endringsdato_skal_føre_til_valideringsfeil() {
        var perioder = List.of(periode(LocalDate.of(2017, 12, 1), LocalDate.of(2017, 12, 31), PeriodeResultatType.INNVILGET));
        var opprinneligePerioder = List.of(periode(LocalDate.of(2017, 12, 1), LocalDate.of(2017, 12, 31), PeriodeResultatType.AVSLÅTT));
        var validering = new EndringerBareEtterEndringsdatoValidering(opprinneligePerioder, LocalDate.of(2018, 1, 1));

        assertThrows(TekniskException.class, () -> validering.utfør(perioder));
    }

    @Test
    void ingen_endring_av_periode_før_endringsdato_skal_ikke_føre_til_valideringsfeil() {
        var perioder = List.of(periode(LocalDate.of(2017, 12, 1), LocalDate.of(2017, 12, 31), PeriodeResultatType.INNVILGET));
        var opprinneligePerioder = List.of(periode(LocalDate.of(2017, 12, 1), LocalDate.of(2017, 12, 31), PeriodeResultatType.INNVILGET));
        var validering = new EndringerBareEtterEndringsdatoValidering(opprinneligePerioder, LocalDate.of(2018, 1, 1));
        validering.utfør(perioder);

        assertDoesNotThrow(() -> validering.utfør(perioder));
    }

    @Test
    void endring_av_periode_med_start_på_endringsdato_skal_ikke_føre_til_valideringsfeil() {
        var perioder = List.of(periode(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 31), PeriodeResultatType.INNVILGET));
        var opprinneligePerioder = List.of(periode(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 31), PeriodeResultatType.AVSLÅTT));
        var validering = new EndringerBareEtterEndringsdatoValidering(opprinneligePerioder, LocalDate.of(2018, 1, 1));

        assertDoesNotThrow(() -> validering.utfør(perioder));
    }

    @Test
    void endring_av_periode_med_start_etter_endringsdato_skal_ikke_føre_til_valideringsfeil() {
        var perioder = List.of(periode(LocalDate.of(2018, 1, 12), LocalDate.of(2018, 1, 31), PeriodeResultatType.INNVILGET));
        var opprinneligePerioder = List.of(periode(LocalDate.of(2018, 1, 12), LocalDate.of(2018, 1, 31), PeriodeResultatType.AVSLÅTT));
        var validering = new EndringerBareEtterEndringsdatoValidering(opprinneligePerioder, LocalDate.of(2018, 1, 1));

        assertDoesNotThrow(() -> validering.utfør(perioder));
    }

    private ForeldrepengerUttakPeriode periode(LocalDate fom, LocalDate tom, PeriodeResultatType resultatType) {
        return new ForeldrepengerUttakPeriode.Builder().medTidsperiode(new LocalDateInterval(fom, tom)).medResultatType(resultatType).build();
    }


}
