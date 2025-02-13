package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class HindreTilbaketrekkNårAlleredeUtbetaltTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.JANUARY, 20);
    private static final LocalDate SISTE_UTTAKSDAG = LocalDate.of(2019, Month.APRIL, 4);
    private static final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG + "1");
    private static final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG + "2");
    private static final Arbeidsgiver ARBEIDSGIVER3 = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG + "3");
    private static final InternArbeidsforholdRef MED_INTERNREFERANSE = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef UTEN_INTERNREFERANSE = InternArbeidsforholdRef.nullRef();


    @Test
    void omfordeling_med_2_tilkomne_arbeidsforhold_skal_ikke_ta_med_tilkommet_i_flyttbar_dagsats() {
        // Arrange
        var PERIODE_FOM = LocalDate.of(2020, 3, 24);
        var GAMMELT_ARBEID = ARBEIDSGIVER1;
        var TILKOMMET1 = ARBEIDSGIVER2;
        var TILKOMMET2 = ARBEIDSGIVER3;

        var forrigeBrp = lagBeregningsresultatPeriode(PERIODE_FOM, LocalDate.of(2020, 3, 31));
        lagSNAndel(forrigeBrp, 1225);
        var gammeltArbeidDagsats = 542;
        lagAndel(forrigeBrp, GAMMELT_ARBEID, true, gammeltArbeidDagsats, UTEN_INTERNREFERANSE);

        var forrigeTY = forrigeBrp.getBeregningsresultat();

        var beregningsgrunnlagBrp = lagBeregningsresultatPeriode(LocalDate.of(2020, 3, 24),
                LocalDate.of(2020, 3, 31));
        lagSNAndel(beregningsgrunnlagBrp, 152);
        lagAndel(beregningsgrunnlagBrp, GAMMELT_ARBEID, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, TILKOMMET1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, TILKOMMET2, false, 1400, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, TILKOMMET2, true, 0, UTEN_INTERNREFERANSE);
        var beregningsgrunnlagTY = beregningsgrunnlagBrp.getBeregningsresultat();

        var gammelt_arbeid = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 8, 1), LocalDate.of(2020, 2, 1))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
                .medArbeidsgiver(GAMMELT_ARBEID).build();

        var tilkommet_arbeid1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 23))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(TILKOMMET1).build();

        var tilkommet_arbeid2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.of(2020, 3, 24))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(TILKOMMET2).build();

        // Act
        var utbetTY = HindreTilbaketrekkNårAlleredeUtbetalt.reberegn(beregningsgrunnlagTY,
                MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(forrigeTY.getBeregningsresultatPerioder(),
                        beregningsgrunnlagTY.getBeregningsresultatPerioder(), LocalDate.now()),
                List.of(tilkommet_arbeid1, gammelt_arbeid, tilkommet_arbeid2), SKJÆRINGSTIDSPUNKT);

        // Assert
        var beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(1);
        var p0 = beregningsresultatPerioder.getFirst();
        assertThat(p0.getBeregningsresultatPeriodeFom()).isEqualTo(PERIODE_FOM);
        var p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler)
            .hasSize(5)
            .anySatisfy(andel -> {
                assertThat(andel.getAktivitetStatus()).isEqualByComparingTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(1010);
            }).anySatisfy(andel -> {
                assertThat(andel.getArbeidsgiver()).containsSame(GAMMELT_ARBEID);
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(gammeltArbeidDagsats);
            }).anySatisfy(andel -> {
                assertThat(andel.getArbeidsgiver()).containsSame(TILKOMMET2);
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getDagsats()).as("dagsats").isZero();
                assertThat(andel.getDagsatsFraBg()).as("dagsatsBG").isEqualTo(1400);
            }).anySatisfy(andel -> {
                assertThat(andel.getArbeidsgiver()).containsSame(TILKOMMET2);
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getDagsats()).as("dagsats").isZero();
                assertThat(andel.getDagsatsFraBg()).as("dagsatsBG").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.getArbeidsgiver()).containsSame(TILKOMMET1);
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getDagsats()).as("dagsats").isZero();
            });
    }

    @Test
    void omfordel_til_bruker_når_det_krever_refusjon_tilbake_i_tid_for_tilkommet_arbeidsforhold() {
        // Arrange
        var dagensDato = LocalDate.of(2019, Month.FEBRUARY, 4);
        var forrigeBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, true, 1500, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, false, 0, UTEN_INTERNREFERANSE);
        var forrigeTY = forrigeBrp.getBeregningsresultat();

        var beregningsgrunnlagBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, false, 1500, UTEN_INTERNREFERANSE);
        var beregningsgrunnlagTY = beregningsgrunnlagBrp.getBeregningsresultat();

        var yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(20), SKJÆRINGSTIDSPUNKT.plusDays(14))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(ARBEIDSGIVER1).build();

        var yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusDays(15))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(ARBEIDSGIVER2).build();

        // Act
        var utbetTY = HindreTilbaketrekkNårAlleredeUtbetalt.reberegn(beregningsgrunnlagTY,
                MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                        forrigeTY.getBeregningsresultatPerioder(),
                        beregningsgrunnlagTY.getBeregningsresultatPerioder(), dagensDato),
                List.of(yrkesaktivitet1, yrkesaktivitet2), SKJÆRINGSTIDSPUNKT);

        // Assert
        var beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(2);
        var p0 = beregningsresultatPerioder.getFirst();
        assertThat(p0.getBeregningsresultatPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(p0.getBeregningsresultatPeriodeTom()).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
        var p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler)
            .hasSize(3)
            .anySatisfy(andel -> {
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER2);
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getDagsats()).as("dagsats").isZero();
                assertThat(andel.getDagsatsFraBg()).as("dagsatsBG").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER2);
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getDagsats()).as("dagsats").isZero();
                assertThat(andel.getDagsatsFraBg()).as("dagsatsBG").isEqualTo(1500);
            }).anySatisfy(andel -> {
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER1);
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(1500);
            });
    }

    /**
     * Forrige tilkjent ytelse:
     * <ul>
     * <li>20.01 - 04.04</li>
     * <li>kun utbetalt til bruker</li>
     * <li>dagsats = 1000</li>
     * </ul>
     * Tilkjent ytelse (Beregningsgrunnlag):
     * <ul>
     * <li>20.01 - 04.04</li>
     * <li>full refusjon til arbeidsgiver</li>
     * <li>dagsats = 1000</li>
     * </ul>
     * dagens dato: 04.02
     */
    @Test
    void omfordel_til_bruker_når_det_krever_refusjon_tilbake_i_tid() {
        // Arrange
        var dagensDato = LocalDate.of(2019, Month.FEBRUARY, 4);
        var forventetDagsats = 1000;
        var forrigeTY = lagBeregningsresultatFP(true, forventetDagsats);
        var beregningsgrunnlagTY = lagBeregningsresultatFP(false, forventetDagsats);

        // Act
        var utbetTY = HindreTilbaketrekkNårAlleredeUtbetalt.reberegn(beregningsgrunnlagTY,
                MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(forrigeTY.getBeregningsresultatPerioder(),
                        beregningsgrunnlagTY.getBeregningsresultatPerioder(), dagensDato),
                List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        var beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(2);
        var p0 = beregningsresultatPerioder.getFirst();
        assertThat(p0.getBeregningsresultatPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(p0.getBeregningsresultatPeriodeTom()).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
        var p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler)
            .hasSize(2)
            .anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(forventetDagsats);
                assertThat(andel.getDagsatsFraBg()).as("dagsatsBG").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getDagsats()).as("dagsats").isZero();
                assertThat(andel.getDagsatsFraBg()).as("dagsatsBG").isEqualTo(forventetDagsats);
            });

        var p1 = beregningsresultatPerioder.get(1);
        assertThat(p1.getBeregningsresultatPeriodeFom()).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        assertThat(p1.getBeregningsresultatPeriodeTom()).isEqualTo(SISTE_UTTAKSDAG);
        var p1andeler = p1.getBeregningsresultatAndelList();
        assertThat(p1andeler)
            .hasSize(2)
            .anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getDagsats()).as("dagsats").isZero();
                assertThat(andel.getDagsatsFraBg()).as("dagsatsBG").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(forventetDagsats);
                assertThat(andel.getDagsatsFraBg()).as("dagsatsBG").isEqualTo(forventetDagsats);
            });
    }

    /**
     * Forrige tilkjent ytelse:
     * <ul>
     * <li>20.01 - 04.04</li>
     * <li>Dagsats A1 refusjon: 1500</li>
     * <li>Dagsats A2 bruker: 600</li>
     * </ul>
     * Tilkjent ytelse (Beregningsgrunnlag):
     * <ul>
     * <li>20.01 - 04.04</li>
     * <li>Dagsats A1 refusjon: 1050</li>
     * <li>Dagsats A2 refusjon: 1050</li>
     * </ul>
     * dagens dato: 04.02
     */
    @Test
    void skal_avkorte_men_ikke_omfordele_fra_bruker() {
        // Arrange
        var dagensDato = LocalDate.of(2019, Month.FEBRUARY, 4);
        var forrigeBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, false, 1500, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER2, true, 600, UTEN_INTERNREFERANSE);
        var forrigeTY = forrigeBrp.getBeregningsresultat();

        var beregningsgrunnlagBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, false, 1050, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, false, 1050, UTEN_INTERNREFERANSE);
        var beregningsgrunnlagTY = beregningsgrunnlagBrp.getBeregningsresultat();

        // Act
        var utbetTY = HindreTilbaketrekkNårAlleredeUtbetalt.reberegn(beregningsgrunnlagTY,
                MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                        forrigeTY.getBeregningsresultatPerioder(),
                        beregningsgrunnlagTY.getBeregningsresultatPerioder(), dagensDato),
                List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        var beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(2);
        var p0 = beregningsresultatPerioder.getFirst();
        assertThat(p0.getBeregningsresultatPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(p0.getBeregningsresultatPeriodeTom()).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
        var p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler)
            .hasSize(4)
            .anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER1);
                assertThat(andel.getDagsats()).as("dagsats").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER1);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(1050);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER2);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(600);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER2);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(450);
            });

        var p1 = beregningsresultatPerioder.get(1);
        assertThat(p1.getBeregningsresultatPeriodeFom()).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        assertThat(p1.getBeregningsresultatPeriodeTom()).isEqualTo(SISTE_UTTAKSDAG);
        var p1andeler = p1.getBeregningsresultatAndelList();
        assertThat(p1andeler)
            .hasSize(4)
            .anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER1);
                assertThat(andel.getDagsats()).as("dagsats").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER1);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(1050);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER2);
                assertThat(andel.getDagsats()).as("dagsats").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER2);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(1050);
            });
    }

    /**
     * Forrige tilkjent ytelse:
     * <ul>
     * <li>20.01 - 04.04</li>
     * <li>Dagsats A1 refusjon: 1500</li>
     * <li>Dagsats A2 bruker: 240</li>
     * <li>Dagsats A3 bruker: 360</li>
     * </ul>
     * Tilkjent ytelse (Beregningsgrunnlag):
     * <ul>
     * <li>20.01 - 04.04</li>
     * <li>Dagsats A1 refusjon: 600</li>
     * <li>Dagsats A2 refusjon: 600</li>
     * <li>Dagsats A3 refusjon: 900</li>
     * </ul>
     * dagens dato: 04.02
     */
    @Test
    void skal_fordele_refusjon_til_to_nye_arbeidsgivere() {
        // Arrange
        var dagensDato = LocalDate.of(2019, Month.FEBRUARY, 4);
        var forrigeBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, false, 1500, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER2, true, 240, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER3, true, 360, UTEN_INTERNREFERANSE);
        var forrigeTY = forrigeBrp.getBeregningsresultat();

        var beregningsgrunnlagBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, false, 600, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, false, 600, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER3, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER3, false, 900, UTEN_INTERNREFERANSE);
        var beregningsgrunnlagTY = beregningsgrunnlagBrp.getBeregningsresultat();

        // Act
        var utbetTY = HindreTilbaketrekkNårAlleredeUtbetalt.reberegn(beregningsgrunnlagTY,
                MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                        forrigeTY.getBeregningsresultatPerioder(),
                        beregningsgrunnlagTY.getBeregningsresultatPerioder(), dagensDato),
                List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        var beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(2);
        var p0 = beregningsresultatPerioder.getFirst();
        assertThat(p0.getBeregningsresultatPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(p0.getBeregningsresultatPeriodeTom()).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
        var p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler)
            .hasSize(6)
            .anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER1);
                assertThat(andel.getDagsats()).as("dagsats").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER1);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(600);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER2);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(240);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER2);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(360);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER3);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(360);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER3);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(540);
            });

        var p1 = beregningsresultatPerioder.get(1);
        assertThat(p1.getBeregningsresultatPeriodeFom()).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        assertThat(p1.getBeregningsresultatPeriodeTom()).isEqualTo(SISTE_UTTAKSDAG);
        var p1andeler = p1.getBeregningsresultatAndelList();
        assertThat(p1andeler)
            .hasSize(6)
            .anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER1);
                assertThat(andel.getDagsats()).as("dagsats").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER1);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(600);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER2);
                assertThat(andel.getDagsats()).as("dagsats").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER2);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(600);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER3);
                assertThat(andel.getDagsats()).as("dagsats").isZero();
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsgiver()).containsSame(ARBEIDSGIVER3);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(900);
            });
    }

    @Test
    void skalIkkeOmfordeleTilTilkommetArbeidsforholdISammeOrganisasjonNårEndring() {
        // Arrange
        var dagensDato = SISTE_UTTAKSDAG.plusMonths(2);
        var forrigeBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);

        lagAndel(forrigeBrp, ARBEIDSGIVER1, false, 834, MED_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, true, 1, MED_INTERNREFERANSE);
        var forrigeTY = forrigeBrp.getBeregningsresultat();

        var beregningsgrunnlagBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, false, 834, MED_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, true, 0, MED_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);

        var beregningsgrunnlagTY = beregningsgrunnlagBrp.getBeregningsresultat();

        var eksisterendeAktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2))))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsforholdId(MED_INTERNREFERANSE)
                .medArbeidsgiver(ARBEIDSGIVER1).build();
        var tilkommetAktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT)))
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsforholdId(UTEN_INTERNREFERANSE)
                .medArbeidsgiver(ARBEIDSGIVER1).build();

        // Act
        var utbetTY = HindreTilbaketrekkNårAlleredeUtbetalt.reberegn(beregningsgrunnlagTY,
                MapBRAndelSammenligningTidslinje.opprettTidslinjeTest(
                        forrigeTY.getBeregningsresultatPerioder(),
                        beregningsgrunnlagTY.getBeregningsresultatPerioder(), dagensDato),
                List.of(eksisterendeAktivitet, tilkommetAktivitet), SKJÆRINGSTIDSPUNKT);

        // Assert
        var beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(1);
        var p0 = beregningsresultatPerioder.getFirst();

        var p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler)
            .hasSize(3)
            .anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
                assertThat(andel.getArbeidsforholdRef()).isSameAs(MED_INTERNREFERANSE);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(833);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsforholdRef()).isSameAs(MED_INTERNREFERANSE);
                assertThat(andel.getDagsats()).as("dagsats").isEqualTo(1);
            }).anySatisfy(andel -> {
                assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
                assertThat(andel.getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.nullRef());
                assertThat(andel.getDagsats()).as("dagsats").isZero();
            });
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP(boolean brukerErMottaker, int dagsats) {
        var brp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        if (!brukerErMottaker) {
            lagAndel(brp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        }
        lagAndel(brp, ARBEIDSGIVER1, brukerErMottaker, dagsats, UTEN_INTERNREFERANSE);
        return brp.getBeregningsresultat();
    }

    private BeregningsresultatPeriode lagBeregningsresultatPeriode(LocalDate fom, LocalDate tom) {
        var br = BeregningsresultatEntitet.builder()
                .medRegelSporing("regelsporing")
                .medRegelInput("regelinput")
                .build();
        return BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(fom, tom)
                .build(br);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatPeriode brp, Arbeidsgiver arbeidsgiver, boolean brukerErMottaker, int dagsats,
            InternArbeidsforholdRef internArbeidsforholdRef) {
        return BeregningsresultatAndel.builder()
                .medBrukerErMottaker(brukerErMottaker)
                .medArbeidsgiver(arbeidsgiver)
                .medStillingsprosent(new BigDecimal(100))
                .medUtbetalingsgrad(new BigDecimal(100))
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medArbeidsforholdRef(internArbeidsforholdRef)
                .medDagsats(dagsats)
                .medDagsatsFraBg(dagsats)
                .build(brp);
    }

    private BeregningsresultatAndel lagSNAndel(BeregningsresultatPeriode brp, int dagsats) {
        return BeregningsresultatAndel.builder()
                .medBrukerErMottaker(true)
                .medStillingsprosent(new BigDecimal(100))
                .medUtbetalingsgrad(new BigDecimal(100))
                .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medDagsats(dagsats)
                .medDagsatsFraBg(dagsats)
                .build(brp);
    }

}
