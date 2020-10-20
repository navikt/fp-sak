package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.util.FPDateUtil;

public class HindreTilbaketrekkNårAlleredeUtbetaltTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2019, Month.JANUARY, 20);
    private static final LocalDate SISTE_UTTAKSDAG = LocalDate.of(2019, Month.APRIL, 4);
    private static final String FUNKSJONELT_TIDSOFFSET = FPDateUtil.SystemConfiguredClockProvider.PROPERTY_KEY_OFFSET_PERIODE;
    private static final Arbeidsgiver ARBEIDSGIVER1 = Arbeidsgiver.virksomhet("900050001");
    private static final Arbeidsgiver ARBEIDSGIVER2 = Arbeidsgiver.virksomhet("900050002");
    private static final Arbeidsgiver ARBEIDSGIVER3 = Arbeidsgiver.virksomhet("900050003");
    private static final InternArbeidsforholdRef MED_INTERNREFERANSE = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef UTEN_INTERNREFERANSE = InternArbeidsforholdRef.nullRef();

    private HindreTilbaketrekkNårAlleredeUtbetalt tjeneste;

    @BeforeEach
    public void setup() {
        tjeneste = new HindreTilbaketrekkNårAlleredeUtbetalt();
    }

    @AfterEach
    public void teardown() {
        settSimulertNåtidTil(LocalDate.now());
        FPDateUtil.init();
    }

    @Test
    public void omfordeling_med_2_tilkomne_arbeidsforhold_skal_ikke_ta_med_tilkommet_i_flyttbar_dagsats() {
        // Arrange
        final LocalDate PERIODE_FOM = LocalDate.of(2020, 3, 24);
        final Arbeidsgiver GAMMELT_ARBEID = Arbeidsgiver.virksomhet("923186956");
        final Arbeidsgiver TILKOMMET1 = Arbeidsgiver.virksomhet("916272421");
        final Arbeidsgiver TILKOMMET2 = Arbeidsgiver.virksomhet("919852925");

        BeregningsresultatPeriode forrigeBrp = lagBeregningsresultatPeriode(PERIODE_FOM, LocalDate.of(2020, 3, 31));
        lagSNAndel(forrigeBrp, 1225);
        int gammeltArbeidDagsats = 542;
        lagAndel(forrigeBrp, GAMMELT_ARBEID, true, gammeltArbeidDagsats, UTEN_INTERNREFERANSE);

        BeregningsresultatEntitet forrigeTY = forrigeBrp.getBeregningsresultat();

        BeregningsresultatPeriode beregningsgrunnlagBrp = lagBeregningsresultatPeriode(
            LocalDate.of(2020, 3, 24),
            LocalDate.of(2020, 3, 31));
        lagSNAndel(beregningsgrunnlagBrp, 152);
        lagAndel(beregningsgrunnlagBrp, GAMMELT_ARBEID, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, TILKOMMET1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, TILKOMMET2, false, 1400, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, TILKOMMET2, true, 0, UTEN_INTERNREFERANSE);
        BeregningsresultatEntitet beregningsgrunnlagTY = beregningsgrunnlagBrp.getBeregningsresultat();


        Yrkesaktivitet gammelt_arbeid = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 8,1), LocalDate.of(2020, 2, 1))))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef())
            .medArbeidsgiver(GAMMELT_ARBEID).build();

        Yrkesaktivitet tilkommet_arbeid1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 1,1), LocalDate.of(2020, 3, 23))))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(TILKOMMET1).build();

        Yrkesaktivitet tilkommet_arbeid2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.of(2020, 3, 24))))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(TILKOMMET2).build();

        // Act
        BeregningsresultatEntitet utbetTY = tjeneste.reberegn(beregningsgrunnlagTY, MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTY.getBeregningsresultatPerioder(),
            beregningsgrunnlagTY.getBeregningsresultatPerioder()), List.of(tilkommet_arbeid1, gammelt_arbeid, tilkommet_arbeid2), SKJÆRINGSTIDSPUNKT);

        // Assert
        List<BeregningsresultatPeriode> beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(1);
        var p0 = beregningsresultatPerioder.get(0);
        assertThat(p0.getBeregningsresultatPeriodeFom()).isEqualTo(PERIODE_FOM);
        List<BeregningsresultatAndel> p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler).hasSize(4);
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.getAktivitetStatus()).isEqualByComparingTo(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(1010);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.getArbeidsgiver().orElse(null)).isSameAs(GAMMELT_ARBEID);
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(gammeltArbeidDagsats);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.getArbeidsgiver().orElse(null)).isSameAs(TILKOMMET2);
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.getArbeidsgiver().orElse(null)).isSameAs(TILKOMMET1);
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
    }



    @Test
    public void omfordel_til_bruker_når_det_krever_refusjon_tilbake_i_tid_for_tilkommet_arbeidsforhold() {
        // Arrange
        settSimulertNåtidTil(LocalDate.of(2019, Month.FEBRUARY, 4));
        BeregningsresultatPeriode forrigeBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, true, 1500, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, false, 0, UTEN_INTERNREFERANSE);
        BeregningsresultatEntitet forrigeTY = forrigeBrp.getBeregningsresultat();

        BeregningsresultatPeriode beregningsgrunnlagBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, false, 1500, UTEN_INTERNREFERANSE);
        BeregningsresultatEntitet beregningsgrunnlagTY = beregningsgrunnlagBrp.getBeregningsresultat();

        Yrkesaktivitet yrkesaktivitet1 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(20), SKJÆRINGSTIDSPUNKT.plusDays(14))))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(ARBEIDSGIVER1).build();

        Yrkesaktivitet yrkesaktivitet2 = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.plusDays(15))))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(ARBEIDSGIVER2).build();

        // Act
        BeregningsresultatEntitet utbetTY = tjeneste.reberegn(beregningsgrunnlagTY, MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTY.getBeregningsresultatPerioder(),
            beregningsgrunnlagTY.getBeregningsresultatPerioder()), List.of(yrkesaktivitet1, yrkesaktivitet2), SKJÆRINGSTIDSPUNKT);

        // Assert
        List<BeregningsresultatPeriode> beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(2);
        var p0 = beregningsresultatPerioder.get(0);
        assertThat(p0.getBeregningsresultatPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(p0.getBeregningsresultatPeriodeTom()).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
        List<BeregningsresultatAndel> p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler).hasSize(2);
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.getArbeidsgiver().orElse(null)).isSameAs(ARBEIDSGIVER2);
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.getArbeidsgiver().orElse(null)).isSameAs(ARBEIDSGIVER1);
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
    public void omfordel_til_bruker_når_det_krever_refusjon_tilbake_i_tid() {
        // Arrange
        settSimulertNåtidTil(LocalDate.of(2019, Month.FEBRUARY, 4));
        int forventetDagsats = 1000;
        BeregningsresultatEntitet forrigeTY = lagBeregningsresultatFP(true, forventetDagsats);
        BeregningsresultatEntitet beregningsgrunnlagTY = lagBeregningsresultatFP(false, forventetDagsats);

        // Act
        BeregningsresultatEntitet utbetTY = tjeneste.reberegn(beregningsgrunnlagTY, MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTY.getBeregningsresultatPerioder(),
            beregningsgrunnlagTY.getBeregningsresultatPerioder()), List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        List<BeregningsresultatPeriode> beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(2);
        var p0 = beregningsresultatPerioder.get(0);
        assertThat(p0.getBeregningsresultatPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(p0.getBeregningsresultatPeriodeTom()).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
        List<BeregningsresultatAndel> p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler).hasSize(1);
        assertThat(p0andeler.get(0)).satisfies(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(forventetDagsats);
        });

        var p1 = beregningsresultatPerioder.get(1);
        assertThat(p1.getBeregningsresultatPeriodeFom()).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        assertThat(p1.getBeregningsresultatPeriodeTom()).isEqualTo(SISTE_UTTAKSDAG);
        List<BeregningsresultatAndel> p1andeler = p1.getBeregningsresultatAndelList();
        assertThat(p1andeler).hasSize(2);
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(forventetDagsats);
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
    public void skal_avkorte_men_ikke_omfordele_fra_bruker() {
        // Arrange
        settSimulertNåtidTil(LocalDate.of(2019, Month.FEBRUARY, 4));
        BeregningsresultatPeriode forrigeBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, false, 1500, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER2, true, 600, UTEN_INTERNREFERANSE);
        BeregningsresultatEntitet forrigeTY = forrigeBrp.getBeregningsresultat();

        BeregningsresultatPeriode beregningsgrunnlagBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, false, 1050, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, false, 1050, UTEN_INTERNREFERANSE);
        BeregningsresultatEntitet beregningsgrunnlagTY = beregningsgrunnlagBrp.getBeregningsresultat();

        // Act
        BeregningsresultatEntitet utbetTY = tjeneste.reberegn(beregningsgrunnlagTY, MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTY.getBeregningsresultatPerioder(),
            beregningsgrunnlagTY.getBeregningsresultatPerioder()), List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        List<BeregningsresultatPeriode> beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(2);
        var p0 = beregningsresultatPerioder.get(0);
        assertThat(p0.getBeregningsresultatPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(p0.getBeregningsresultatPeriodeTom()).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
        List<BeregningsresultatAndel> p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler).hasSize(4);
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER1);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER1);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(1050);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER2);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(600);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER2);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(450);
        });

        var p1 = beregningsresultatPerioder.get(1);
        assertThat(p1.getBeregningsresultatPeriodeFom()).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        assertThat(p1.getBeregningsresultatPeriodeTom()).isEqualTo(SISTE_UTTAKSDAG);
        List<BeregningsresultatAndel> p1andeler = p1.getBeregningsresultatAndelList();
        assertThat(p1andeler).hasSize(4);
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER1);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER1);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(1050);
        });
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER2);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER2);
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
    public void skal_fordele_refusjon_til_to_nye_arbeidsgivere() {
        // Arrange
        settSimulertNåtidTil(LocalDate.of(2019, Month.FEBRUARY, 4));
        BeregningsresultatPeriode forrigeBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, false, 1500, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER2, true, 240, UTEN_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER3, true, 360, UTEN_INTERNREFERANSE);
        BeregningsresultatEntitet forrigeTY = forrigeBrp.getBeregningsresultat();

        BeregningsresultatPeriode beregningsgrunnlagBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, false, 600, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER2, false, 600, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER3, true, 0, UTEN_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER3, false, 900, UTEN_INTERNREFERANSE);
        BeregningsresultatEntitet beregningsgrunnlagTY = beregningsgrunnlagBrp.getBeregningsresultat();

        // Act
        BeregningsresultatEntitet utbetTY = tjeneste.reberegn(beregningsgrunnlagTY, MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTY.getBeregningsresultatPerioder(),
            beregningsgrunnlagTY.getBeregningsresultatPerioder()), List.of(), SKJÆRINGSTIDSPUNKT);

        // Assert
        List<BeregningsresultatPeriode> beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(2);
        var p0 = beregningsresultatPerioder.get(0);
        assertThat(p0.getBeregningsresultatPeriodeFom()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(p0.getBeregningsresultatPeriodeTom()).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
        List<BeregningsresultatAndel> p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler).hasSize(6);
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER1);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER1);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(600);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER2);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(240);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER2);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(360);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER3);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(360);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER3);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(540);
        });

        var p1 = beregningsresultatPerioder.get(1);
        assertThat(p1.getBeregningsresultatPeriodeFom()).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 1));
        assertThat(p1.getBeregningsresultatPeriodeTom()).isEqualTo(SISTE_UTTAKSDAG);
        List<BeregningsresultatAndel> p1andeler = p1.getBeregningsresultatAndelList();
        assertThat(p1andeler).hasSize(6);
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER1);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER1);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(600);
        });
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER2);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER2);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(600);
        });
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER3);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
        assertThat(p1andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsgiver().get()).isSameAs(ARBEIDSGIVER3);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(900);
        });
    }

    @Test
    public void skalIkkeOmfordeleTilTilkommetArbeidsforholdISammeOrganisasjonNårEndring() {
        // Arrange
        settSimulertNåtidTil(SISTE_UTTAKSDAG.plusMonths(2));
        BeregningsresultatPeriode forrigeBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);

        lagAndel(forrigeBrp, ARBEIDSGIVER1, false, 834, MED_INTERNREFERANSE);
        lagAndel(forrigeBrp, ARBEIDSGIVER1, true, 1, MED_INTERNREFERANSE);
        BeregningsresultatEntitet forrigeTY = forrigeBrp.getBeregningsresultat();

        BeregningsresultatPeriode beregningsgrunnlagBrp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, false, 834, MED_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, true, 0, MED_INTERNREFERANSE);
        lagAndel(beregningsgrunnlagBrp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);

        BeregningsresultatEntitet beregningsgrunnlagTY = beregningsgrunnlagBrp.getBeregningsresultat();

        Yrkesaktivitet eksisterendeAktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.minusYears(2))))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsforholdId(MED_INTERNREFERANSE)
            .medArbeidsgiver(ARBEIDSGIVER1).build();
        Yrkesaktivitet tilkommetAktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT)))
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsforholdId(UTEN_INTERNREFERANSE)
            .medArbeidsgiver(ARBEIDSGIVER1).build();

        // Act
        BeregningsresultatEntitet utbetTY = tjeneste.reberegn(beregningsgrunnlagTY, MapBRAndelSammenligningTidslinje.opprettTidslinje(
            forrigeTY.getBeregningsresultatPerioder(),
            beregningsgrunnlagTY.getBeregningsresultatPerioder()), List.of(eksisterendeAktivitet, tilkommetAktivitet), SKJÆRINGSTIDSPUNKT);

        // Assert
        List<BeregningsresultatPeriode> beregningsresultatPerioder = utbetTY.getBeregningsresultatPerioder();
        assertThat(beregningsresultatPerioder).hasSize(1);
        var p0 = beregningsresultatPerioder.get(0);

        List<BeregningsresultatAndel> p0andeler = p0.getBeregningsresultatAndelList();
        assertThat(p0andeler).hasSize(3);
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isFalse();
            assertThat(andel.getArbeidsforholdRef()).isSameAs(MED_INTERNREFERANSE);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(833);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsforholdRef()).isSameAs(MED_INTERNREFERANSE);
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(1);
        });
        assertThat(p0andeler).anySatisfy(andel -> {
            assertThat(andel.erBrukerMottaker()).as("erBrukerMottaker").isTrue();
            assertThat(andel.getArbeidsforholdRef()).isEqualTo(InternArbeidsforholdRef.nullRef());
            assertThat(andel.getDagsats()).as("dagsats").isEqualTo(0);
        });
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP(boolean brukerErMottaker, int dagsats) {
        BeregningsresultatPeriode brp = lagBeregningsresultatPeriode(SKJÆRINGSTIDSPUNKT, SISTE_UTTAKSDAG);
        if (!brukerErMottaker) {
            lagAndel(brp, ARBEIDSGIVER1, true, 0, UTEN_INTERNREFERANSE);
        }
        lagAndel(brp, ARBEIDSGIVER1, brukerErMottaker, dagsats, UTEN_INTERNREFERANSE);
        return brp.getBeregningsresultat();
    }

    private BeregningsresultatPeriode lagBeregningsresultatPeriode(LocalDate fom, LocalDate tom) {
        BeregningsresultatEntitet br = BeregningsresultatEntitet.builder()
            .medRegelSporing("regelsporing")
            .medRegelInput("regelinput")
            .build();
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(br);
    }

    private BeregningsresultatAndel lagAndel(BeregningsresultatPeriode brp, Arbeidsgiver arbeidsgiver, boolean brukerErMottaker, int dagsats, InternArbeidsforholdRef internArbeidsforholdRef) {
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

    private void settSimulertNåtidTil(LocalDate dato) {
        Period periode = Period.between(LocalDate.now(), dato);
        System.setProperty(FUNKSJONELT_TIDSOFFSET, periode.toString());
        FPDateUtil.init();
    }
}
