package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakUtsettelseType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.KodeMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.EndreUttakUtil;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public final class UttakPerioderMapper {

    private UttakPerioderMapper() {
    }

    public static List<ForeldrepengerUttakPeriode> map(List<UttakResultatPeriodeLagreDto> dtoPerioder, List<ForeldrepengerUttakPeriode> gjeldenePerioder) {
        return dtoPerioder.stream().map(p -> map(p, gjeldenePerioder)).collect(Collectors.toList());
    }

    private static ForeldrepengerUttakPeriode map(UttakResultatPeriodeLagreDto dtoPeriode,
                                                  List<ForeldrepengerUttakPeriode> gjeldenePerioder) {
        LocalDateInterval periodeInterval = new LocalDateInterval(dtoPeriode.getFom(), dtoPeriode.getTom());
        List<ForeldrepengerUttakPeriodeAktivitet> aktiviteter = new ArrayList<>();
        for (UttakResultatPeriodeAktivitetLagreDto nyAktivitet : dtoPeriode.getAktiviteter()) {
            var matchendeGjeldendeAktivitet = EndreUttakUtil.finnGjeldendeAktivitetFor(gjeldenePerioder,
                periodeInterval, nyAktivitet.getArbeidsgiver().orElse(null), nyAktivitet.getArbeidsforholdId(), nyAktivitet.getUttakArbeidType());
            aktiviteter.add(map(nyAktivitet, matchendeGjeldendeAktivitet));

        }

        var gjeldendePeriode = EndreUttakUtil.finnGjeldendePeriodeFor(gjeldenePerioder, new LocalDateInterval(dtoPeriode.getFom(), dtoPeriode.getTom()));
        return new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(dtoPeriode.getFom(), dtoPeriode.getTom()))
            .medResultatType(dtoPeriode.getPeriodeResultatType())
            .medResultatÅrsak(mapInnvilgetÅrsak(gjeldendePeriode, dtoPeriode))
            .medBegrunnelse(dtoPeriode.getBegrunnelse())
            .medSamtidigUttak(dtoPeriode.isSamtidigUttak())
            .medSamtidigUttaksprosent(dtoPeriode.getSamtidigUttaksprosent())
            .medFlerbarnsdager(dtoPeriode.isFlerbarnsdager())
            .medGraderingInnvilget(dtoPeriode.isGraderingInnvilget())
            .medGraderingAvslagÅrsak(dtoPeriode.getGraderingAvslagÅrsak())
            .medUtsettelseType(gjeldendePeriode.getUtsettelseType())
            .medOppholdÅrsak(dtoPeriode.getOppholdÅrsak())
            .medAktiviteter(aktiviteter)
            .build();
    }

    private static PeriodeResultatÅrsak mapInnvilgetÅrsak(ForeldrepengerUttakPeriode periode, UttakResultatPeriodeLagreDto nyPeriode) {
        if (PeriodeResultatÅrsak.UKJENT.equals(nyPeriode.getPeriodeResultatÅrsak())) {
            if (!erOppholdsPeriode(nyPeriode) && PeriodeResultatType.INNVILGET.equals(nyPeriode.getPeriodeResultatType())) {
                return toUtsettelseårsaktype(periode.getUtsettelseType());
            }
        }
        return nyPeriode.getPeriodeResultatÅrsak();
    }

    private static InnvilgetÅrsak toUtsettelseårsaktype(UttakUtsettelseType årsakType) {
        return innvilgetUtsettelseÅrsakMapper()
            .map(årsakType)
            .orElse(InnvilgetÅrsak.UTTAK_OPPFYLT);
    }

    private static KodeMapper<UttakUtsettelseType, InnvilgetÅrsak> innvilgetUtsettelseÅrsakMapper() {
        return KodeMapper
            .medMapping(UttakUtsettelseType.ARBEID, InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_100_PROSENT_ARBEID)
            .medMapping(UttakUtsettelseType.FERIE, InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_FERIE)
            .medMapping(UttakUtsettelseType.SYKDOM_SKADE, InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_SYKDOM)
            .medMapping(UttakUtsettelseType.SØKER_INNLAGT, InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_INNLEGGELSE)
            .medMapping(UttakUtsettelseType.BARN_INNLAGT, InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_BARN_INNLAGT)
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
            .build();
    }

    private static boolean erOppholdsPeriode(UttakResultatPeriodeLagreDto uttakResultatPeriode) {
        return !OppholdÅrsak.UDEFINERT.equals(uttakResultatPeriode.getOppholdÅrsak());
    }
}
