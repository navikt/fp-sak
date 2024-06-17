package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;

public class EndreUttakUtil {

    private EndreUttakUtil() {
    }

    public static ForeldrepengerUttakPeriode finnGjeldendePeriodeFor(List<ForeldrepengerUttakPeriode> gjeldende, LocalDateInterval nyPeriode) {
        for (var gjeldendePeriode : gjeldende) {
            if (new LocalDateInterval(gjeldendePeriode.getFom(), gjeldendePeriode.getTom()).contains(nyPeriode)) {
                return gjeldendePeriode;
            }
        }
        var msg = String.format("Fant ikke gjeldende periode for ny periode fom %s tom %s i %s", nyPeriode.getFomDato(), nyPeriode.getTomDato(),
            gjeldende.stream().map(ForeldrepengerUttakPeriode::getTidsperiode).toList());
        throw new TekniskException("FP-817091", msg);
    }

    public static ForeldrepengerUttakPeriodeAktivitet finnGjeldendeAktivitetFor(ForeldrepengerUttakPeriode gjeldendePeriode,
                                                                                Arbeidsgiver arbeidsgiver,
                                                                                InternArbeidsforholdRef arbeidsforholdRef,
                                                                                UttakArbeidType uttakArbeidType) {

        for (var aktivitet : gjeldendePeriode.getAktiviteter()) {
            if (Objects.equals(aktivitet.getArbeidsforholdRef(), arbeidsforholdRef) && Objects.equals(arbeidsgiver,
                aktivitet.getArbeidsgiver().orElse(null)) && Objects.equals(aktivitet.getUttakArbeidType(), uttakArbeidType)) {
                return aktivitet;
            }
        }
        var msg = String.format("Fant ikke gjeldende periode aktivitet for periode fom %s tom %s for arbeidsgiver %s - %s - %s",
            gjeldendePeriode.getFom(), gjeldendePeriode.getTom(), arbeidsforholdRef, arbeidsgiver, uttakArbeidType);
        throw new TekniskException("FP-811231", msg);
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
