package no.nav.foreldrepenger.ytelse.beregning.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.ReferanseType;

public class MapUttakResultatFraVLTilRegelTest {

    private static final int STILLING_70 = 70;
    private static final String ORGNR = "000000000";
    private static final InternArbeidsforholdRef ARB_ID = InternArbeidsforholdRef.namedRef("TEST-REF");

    private Behandling behandling;
    private MapUttakResultatFraVLTilRegel mapper;
    private Behandlingsresultat behandlingresultat;

    @BeforeEach
    public void setup() {
        Fagsak fagsak = FagsakBuilder.nyForeldrepengerForMor().build();
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingresultat = Behandlingsresultat.opprettFor(behandling);
        mapper = new MapUttakResultatFraVLTilRegel() {
            @Override
            protected BigDecimal finnStillingsprosent(UttakInput input, SvangerskapspengerUttakResultatArbeidsforholdEntitet uttakAktivitet) {
                return BigDecimal.valueOf(STILLING_70);
            }
            @Override
            protected BigDecimal finnTotalStillingsprosentHosAG(UttakInput input, SvangerskapspengerUttakResultatArbeidsforholdEntitet uttakAktivitet) {
                return BigDecimal.valueOf(STILLING_70);
            }
        };
    }

    @Test
    public void skalMappeUttakResultatPlanMedEnkeltArbeidsforholdOgEnkelPeriode() {
        //Arrange
        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);
        var uttakPeriode = lagUttaksperiode(fom, tom, BigDecimal.valueOf(66));
        var uttakArbeidsforhold = lagArbeidsforhold(ORGNR, ARB_ID, uttakPeriode);
        var uttakResultat = lagUttak(uttakArbeidsforhold);
        // Act
        UttakResultat regelPlan = mapper.mapFra(uttakResultat, null);

        // Assert
        assertThat(regelPlan).isNotNull();
        assertThat(regelPlan.getUttakResultatPerioder()).hasSize(1);
        UttakResultatPeriode periode1 = regelPlan.getUttakResultatPerioder().iterator().next();
        assertThat(periode1.getFom()).isEqualTo(fom);
        assertThat(periode1.getTom()).isEqualTo(tom);
        List<UttakAktivitet> uttakAktiviteter = periode1.getUttakAktiviteter();
        assertThat(uttakAktiviteter).hasSize(1);
        assertThat(uttakAktiviteter.get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(uttakAktiviteter.get(0).getStillingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(STILLING_70));
        assertThat(uttakAktiviteter.get(0).getArbeidsforhold().getIdentifikator()).isEqualTo(ORGNR);
        assertThat(uttakAktiviteter.get(0).getArbeidsforhold().getArbeidsforholdId()).isEqualTo(ARB_ID.getReferanse());
        assertThat(uttakAktiviteter.get(0).getArbeidsforhold().getReferanseType()).isEqualTo(ReferanseType.ORG_NR);
    }

    @Test
    public void skalMappeUttakResultatPlanMedSNOgEnkelPeriode() {
        //Arrange
        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);
        var uttakPeriode = lagUttaksperiode(fom, tom, BigDecimal.valueOf(66));
        var uttakArbeidsforhold = lagUttakResultatArbeidsforhold(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null, uttakPeriode);
        var uttakResultat = lagUttak(uttakArbeidsforhold);
        // Act
        UttakResultat regelPlan = mapper.mapFra(uttakResultat, null);

        // Assert
        assertThat(regelPlan).isNotNull();
        assertThat(regelPlan.getUttakResultatPerioder()).hasSize(1);
        UttakResultatPeriode periode1 = regelPlan.getUttakResultatPerioder().iterator().next();
        assertThat(periode1.getFom()).isEqualTo(fom);
        assertThat(periode1.getTom()).isEqualTo(tom);
        List<UttakAktivitet> uttakAktiviteter = periode1.getUttakAktiviteter();
        assertThat(uttakAktiviteter).hasSize(1);
        assertThat(uttakAktiviteter.get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(uttakAktiviteter.get(0).getStillingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(uttakAktiviteter.get(0).getAktivitetStatus()).isEqualTo(AktivitetStatus.SN);
    }

    @Test
    public void skalMappeUttakResultatPlanMedEnkeltArbeidsforholdOgFlerePerioder() {
        //Arrange
        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);
        var uttakPeriode = lagUttaksperiode(fom, tom, BigDecimal.valueOf(66));
        var fom2 = tom.plusDays(1);
        var tom2 = LocalDate.of(2019, 4, 30);
        var uttakPeriode2 = lagUttaksperiode(fom2, tom2, BigDecimal.valueOf(66));
        var uttakArbeidsforhold = lagArbeidsforhold(ORGNR, ARB_ID, uttakPeriode, uttakPeriode2);
        var uttakResultat = lagUttak(uttakArbeidsforhold);
        // Act
        UttakResultat regelPlan = mapper.mapFra(uttakResultat, null);

        // Assert
        assertThat(regelPlan).isNotNull();
        List<UttakResultatPeriode> perioder = regelPlan.getUttakResultatPerioder();
        assertThat(perioder).hasSize(2);
        assertThat(perioder.get(0).getFom()).isEqualTo(fom);
        assertThat(perioder.get(0).getTom()).isEqualTo(tom);
        assertThat(perioder.get(1).getFom()).isEqualTo(fom2);
        assertThat(perioder.get(1).getTom()).isEqualTo(tom2);
        perioder.forEach(periode -> {
            List<UttakAktivitet> uttakAktiviteter = periode.getUttakAktiviteter();
            assertThat(uttakAktiviteter).hasSize(1);
            assertThat(uttakAktiviteter.get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(uttakAktiviteter.get(0).getStillingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(STILLING_70));
            assertThat(uttakAktiviteter.get(0).getArbeidsforhold().getIdentifikator()).isEqualTo(ORGNR);
            assertThat(uttakAktiviteter.get(0).getArbeidsforhold().getArbeidsforholdId()).isEqualTo(ARB_ID.getReferanse());
            assertThat(uttakAktiviteter.get(0).getArbeidsforhold().getReferanseType()).isEqualTo(ReferanseType.ORG_NR);
        });
    }

    @Test
    public void skalMappeUttakResultatPlanMedFlereArbeidsforholdOgOverlappendePerioder() {
        //Arrange

        //Arbeidsforhold 1
        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);
        var fom2 = tom.plusDays(1);
        var tom2 = LocalDate.of(2019, 4, 30);
        var uttakPeriode = lagUttaksperiode(fom, tom, BigDecimal.valueOf(66));
        var uttakPeriode2 = lagUttaksperiode(fom2, tom2, BigDecimal.valueOf(66));
        var uttakArbeidsforhold = lagArbeidsforhold(ORGNR, ARB_ID, uttakPeriode, uttakPeriode2);
        //Arbeidsforhold 2
        var fom3 = LocalDate.of(2019, Month.FEBRUARY, 1);
        var tom3 = LocalDate.of(2019, Month.APRIL, 15);
        var fom4 = tom3.plusDays(1);
        var tom4 = LocalDate.of(2019, 5, 15);
        var uttakPeriode3 = lagUttaksperiode(fom3, tom3, BigDecimal.valueOf(66));
        var uttakPeriode4 = lagUttaksperiode(fom4, tom4, BigDecimal.valueOf(66));
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var uttakArbeidsforhold2 = lagArbeidsforhold("123", arbeidsforholdId, uttakPeriode3, uttakPeriode4);
        var uttakResultat = lagUttak(uttakArbeidsforhold, uttakArbeidsforhold2);

        // Act
        UttakResultat regelPlan = mapper.mapFra(uttakResultat, null);

        // Assert
        assertThat(regelPlan).isNotNull();
        List<UttakResultatPeriode> perioder = regelPlan.getUttakResultatPerioder();
        assertThat(perioder).hasSize(5);
        assertThat(perioder.get(0).getFom()).isEqualTo(fom);//01.01
        assertThat(perioder.get(0).getTom()).isEqualTo(fom3.minusDays(1));//31.01
        assertThat(perioder.get(0).getUttakAktiviteter()).hasSize(1);
        assertThat(perioder.get(1).getFom()).isEqualTo(fom3);//01.02
        assertThat(perioder.get(1).getTom()).isEqualTo(tom);//31.03
        assertThat(perioder.get(1).getUttakAktiviteter()).hasSize(2);
        assertThat(perioder.get(2).getFom()).isEqualTo(fom2);//01.04
        assertThat(perioder.get(2).getTom()).isEqualTo(tom3);//15.04
        assertThat(perioder.get(2).getUttakAktiviteter()).hasSize(2);
        assertThat(perioder.get(3).getFom()).isEqualTo(fom4);//16.04
        assertThat(perioder.get(3).getTom()).isEqualTo(tom2);//30.4
        assertThat(perioder.get(3).getUttakAktiviteter()).hasSize(2);
        assertThat(perioder.get(4).getFom()).isEqualTo(tom2.plusDays(1));//01.05
        assertThat(perioder.get(4).getTom()).isEqualTo(tom4);//15.05
        assertThat(perioder.get(4).getUttakAktiviteter()).hasSize(1);
    }

    private SvangerskapspengerUttakResultatEntitet lagUttak(SvangerskapspengerUttakResultatArbeidsforholdEntitet... uttakArbeidsforhold) {
        var builder = new SvangerskapspengerUttakResultatEntitet.Builder(behandlingresultat);
        Stream.of(uttakArbeidsforhold).forEach(builder::medUttakResultatArbeidsforhold);
        return builder.build();
    }

    private SvangerskapspengerUttakResultatArbeidsforholdEntitet lagArbeidsforhold(String orgnr,
                                                                                   InternArbeidsforholdRef arbId,
                                                                                   SvangerskapspengerUttakResultatPeriodeEntitet... uttakPeriode) {
        return lagUttakResultatArbeidsforhold(UttakArbeidType.ORDINÆRT_ARBEID, orgnr, arbId, uttakPeriode);
    }

    private SvangerskapspengerUttakResultatArbeidsforholdEntitet lagUttakResultatArbeidsforhold(UttakArbeidType uttakArbeidType,
                                                                                                String orgnr,
                                                                                                InternArbeidsforholdRef arbId,
                                                                                                SvangerskapspengerUttakResultatPeriodeEntitet... uttakPeriode) {
        var builder = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medUttakArbeidType(uttakArbeidType);
        if (orgnr != null) {
            builder.medArbeidsforhold(Arbeidsgiver.virksomhet(orgnr), arbId);
        }
        Stream.of(uttakPeriode).forEach(builder::medPeriode);
        return builder.build();
    }

    private SvangerskapspengerUttakResultatPeriodeEntitet lagUttaksperiode(LocalDate fom, LocalDate tom, BigDecimal utbetalingsgrad) {
        return new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(utbetalingsgrad)
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak.INGEN)
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .build();
    }

}
