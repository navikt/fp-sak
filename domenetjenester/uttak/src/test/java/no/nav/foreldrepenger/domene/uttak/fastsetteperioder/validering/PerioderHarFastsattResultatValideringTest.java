package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

public class PerioderHarFastsattResultatValideringTest {

    @Test
    public void feilVedPeriodeMedNullResultat() {
        var nyePerioder = List.of(periodeMedResultat(null));

        PerioderHarFastsattResultatValidering validering = new PerioderHarFastsattResultatValidering();
        assertThatThrownBy(() -> validering.utfør(nyePerioder)).isInstanceOf(TekniskException.class);
    }

    @Test
    public void feilVedPeriodeMedResultatIkkeFastsatt() {
        var nyePerioder = List.of(periodeMedResultat(PeriodeResultatType.IKKE_FASTSATT));

        PerioderHarFastsattResultatValidering validering = new PerioderHarFastsattResultatValidering();
        assertThatThrownBy(() -> validering.utfør(nyePerioder)).isInstanceOf(TekniskException.class);
    }

    @Test
    public void okHvisPerioderHarFastsattResultat() {
        var nyePerioder = List.of(periodeMedResultat(PeriodeResultatType.INNVILGET));

        PerioderHarFastsattResultatValidering validering = new PerioderHarFastsattResultatValidering();
        assertThatCode(() -> validering.utfør(nyePerioder)).doesNotThrowAnyException();
    }

    private ForeldrepengerUttakPeriode periodeMedResultat(PeriodeResultatType resultatType) {
        return new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(LocalDate.now(), LocalDate.now().plusDays(1)))
            .medResultatType(resultatType)
            .build();
    }
}
