package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.TilbaketrekkVedTilkommetArbeidsforholdTjeneste.finnStørsteTilbaketrekkForTilkomneArbeidsforhold;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsgiverNøkkel;
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
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class TilbaketrekkVedTilkommetArbeidsforholdTjenesteTest {


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
    public void skal_finne_største_tilbaketrekk_ved_tilbaketrekk_mellom_to_arbeidsforhold() {
        // Arrange
        Yrkesaktivitet eksisterendeAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1)), ARBEIDSGIVER1, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet tilkommetAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1)), ARBEIDSGIVER2, InternArbeidsforholdRef.nullRef());

        BRNøkkelMedAndeler originalNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER1, originalPeriode);
        BRNøkkelMedAndeler revurderingNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER1, revurderingPeriode);
        BRNøkkelMedAndeler tilkommetNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilArbeidsgiver(DAGSATS, ARBEIDSGIVER2, revurderingPeriode);

        List<BRNøkkelMedAndeler> revurderingNøkler = List.of(tilkommetNøkkel, revurderingNøkkel);
        List<BRNøkkelMedAndeler> originaleNøkler = List.of(originalNøkkel);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(eksisterendeAktivitet, tilkommetAktivitet);

        // Act
        Optional<TilbaketrekkForTilkommetArbeidEntry> tilbaketrekkForTilkommetArbeidEntry = finnStørsteTilbaketrekkForTilkomneArbeidsforhold(revurderingNøkler, originaleNøkler, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(tilbaketrekkForTilkommetArbeidEntry).isPresent();
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().finnHindretTilbaketrekk()).isEqualTo(DAGSATS);
    }

    @Test
    public void skal_finne_største_tilbaketrekk_ved_tilbaketrekk_mellom_to_arbeidsforhold_der_bortfalt_har_fleire_tidligere_avtaler() {
        // Arrange
        Yrkesaktivitet eksisterendeAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1)), ARBEIDSGIVER1, InternArbeidsforholdRef.nyRef());
        Yrkesaktivitet avsluttetGammelAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.minusMonths(5)), ARBEIDSGIVER1, InternArbeidsforholdRef.nyRef());

        Yrkesaktivitet tilkommetAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1)), ARBEIDSGIVER2, InternArbeidsforholdRef.nullRef());

        BRNøkkelMedAndeler originalNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER1, originalPeriode);
        BRNøkkelMedAndeler revurderingNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER1, revurderingPeriode);
        BRNøkkelMedAndeler tilkommetNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilArbeidsgiver(DAGSATS, ARBEIDSGIVER2, revurderingPeriode);

        List<BRNøkkelMedAndeler> revurderingNøkler = List.of(tilkommetNøkkel, revurderingNøkkel);
        List<BRNøkkelMedAndeler> originaleNøkler = List.of(originalNøkkel);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(avsluttetGammelAktivitet, eksisterendeAktivitet, tilkommetAktivitet);

        // Act
        Optional<TilbaketrekkForTilkommetArbeidEntry> tilbaketrekkForTilkommetArbeidEntry = finnStørsteTilbaketrekkForTilkomneArbeidsforhold(revurderingNøkler, originaleNøkler, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(tilbaketrekkForTilkommetArbeidEntry).isPresent();
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().finnHindretTilbaketrekk()).isEqualTo(DAGSATS);
    }


    @Test
    public void skal_ikkje_finne_tilbaketrekk_når_det_ikkje_skal_gjerast_tilbaketrekk() {
        // Arrange
        Yrkesaktivitet eksisterendeAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1)), ARBEIDSGIVER1, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet tilkommetAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1)), ARBEIDSGIVER2, InternArbeidsforholdRef.nullRef());

        BRNøkkelMedAndeler originalNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER1, originalPeriode);
        BRNøkkelMedAndeler revurderingNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER1, revurderingPeriode);
        BRNøkkelMedAndeler tilkommetNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER2, revurderingPeriode);

        List<BRNøkkelMedAndeler> revurderingNøkler = List.of(tilkommetNøkkel, revurderingNøkkel);
        List<BRNøkkelMedAndeler> originaleNøkler = List.of(originalNøkkel);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(eksisterendeAktivitet, tilkommetAktivitet);

        // Act
        Optional<TilbaketrekkForTilkommetArbeidEntry> tilbaketrekkForTilkommetArbeidEntry = finnStørsteTilbaketrekkForTilkomneArbeidsforhold(revurderingNøkler, originaleNøkler, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(tilbaketrekkForTilkommetArbeidEntry).isEmpty();
    }

    @Test
    public void skal_finne_største_tilbaketrekk_ved_tilbaketrekk_mellom_to_avsluttede_og_en_tilkommet_refusjon_mindre_enn_tilbaketrekk() {
        // Arrange
        Yrkesaktivitet eksisterendeAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1)), ARBEIDSGIVER1, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet eksisterendeAktivitet2 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusDays(15)), ARBEIDSGIVER3, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet tilkommetAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1)), ARBEIDSGIVER2, InternArbeidsforholdRef.nullRef());

        BRNøkkelMedAndeler originalNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER1, originalPeriode);
        BRNøkkelMedAndeler originalNøkkel2 = lagNøkkelForAndelerMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER3, originalPeriode);
        BRNøkkelMedAndeler revurderingNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER1, revurderingPeriode);
        BRNøkkelMedAndeler revurderingNøkkel2 = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER3, revurderingPeriode);
        BRNøkkelMedAndeler tilkommetNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilArbeidsgiver(DAGSATS, ARBEIDSGIVER2, revurderingPeriode);

        List<BRNøkkelMedAndeler> revurderingNøkler = List.of(tilkommetNøkkel, revurderingNøkkel, revurderingNøkkel2);
        List<BRNøkkelMedAndeler> originaleNøkler = List.of(originalNøkkel, originalNøkkel2);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(eksisterendeAktivitet, tilkommetAktivitet, eksisterendeAktivitet2);

        // Act
        Optional<TilbaketrekkForTilkommetArbeidEntry> tilbaketrekkForTilkommetArbeidEntry = finnStørsteTilbaketrekkForTilkomneArbeidsforhold(revurderingNøkler, originaleNøkler, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(tilbaketrekkForTilkommetArbeidEntry).isPresent();
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().finnHindretTilbaketrekk()).isEqualTo(DAGSATS);
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().getAndelerIRevurderingMedSluttFørDatoSortertPåDato()).hasSize(2);
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().getTilkomneNøklerMedStartEtterDato()).hasSize(1);
    }


    @Test
    public void skal_finne_største_tilbaketrekk_ved_tilbaketrekk_mellom_tre_avsluttede_og_en_tilkommet_tilbaketrekk_mindre_enn_refusjon() {
        // Arrange
        Yrkesaktivitet eksisterendeAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1)), ARBEIDSGIVER1, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet eksisterendeAktivitet2 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusDays(15)), ARBEIDSGIVER3, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet eksisterendeAktivitet3 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(2).plusDays(1)), ARBEIDSGIVER4, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet tilkommetAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(1).plusDays(1)), ARBEIDSGIVER2, InternArbeidsforholdRef.nullRef());

        BRNøkkelMedAndeler originalNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER1, originalPeriode);
        BRNøkkelMedAndeler originalNøkkel2 = lagNøkkelForAndelerMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER3, originalPeriode);
        BRNøkkelMedAndeler originalNøkkel3 = lagNøkkelForAndelerMedFullUtbetalingTilBruker(DAGSATS, ARBEIDSGIVER4, originalPeriode);
        BRNøkkelMedAndeler revurderingNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER1, revurderingPeriode);
        BRNøkkelMedAndeler revurderingNøkkel2 = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER3, revurderingPeriode);
        BRNøkkelMedAndeler revurderingNøkkel3 = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER4, revurderingPeriode);
        BRNøkkelMedAndeler tilkommetNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilArbeidsgiver(3*DAGSATS, ARBEIDSGIVER2, revurderingPeriode);

        List<BRNøkkelMedAndeler> revurderingNøkler = List.of(tilkommetNøkkel, revurderingNøkkel, revurderingNøkkel2, revurderingNøkkel3);
        List<BRNøkkelMedAndeler> originaleNøkler = List.of(originalNøkkel, originalNøkkel2, originalNøkkel3);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(eksisterendeAktivitet, tilkommetAktivitet, eksisterendeAktivitet2, eksisterendeAktivitet3);

        // Act
        Optional<TilbaketrekkForTilkommetArbeidEntry> tilbaketrekkForTilkommetArbeidEntry = finnStørsteTilbaketrekkForTilkomneArbeidsforhold(revurderingNøkler, originaleNøkler, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(tilbaketrekkForTilkommetArbeidEntry).isPresent();
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().finnHindretTilbaketrekk()).isEqualTo(2*DAGSATS);
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().getAndelerIRevurderingMedSluttFørDatoSortertPåDato()).hasSize(2);
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().getTilkomneNøklerMedStartEtterDato()).hasSize(1);
    }

    @Test
    public void skal_finne_største_tilbaketrekk_ved_tilbaketrekk_mellom_tre_avsluttede_og_tre_tilkommet_men_kun_deler_av_settet_er_med_i_tilbaketrekk() {
        // Arrange
        Yrkesaktivitet eksisterendeAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusDays(16)), ARBEIDSGIVER1, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet eksisterendeAktivitet2 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusDays(15)), ARBEIDSGIVER3, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet eksisterendeAktivitet3 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT.plusMonths(1)), ARBEIDSGIVER4, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet tilkommetAktivitet = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusDays(20)), ARBEIDSGIVER2, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet tilkommetAktivitet2 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(2)), ARBEIDSGIVER5, InternArbeidsforholdRef.nullRef());
        Yrkesaktivitet tilkommetAktivitet3 = lagYrkesaktivitet(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusMonths(2).plusDays(10)), ARBEIDSGIVER6, InternArbeidsforholdRef.nullRef());

        BRNøkkelMedAndeler originalNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(50, ARBEIDSGIVER1, originalPeriode);
        BRNøkkelMedAndeler originalNøkkel2 = lagNøkkelForAndelerMedFullUtbetalingTilBruker(80, ARBEIDSGIVER3, originalPeriode);
        BRNøkkelMedAndeler originalNøkkel3 = lagNøkkelForAndelerMedFullUtbetalingTilBruker(100, ARBEIDSGIVER4, originalPeriode);
        BRNøkkelMedAndeler revurderingNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER1, revurderingPeriode);
        BRNøkkelMedAndeler revurderingNøkkel2 = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER3, revurderingPeriode);
        BRNøkkelMedAndeler revurderingNøkkel3 = lagNøkkelForAndelerMedFullUtbetalingTilBruker(0, ARBEIDSGIVER4, revurderingPeriode);
        BRNøkkelMedAndeler tilkommetNøkkel = lagNøkkelForAndelerMedFullUtbetalingTilArbeidsgiver(100, ARBEIDSGIVER2, revurderingPeriode);
        BRNøkkelMedAndeler tilkommetNøkkel2 = lagNøkkelForAndelerMedFullUtbetalingTilArbeidsgiver(60, ARBEIDSGIVER5, revurderingPeriode);
        BRNøkkelMedAndeler tilkommetNøkkel3 = lagNøkkelForAndelerMedFullUtbetalingTilArbeidsgiver(120, ARBEIDSGIVER6, revurderingPeriode);

        List<BRNøkkelMedAndeler> revurderingNøkler = List.of(tilkommetNøkkel, tilkommetNøkkel2, tilkommetNøkkel3, revurderingNøkkel, revurderingNøkkel2, revurderingNøkkel3);
        List<BRNøkkelMedAndeler> originaleNøkler = List.of(originalNøkkel, originalNøkkel2, originalNøkkel3);
        List<Yrkesaktivitet> yrkesaktiviteter = List.of(eksisterendeAktivitet, tilkommetAktivitet, tilkommetAktivitet2, tilkommetAktivitet3, eksisterendeAktivitet2, eksisterendeAktivitet3);

        // Act
        Optional<TilbaketrekkForTilkommetArbeidEntry> tilbaketrekkForTilkommetArbeidEntry = finnStørsteTilbaketrekkForTilkomneArbeidsforhold(revurderingNøkler, originaleNøkler, yrkesaktiviteter, SKJÆRINGSTIDSPUNKT);

        // Assert
        assertThat(tilbaketrekkForTilkommetArbeidEntry).isPresent();
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().finnHindretTilbaketrekk()).isEqualTo(180);
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().getAndelerIRevurderingMedSluttFørDatoSortertPåDato()).hasSize(3);
        assertThat(tilbaketrekkForTilkommetArbeidEntry.get().getTilkomneNøklerMedStartEtterDato()).hasSize(2);
    }


    private Yrkesaktivitet lagYrkesaktivitet(DatoIntervallEntitet periode, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdId) {
        return YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny().medPeriode(periode))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsgiver(arbeidsgiver).build();
    }

    private BRNøkkelMedAndeler lagNøkkelForAndelerMedFullUtbetalingTilBruker(int dagsats, Arbeidsgiver arbeidsgiver, BeregningsresultatPeriode periode) {
        BeregningsresultatAndel brukersAndel = BeregningsresultatAndel.builder()
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
        BRNøkkelMedAndeler nøkkel = new BRNøkkelMedAndeler(new AktivitetOgArbeidsgiverNøkkel(brukersAndel));
        nøkkel.leggTilAndel(brukersAndel);
        return nøkkel;
    }

    private BRNøkkelMedAndeler lagNøkkelForAndelerMedFullUtbetalingTilArbeidsgiver(int dagsats, Arbeidsgiver arbeidsgiver, BeregningsresultatPeriode periode) {
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
        BRNøkkelMedAndeler nøkkel = new BRNøkkelMedAndeler(new AktivitetOgArbeidsgiverNøkkel(brukersAndel));
        nøkkel.leggTilAndel(brukersAndel);
        nøkkel.leggTilAndel(arbeidsgiversAndel);
        return nøkkel;
    }

}
