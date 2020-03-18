package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.validering;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

public class EndringerBareEtterEndringsdatoValideringTest {

    @Test(expected = TekniskException.class)
    public void endring_av_periode_før_endringsdato_skal_føre_til_valideringsfeil() {
        var perioder = List.of(periode(LocalDate.of(2017, 12, 1), LocalDate.of(2017, 12, 31)));
        var validering = new EndringerBareEtterEndringsdatoValidering(LocalDate.of(2018, 1, 1));

        validering.utfør(perioder);

        //Validering skal feile
    }

    @Test
    public void endring_av_periode_med_start_på_endringsdato_skal_ikke_føre_til_valideringsfeil() {
        var perioder = List.of(periode(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 1, 31)));
        var validering = new EndringerBareEtterEndringsdatoValidering(LocalDate.of(2018, 1, 1));

        validering.utfør(perioder);

        //Validering OK
    }

    @Test
    public void endring_av_periode_med_start_etter_endringsdato_skal_ikke_føre_til_valideringsfeil() {
        var perioder = List.of(periode(LocalDate.of(2018, 1, 12), LocalDate.of(2018, 1, 31)));
        var validering = new EndringerBareEtterEndringsdatoValidering(LocalDate.of(2018, 1, 1));

        validering.utfør(perioder);

        //Validering OK
    }

    private ForeldrepengerUttakPeriode periode(LocalDate fom, LocalDate tom) {
        return new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(fom, tom))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .build();
    }


}
