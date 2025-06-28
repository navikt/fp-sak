package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.EndreUttakUtil;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public final class UttakPerioderMapper {

    private UttakPerioderMapper() {
    }

    public static List<ForeldrepengerUttakPeriode> map(List<UttakResultatPeriodeLagreDto> dtoPerioder,
                                                       List<ForeldrepengerUttakPeriode> gjeldenePerioder) {
        return dtoPerioder.stream().map(p -> map(p, gjeldenePerioder)).toList();
    }

    private static ForeldrepengerUttakPeriode map(UttakResultatPeriodeLagreDto dtoPeriode,
                                                  List<ForeldrepengerUttakPeriode> gjeldenePerioder) {
        var periodeInterval = new LocalDateInterval(dtoPeriode.getFom(), dtoPeriode.getTom());
        List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = new ArrayList<>();
        for (var nyAktivitet : dtoPeriode.getAktiviteter()) {
            var matchendeGjeldendeAktivitet = EndreUttakUtil.finnGjeldendeAktivitetFor(gjeldenePerioder,
                periodeInterval, nyAktivitet.getArbeidsgiver().orElse(null), nyAktivitet.getArbeidsforholdId(),
                nyAktivitet.getUttakArbeidType());
            aktiviteter.add(map(nyAktivitet, matchendeGjeldendeAktivitet));

        }

        var gjeldendePeriode = EndreUttakUtil.finnGjeldendePeriodeFor(gjeldenePerioder,
            new LocalDateInterval(dtoPeriode.getFom(), dtoPeriode.getTom()));
        return new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(dtoPeriode.getFom(), dtoPeriode.getTom()))
            .medResultatType(dtoPeriode.getPeriodeResultatType())
            .medResultatÅrsak(dtoPeriode.getPeriodeResultatÅrsak())
            .medBegrunnelse(dtoPeriode.getBegrunnelse())
            .medSamtidigUttak(dtoPeriode.isSamtidigUttak())
            .medSamtidigUttaksprosent(dtoPeriode.getSamtidigUttaksprosent())
            .medFlerbarnsdager(dtoPeriode.isFlerbarnsdager())
            .medGraderingInnvilget(dtoPeriode.isGraderingInnvilget())
            .medGraderingAvslagÅrsak(dtoPeriode.getGraderingAvslagÅrsak())
            .medUtsettelseType(dtoPeriode.getUtsettelseType())
            .medOppholdÅrsak(dtoPeriode.getOppholdÅrsak())
            .medOverføringÅrsak(gjeldendePeriode.getOverføringÅrsak())
            .medAktiviteter(aktiviteter)
            .medMottattDato(gjeldendePeriode.getMottattDato())
            .medMorsAktivitet(gjeldendePeriode.getMorsAktivitet())
            .medErFraSøknad(gjeldendePeriode.erFraSøknad())
            .medDokumentasjonVurdering(gjeldendePeriode.getDokumentasjonVurdering().orElse(null))
            .build();
    }

    private static ForeldrepengerUttakPeriodeAktivitet map(UttakResultatPeriodeAktivitetLagreDto dto,
                                                           ForeldrepengerUttakPeriodeAktivitet matchendeGjeldendeAktivitet) {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medUtbetalingsgrad(dto.getUtbetalingsgrad())
            .medArbeidsprosent(matchendeGjeldendeAktivitet.getArbeidsprosent())
            .medTrekkonto(dto.getStønadskontoType())
            .medAktivitet(new ForeldrepengerUttakAktivitet(matchendeGjeldendeAktivitet.getUttakArbeidType(),
                matchendeGjeldendeAktivitet.getArbeidsgiver().orElse(null),
                matchendeGjeldendeAktivitet.getArbeidsforholdRef()))
            .medTrekkdager(dto.getTrekkdagerDesimaler())
            .medSøktGraderingForAktivitetIPeriode(matchendeGjeldendeAktivitet.isSøktGraderingForAktivitetIPeriode())
            .build();
    }
}
