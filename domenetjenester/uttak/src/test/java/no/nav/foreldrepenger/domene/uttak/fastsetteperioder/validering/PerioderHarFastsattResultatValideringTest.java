package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

class PerioderHarFastsattResultatValideringTest {

    @Test
    void feilVedPeriodeMedNullResultat() {
        var nyePerioder = List.of(periodeMedResultat(null));

        var validering = new PerioderHarFastsattResultatValidering();
        assertThrows(TekniskException.class, () -> validering.utfør(nyePerioder));
    }

    @Test
    void feilVedPeriodeMedResultatManuellRevurdering() {
        var nyePerioder = List.of(periodeMedResultat(PeriodeResultatType.MANUELL_BEHANDLING));

        var validering = new PerioderHarFastsattResultatValidering();
        assertThrows(TekniskException.class, () -> validering.utfør(nyePerioder));
    }

    @Test
    void okHvisPerioderHarFastsattResultat() {
        var nyePerioder = List.of(periodeMedResultat(PeriodeResultatType.INNVILGET));

        var validering = new PerioderHarFastsattResultatValidering();
        assertDoesNotThrow(() -> validering.utfør(nyePerioder));
    }

    @Test
    void feilVedPeriodeUtenResultatÅrsak() {
        var nyePerioder = List.of(periodeMedResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT));

        var validering = new PerioderHarFastsattResultatValidering();
        assertThrows(TekniskException.class, () -> validering.utfør(nyePerioder));
    }

    private ForeldrepengerUttakPeriode periodeMedResultat(PeriodeResultatType resultatType) {
        return periodeMedResultat(resultatType, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE);
    }

    private ForeldrepengerUttakPeriode periodeMedResultat(PeriodeResultatType resultatType, PeriodeResultatÅrsak periodeResultatÅrsak) {
        return new ForeldrepengerUttakPeriode.Builder().medTidsperiode(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(1)))
            .medResultatType(resultatType)
            .medResultatÅrsak(periodeResultatÅrsak)
            .build();
    }
}
