package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.FinnEndringerIResultatForTilkommetArbeidsforhold.finnEndringer;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class FinnEndringerIResultatForTilkommetArbeidsforholdTest {

    public static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusMonths(1);
    public static final String ORGNR1 = KUNSTIG_ORG + "1";
    public static final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.virksomhet(ORGNR1);
    public static final String ORGNR2 = KUNSTIG_ORG + "2";
    public static final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.virksomhet(ORGNR2);
    public static final String ORGNR3 = KUNSTIG_ORG + "3";
    public static final Arbeidsgiver ARBEIDSGIVER3 = Arbeidsgiver.virksomhet(ORGNR3);
    public static final String ORGNR4 = KUNSTIG_ORG + "4";
    public static final Arbeidsgiver ARBEIDSGIVER4 = Arbeidsgiver.virksomhet(ORGNR4);
    public static final String ORGNR5 = KUNSTIG_ORG + "5";
    public static final Arbeidsgiver ARBEIDSGIVER5 = Arbeidsgiver.virksomhet(ORGNR5);
    public static final String ORGNR6 = KUNSTIG_ORG + "6";
    public static final Arbeidsgiver ARBEIDSGIVER6 = Arbeidsgiver.virksomhet(ORGNR6);
    public static final int DAGSATS = 2134;
    private BeregningsresultatPeriode revurderingPeriode;
    private BeregningsresultatPeriode originalPeriode;

    @Before
    public void setUp() {
        BeregningsresultatEntitet revurderingResultat = BeregningsresultatEntitet.builder()
            .medRegelInput("regelinput")
            .medRegelSporing("Regelsporing")
            .build();
        revurderingPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2))
            .build(revurderingResultat);
        BeregningsresultatEntitet originalResultat = BeregningsresultatEntitet.builder()
            .medRegelInput("regelinput")
            .medRegelSporing("Regelsporing")
            .build();
        originalPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2))
            .build(originalResultat);
    }

    @Test
    public void skal_hindre_tilbaketrekk_ved_omfordeling_mellom_to_arbeidsforhold() {
        // Arrange
        Yrkesaktivitet eksisterendeAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1)), ARBEIDSGIVER1);
        Yrkesaktivitet tilkommetAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1)), ARBEIDSGIVER2);

        BeregningsresultatAndel originalAndel = lagAndelMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER1, originalPeriode);
        BeregningsresultatAndel revurderingAndel = lagAndelMedFullUtbetalingTilBruker(0, ARBEIDSGIVER1, revurderingPeriode);
        List<BeregningsresultatAndel> tilkommetAndeler = lagAndelerMedFullUtbetalingTilArbeidsgiver(DAGSATS, ARBEIDSGIVER2, revurderingPeriode);

        List<BeregningsresultatAndel> revurderingAndeler = List.of(revurderingAndel, tilkommetAndeler.get(0), tilkommetAndeler.get(1));
        List<BeregningsresultatAndel> originaleAndeler = List.of(originalAndel);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(eksisterendeAktivitet, tilkommetAktivitet);

        // Act
        List<EndringIBeregningsresultat> endringer = finnEndringer(originaleAndeler, revurderingAndeler, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(endringer).hasSize(2);
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER1);
            assertThat(endring.erBrukerMottaker()).isEqualTo(true);
            assertThat(endring.getDagsats()).isEqualTo(DAGSATS);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(0);
        });
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER2);
            assertThat(endring.erBrukerMottaker()).isEqualTo(false);
            assertThat(endring.getDagsats()).isEqualTo(0);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(DAGSATS);
        });
    }

    @Test
    public void skal_finne_største_tilbaketrekk_ved_tilbaketrekk_mellom_to_avsluttede_og_en_tilkommet_refusjon_mindre_enn_tilbaketrekk() {
        // Arrange
        Yrkesaktivitet eksisterendeAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1)), ARBEIDSGIVER1);
        Yrkesaktivitet eksisterendeAktivitet2 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusDays(15)), ARBEIDSGIVER3);
        Yrkesaktivitet tilkommetAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1)), ARBEIDSGIVER2);

        BeregningsresultatAndel originalAndel = lagAndelMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER1, originalPeriode);
        BeregningsresultatAndel originalAndel2 = lagAndelMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER3, originalPeriode);
        BeregningsresultatAndel revurderingAndel = lagAndelMedFullUtbetalingTilBruker(0, ARBEIDSGIVER1, revurderingPeriode);
        BeregningsresultatAndel revurderingAndel2 = lagAndelMedFullUtbetalingTilBruker(0, ARBEIDSGIVER3, revurderingPeriode);
        List<BeregningsresultatAndel> tilkommetAndel = lagAndelerMedFullUtbetalingTilArbeidsgiver(DAGSATS, ARBEIDSGIVER2, revurderingPeriode);

        List<BeregningsresultatAndel> revurderingAndeler = List.of(tilkommetAndel.get(0), tilkommetAndel.get(1), revurderingAndel, revurderingAndel2);
        List<BeregningsresultatAndel> originaleAndeler = List.of(originalAndel, originalAndel2);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(eksisterendeAktivitet, tilkommetAktivitet, eksisterendeAktivitet2);

        // Act
        List<EndringIBeregningsresultat> endringer = finnEndringer(originaleAndeler, revurderingAndeler, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(endringer).hasSize(2);
        assertThat(endringer).anySatisfy(endring -> {
            // Det er tilfeldig kva for ein andel som får hindret tilbaketrekk her, kan vere enten ARBEIDSGIVER1 eller ARBEIDSGIVER3
            assertThat(endring.getArbeidsgiver().equals(ARBEIDSGIVER1) || endring.getArbeidsgiver().equals(ARBEIDSGIVER3)).isEqualTo(true);
            assertThat(endring.erBrukerMottaker()).isEqualTo(true);
            assertThat(endring.getDagsats()).isEqualTo(DAGSATS);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(0);
        });
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER2);
            assertThat(endring.erBrukerMottaker()).isEqualTo(false);
            assertThat(endring.getDagsats()).isEqualTo(0);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(DAGSATS);
        });
    }


    @Test
    public void skal_hindre_tilbaketrekk_ved_omfordeling_mellom_tre_avsluttede_og_en_tilkommet_tilbaketrekk_mindre_enn_refusjon() {
        // Arrange
        Yrkesaktivitet eksisterendeAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1)), ARBEIDSGIVER1);
        Yrkesaktivitet eksisterendeAktivitet2 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusDays(15)), ARBEIDSGIVER3);
        Yrkesaktivitet eksisterendeAktivitet3 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1)), ARBEIDSGIVER4);
        Yrkesaktivitet tilkommetAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(2).plusDays(1)), ARBEIDSGIVER2);

        BeregningsresultatAndel originalAndel = lagAndelMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER1, originalPeriode);
        BeregningsresultatAndel originalAndel2 = lagAndelMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER3, originalPeriode);
        BeregningsresultatAndel originalAndel3 = lagAndelMedFullUtbetalingTilBruker(DAGSATS/2, ARBEIDSGIVER4, originalPeriode);
        BeregningsresultatAndel revurderingAndel = lagAndelMedFullUtbetalingTilBruker(0, ARBEIDSGIVER1, revurderingPeriode);
        BeregningsresultatAndel revurderingAndel2 = lagAndelMedFullUtbetalingTilBruker(0, ARBEIDSGIVER3, revurderingPeriode);
        BeregningsresultatAndel revurderingAndel3 = lagAndelMedFullUtbetalingTilBruker(0, ARBEIDSGIVER4, revurderingPeriode);
        List<BeregningsresultatAndel> tilkommetAndeler = lagAndelerMedFullUtbetalingTilArbeidsgiver(3*DAGSATS, ARBEIDSGIVER2, revurderingPeriode);

        List<BeregningsresultatAndel> revurderingAndeler = List.of(tilkommetAndeler.get(0), tilkommetAndeler.get(1), revurderingAndel, revurderingAndel2, revurderingAndel3);
        List<BeregningsresultatAndel> originaleAndeler = List.of(originalAndel, originalAndel2, originalAndel3);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(eksisterendeAktivitet, tilkommetAktivitet, eksisterendeAktivitet2, eksisterendeAktivitet3);

        // Act
        List<EndringIBeregningsresultat> endringer = finnEndringer(originaleAndeler, revurderingAndeler, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(endringer).hasSize(4);
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER1);
            assertThat(endring.erBrukerMottaker()).isEqualTo(true);
            assertThat(endring.getDagsats()).isEqualTo(DAGSATS);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(0);
        });
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER3);
            assertThat(endring.erBrukerMottaker()).isEqualTo(true);
            assertThat(endring.getDagsats()).isEqualTo(DAGSATS);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(0);
        });
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER4);
            assertThat(endring.erBrukerMottaker()).isEqualTo(true);
            assertThat(endring.getDagsats()).isEqualTo(DAGSATS/2);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(0);
        });
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER2);
            assertThat(endring.erBrukerMottaker()).isEqualTo(false);
            assertThat(endring.getDagsats()).isEqualTo(DAGSATS/2);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(3*DAGSATS);
        });
    }

    @Test
    public void skal_hindre_tilbaketrekk_ved_omfordeling_mellom_tre_avsluttede_og_tre_tilkommet_men_kun_deler_av_settet_er_med_i_fordeling() {
        // Arrange
        Yrkesaktivitet eksisterendeAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusDays(16)), ARBEIDSGIVER1);
        Yrkesaktivitet eksisterendeAktivitet2 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusDays(15)), ARBEIDSGIVER3);
        Yrkesaktivitet eksisterendeAktivitet3 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1)), ARBEIDSGIVER4);
        Yrkesaktivitet tilkommetAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusDays(20)), ARBEIDSGIVER2);
        Yrkesaktivitet tilkommetAktivitet2 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(2)), ARBEIDSGIVER5);
        Yrkesaktivitet tilkommetAktivitet3 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(2).plusDays(10)), ARBEIDSGIVER6);

        BeregningsresultatAndel originalAndel = lagAndelMedFullUtbetalingTilBruker(50, ARBEIDSGIVER1, originalPeriode);
        BeregningsresultatAndel originalAndel2 = lagAndelMedFullUtbetalingTilBruker(80, ARBEIDSGIVER3, originalPeriode);
        BeregningsresultatAndel originalAndel3 = lagAndelMedFullUtbetalingTilBruker(100, ARBEIDSGIVER4, originalPeriode);
        BeregningsresultatAndel revurderingAndel = lagAndelMedFullUtbetalingTilBruker(0, ARBEIDSGIVER1, revurderingPeriode);
        BeregningsresultatAndel revurderingAndel2 = lagAndelMedFullUtbetalingTilBruker(0, ARBEIDSGIVER3, revurderingPeriode);
        BeregningsresultatAndel revurderingAndel3 = lagAndelMedFullUtbetalingTilBruker(0, ARBEIDSGIVER4, revurderingPeriode);
        List<BeregningsresultatAndel> tilkommetAndeler = lagAndelerMedFullUtbetalingTilArbeidsgiver(100, ARBEIDSGIVER2, revurderingPeriode);
        List<BeregningsresultatAndel> tilkommetAndeler2 = lagAndelerMedFullUtbetalingTilArbeidsgiver(60, ARBEIDSGIVER5, revurderingPeriode);
        List<BeregningsresultatAndel> tilkommetAndeler3 = lagAndelerMedFullUtbetalingTilArbeidsgiver(120, ARBEIDSGIVER6, revurderingPeriode);

        List<BeregningsresultatAndel> revurderingAndeler = List.of(tilkommetAndeler.get(0), tilkommetAndeler.get(1),
            tilkommetAndeler2.get(0), tilkommetAndeler2.get(1), tilkommetAndeler3.get(0), tilkommetAndeler3.get(1), revurderingAndel, revurderingAndel2, revurderingAndel3);
        List<BeregningsresultatAndel> originaleAndeler = List.of(originalAndel, originalAndel2, originalAndel3);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(eksisterendeAktivitet, tilkommetAktivitet, tilkommetAktivitet2, tilkommetAktivitet3, eksisterendeAktivitet2, eksisterendeAktivitet3);

        // Act
        List<EndringIBeregningsresultat> endringer = finnEndringer(originaleAndeler, revurderingAndeler, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(endringer).hasSize(5);
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER1);
            assertThat(endring.erBrukerMottaker()).isEqualTo(true);
            assertThat(endring.getDagsats()).isEqualTo(50);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(0);
        });
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER3);
            assertThat(endring.erBrukerMottaker()).isEqualTo(true);
            assertThat(endring.getDagsats()).isEqualTo(30);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(0);
        });
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER4);
            assertThat(endring.erBrukerMottaker()).isEqualTo(true);
            assertThat(endring.getDagsats()).isEqualTo(100);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(0);
        });
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER5);
            assertThat(endring.erBrukerMottaker()).isEqualTo(false);
            assertThat(endring.getDagsats()).isEqualTo(0);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(60);
        });
        assertThat(endringer).anySatisfy(endring -> {
            assertThat(endring.getArbeidsgiver()).isEqualTo(ARBEIDSGIVER6);
            assertThat(endring.erBrukerMottaker()).isEqualTo(false);
            assertThat(endring.getDagsats()).isEqualTo(0);
            assertThat(endring.getDagsatsFraBg()).isEqualTo(120);
        });
    }


    private Yrkesaktivitet lagYrkesaktivitet(DatoIntervallEntitet periode, Arbeidsgiver arbeidsgiver) {
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(periode))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver).build();
    }

    private BeregningsresultatAndel lagAndelMedFullUtbetalingTilBruker(int dagsats, Arbeidsgiver arbeidsgiver, BeregningsresultatPeriode periode) {
        return BeregningsresultatAndel.builder()
            .medDagsats(dagsats)
            .medBrukerErMottaker(true)
            .medArbeidsgiver(arbeidsgiver)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsatsFraBg(dagsats)
            .build(periode);
    }

    private List<BeregningsresultatAndel> lagAndelerMedFullUtbetalingTilArbeidsgiver(int dagsats, Arbeidsgiver arbeidsgiver, BeregningsresultatPeriode periode) {
        BeregningsresultatAndel brukersAndel = BeregningsresultatAndel.builder()
            .medDagsats(0)
            .medBrukerErMottaker(true)
            .medArbeidsgiver(arbeidsgiver)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsatsFraBg(0)
            .build(periode);
        BeregningsresultatAndel arbeidsgiversAndel = BeregningsresultatAndel.builder()
            .medDagsats(dagsats)
            .medBrukerErMottaker(false)
            .medArbeidsgiver(arbeidsgiver)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medDagsatsFraBg(dagsats)
            .build(periode);
        return List.of(brukersAndel, arbeidsgiversAndel);
    }
}
