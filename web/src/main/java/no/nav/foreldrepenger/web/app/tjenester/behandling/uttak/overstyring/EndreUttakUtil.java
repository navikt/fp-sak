package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.feil.FeilFactory;

public class EndreUttakUtil {

    private EndreUttakUtil() {
    }

    public static ForeldrepengerUttakPeriode finnGjeldendePeriodeFor(List<ForeldrepengerUttakPeriode> gjeldende,
                                                                     LocalDateInterval nyPeriode) {
        for (var gjeldendePeriode : gjeldende) {
            if (new LocalDateInterval(gjeldendePeriode.getFom(), gjeldendePeriode.getTom()).contains(nyPeriode)) {
                return gjeldendePeriode;
            }
        }
        throw FeilFactory.create(EndreUttakFeil.class).fantIkkeMatchendeGjeldendePeriode(nyPeriode.getFomDato(),
            nyPeriode.getTomDato()).toException();
    }

    public static ForeldrepengerUttakPeriodeAktivitet finnGjeldendeAktivitetFor(ForeldrepengerUttakPeriode gjeldendePeriode,
                                                                                Arbeidsgiver arbeidsgiver,
                                                                                InternArbeidsforholdRef arbeidsforholdRef,
                                                                                UttakArbeidType uttakArbeidType) {

        for (var aktivitet : gjeldendePeriode.getAktiviteter()) {
            if (Objects.equals(aktivitet.getArbeidsforholdRef(), arbeidsforholdRef) &&
                Objects.equals(arbeidsgiver, aktivitet.getArbeidsgiver().orElse(null)) &&
                Objects.equals(aktivitet.getUttakArbeidType(), uttakArbeidType)) {
                return aktivitet;
            }
        }
        throw FeilFactory.create(EndreUttakFeil.class).fantIkkeMatchendeGjeldendePeriodeAktivitet(gjeldendePeriode.getFom(),
            gjeldendePeriode.getTom(), arbeidsforholdRef, arbeidsgiver, uttakArbeidType).toException();
    }

    public static ForeldrepengerUttakPeriodeAktivitet finnGjeldendeAktivitetFor(List<ForeldrepengerUttakPeriode> gjeldeneperioder,
                                                                                LocalDateInterval periodeInterval,
                                                                                Arbeidsgiver arbeidsgiver,
                                                                                InternArbeidsforholdRef arbeidsforholdRef,
                                                                                UttakArbeidType uttakArbeidType) {
        return finnGjeldendeAktivitetFor(finnGjeldendePeriodeFor(gjeldeneperioder, periodeInterval), arbeidsgiver, arbeidsforholdRef,
            uttakArbeidType);
    }
}
