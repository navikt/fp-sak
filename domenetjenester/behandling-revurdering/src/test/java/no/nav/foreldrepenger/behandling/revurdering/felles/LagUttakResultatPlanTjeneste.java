package no.nav.foreldrepenger.behandling.revurdering.felles;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class LagUttakResultatPlanTjeneste {
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");

    public static SvangerskapspengerUttakResultatEntitet lagUttakResultatPlanSVPTjeneste(Behandling behandling,
                                                                                         List<LocalDateInterval> perioder,
                                                                                         List<PeriodeResultatType> periodeResultatTyper,
                                                                                         List<PeriodeIkkeOppfyltÅrsak> ikkeOppfyltÅrsak,
                                                                                         List<Integer> utbetalingsgrad) {
        var uttakResultatArbeidsforholdBuilder = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medArbeidsforhold(
            Arbeidsgiver.person(behandling.getAktørId()), null).medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID);
        for (var i = 0; i < perioder.size(); i++) {
            var p = perioder.get(i);
            uttakResultatArbeidsforholdBuilder.medPeriode(
                new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(p.getFomDato(), p.getTomDato()).medRegelInput("{}")
                    .medRegelEvaluering("{}")
                    .medUtbetalingsgrad(new Utbetalingsgrad(utbetalingsgrad.get(i)))
                    .medPeriodeIkkeOppfyltÅrsak(ikkeOppfyltÅrsak.get(i))
                    .medPeriodeResultatType(periodeResultatTyper.get(i))
                    .build());
        }

        var uttakArbeidsforhold = uttakResultatArbeidsforholdBuilder.build();
        return new SvangerskapspengerUttakResultatEntitet.Builder(behandling.getBehandlingsresultat()).medUttakResultatArbeidsforhold(
            uttakArbeidsforhold).build();
    }

    public static UttakResultatEntitet lagUttakResultatPlanTjeneste(Behandling behandling,
                                                                    List<LocalDateInterval> perioder,
                                                                    List<Boolean> samtidigUttak,
                                                                    List<PeriodeResultatType> periodeResultatTyper,
                                                                    List<PeriodeResultatÅrsak> periodeResultatÅrsak,
                                                                    List<Boolean> graderingInnvilget,
                                                                    List<Integer> andelIArbeid,
                                                                    List<Integer> utbetalingsgrad,
                                                                    List<Trekkdager> trekkdager,
                                                                    List<UttakPeriodeType> stønadskontoTyper) {
        var uttakResultatPlanBuilder = new UttakResultatEntitet.Builder(behandling.getBehandlingsresultat());
        var uttakResultatPerioder = new UttakResultatPerioderEntitet();
        assertThat(perioder).hasSize(samtidigUttak.size());
        assertThat(perioder).hasSize(periodeResultatTyper.size());
        assertThat(perioder).hasSize(periodeResultatÅrsak.size());
        assertThat(perioder).hasSize(graderingInnvilget.size());
        var antallPerioder = perioder.size();
        for (var i = 0; i < antallPerioder; i++) {
            lagUttakPeriodeMedPeriodeAktivitet(uttakResultatPerioder, perioder.get(i), samtidigUttak.get(i), periodeResultatTyper.get(i),
                periodeResultatÅrsak.get(i), graderingInnvilget.get(i), andelIArbeid, utbetalingsgrad, trekkdager, stønadskontoTyper);
        }
        return uttakResultatPlanBuilder.medOpprinneligPerioder(uttakResultatPerioder).build();
    }

    private static void lagUttakPeriodeMedPeriodeAktivitet(UttakResultatPerioderEntitet uttakResultatPerioder,
                                                           LocalDateInterval periode,
                                                           boolean samtidigUttak,
                                                           PeriodeResultatType periodeResultatType,
                                                           PeriodeResultatÅrsak periodeResultatÅrsak,
                                                           boolean graderingInnvilget,
                                                           List<Integer> andelIArbeid,
                                                           List<Integer> utbetalingsgrad,
                                                           List<Trekkdager> trekkdager,
                                                           List<UttakPeriodeType> stønadskontoTyper) {
        var uttakResultatPeriode = byggPeriode(periode.getFomDato(), periode.getTomDato(), samtidigUttak, periodeResultatType, periodeResultatÅrsak,
            graderingInnvilget);

        var antallAktiviteter = stønadskontoTyper.size();
        for (var i = 0; i < antallAktiviteter; i++) {
            var periodeAktivitet = lagPeriodeAktivitet(stønadskontoTyper.get(i), uttakResultatPeriode, trekkdager.get(i), andelIArbeid.get(i),
                utbetalingsgrad.get(i));
            uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);
        }
        uttakResultatPerioder.leggTilPeriode(uttakResultatPeriode);
    }

    private static UttakResultatPeriodeAktivitetEntitet lagPeriodeAktivitet(UttakPeriodeType stønadskontoType,
                                                                            UttakResultatPeriodeEntitet uttakResultatPeriode,
                                                                            Trekkdager trekkdager,
                                                                            int andelIArbeid,
                                                                            int utbetalingsgrad) {
        var uttakAktivitet = new UttakAktivitetEntitet.Builder().medArbeidsforhold(Arbeidsgiver.virksomhet(KUNSTIG_ORG), ARBEIDSFORHOLD_ID)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        return UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode, uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(trekkdager)
            .medArbeidsprosent(BigDecimal.valueOf(andelIArbeid))
            .medUtbetalingsgrad(new Utbetalingsgrad(utbetalingsgrad))
            .build();
    }

    private static UttakResultatPeriodeEntitet byggPeriode(LocalDate fom,
                                                           LocalDate tom,
                                                           boolean samtidigUttak,
                                                           PeriodeResultatType periodeResultatType,
                                                           PeriodeResultatÅrsak periodeResultatÅrsak,
                                                           boolean graderingInnvilget) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom).medSamtidigUttak(samtidigUttak)
            .medResultatType(periodeResultatType, periodeResultatÅrsak)
            .medGraderingInnvilget(graderingInnvilget)
            .build();
    }

}
