package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetLagreDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeLagreDto;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class UttakPerioderMapperTest {

    private static final String ORGNR = OrgNummer.KUNSTIG_ORG;
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_REF = InternArbeidsforholdRef.nyRef();

    @Test
    public void skalBrukeProsentArbeidFraTidligere() {
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusWeeks(1);
        UttakResultatPeriodeLagreDto periode = new UttakResultatPeriodeLagreDto.Builder()
            .medTidsperiode(fom, tom)
            .medAktiviteter(Collections.singletonList(minimal().build()))
            .medPeriodeResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .build();
        OverstyringUttakDto dto = new OverstyringUttakDto(Collections.singletonList(periode));
        var periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medArbeidsprosent(BigDecimal.valueOf(77))
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, Arbeidsgiver.virksomhet(ORGNR), ARBEIDSFORHOLD_REF))
            .build();
        var opprinneligPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(fom, tom))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medAktiviteter(List.of(periodeAktivitet))
            .build();
        List<ForeldrepengerUttakPeriode> gjeldendePerioder = List.of(opprinneligPeriode);
        var mapped = UttakPerioderMapper.map(dto.getPerioder(), gjeldendePerioder);

        assertThat(mapped.get(0).getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(periodeAktivitet.getArbeidsprosent());
    }

    @Test
    public void skalSkilleMellomFrilansOgAnnet() {
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusWeeks(1);
        UttakResultatPeriodeAktivitetLagreDto frilans = new UttakResultatPeriodeAktivitetLagreDto.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medTrekkdager(BigDecimal.ZERO)
            .build();
        UttakResultatPeriodeAktivitetLagreDto annet = new UttakResultatPeriodeAktivitetLagreDto.Builder()
            .medUttakArbeidType(UttakArbeidType.ANNET)
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medTrekkdager(BigDecimal.ZERO)
            .build();
        UttakResultatPeriodeLagreDto periode = new UttakResultatPeriodeLagreDto.Builder()
            .medTidsperiode(fom, tom)
            .medPeriodeResultatÅrsak(PeriodeResultatÅrsak.UKJENT)
            .medAktiviteter(List.of(frilans, annet))
            .build();
        OverstyringUttakDto dto = new OverstyringUttakDto(Collections.singletonList(periode));
        var periodeAktivitet1 = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medArbeidsprosent(BigDecimal.valueOf(77))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
            .build();
        var periodeAktivitet2 = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medArbeidsprosent(BigDecimal.valueOf(77))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ANNET))
            .build();
        var opprinneligPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .medAktiviteter(List.of(periodeAktivitet1, periodeAktivitet2))
            .build();
        var mapped = UttakPerioderMapper.map(dto.getPerioder(), List.of(opprinneligPeriode));

        assertThat(mapped.get(0).getAktiviteter().get(0).getUttakArbeidType()).isIn(UttakArbeidType.FRILANS, UttakArbeidType.ANNET);
        assertThat(mapped.get(0).getAktiviteter().get(1).getUttakArbeidType()).isIn(UttakArbeidType.FRILANS, UttakArbeidType.ANNET);
    }

    @Test
    public void skalSetteGraderingAvslagÅrsakTilUkjentHvisGraderingInnvilget() {
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusWeeks(1);
        UttakResultatPeriodeAktivitetLagreDto aktivitetDto = new UttakResultatPeriodeAktivitetLagreDto.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medTrekkdager(BigDecimal.ZERO)
            .build();
        UttakResultatPeriodeLagreDto periode = new UttakResultatPeriodeLagreDto.Builder()
            .medTidsperiode(fom, tom)
            .medGraderingAvslåttÅrsak(GraderingAvslagÅrsak.GRADERING_FØR_UKE_7)
            .medGraderingInnvilget(true)
            .medAktiviteter(Collections.singletonList(aktivitetDto))
            .build();
        OverstyringUttakDto dto = new OverstyringUttakDto(Collections.singletonList(periode));
        var periodeAktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder()
            .medArbeidsprosent(BigDecimal.valueOf(77))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
            .medSøktGraderingForAktivitetIPeriode(true)
            .build();
        var opprinneligPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(new LocalDateInterval(fom, tom))
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medGraderingInnvilget(false)
            .medGraderingAvslagÅrsak(GraderingAvslagÅrsak.GRADERING_FØR_UKE_7)
            .medAktiviteter(List.of(periodeAktivitet))
            .build();
        var mapped = UttakPerioderMapper.map(dto.getPerioder(), List.of(opprinneligPeriode));

        assertThat(mapped.get(0).isGraderingInnvilget()).isTrue();
        assertThat(mapped.get(0).getGraderingAvslagÅrsak()).isEqualTo(GraderingAvslagÅrsak.UKJENT);
    }

    private UttakResultatPeriodeAktivitetLagreDto.Builder minimal() {
        return new UttakResultatPeriodeAktivitetLagreDto.Builder()
            .medArbeidsgiver(new ArbeidsgiverLagreDto(ORGNR))
            .medArbeidsforholdId(ARBEIDSFORHOLD_REF)
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medTrekkdager(BigDecimal.ZERO);
    }

}
