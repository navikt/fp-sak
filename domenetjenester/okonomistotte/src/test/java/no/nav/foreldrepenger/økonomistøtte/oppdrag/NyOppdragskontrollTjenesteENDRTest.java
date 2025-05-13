package no.nav.foreldrepenger.økonomistøtte.oppdrag;

import static no.nav.foreldrepenger.økonomistøtte.oppdrag.OppdragskontrollTestVerktøy.endreTilElleveSiffer;
import static no.nav.foreldrepenger.økonomistøtte.oppdrag.OppdragskontrollTestVerktøy.verifiserOppdr150SomErNy;
import static no.nav.foreldrepenger.økonomistøtte.oppdrag.OppdragskontrollTestVerktøy.verifiserOppdr150SomErOpphørt;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.OppdragMedPositivKvitteringTestUtil;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.TilkjentYtelseMapper;

public class NyOppdragskontrollTjenesteENDRTest extends NyOppdragskontrollTjenesteTestBase {

    public static final String ANSVARLIG_SAKSBEHANDLER = "Antonina";

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void skalSendeEndringsoppdragOppdragMedFeriepengerNårEndringsdatoErFørsteUttaksdag() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatFP(true);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        var originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(true);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        verifiserOppdragslinje150_ENDR(oppdragRevurdering, originaltOppdragslinje150, true, false, 80);
    }

    @Test
    void skalSendeOppdragMedOmpostering116HvisAvslåttInntrekk() {
        // Arrange
        var b1fom = LocalDate.of(I_ÅR, 1, 1);
        var b1tom = LocalDate.of(I_ÅR, 8, 20);
        var beregningsresultat = buildBeregningsresultatBrukerFP(400, 400, b1fom, b1tom);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(300, 300, b1fom, b1tom);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)))
            .medBrukInntrekk(false);

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        assertThat(oppdragRevurdering).isNotNull();
        var oppdrag110 = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdragRevurdering.getOppdrag110Liste());
        assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        assertThat(oppdrag110.getOmpostering116()).isPresent();
        var ompostering116 = oppdrag110.getOmpostering116().get();
        assertThat(ompostering116.getOmPostering()).isFalse();
        assertThat(ompostering116.getDatoOmposterFom()).isNull();
    }

    @Test
//    @Disabled(value = "Midlertidig slått av Ompostering siden økonomi klarer å utlede selv den beste datoen for ompostering.")
    void skalSendeOppdragMedOmpostering116HvisIkkeAvslåttInntrekkOgDetFinnesForrigeOppdrag() {
        // Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet);
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b1Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet);
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1200, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_2, false, 1000, BigDecimal.valueOf(100), virksomhet);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        //Bruker
        var oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdragRevurdering.getOppdrag110Liste());
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        assertThat(oppdrag110Bruker.getOmpostering116()).isPresent();
        var ompostering116 = oppdrag110Bruker.getOmpostering116().get();
        assertThat(ompostering116.getOmPostering()).isTrue();
        assertThat(ompostering116.getDatoOmposterFom()).isEqualTo(b1Periode_1.getBeregningsresultatPeriodeFom());
        //Arbeidsgiver
        var oppdrag110Arbeidsgiver = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdragRevurdering.getOppdrag110Liste(), virksomhet);
        assertThat(oppdrag110Arbeidsgiver.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        assertThat(oppdrag110Arbeidsgiver.getOmpostering116()).isNotPresent();
    }

    @Test
    @DisplayName("Tester tilfeller hvor ompostering116 ble ikke sendt fordi bruker har fått opphør på feriepenger men ikke selve ytelsen.")
//    @Disabled(value = "Midlertidig slått av Ompostering siden økonomi klarer å utlede selv den beste datoen for ompostering.")
    void test_manglende_inntrekk_tfp_5130() {
        // Arrange
        // Arrange : Førstegangsbehandling
        var beregningsresultatFP_1 = buildEmptyBeregningsresultatFP();

        var feriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(LocalDate.of(2022, 5, 1))
            .medFeriepengerPeriodeTom(LocalDate.of(2022, 5, 31))
            .medFeriepengerRegelInput("input")
            .medFeriepengerRegelSporing("sporing")
            .build();

        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, LocalDate.of(2021, 10, 11), LocalDate.of(2021, 10, 29));
        //Andeler for bruker i periode#1
        var b1Andel = buildBeregningsresultatAndel(b1Periode_1, true, 1876, BigDecimal.valueOf(100), virksomhet);
        //Andeler for bruker i periode#2
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, LocalDate.of(2021, 11, 1), LocalDate.of(2021, 12, 12));
        buildBeregningsresultatAndel(b1Periode_2, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b1Periode_3 = buildBeregningsresultatPeriode(beregningsresultatFP_1, LocalDate.of(2021, 12, 13), LocalDate.of(2022, 2, 11));
        buildBeregningsresultatAndel(b1Periode_3, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b1Periode_4 = buildBeregningsresultatPeriode(beregningsresultatFP_1, LocalDate.of(2022, 2, 14), LocalDate.of(2022, 4, 8));
        buildBeregningsresultatAndel(b1Periode_4, true, 1876, BigDecimal.valueOf(100), virksomhet);

        buildBeregningsresultatFeriepengerPrÅr(feriepenger, b1Andel, 11481L, LocalDate.of(2021, 5, 1));

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1, feriepenger);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Revurdering 1

        var beregningsresultatFP_2 = buildEmptyBeregningsresultatFP();

        var feriepenger2 = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(LocalDate.of(2022, 5, 1))
            .medFeriepengerPeriodeTom(LocalDate.of(2022, 5, 31))
            .medFeriepengerRegelInput("input")
            .medFeriepengerRegelSporing("sporing")
            .build();

        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_2, LocalDate.of(2021, 10, 11), LocalDate.of(2021, 10, 29));
        //Andeler for bruker i periode#1
        var b2Andel = buildBeregningsresultatAndel(b2Periode_1, true, 1876, BigDecimal.valueOf(100), virksomhet);
        //Andeler for bruker i periode#2
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_2, LocalDate.of(2021, 11, 1), LocalDate.of(2021, 11, 29));
        buildBeregningsresultatAndel(b2Periode_2, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b2Periode_3 = buildBeregningsresultatPeriode(beregningsresultatFP_2, LocalDate.of(2022, 1, 13), LocalDate.of(2022, 3, 29));
        buildBeregningsresultatAndel(b2Periode_3, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b2Periode_4 = buildBeregningsresultatPeriode(beregningsresultatFP_2, LocalDate.of(2022, 3, 30), LocalDate.of(2022, 5, 24));
        buildBeregningsresultatAndel(b2Periode_4, true, 1876, BigDecimal.valueOf(100), virksomhet);

        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, b2Andel, 6888L, LocalDate.of(2021, 5, 1));
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, b2Andel, 4592L, LocalDate.of(2022, 5, 1));


        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatFP_2, feriepenger2);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Revurdering 2
        var beregningsresultatFP_3 = buildEmptyBeregningsresultatFP();

        var feriepenger3 = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(LocalDate.of(2022, 5, 1))
            .medFeriepengerPeriodeTom(LocalDate.of(2022, 5, 31))
            .medFeriepengerRegelInput("input")
            .medFeriepengerRegelSporing("sporing")
            .build();

        var b3Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_3, LocalDate.of(2021, 10, 11), LocalDate.of(2021, 10, 29));
        //Andeler for bruker i periode#1
        var b3Andel = buildBeregningsresultatAndel(b3Periode_1, true, 1876, BigDecimal.valueOf(100), virksomhet);
        //Andeler for bruker i periode#2
        var b3Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_3, LocalDate.of(2021, 11, 1), LocalDate.of(2021, 11, 29));
        buildBeregningsresultatAndel(b3Periode_2, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b3Periode_3 = buildBeregningsresultatPeriode(beregningsresultatFP_3, LocalDate.of(2022, 2, 9), LocalDate.of(2022, 2, 11));
        buildBeregningsresultatAndel(b3Periode_3, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b3Periode_4 = buildBeregningsresultatPeriode(beregningsresultatFP_3, LocalDate.of(2022, 3, 1), LocalDate.of(2022, 5, 10));
        buildBeregningsresultatAndel(b3Periode_4, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b3Periode_5 = buildBeregningsresultatPeriode(beregningsresultatFP_3, LocalDate.of(2022, 5, 11), LocalDate.of(2022, 7, 5));
        buildBeregningsresultatAndel(b3Periode_5, true, 1876, BigDecimal.valueOf(100), virksomhet);

        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, b3Andel, 6888L, LocalDate.of(2021, 5, 1));
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, b3Andel, 4592L, LocalDate.of(2022, 5, 1));


        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatFP_3, feriepenger3);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering)));

        // Act
        var oppdragRevurdering2 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder3.build());

        // Revurdering 3
        var beregningsresultatFP_4 = buildEmptyBeregningsresultatFP();

        var feriepenger4 = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(LocalDate.of(2022, 5, 1))
            .medFeriepengerPeriodeTom(LocalDate.of(2022, 5, 31))
            .medFeriepengerRegelInput("input")
            .medFeriepengerRegelSporing("sporing")
            .build();

        var b4Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_4, LocalDate.of(2021, 10, 11), LocalDate.of(2021, 10, 29));
        //Andeler for bruker i periode#1
        var b4Andel = buildBeregningsresultatAndel(b4Periode_1, true, 1876, BigDecimal.valueOf(100), virksomhet);
        //Andeler for bruker i periode#2
        var b4Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_4, LocalDate.of(2021, 11, 1), LocalDate.of(2021, 11, 29));
        buildBeregningsresultatAndel(b4Periode_2, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b4Periode_3 = buildBeregningsresultatPeriode(beregningsresultatFP_4, LocalDate.of(2022, 2, 9), LocalDate.of(2022, 2, 11));
        buildBeregningsresultatAndel(b4Periode_3, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b4Periode_4 = buildBeregningsresultatPeriode(beregningsresultatFP_4, LocalDate.of(2022, 3, 1), LocalDate.of(2022, 5, 10));
        buildBeregningsresultatAndel(b4Periode_4, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b4Periode_5 = buildBeregningsresultatPeriode(beregningsresultatFP_4, LocalDate.of(2022, 5, 11), LocalDate.of(2022, 7, 5));
        buildBeregningsresultatAndel(b4Periode_5, true, 1876, BigDecimal.valueOf(100), virksomhet);

        buildBeregningsresultatFeriepengerPrÅr(feriepenger4, b4Andel, 6888L, LocalDate.of(2021, 5, 1));


        var gruppertYtelse4 = mapper.fordelPåNøkler(beregningsresultatFP_4, feriepenger4);
        var builder4 = getInputStandardBuilder(gruppertYtelse4).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering, oppdragRevurdering2)));

        // Act
        var oppdragRevurdering3 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder4.build());

        // Revurdering 4
        var beregningsresultatFP_5 = buildEmptyBeregningsresultatFP();

        var feriepenger5 = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(LocalDate.of(2022, 5, 1))
            .medFeriepengerPeriodeTom(LocalDate.of(2022, 5, 31))
            .medFeriepengerRegelInput("input")
            .medFeriepengerRegelSporing("sporing")
            .build();

        var b5Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_5, LocalDate.of(2021, 10, 11), LocalDate.of(2021, 10, 29));
        //Andeler for bruker i periode#1
        var b5Andel = buildBeregningsresultatAndel(b5Periode_1, true, 1876, BigDecimal.valueOf(100), virksomhet);
        //Andeler for bruker i periode#2
        var b5Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_5, LocalDate.of(2021, 11, 1), LocalDate.of(2021, 11, 29));
        buildBeregningsresultatAndel(b5Periode_2, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b5Periode_3 = buildBeregningsresultatPeriode(beregningsresultatFP_5, LocalDate.of(2022, 2, 9), LocalDate.of(2022, 2, 11));
        buildBeregningsresultatAndel(b5Periode_3, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b5Periode_4 = buildBeregningsresultatPeriode(beregningsresultatFP_5, LocalDate.of(2022, 2, 28), LocalDate.of(2022, 4, 28));
        buildBeregningsresultatAndel(b5Periode_4, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b5Periode_5 = buildBeregningsresultatPeriode(beregningsresultatFP_5, LocalDate.of(2022, 5, 9), LocalDate.of(2022, 8, 26));
        buildBeregningsresultatAndel(b5Periode_5, true, 1876, BigDecimal.valueOf(100), virksomhet);

        var b5Periode_6 = buildBeregningsresultatPeriode(beregningsresultatFP_5, LocalDate.of(2022, 8, 29), LocalDate.of(2022, 9, 6));
        buildBeregningsresultatAndel(b5Periode_6, true, 1876, BigDecimal.valueOf(100), virksomhet);

        buildBeregningsresultatFeriepengerPrÅr(feriepenger5, b5Andel, 6888L, LocalDate.of(2021, 5, 1));

        var gruppertYtelse5 = mapper.fordelPåNøkler(beregningsresultatFP_5, feriepenger5);
        var builder5 = getInputStandardBuilder(gruppertYtelse5).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering, oppdragRevurdering2, oppdragRevurdering3)));

        // Act
        var oppdragRevurdering4 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder5.build());

        // Assert
        //Bruker
        var oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdragRevurdering4.getOppdrag110Liste());
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        assertThat(oppdrag110Bruker.getOmpostering116()).isPresent();
        var ompostering116 = oppdrag110Bruker.getOmpostering116().get();
        assertThat(ompostering116.getOmPostering()).isTrue();
        assertThat(ompostering116.getDatoOmposterFom()).isEqualTo(b1Periode_1.getBeregningsresultatPeriodeFom());
    }

    @Test
//    @Disabled(value = "Midlertidig slått av Ompostering siden økonomi klarer å utlede selv den beste datoen for ompostering.")
    void skalSendeOppdragMedOmpostering116OgSetteDatoOmposterFomTilFørsteUttaksdatoFraForrigeBehandlingForBrukerNårEndringsdatoErTidligere() {
        // Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode, true, 1500, BigDecimal.valueOf(100), virksomhet);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 20);
        buildBeregningsresultatAndel(b2Periode, true, 1500, BigDecimal.valueOf(100), virksomhet);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdragRevurdering.getOppdrag110Liste());
        // assertThat(oppdrag110Bruker.getOmpostering116()).isPresent();
        // var ompostering116 = oppdrag110Bruker.getOmpostering116().get();
        // assertThat(ompostering116.getOmPostering()).isTrue();
        // assertThat(ompostering116.getDatoOmposterFom()).isEqualTo(b1Periode.getBeregningsresultatPeriodeFom());
    }

    /**
     * Førstegangsbehandling med en periode <br>
     * Periode 1: Dagsats bruker 400 kr<br>
     * Revurdering med to perioder<br>
     * Periode 1: Dagsats bruker 400 kr<br>
     * Periode 2: Dagsats bruker 300 kr
     */
    @Test
    void skalSendeOppdragFomEndringsdatoNårDetErEndringFraAndrePeriodeIRevurdering() {
        // Arrange
        var b1fom = LocalDate.of(I_ÅR, 8, 1);
        var b1tom = LocalDate.of(I_ÅR, 8, 20);
        var beregningsresultat = buildBeregningsresultatBrukerFP(400, 400, b1fom, b1tom);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var b2p1fom = LocalDate.of(I_ÅR, 8, 1);
        var b2p1tom = LocalDate.of(I_ÅR, 8, 10);
        var b2p2fom = LocalDate.of(I_ÅR, 8, 11);
        var b2p2tom = LocalDate.of(I_ÅR, 8, 20);
        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(400, 300), List.of(400, 300), b2p1fom, b2p1tom, b2p2fom,
            b2p2tom);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        assertThat(opp150RevurderingListe).hasSize(6).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(
                beregningsresultat.getGjeldendePerioder().get(0).getBeregningsresultatPeriodeFom().plusDays(10));
        }).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(300));
        });
        var opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).anySatisfy(
            feriepenger -> assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER));
        var opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).anySatisfy(
            feriepenger -> assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_FERIEPENGER_AG));
    }

    private List<Oppdragslinje150> getOppdragslinje150ForMottaker(Oppdragskontroll oppdragRevurdering, boolean erBruker) {
        return oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .filter(oppdrag110 -> !oppdrag110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver() == erBruker)
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();
    }

    @Test
    void skal_sende_oppdrag_hvor_den_første_perioden_i_original_behandling_ikke_har_en_korresponderende_periode() {
        // Arrange
        var b1fom = LocalDate.of(I_ÅR, 8, 1);
        var b1tom = LocalDate.of(I_ÅR, 8, 10);
        var b1p2fom = LocalDate.of(I_ÅR, 8, 11);
        var b1p2tom = LocalDate.of(I_ÅR, 8, 20);
        var beregningsresultat = buildBeregningsresultatBrukerFP(List.of(400, 400), List.of(400, 400), b1fom, b1tom, b1p2fom, b1p2tom);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var b2p1fom = LocalDate.of(I_ÅR, 8, 11);
        var b2p1tom = LocalDate.of(I_ÅR, 8, 20);

        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(400), List.of(400), b2p1fom, b2p1tom);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        var opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(3)
            .anySatisfy(feriepenger -> assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER));
        var opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListe).hasSize(6).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(
                beregningsresultat.getGjeldendePerioder().get(0).getBeregningsresultatPeriodeFom());
        }).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(400));
        });
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(3)
            .anySatisfy(feriepenger -> assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_FERIEPENGER_AG));
    }

    @Test
    void skal_sende_oppdrag_hvor_den_siste_perioden_i_revurderingen_ikke_har_en_korresponderende_periode() {
        // Arrange
        var b1fom = LocalDate.of(I_ÅR, 8, 1);
        var b1tom = LocalDate.of(I_ÅR, 8, 10);
        var beregningsresultat = buildBeregningsresultatBrukerFP(List.of(400), List.of(400), b1fom, b1tom);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var b2p1fom = LocalDate.of(I_ÅR, 8, 1);
        var b2p1tom = LocalDate.of(I_ÅR, 8, 10);
        var b2p2fom = LocalDate.of(I_ÅR, 8, 11);
        var b2p2tom = LocalDate.of(I_ÅR, 8, 20);

        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(400, 300), List.of(400, 300), b2p1fom, b2p1tom, b2p2fom,
            b2p2tom);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        assertThat(opp150RevurderingListe).hasSize(4);
        var opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(2);
        var opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(2);
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getDatoVedtakFom()).isEqualTo(LocalDate.of(I_ÅR, 8, 11));
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(300));
        });
        assertThat(opp150RevurderingListeForBruker).anySatisfy(
            feriepenger -> assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER));
        assertThat(opp150RevurderingListeForArbeidsgiver).anySatisfy(
            feriepenger -> assertThat(feriepenger.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_FERIEPENGER_AG));
        //Kjeding for bruker
        var forrigeOpp150ListeForBruker = getOppdragslinje150ForMottaker(originaltOppdrag, true);
        var forrigeOpp150ListeUtenFeriepgForBruker = getOpp150MedKodeklassifik(forrigeOpp150ListeForBruker, KodeKlassifik.FPF_ARBEIDSTAKER);
        var forrigeOpp150ForBruker = forrigeOpp150ListeUtenFeriepgForBruker.get(0);
        var opp150RevurderingListeUtenFeriepgForBruker = getOpp150MedKodeklassifik(opp150RevurderingListeForBruker, KodeKlassifik.FPF_ARBEIDSTAKER);
        var opp150RevurderingForBruker = opp150RevurderingListeUtenFeriepgForBruker.get(0);
        assertThat(forrigeOpp150ForBruker.getDelytelseId()).isEqualTo(opp150RevurderingForBruker.getRefDelytelseId());
        //Kjeding for arbeidsgiver
        var forrigeOpp150ListeForArbeidsgiver = getOppdragslinje150ForMottaker(originaltOppdrag, false);
        var forrigeOpp150ListeUtenFeriepgForArbeidsgiver = getOpp150MedKodeklassifik(forrigeOpp150ListeForArbeidsgiver,
            KodeKlassifik.FPF_REFUSJON_AG);
        var forrigeOpp150ForArbeidsgiver = forrigeOpp150ListeUtenFeriepgForArbeidsgiver.get(0);
        var opp150RevurderingListeUtenFeriepgForArbeidsgiver = getOpp150MedKodeklassifik(opp150RevurderingListeForArbeidsgiver,
            KodeKlassifik.FPF_REFUSJON_AG);
        var opp150RevurderingForArbeidsgiver = opp150RevurderingListeUtenFeriepgForArbeidsgiver.get(0);
        assertThat(forrigeOpp150ForArbeidsgiver.getDelytelseId()).isEqualTo(opp150RevurderingForArbeidsgiver.getRefDelytelseId());
    }

    /*
     * Førstegangsbehandling med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 15.05
     * Periode 2: Dagsats bruker 300 kr; fom-tom: 16.05 - 30.05
     * Revurdering med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 15.05
     * Periode 2: Dagsats bruker 200 kr; fom-tom: 16.05 - 30.05
     */
    @Test
    void skal_sende_oppdrag_når_forrige_og_ny_behanling_har_to_perioder_og_det_blir_endring_i_andel_i_andre_periode_i_revurdering() {

        // Arrange
        var b1p1fom = LocalDate.of(I_ÅR, 5, 1);
        var b1p1tom = LocalDate.of(I_ÅR, 5, 15);
        var b1p2fom = LocalDate.of(I_ÅR, 5, 16);
        var b1p2tom = LocalDate.of(I_ÅR, 5, 30);
        var beregningsresultat = buildBeregningsresultatBrukerFP(List.of(400, 300), List.of(400, 300), b1p1fom, b1p1tom, b1p2fom, b1p2tom);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var b2p1fom = LocalDate.of(I_ÅR, 5, 1);
        var b2p1tom = LocalDate.of(I_ÅR, 5, 15);
        var b2p2fom = LocalDate.of(I_ÅR, 5, 16);
        var b2p2tom = LocalDate.of(I_ÅR, 5, 30);
        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(400, 200), List.of(400, 200), b2p1fom, b2p1tom, b2p2fom,
            b2p2tom);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        assertThat(opp150RevurderingListe).hasSize(4).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(LocalDate.of(I_ÅR, 5, 16));
        }).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(200));
        });
        var opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(2)
            .allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER));
        var opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(2)
            .allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG));
    }


    /*
     * Førstegangsbehandling med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 15.05
     * Periode 2: Dagsats bruker 300 kr; fom-tom: 16.05 - 30.05
     * Revurdering med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 10.05
     * Periode 2: Dagsats bruker 200 kr; fom-tom: 11.05 - 30.05
     */
    @Test
    void skal_sende_oppdrag_når_forrige_og_ny_behandling_har_to_perioder_og_det_blir_endring_midt_i_første_periode_i_forrige() {

        // Arrange
        var b1p1fom = LocalDate.of(I_ÅR, 5, 1);
        var b1p1tom = LocalDate.of(I_ÅR, 5, 15);
        var b1p2fom = LocalDate.of(I_ÅR, 5, 16);
        var b1p2tom = LocalDate.of(I_ÅR, 5, 30);
        var beregningsresultat = buildBeregningsresultatBrukerFP(List.of(400, 300), List.of(400, 300), b1p1fom, b1p1tom, b1p2fom, b1p2tom);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var b2p1fom = LocalDate.of(I_ÅR, 5, 1);
        var b2p1tom = LocalDate.of(I_ÅR, 5, 10);
        var b2p2fom = LocalDate.of(I_ÅR, 5, 11);
        var b2p2tom = LocalDate.of(I_ÅR, 5, 30);
        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(400, 200), List.of(400, 200), b2p1fom, b2p1tom, b2p2fom,
            b2p2tom);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        assertThat(opp150RevurderingListe).hasSize(4);
        var opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(2)
            .allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER));
        var opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(2).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(LocalDate.of(I_ÅR, 5, 11));
        }).allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG));
        assertThat(opp150RevurderingListe).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(200));
        });
    }

    /*
     * Førstegangsbehandling med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 15.05
     * Periode 2: Dagsats bruker 300 kr; fom-tom: 16.05 - 30.05
     * Revurdering med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 20.05
     * Periode 2: Dagsats bruker 200 kr; fom-tom: 21.05 - 30.05
     */
    @Test
    void skal_sende_oppdrag_når_forrige_og_ny_behanling_har_to_perioder_og_andre_periode_i_original_behandlingen_blir_til_2_perioder_i_revurdering() {

        // Arrange
        var b1p1fom = LocalDate.of(I_ÅR, 5, 1);
        var b1p1tom = LocalDate.of(I_ÅR, 5, 15);
        var b1p2fom = LocalDate.of(I_ÅR, 5, 16);
        var b1p2tom = LocalDate.of(I_ÅR, 5, 30);
        var beregningsresultat = buildBeregningsresultatBrukerFP(List.of(400, 300), List.of(400, 300), b1p1fom, b1p1tom, b1p2fom, b1p2tom);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var b2p1fom = LocalDate.of(I_ÅR, 5, 1);
        var b2p1tom = LocalDate.of(I_ÅR, 5, 20);
        var b2p2fom = LocalDate.of(I_ÅR, 5, 21);
        var b2p2tom = LocalDate.of(I_ÅR, 5, 30);

        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(400, 200), List.of(400, 200), b2p1fom, b2p1tom, b2p2fom,
            b2p2tom);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        var opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        var opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(3)
            .allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG));
        assertThat(opp150RevurderingListe).hasSize(6).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(b1p2fom);
        }).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(200));
        });
        assertThat(opp150RevurderingListeForBruker).hasSize(3)
            .allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER));
    }

    /*
     * Førstegangsbehandling med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 01.05 - 15.05
     * Periode 2: Dagsats bruker 300 kr; fom-tom: 16.05 - 30.05
     * Revurdering med to perioder
     * Periode 1: Dagsats bruker 400 kr; fom-tom: 20.04 - 20.05
     * Periode 2: Dagsats bruker 200 kr; fom-tom: 21.05 - 10.06
     */
    @Test
    void skal_opphøre_hele_forrige_oppdrag_og_sende_ny_oppdrag_når_første_uttaksdato_av_revurdering_blir_tidligere_enn_første_uttaksdato_av_forrige() {

        // Arrange
        var b1p1fom = LocalDate.of(I_ÅR, 5, 1);
        var b1p1tom = LocalDate.of(I_ÅR, 5, 15);
        var b1p2fom = LocalDate.of(I_ÅR, 5, 16);
        var b1p2tom = LocalDate.of(I_ÅR, 5, 30);
        var beregningsresultat = buildBeregningsresultatBrukerFP(List.of(400, 300), List.of(400, 300), b1p1fom, b1p1tom, b1p2fom, b1p2tom);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var b2p1fom = LocalDate.of(I_ÅR, 4, 20);
        var b2p1tom = LocalDate.of(I_ÅR, 5, 20);
        var b2p2fom = LocalDate.of(I_ÅR, 5, 21);
        var b2p2tom = LocalDate.of(I_ÅR, 6, 10);
        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(400, 200), List.of(400, 200), b2p1fom, b2p1tom, b2p2fom,
            b2p2tom);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        var opp150RevurderingListeForBruker = getOppdragslinje150ForMottaker(oppdragRevurdering, true);
        assertThat(opp150RevurderingListeForBruker).hasSize(3);
        var opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).hasSize(3)
            .allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG));
        var forventetDatoStatusFom = beregningsresultat.getGjeldendePerioder().get(0).getBeregningsresultatPeriodeFom();
        assertThat(opp150RevurderingListe).hasSize(6).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(forventetDatoStatusFom);
        }).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(400));
        }).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(200));
        });
        assertThat(opp150RevurderingListeForBruker).allSatisfy(
            oppdragslinje150 -> assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER));
    }

    @Test
    void skal_sende_oppdrag_hvor_det_blir_en_ny_mottaker_i_revurdering() {
        // Arrange
        var b1fom = LocalDate.of(I_ÅR, 8, 1);
        var b1tom = LocalDate.of(I_ÅR, 8, 20);
        var beregningsresultat = buildBeregningsresultatBrukerFP(List.of(400), List.of(0), b1fom, b1tom);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var b2p1fom = LocalDate.of(I_ÅR, 8, 1);
        var b2p1tom = LocalDate.of(I_ÅR, 8, 10);
        var b2p2fom = LocalDate.of(I_ÅR, 8, 11);
        var b2p2tom = LocalDate.of(I_ÅR, 8, 20);

        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(400, 0), List.of(0, 400), b2p1fom, b2p1tom, b2p2fom, b2p2tom);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var oppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste).hasSize(2);
        var oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdrag110Liste);
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        var oppdrag110Arbeidsgiver = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdrag110Liste, virksomhet);
        assertThat(oppdrag110Arbeidsgiver.getKodeEndring()).isEqualTo(KodeEndring.NY);

        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        assertThat(opp150RevurderingListe).hasSize(3).anySatisfy(linjeOpphør -> {
            assertThat(linjeOpphør.gjelderOpphør()).isTrue();
            assertThat(linjeOpphør.getDatoStatusFom()).isEqualTo(LocalDate.of(I_ÅR, 8, 11));
        }).anySatisfy(linjeEndring -> {
            assertThat(linjeEndring.gjelderOpphør()).isFalse();
            assertThat(linjeEndring.getSats()).isEqualTo(Sats.på(400));
            assertThat(linjeEndring.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
        });
        var opp150RevurderingListeForArbeidsgiver = getOppdragslinje150ForMottaker(oppdragRevurdering, false);
        assertThat(opp150RevurderingListeForArbeidsgiver).anySatisfy(
            oppdragslinje150 -> assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_FERIEPENGER_AG));
    }

    @Test
    void skalSendeEndringsoppdragNårEndringsdatoErMidtIFørstePeriodeIRevurderingOgDetErFlereMottakereSomBrukerOgArbeidsgiver() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatFP();

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        var originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(false);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, false);
        verifiserOppdragslinje150_ENDR(oppdragRevurdering, originaltOppdragslinje150, false, false, 80);
    }

    @Test
    void skalOppretteEndringsoppdragNårBehandlingsresultatErInnvilgetOgForrigeOppdragEksisterer() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatFP(true);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        var originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(true);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        verifiserOppdragslinje150_ENDR(oppdragRevurdering, originaltOppdragslinje150, true, false, 80);
        verifiserOppdr150SomErUendret(oppdragRevurdering);
    }

    /**
     * Førstegangsbehandling: Mottaker:Bruker og Arbeidsgiver, Inntektskatagori: AT
     * Revurdering: Mottaker:Bruker og Arbeidsgiver, Inntektskatagori: AT og FL
     * Endringsdato: Første uttaksdato
     */
    @Test
    void skalSendeEndringsoppdragNårDetErEnKlassekodeIForrigeOgFlereKlassekodeINyOppdrag() {
        // Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet);
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        var originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_1, true, 1000, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_2, true, 1000, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        var oppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste).hasSize(1);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, false);
        var opp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110Liste);
        //En Opphør på AT, En ny AT, En ny FL
        assertThat(opp150Liste).hasSize(3);
        var opp150ForFLListe = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150Liste, KodeKlassifik.FPF_FRILANSER);
        assertThat(opp150ForFLListe).isNotEmpty().allSatisfy(opp150 -> assertThat(opp150.gjelderOpphør()).isFalse());
        verifiserOppdragslinje150_ENDR(oppdragRevurdering, originaltOppdragslinje150, false, true, 100);
        OppdragskontrollTestVerktøy.verifiserOppdragslinje150ForHverKlassekode(originaltOppdrag, oppdragRevurdering);
    }

    /**
     * Førstegangsbehandling: Mottaker:Bruker og Arbeidsgiver, Inntektskatagori: AT og FL
     * Revurdering: Mottaker:Bruker og Arbeidsgiver, Inntektskatagori: AT og FL
     * Endringsdato: Første uttaksdato
     */
    @Test
    void skalSendeEndringsoppdragNårDetErFlereKlassekodeBådeIForrigeOgNyOppdragOgDeErLike() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        var originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingMedFlereInntektskategoriFP(AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        verifiserOppdragslinje150_ENDR(oppdragRevurdering, originaltOppdragslinje150, true, true, 80);
        OppdragskontrollTestVerktøy.verifiserOppdragslinje150ForHverKlassekode(originaltOppdrag, oppdragRevurdering);
    }

    /**
     * Førstegangsbehandling: Bruker er mottaker, Inntektskatagori: SN og FL
     * Revurdering: Bruker er mottaker, Inntektskatagori: AT og FL
     * Endringsdato: Første uttaksdato
     */
    @Test
    void skalSendeEndringsoppdragNårDetErFlereKlassekodeBådeIForrigeOgNyOppdragOgEnInntektskategoriIForrigeBehandlingBlirAnnerledesIRevurdering() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 5);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 6, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        // Assert : Første førstegangsbehandling
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        var oppdrag110Liste_1 = originaltOppdrag.getOppdrag110Liste();
        assertThat(oppdrag110Liste_1).hasSize(1);

        // Assert : Revurdering
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        var oppdrag110Liste_2 = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste_2).hasSize(1);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), oppdrag110Liste_2, false);
        var originaltOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);
        var opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        verifiserKodeklassifikNårRevurderingHarNye(originaltOpp150Liste, opp150RevurdListe);
        verifiserKjeding(originaltOpp150Liste, opp150RevurdListe);
    }

    /**
     * Førstegangsbehandling: Bruker er mottaker, Inntektskatagori: SN og FL
     * Revurdering: Bruker er mottaker, Inntektskatagori: AT og Dagpenger
     * Endringsdato: Første uttaksdato
     */
    @Test
    void skalSendeEndringsoppdragNårDetErFlereKlassekodeBådeIForrigeOgNyOppdragOgDeErUlike() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering som endrer både klassekoder og perioder etter de første dagene
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 5);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.DAGPENGER, Inntektskategori.DAGPENGER);
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 15, 25);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.DAGPENGER, Inntektskategori.DAGPENGER);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        var oppdrag110RevurderingListe = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110RevurderingListe).hasSize(1);
        var opp150RevurderingListe = oppdrag110RevurderingListe.get(0).getOppdragslinje150Liste();
        assertThat(opp150RevurderingListe).hasSize(6);
        //Opphør for FL
        var opp150OpphForFLListe = getOpp150MedKodeklassifik(opp150RevurderingListe, KodeKlassifik.FPF_FRILANSER);
        assertThat(opp150OpphForFLListe).hasSize(1).allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.gjelderOpphør()).isTrue());
        //Opphør for SN
        var opp150OpphForSNListe = getOpp150MedKodeklassifik(opp150RevurderingListe, KodeKlassifik.FPF_SELVSTENDIG);
        assertThat(opp150OpphForSNListe).hasSize(1).allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.gjelderOpphør()).isTrue());
        //Oppdragslinje150 for AT
        var opp150ForATListe = getOpp150MedKodeklassifik(opp150RevurderingListe, KodeKlassifik.FPF_ARBEIDSTAKER);
        assertThat(opp150ForATListe).hasSize(2).allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.gjelderOpphør()).isFalse());
        var sortertOpp150OpphForATListe = sortOppdragslinj150Liste(opp150ForATListe);
        var førsteOpp150ForAT = sortertOpp150OpphForATListe.get(0);
        assertThat(førsteOpp150ForAT.getRefFagsystemId()).isNull();
        assertThat(førsteOpp150ForAT.getRefDelytelseId()).isNull();
        var andreOpp150ForAT = sortertOpp150OpphForATListe.get(1);
        assertThat(andreOpp150ForAT.getRefDelytelseId()).isEqualTo(førsteOpp150ForAT.getDelytelseId());
        //Oppdragslinje150 for DP
        var opp150OpphForDPListe = getOpp150MedKodeklassifik(opp150RevurderingListe, KodeKlassifik.FPF_DAGPENGER);
        assertThat(opp150OpphForDPListe).hasSize(2).allSatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.gjelderOpphør()).isFalse());
        var sortertOpp150OpphForDPListe = sortOppdragslinj150Liste(opp150OpphForDPListe);
        var førsteOpp150ForDP = sortertOpp150OpphForDPListe.get(0);
        assertThat(førsteOpp150ForDP.getRefFagsystemId()).isNull();
        assertThat(førsteOpp150ForDP.getRefDelytelseId()).isNull();
        var andreOpp150ForDP = sortertOpp150OpphForDPListe.get(1);
        assertThat(andreOpp150ForDP.getRefDelytelseId()).isEqualTo(førsteOpp150ForDP.getDelytelseId());
        //Sjekk om delytelseId er unikt for oppdragslinje150
        var delytelseIdList = opp150RevurderingListe.stream().map(Oppdragslinje150::getDelytelseId).toList();
        Set<Long> delytelseIdSet = Sets.newHashSet(delytelseIdList);
        assertThat(delytelseIdList).hasSize(delytelseIdSet.size());
    }

    /**
     * Førstegangsbehandling: Mottaker:Bruker, En inntektskategori i periode 1(FL) og to i periode 2 (AT(orgnr1), FL)
     * Revurdering: Mottaker: Bruker, En inntektskategori i periode 1 (AT(orgnr2)) og to i periode 2 (AT(orgnr1)), AT(orgnr2))
     */
    @Test
    void skalSendeEndringsOppdragOgSlåArbeidstakerAndelerSammenHvisBrukerHarFlereISammePeriode() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 5);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet2, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 6, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet2, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Første førstegangsbehandling
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        var oppdrag110Liste_1 = originaltOppdrag.getOppdrag110Liste();
        assertThat(oppdrag110Liste_1).hasSize(1);

        // Assert : Revurdering
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        var originaltOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);
        var opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        verifiserKodeklassifik(originaltOpp150Liste, opp150RevurdListe);
        var opp150IAndrePeriode = opp150RevurdListe.stream()
            .filter(opp150 -> opp150.getDatoVedtakFom().equals(b2Periode_2.getBeregningsresultatPeriodeFom()))
            .toList();
        assertThat(opp150IAndrePeriode).hasSize(1);
        assertThat(opp150IAndrePeriode.get(0).getSats()).isEqualTo(Sats.på(3000));
    }

    @Test
    void skalSendeEndringsOppdragHvisEndringIUtbetalingsgrad() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(80), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(80), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(80), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Første førstegangsbehandling
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        var oppdrag110Liste_1 = originaltOppdrag.getOppdrag110Liste();
        assertThat(oppdrag110Liste_1).hasSize(1);

        // Assert : Revurdering
        OppdragskontrollTestVerktøy.verifiserAvstemming(originaltOppdrag);
        var originaltOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);
        var opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        verifiserKodeklassifik(originaltOpp150Liste, opp150RevurdListe);
        var opp150IAndrePeriode = opp150RevurdListe.stream().filter(opp150 -> !opp150.gjelderOpphør()).toList();
        assertThat(opp150IAndrePeriode).hasSize(2).allSatisfy(opp150 -> assertThat(opp150.getUtbetalingsgrad().getVerdi()).isEqualTo(80));
    }

    @Test
    void skalSendeEndringsoppdragNårDetErFlereKlassekodeIForrigeOppdragOgEnNyKlassekodeINyOppdrag() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        var originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(AktivitetStatus.DAGPENGER, Inntektskategori.DAGPENGER);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        var opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomErOpphørt(opp150RevurdListe, originaltOppdragslinje150, true, true, false);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomErNy(opp150RevurdListe, originaltOppdragslinje150, List.of(80));
        OppdragskontrollTestVerktøy.verifiserOppdr150MedNyKlassekode(opp150RevurdListe);
    }

    @Test
    void skalSendeOppdragMedEnInntektskategoriIOriginalOgFlereIRevurdering() {
        // Førstegang behandling
        var fom = LocalDate.of(I_ÅR, 8, 1);
        var tom = LocalDate.of(I_ÅR, 8, 7);
        var beregningsresultat = buildBeregningsresultatBrukerFP(1500, 500, fom, tom);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Ny revurdering behandling
        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingMedFlereInntektskategoriFP(AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
    }

    @Test
    void skalSendeOppdragNårEnMottakerHarFlereAndelerMedSammeKlassekodeIEnPeriode() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        var originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingMedFlereInntektskategoriFP(AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        var opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomErOpphørt(opp150RevurdListe, originaltOppdragslinje150, true, true, false);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomErNy(opp150RevurdListe, originaltOppdragslinje150, List.of(80, 100));
        OppdragskontrollTestVerktøy.verifiserOppdr150SomAndelerSlåSammen(originaltOppdrag, oppdragRevurdering);
    }

    @Test
    void skalOppretteEndringsoppdragNårBehandlingsresultatErOpphørOgOpphørsdatoErEtterStp() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        var originaltOppdragslinje150 = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);

        var beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingMedFlereInntektskategoriFP(AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        var opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_ENDR(oppdragRevurdering.getOppdrag110Liste(), originaltOppdrag110Liste, true);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomErOpphørt(opp150RevurdListe, originaltOppdragslinje150, true, true, true);
        OppdragskontrollTestVerktøy.verifiserOppdr150SomAndelerSlåSammen(originaltOppdrag, oppdragRevurdering);
    }

    @Test
    void skalSendeKunOpphørSomEnDelAvEndringsoppdragHvisEndringsdatoErEtterSisteDatoITidligereOppdragForBruker() {
        // Arrange
        var beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(true, false);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);

        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110 = originaltOppdrag.getOppdrag110Liste().get(0);

        var sistePeriodeTom = beregningsresultat.getGjeldendePerioder().get(1).getBeregningsresultatPeriodeTom();
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1),
            sistePeriodeTom.minusDays(6));
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100), virksomhet);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        var revurderingOppdrag110 = oppdragRevurdering.getOppdrag110Liste().get(0);

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        assertThat(originaltOppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FP);
        assertThat(originaltOppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
        assertThat(revurderingOppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FP);
        assertThat(revurderingOppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        var revurderingOpp150Liste = revurderingOppdrag110.getOppdragslinje150Liste();
        assertThat(revurderingOpp150Liste).hasSize(2);
        var opp150Opphør = revurderingOpp150Liste.get(0);
        assertThat(opp150Opphør.getDatoStatusFom()).isEqualTo(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1));
        assertThat(opp150Opphør.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
        assertThat(opp150Opphør.gjelderOpphør()).isTrue();
    }

    @Test
    void skalSendeKunOpphørSomEnDelAvEndringsoppdragHvisDetErFlereMottakereSomErArbeidsgivereOgEndringsdatoErEtterSisteDatoINyTilkjentYtelse() {
        // Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        var sistePeriodeTom = beregningsresultat.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeTom();
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriodeRevurdering_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        var brPeriodeRevurdering_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 16);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());
        var revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        assertThat(originaltOppdrag110Liste).hasSize(2).allSatisfy(oppdrag110 -> {
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FPREF);
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
        });
        assertThat(revurderingOppdrag110Liste).hasSize(2).allSatisfy(oppdrag110 -> {
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FPREF);
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        });
        var revurderingOpp150Liste = revurderingOppdrag110Liste.stream()
            .flatMap(opp110 -> opp110.getOppdragslinje150Liste().stream())
            .toList();
        assertThat(revurderingOpp150Liste).hasSize(2).allSatisfy(opp150 -> {
            assertThat(opp150.getDatoStatusFom()).isEqualTo(sistePeriodeTom.minusDays(3));
            assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
            assertThat(opp150.gjelderOpphør()).isTrue();
        });
        var opp150ForVirksomhet1 = OppdragskontrollTestVerktøy.getOpp150ForEnVirksomhet(revurderingOpp150Liste, virksomhet);
        var opp150ForVirksomhet2 = OppdragskontrollTestVerktøy.getOpp150ForEnVirksomhet(revurderingOpp150Liste, virksomhet2);
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getRefunderesId()).isEqualTo(endreTilElleveSiffer(virksomhet));
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getDatoFom()).isEqualTo(LocalDate.now());
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getMaksDato()).isEqualTo(LocalDate.now().plusDays(16));
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getRefunderesId()).isEqualTo(endreTilElleveSiffer(virksomhet2));
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getDatoFom()).isEqualTo(LocalDate.now());
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getMaksDato()).isEqualTo(LocalDate.now().plusDays(16));
    }

    @Test
    void skalOppdatereRefusjonsInfoTilSisteutbetalingsdagHvisIkkeOpphør() {
        // Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        var sistePeriodeTom = beregningsresultat.getBeregningsresultatPerioder().get(1).getBeregningsresultatPeriodeTom();
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriodeRevurdering_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        var brPeriodeRevurdering_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 25);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());
        var revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        assertThat(originaltOppdrag110Liste).hasSize(2).allSatisfy(oppdrag110 -> {
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FPREF);
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
        });
        assertThat(revurderingOppdrag110Liste).hasSize(2).allSatisfy(oppdrag110 -> {
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FPREF);
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        });
        var revurderingOpp150Liste = revurderingOppdrag110Liste.stream()
            .flatMap(opp110 -> opp110.getOppdragslinje150Liste().stream())
            .toList();
        assertThat(revurderingOpp150Liste).hasSize(2).allSatisfy(opp150 -> {
            assertThat(opp150.getDatoVedtakFom()).isEqualTo(sistePeriodeTom.plusDays(1));
            assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
            assertThat(opp150.gjelderOpphør()).isFalse();
        });
        var opp150ForVirksomhet1 = OppdragskontrollTestVerktøy.getOpp150ForEnVirksomhet(revurderingOpp150Liste, virksomhet);
        var opp150ForVirksomhet2 = OppdragskontrollTestVerktøy.getOpp150ForEnVirksomhet(revurderingOpp150Liste, virksomhet2);
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getRefunderesId()).isEqualTo(endreTilElleveSiffer(virksomhet));
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getDatoFom()).isEqualTo(LocalDate.now());
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getMaksDato()).isEqualTo(LocalDate.now().plusDays(25));
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getRefunderesId()).isEqualTo(endreTilElleveSiffer(virksomhet2));
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getDatoFom()).isEqualTo(LocalDate.now());
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getMaksDato()).isEqualTo(LocalDate.now().plusDays(25));
    }

    @Test
    void skalOppdatereRefusjonsInfoHvisFørsteUttaksdagBlirTidligere() {
        // Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriodeRevurdering_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, -5, 10);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        var brPeriodeRevurdering_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());
        var revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        assertThat(originaltOppdrag110Liste).hasSize(2).allSatisfy(oppdrag110 -> {
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FPREF);
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
        });
        assertThat(revurderingOppdrag110Liste).hasSize(2).allSatisfy(oppdrag110 -> {
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FPREF);
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        });
        var revurderingOpp150Liste = revurderingOppdrag110Liste.stream()
            .flatMap(opp110 -> opp110.getOppdragslinje150Liste().stream())
            .toList();
        assertThat(revurderingOpp150Liste).hasSize(6)
            .allSatisfy(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG));
        var opp150ForVirksomhet1 = OppdragskontrollTestVerktøy.getOpp150ForEnVirksomhet(revurderingOpp150Liste, virksomhet);
        var opp150ForVirksomhet2 = OppdragskontrollTestVerktøy.getOpp150ForEnVirksomhet(revurderingOpp150Liste, virksomhet2);
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getRefunderesId()).isEqualTo(endreTilElleveSiffer(virksomhet));
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getDatoFom()).isEqualTo(LocalDate.now());
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getMaksDato()).isEqualTo(LocalDate.now().plusDays(20));
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getRefunderesId()).isEqualTo(endreTilElleveSiffer(virksomhet2));
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getDatoFom()).isEqualTo(LocalDate.now());
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getMaksDato()).isEqualTo(LocalDate.now().plusDays(20));
    }

    @Test
    void skalOppdatereRefusjonsInfoHvisFørsteUttaksdagBlirSenere() {
        // Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriodeRevurdering_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 5, 10);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        var brPeriodeRevurdering_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());
        var revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        assertThat(originaltOppdrag110Liste).hasSize(2).allSatisfy(oppdrag110 -> {
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FPREF);
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
        });
        assertThat(revurderingOppdrag110Liste).hasSize(2).allSatisfy(oppdrag110 -> {
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FPREF);
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        });
        var revurderingOpp150Liste = revurderingOppdrag110Liste.stream()
            .flatMap(opp110 -> opp110.getOppdragslinje150Liste().stream())
            .toList();
        assertThat(revurderingOpp150Liste).hasSize(6)
            .allSatisfy(opp150 -> assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG));
        var opp150ForVirksomhet1 = OppdragskontrollTestVerktøy.getOpp150ForEnVirksomhet(revurderingOpp150Liste, virksomhet);
        var opp150ForVirksomhet2 = OppdragskontrollTestVerktøy.getOpp150ForEnVirksomhet(revurderingOpp150Liste, virksomhet2);
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getRefunderesId()).isEqualTo(endreTilElleveSiffer(virksomhet));
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getDatoFom()).isEqualTo(LocalDate.now());
        assertThat(opp150ForVirksomhet1.getRefusjonsinfo156().getMaksDato()).isEqualTo(LocalDate.now().plusDays(20));
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getRefunderesId()).isEqualTo(endreTilElleveSiffer(virksomhet2));
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getDatoFom()).isEqualTo(LocalDate.now());
        assertThat(opp150ForVirksomhet2.getRefusjonsinfo156().getMaksDato()).isEqualTo(LocalDate.now().plusDays(20));
    }

    /**
     * Førstegangsbehandling: Både bruker og arbeidsgiver er mottaker, Inntektskategori for bruker: AT og SN
     * Revurdering: Arbeidsgiver er eneste mottaker
     * Endringsdato: Første uttaksdato
     */
    @Test
    void skalSendeFullstendigOpphørForBrukerMedFlereInntektskategoriIEndringsoppdragNårBrukerErIkkeMottakerIRevurderingLenger() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andeler for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andeler for bruker i periode#2
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        //Andel for arbeidsgiver i periode#2
        buildBeregningsresultatAndel(b1Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b2Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#2
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Førstegangsbehandling
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originaltOppdrag110Liste).hasSize(2);

        //Revurdering
        var revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();
        assertThat(revurderingOppdrag110Liste).hasSize(1); // Kun opphør for SND og Bruker siden AG uten endring.
        //Oppdrag110 for Bruker
        var oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(revurderingOppdrag110Liste);
        assertThat(oppdrag110Bruker.getKodeFagomrade()).isEqualTo(KodeFagområde.FP);
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(KodeEndring.ENDR);

        //Oppdragslinj150 for Bruker
        var revurderingOpp150ListeForBruker = oppdrag110Bruker.getOppdragslinje150Liste();
        assertThat(revurderingOpp150ListeForBruker).hasSize(2).allSatisfy(opp150 -> {
            assertThat(opp150.getDatoStatusFom()).isEqualTo(b1Periode_1.getBeregningsresultatPeriodeFom());
            assertThat(opp150.gjelderOpphør()).isTrue();
        });
        var oppdragslinje150OpphPå_AT = revurderingOpp150ListeForBruker.stream()
            .filter(opp150 -> KodeKlassifik.FPF_ARBEIDSTAKER.equals(opp150.getKodeKlassifik()))
            .toList();
        assertThat(oppdragslinje150OpphPå_AT).hasSize(1);
        var oppdragslinje150OpphPå_SN = revurderingOpp150ListeForBruker.stream()
            .filter(opp150 -> KodeKlassifik.FPF_SELVSTENDIG.equals(opp150.getKodeKlassifik()))
            .toList();
        assertThat(oppdragslinje150OpphPå_SN).hasSize(1);
    }

    /**
     * Førstegangsbehandling: Bruker er eneste mottaker, Inntektskategori for bruker: AT og SN
     * Revurdering: Bruker er eneste mottaker, Inntektskategori for bruker: AT
     * Endringsdato: Startdato av andre periode i revurdering
     */
    @Test
    void skalIkkeSendeOpphørForBrukerMedFlereInntektskategoriIEndringsoppdragNårEndringsdatoErEtterSistePeriodeTomIForrigeBehandling() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andeler for bruker i periode#1
        var andelAT = buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        var feriepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelAT, 20000L, List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO));

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        var andelRevurderingAT = buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for bruker i periode#2
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var feriepengerRevurdering = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(feriepengerRevurdering, andelRevurderingAT, 20000L,
            List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO));

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        //Revurdering
        var revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();
        assertThat(revurderingOppdrag110Liste).hasSize(1);
        //Oppdrag110 for Bruker
        var oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(revurderingOppdrag110Liste);
        assertThat(oppdrag110Bruker.getKodeFagomrade()).isEqualTo(KodeFagområde.FP);
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        //Oppdragslinj150 for Bruker
        var revurderingOpp150ListeForBruker = oppdrag110Bruker.getOppdragslinje150Liste();
        assertThat(revurderingOpp150ListeForBruker).hasSize(2)
            .anySatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.gjelderOpphør()).isTrue())
            .anySatisfy(oppdragslinje150 -> assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER));
    }

    /**
     * Førstegangsbehandling: Bruker er eneste mottaker, Inntektskategori for bruker: AT og SN <br/>
     * <---AT---><---SN---><br/>
     * Revurdering: Bruker er eneste mottaker, Inntektskategori for bruker: AT<br/>
     * <---AT---><br/>
     * Endringsdato: En dag senere enn siste periode tom i revurdering
     */
    @Test
    void skalSendeKunOpphørSomEnDelAvEndringsoppdragHvisEndringsdatoErEtterSistePeriodeTomIRevurderingForBrukerMedFlereInntektskategoriIForrigeBehandling() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andeler for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        //Andeler for bruker i periode#2
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andel for bruker i periode#2
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 15);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        //Oppdrag110 for førstegangsbehandling
        var oppdrag110ListeForBruker = originaltOppdrag.getOppdrag110Liste();
        assertThat(oppdrag110ListeForBruker).hasSize(1);
        assertThat(oppdrag110ListeForBruker.get(0).getKodeFagomrade()).isEqualTo(KodeFagområde.FP);
        assertThat(oppdrag110ListeForBruker.get(0).getKodeEndring()).isEqualTo(KodeEndring.NY);
        //Oppdrag110 for revurdering
        var oppdrag110ListeForBrukerIRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110ListeForBrukerIRevurdering).hasSize(1);
        assertThat(oppdrag110ListeForBrukerIRevurdering.get(0).getKodeFagomrade()).isEqualTo(KodeFagområde.FP);
        assertThat(oppdrag110ListeForBrukerIRevurdering.get(0).getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        //Oppdragslinje150 for bruker i revurdering
        var opp150ListeForBrukerIRevurdering = oppdrag110ListeForBrukerIRevurdering.get(0).getOppdragslinje150Liste();
        assertThat(opp150ListeForBrukerIRevurdering).hasSize(3);
        assertThat(opp150ListeForBrukerIRevurdering.stream().filter(Oppdragslinje150::gjelderOpphør)).allSatisfy(opp150 -> {
            assertThat(opp150.getDatoStatusFom()).isEqualTo(b1Periode_1.getBeregningsresultatPeriodeFom());
            assertThat(opp150.gjelderOpphør()).isTrue();
        });
        var opp150ForBrukerAT = opp150ListeForBrukerIRevurdering.stream()
            .filter(opp150 -> KodeKlassifik.FPF_ARBEIDSTAKER.equals(opp150.getKodeKlassifik()))
            .findFirst();
        assertThat(opp150ForBrukerAT).isPresent();
        var opp150ForBrukerSN = opp150ListeForBrukerIRevurdering.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .filter(opp150 -> KodeKlassifik.FPF_SELVSTENDIG.equals(opp150.getKodeKlassifik()))
            .findFirst();
        assertThat(opp150ForBrukerSN).isPresent();
    }

    /**
     * Førstegangsbehandling: Bruker er eneste mottaker, Inntektskategori for bruker: AT og SN
     * Revurdering: Bruker er eneste mottaker, Inntektskategori for bruker: AT og SN
     * Endringsdato: En dag senere enn siste periode tom i revurdering
     */
    @Test
    void skalSendeKunOpphørSomEnDelAvEndringsoppdragHvisEndringsdatoErEtterSistePeriodeTomIRevurderingForBrukerMedFlereInntektskategoriIBådeForrigeOgNyBehandling() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andeler for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        //Andeler for bruker i periode#2
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        //Andel for bruker i periode#2
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 15);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE,
            Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        //Oppdrag110 for førstegangsbehandling
        var oppdrag110ListeForBruker = originaltOppdrag.getOppdrag110Liste();
        assertThat(oppdrag110ListeForBruker).hasSize(1);
        assertThat(oppdrag110ListeForBruker.get(0).getKodeFagomrade()).isEqualTo(KodeFagområde.FP);
        assertThat(oppdrag110ListeForBruker.get(0).getKodeEndring()).isEqualTo(KodeEndring.NY);
        //Oppdrag110 for revurdering
        var oppdrag110ListeForBrukerIRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110ListeForBrukerIRevurdering).hasSize(1);
        assertThat(oppdrag110ListeForBrukerIRevurdering.get(0).getKodeFagomrade()).isEqualTo(KodeFagområde.FP);
        assertThat(oppdrag110ListeForBrukerIRevurdering.get(0).getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        //Oppdragslinje150 for bruker i revurdering
        var opp150ListeForBrukerIRevurdering = oppdrag110ListeForBrukerIRevurdering.get(0).getOppdragslinje150Liste();
        assertThat(opp150ListeForBrukerIRevurdering).hasSize(2).allSatisfy(opp150 -> {
            assertThat(opp150.getDatoStatusFom()).isEqualTo(b2Periode_2.getBeregningsresultatPeriodeTom().plusDays(1));
            assertThat(opp150.gjelderOpphør()).isTrue();
        });
        var opp150ForBrukerAT = opp150ListeForBrukerIRevurdering.stream()
            .filter(opp150 -> KodeKlassifik.FPF_ARBEIDSTAKER.equals(opp150.getKodeKlassifik()))
            .findFirst();
        assertThat(opp150ForBrukerAT).isPresent();
        var opp150ForBrukerSN = opp150ListeForBrukerIRevurdering.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .filter(opp150 -> KodeKlassifik.FPF_SELVSTENDIG.equals(opp150.getKodeKlassifik()))
            .findFirst();
        assertThat(opp150ForBrukerSN).isPresent();
    }

    @Test
    void skalSendeEndringsoppdragHvisDetErFlereMottakereSomErArbeidsgiverOgFinnesMerEnnToBeregningsresultatPerioder() {
        // Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 5);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 600, BigDecimal.valueOf(100), virksomhet2);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 6, 10);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var brPeriode_3 = buildBeregningsresultatPeriode(beregningsresultat, 11, 15);
        buildBeregningsresultatAndel(brPeriode_3, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_3, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var brPeriode_4 = buildBeregningsresultatPeriode(beregningsresultat, 16, 20);
        buildBeregningsresultatAndel(brPeriode_4, false, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode_4, false, 1100, BigDecimal.valueOf(100), virksomhet2);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());
        var originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        var førstePeriodeFom = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom();
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var brPeriodeRevurdering_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 6);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 400, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_1, false, 300, BigDecimal.valueOf(100), virksomhet2);
        var brPeriodeRevurdering_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 7, 11);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_2, false, 700, BigDecimal.valueOf(100), virksomhet2);
        var brPeriodeRevurdering_3 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 12, 15);
        buildBeregningsresultatAndel(brPeriodeRevurdering_3, false, 400, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_3, false, 300, BigDecimal.valueOf(100), virksomhet2);
        var brPeriodeRevurdering_4 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 16, 20);
        buildBeregningsresultatAndel(brPeriodeRevurdering_4, false, 500, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriodeRevurdering_4, false, 700, BigDecimal.valueOf(100), virksomhet2);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());
        var revurderingOppdrag110Liste = oppdragRevurdering.getOppdrag110Liste();

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        assertThat(originaltOppdrag110Liste).hasSize(2).allSatisfy(oppdrag110 -> {
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FPREF);
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.NY);
        });
        assertThat(revurderingOppdrag110Liste).hasSize(2).allSatisfy(oppdrag110 -> {
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(KodeFagområde.FPREF);
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        });
        var revurderingOpp150Arbgvr1 = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(revurderingOppdrag110Liste, virksomhet);
        var revurderingOpp150Arbgvr2 = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(revurderingOppdrag110Liste, virksomhet2);
        verifiserOpp150NårDetErFlereArbeidsgivereSomMottaker(førstePeriodeFom, revurderingOpp150Arbgvr1);
        verifiserOpp150NårDetErFlereArbeidsgivereSomMottaker(førstePeriodeFom, revurderingOpp150Arbgvr2);
    }

    /**
     * <p>
     * Førstegangsbehandling <br>
     * Perioder: To perioder <br>
     * Mottaker: Bruker, Inntektskategori: AT og FL <br>
     * Feriepenger: Ja, Feriepengeår: i_År.plusYears(1), Beløp: 3000 <br>
     * <p>
     * Revurdering <br>
     * Perioder: Tre perioder <br>
     * Mottaker: Bruker, Inntektskategori: AT og FL <br>
     * Feriepenger: Ja, Feriepengeår: i_År.plusYears(1) og i_År.plusYears(2), Beløp: 3000 og 3000 <br>
     * Endringsdato: Start dato av siste periode i revurdering
     */
    @Test
    void skalSendeEndringsoppdragUtenOpphørNårDetBlirLagtTilEnNyTilkjentYtelsePeriodeIRevurderingForBrukerMedFlereKlassekode() {

        // Arrange : Førstegangsbehandling
        var beregningsresultatFP_1 = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        //Andeler for bruker i periode#1
        var b1Andel = buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        //Andeler for bruker i periode#2
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var b1_feriepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(b1_feriepenger, b1Andel, 3000L, List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO));
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1, b1_feriepenger);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andeler for bruker i periode#1
        var b2Andel = buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        //Andeler for bruker i periode#2
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        //Andeler for bruker i periode#3
        var b2Periode_3 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 21, 30);
        buildBeregningsresultatAndel(b2Periode_3, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_3, true, 1500, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        var b2_feriepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(b2_feriepenger, b2Andel, 3000L,
            List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO, NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusYears(1)));

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP, b2_feriepenger);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        //Oppdrag110
        var originalOpp110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originalOpp110Liste).hasSize(1);
        var opp110ListeForRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(opp110ListeForRevurdering).hasSize(1);
        var oppdrag110ForBruker = opp110ListeForRevurdering.get(0);
        //Oppdragslinje150
        var opp150ListeForBruker = oppdrag110ForBruker.getOppdragslinje150Liste();
        assertThat(opp150ListeForBruker).hasSize(3);
        //Oppdragslinje150 for feriepenger
        var opp150ForFeriepengerList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150ListeForBruker,
            KodeKlassifik.FERIEPENGER_BRUKER);
        assertThat(opp150ForFeriepengerList).hasSize(1);
        var opp150ForFeriepenger = opp150ForFeriepengerList.get(0);
        assertThat(opp150ForFeriepenger.gjelderOpphør()).isFalse();
        assertThat(opp150ForFeriepenger.getDatoVedtakFom().getYear()).isEqualTo(I_ÅR + 2);
        //Oppdragslinje150 for AT
        var opp150ForATIRevurderingList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150ListeForBruker,
            KodeKlassifik.FPF_ARBEIDSTAKER);
        assertThat(opp150ForATIRevurderingList).hasSize(1);
        var opp150ForATIRevurdering = opp150ForATIRevurderingList.get(0);
        assertThat(opp150ForATIRevurdering.gjelderOpphør()).isFalse();
        var tidligereOpp150ForATList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(
            originalOpp110Liste.get(0).getOppdragslinje150Liste(), KodeKlassifik.FPF_ARBEIDSTAKER);
        var sisteOpp150ForAT = tidligereOpp150ForATList.stream().max(Comparator.comparing(Oppdragslinje150::getDelytelseId)).get();
        assertThat(opp150ForATIRevurdering.getRefDelytelseId()).isEqualTo(sisteOpp150ForAT.getDelytelseId());
        //Oppdragslinje150 for FL
        var opp150ForFLIRevurderingList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150ListeForBruker,
            KodeKlassifik.FPF_FRILANSER);
        assertThat(opp150ForFLIRevurderingList).hasSize(1);
        var opp150ForFLIRevurdering = opp150ForFLIRevurderingList.get(0);
        assertThat(opp150ForFLIRevurdering.gjelderOpphør()).isFalse();
        var tidligereOpp150ForFLList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(
            originalOpp110Liste.get(0).getOppdragslinje150Liste(), KodeKlassifik.FPF_FRILANSER);
        var sisteOpp150ForFL = tidligereOpp150ForFLList.stream().max(Comparator.comparing(Oppdragslinje150::getDelytelseId)).get();
        assertThat(opp150ForFLIRevurdering.getRefDelytelseId()).isEqualTo(sisteOpp150ForFL.getDelytelseId());
    }

    /**
     * <p>
     * Førstegangsbehandling <br>
     * Perioder: To perioder <br>
     * Mottaker: Bruker, Inntektskategori: AT<br>
     * Feriepenger: Ja, Feriepengeår: i_År.plusYears(1), Beløp: 3000 <br>
     * <p>
     * Revurdering <br>
     * Perioder: Tre perioder <br>
     * Mottaker: Bruker, Inntektskategori: AT<br>
     * Feriepenger: Ja, Feriepengeår: i_År.plusYears(1) og i_År.plusYears(2), Beløp: 3000 og 3000 <br>
     * Endringsdato: Start dato av siste periode i revurdering
     */
    @Test
    void skalSendeEndringsoppdragUtenOpphørNårDetBlirLagtTilEnNyTilkjentYtelsePeriodeIRevurderingForBrukerMedEnKlassekode() {

        // Arrange : Førstegangsbehandling
        var beregningsresultatFP_1 = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        //Andeler for bruker i periode#1
        var b1Andel = buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andeler for bruker i periode#2
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var b1_feriepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(b1_feriepenger, b1Andel, 3000L, List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO));
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1, b1_feriepenger);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andeler for bruker i periode#1
        var b2Andel = buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andeler for bruker i periode#2
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andeler for bruker i periode#3
        var b2Periode_3 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 21, 30);
        buildBeregningsresultatAndel(b2Periode_3, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var b2_feriepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(b2_feriepenger, b2Andel, 3000L,
            List.of(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO, NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusYears(1)));
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP, b2_feriepenger);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        //Oppdrag110
        var originalOpp110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originalOpp110Liste).hasSize(1);
        var opp110ListeForRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(opp110ListeForRevurdering).hasSize(1);
        var oppdrag110ForBruker = opp110ListeForRevurdering.get(0);
        //Oppdragslinje150
        var opp150ListeForBruker = oppdrag110ForBruker.getOppdragslinje150Liste();
        assertThat(opp150ListeForBruker).hasSize(2);
        //Oppdragslinje150 for feriepenger
        var opp150ForFeriepengerList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150ListeForBruker,
            KodeKlassifik.FERIEPENGER_BRUKER);
        assertThat(opp150ForFeriepengerList).hasSize(1);
        var opp150ForFeriepenger = opp150ForFeriepengerList.get(0);
        assertThat(opp150ForFeriepenger.gjelderOpphør()).isFalse();
        assertThat(opp150ForFeriepenger.getDatoVedtakFom().getYear()).isEqualTo(I_ÅR + 2);
        //Oppdragslinje150 for AT
        var opp150ForATIRevurderingList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(opp150ListeForBruker,
            KodeKlassifik.FPF_ARBEIDSTAKER);
        assertThat(opp150ForATIRevurderingList).hasSize(1);
        var opp150ForATIRevurdering = opp150ForATIRevurderingList.get(0);
        assertThat(opp150ForATIRevurdering.gjelderOpphør()).isFalse();
        var tidligereOpp150ForATList = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(
            originalOpp110Liste.get(0).getOppdragslinje150Liste(), KodeKlassifik.FPF_ARBEIDSTAKER);
        var sisteOpp150ForAT = tidligereOpp150ForATList.stream().max(Comparator.comparing(Oppdragslinje150::getDelytelseId)).get();
        assertThat(opp150ForATIRevurdering.getRefDelytelseId()).isEqualTo(sisteOpp150ForAT.getDelytelseId());
    }

    /**
     * <p>
     * Førstegangsbehandling <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker og arbeidsgiver(har andel kun før endringsdato), Inntektskategori: AT<br>
     * <p>
     * Revurdering <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker og arbeidsgiver(har andel kun før endringsdato), Inntektskategori: AT<br>
     * Endringsdato: Start dato av andre periode i revurdering
     */
    @Test
    void skalIkkeSendeOppdragForArbeidsgiverHvisDetFinnesIngenAndelerFomEndringsdatoIRevurderingOgIngenAndelerSomSkalOpphøresIForrige() {

        // Arrange : Førstegangsbehandling
        var beregningsresultatFP_1 = buildEmptyBeregningsresultatFP();
        // Periode#1
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Periode#2
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        //Andel for bruker i periode#2
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b2Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andeler for bruker i periode#2
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 800, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        //Oppdrag110
        var originalOpp110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originalOpp110Liste).hasSize(2);
        var opp110ListeForRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(opp110ListeForRevurdering).hasSize(1);
        var oppdrag110ForBruker = opp110ListeForRevurdering.get(0);
        assertThat(!oppdrag110ForBruker.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver()).isTrue();
        //Oppdragslinje150
        var opp150ListeForBruker = oppdrag110ForBruker.getOppdragslinje150Liste();
        assertThat(opp150ListeForBruker).hasSize(2);
    }

    /**
     * <p>
     * Førstegangsbehandling <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker(har andel kun før endringsdato) og arbeidsgiver, Inntektskategori: AT<br>
     * <p>
     * Revurdering <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker(har andel kun før endringsdato) og arbeidsgiver, Inntektskategori: AT<br>
     * Endringsdato: Start dato av andre periode i revurdering
     */
    @Test
    void skalIkkeSendeOppdragForBrukerHvisDetFinnesIngenAndelerFomEndringsdatoIRevurderingOgIngenAndelerSomSkalOpphøresIForrige() {

        // Arrange : Førstegangsbehandling
        var beregningsresultatFP_1 = buildEmptyBeregningsresultatFP();
        // Periode#1
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Periode#2
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        //Andel for arbeidsgiver i periode#2
        buildBeregningsresultatAndel(b1Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b2Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andeler for arbeidsgiver i periode#2
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, false, 800, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        //Oppdrag110
        var originalOpp110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originalOpp110Liste).hasSize(2);
        var opp110ListeForRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(opp110ListeForRevurdering).hasSize(1);
        var oppdrag110ForArbeidsgiver = opp110ListeForRevurdering.get(0);
        assertThat(oppdrag110ForArbeidsgiver.getKodeFagomrade()).isNotEqualTo(KodeFagområde.FP);
        //Oppdragslinje150
        var opp150ListeForArbeidsgiver = oppdrag110ForArbeidsgiver.getOppdragslinje150Liste();
        assertThat(opp150ListeForArbeidsgiver).hasSize(2);
    }

    /**
     * <p>
     * Førstegangsbehandling <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker(har andeler kun før endringsdato) og arbeidsgiver, Inntektskategori: AT og FL<br>
     * <p>
     * Revurdering <br>
     * Perioder: To perioder <br>
     * Mottakere: Bruker(har andel kun før endringsdato) og arbeidsgiver, Inntektskategori: AT og FL<br>
     * Endringsdato: Start dato av andre periode i revurdering
     */
    @Test
    void skalIkkeSendeOppdragForBrukerMedFlereInntektskategoriHvisDetFinnesIngenAndelerFomEndringsdatoIRevurderingOgIngenAndelerSomSkalOpphøresIForrige() {

        // Arrange : Førstegangsbehandling
        var beregningsresultatFP_1 = buildEmptyBeregningsresultatFP();
        // Periode#1
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        //Andeler for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1100, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Periode#2
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        //Andel for arbeidsgiver i periode#2
        buildBeregningsresultatAndel(b1Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultatFP_1);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), null, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b2Periode_1, false, 1000, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        //Andeler for arbeidsgiver i periode#2
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, false, 800, BigDecimal.valueOf(100), virksomhet, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        assertThat(oppdragRevurdering).isNotNull();
        //Oppdrag110
        var originalOpp110Liste = originaltOppdrag.getOppdrag110Liste();
        assertThat(originalOpp110Liste).hasSize(2);
        var opp110ListeForRevurdering = oppdragRevurdering.getOppdrag110Liste();
        assertThat(opp110ListeForRevurdering).hasSize(1);
        var oppdrag110ForArbeidsgiver = opp110ListeForRevurdering.get(0);
        assertThat(!oppdrag110ForArbeidsgiver.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver()).isFalse();
        //Oppdragslinje150
        var opp150ListeForArbeidsgiver = oppdrag110ForArbeidsgiver.getOppdragslinje150Liste();
        assertThat(opp150ListeForArbeidsgiver).hasSize(2);
    }

    /**
     * Førstegangsbehandling: Mottaker:Bruker
     * Andeler: AT(virksomhet) og AT(Privat arbgvr)
     * Revurdering: Mottaker: Bruker
     * Andeler: AT(virksomhet) og AT(Privat arbgvr)
     */
    @Test
    void skalSendeEndringsOppdragOgSlåATAndelerSammenHvisBrukerHarArbeidsforholdHosBådeEnOrganisasjonOgPrivatArbeidsgiver() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b1Periode_1, true, 1200, BigDecimal.valueOf(100), null);
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b1Periode_2, true, 1200, BigDecimal.valueOf(100), null);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_1, true, 1300, BigDecimal.valueOf(100), null);
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1100, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_2, true, 1300, BigDecimal.valueOf(100), null);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Revurdering
        var oppdra110BrukerList = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdra110BrukerList).hasSize(1);
        assertThat(oppdra110BrukerList.get(0).getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        var alleOpp150BrukerListe = oppdra110BrukerList.get(0).getOppdragslinje150Liste();
        assertThat(alleOpp150BrukerListe).hasSize(2).anySatisfy(opp150 -> assertThat(opp150.gjelderOpphør()).isTrue());
        var opp150UtenOpphBrukerListe = alleOpp150BrukerListe.stream().filter(opp150 -> !opp150.gjelderOpphør()).toList();
        assertThat(opp150UtenOpphBrukerListe).hasSize(1).allSatisfy(opp150 -> {
            assertThat(opp150.getSats()).isEqualTo(Sats.på(2400L));
            assertThat(opp150.getRefDelytelseId()).isNotNull();
            assertThat(opp150.getRefFagsystemId()).isNotNull();
        });
    }

    /**
     * Førstegangsbehandling: Mottakere:Bruker og privat arbeidsgiver
     * Andeler: AT(virksomhet), AT(Privat arbgvr), Refusjon - AT(Privat arbgvr)
     * Revurdering: Mottakere: Bruker og privat arbeidsgiver
     * Andeler: AT(virksomhet), AT(Privat arbgvr), Refusjon - AT(Privat arbgvr)
     */
    @Test
    void skalSendeEndringsOppdragOgSlåATAndelerSammenHvisBrukerHarArbeidsforholdHosBådeEnOrganisasjonOgPrivatArbgvrOgFinnesRefusjonTilPrivatArbgvr() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b1Periode_1, true, 1200, BigDecimal.valueOf(100), null);
        buildBeregningsresultatAndel(b1Periode_1, false, 1200, BigDecimal.valueOf(100), null);
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1000, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b1Periode_2, true, 1200, BigDecimal.valueOf(100), null);
        buildBeregningsresultatAndel(b1Periode_2, false, 1200, BigDecimal.valueOf(100), null);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1100, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_1, true, 1300, BigDecimal.valueOf(100), null);
        buildBeregningsresultatAndel(b2Periode_1, false, 1300, BigDecimal.valueOf(100), null);
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1100, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_2, true, 1300, BigDecimal.valueOf(100), null);
        buildBeregningsresultatAndel(b2Periode_2, false, 1300, BigDecimal.valueOf(100), null);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Revurdering
        var oppdra110BrukerList = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdra110BrukerList).hasSize(1);
        assertThat(oppdra110BrukerList.get(0).getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        var alleOpp150BrukerListe = oppdra110BrukerList.get(0).getOppdragslinje150Liste();
        assertThat(alleOpp150BrukerListe).hasSize(2).anySatisfy(opp150 -> assertThat(opp150.gjelderOpphør()).isTrue());
        var opp150UtenOpphBrukerListe = alleOpp150BrukerListe.stream().filter(opp150 -> !opp150.gjelderOpphør()).toList();
        assertThat(opp150UtenOpphBrukerListe).hasSize(1).allSatisfy(opp150 -> {
            assertThat(opp150.getSats()).isEqualTo(Sats.på(3700L));
            assertThat(opp150.getRefDelytelseId()).isNotNull();
            assertThat(opp150.getRefFagsystemId()).isNotNull();
        });
    }

    /**
     * Førstegangsbehandling: Mottaker: Privat arbeidsgiver
     * Andeler: Refusjon - AT(Privat arbgvr)
     * Revurdering: Mottaker: Bruker
     * Andeler: AT(Privat arbgvr)
     */
    @Test
    @Disabled("må vurdere hvordan man skal løse problemet - send oppgave tit NØS uansett?")
    void skalSendeEndringsOppdragNårPrivatArbgvrHarRefusjonIForrigeBehandlingOgBrukerBlirMottakerIRevurdering() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, false, 1200, BigDecimal.valueOf(100), null);
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, false, 1200, BigDecimal.valueOf(100), null);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, true, 1200, BigDecimal.valueOf(100), null);
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, true, 1200, BigDecimal.valueOf(100), null);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Revurdering
        var oppdra110BrukerList = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdra110BrukerList).hasSize(1);
        assertThat(oppdra110BrukerList.get(0).getKodeEndring()).isEqualTo(KodeEndring.ENDR);
        var alleOpp150BrukerListe = oppdra110BrukerList.get(0).getOppdragslinje150Liste();
        assertThat(alleOpp150BrukerListe).hasSize(3).anySatisfy(opp150 -> assertThat(opp150.gjelderOpphør()).isTrue());
        var opp150UtenOpphBrukerListe = alleOpp150BrukerListe.stream().filter(opp150 -> !opp150.gjelderOpphør()).toList();
        assertThat(opp150UtenOpphBrukerListe).hasSize(2).allSatisfy(opp150 -> {
            assertThat(opp150.getSats()).isEqualTo(Sats.på(1200));
            assertThat(opp150.getRefDelytelseId()).isNotNull();
            assertThat(opp150.getRefFagsystemId()).isNotNull();
        });
    }

    /**
     * Førstegangsbehandling: Mottaker: Arbeidsgiver(Virksomhet)
     * Andeler: Refusjon - AT(Virksomhet)
     * Revurdering: Mottaker: Privat arbgvr
     * Andeler: Refusjon - AT(Privat arbgvr)
     */
    @Test
    void skalSendeFørstegangsoppdragForBrukerSomEnDelAvEndringsOppdragNårPrivatArbgvrIkkeErMottakerIForrigeOgHarRefusjonFørstegangIRevurdering() {

        // Arrange : Førstegangsbehandling
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(b1Periode_1, false, 1200, BigDecimal.valueOf(100), virksomhet);
        var b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, false, 1200, BigDecimal.valueOf(100), virksomhet);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);

        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // Arrange : Revurdering
        var beregningsresultatRevurderingFP = buildEmptyBeregningsresultatFP();
        var b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 10);
        buildBeregningsresultatAndel(b2Periode_1, false, 1200, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_1, false, 1000, BigDecimal.valueOf(100), null);
        var b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, false, 1200, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(b2Periode_2, false, 1000, BigDecimal.valueOf(100), null);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));
        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // Assert : Revurdering
        var oppdra110List = oppdragRevurdering.getOppdrag110Liste();
        assertThat(oppdra110List).hasSize(1); // ny oppdag for privat arbeidsgiver
        //Oppdrag110 privat arbgvr
        var oppdrag110ForPrivatArbgvr = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdra110List);
        assertThat(oppdrag110ForPrivatArbgvr.getKodeEndring()).isEqualTo(KodeEndring.NY);
        //Oppdragslinje150 privat arbgvr
        var opp150PrivatArbgvrListe = oppdrag110ForPrivatArbgvr.getOppdragslinje150Liste();
        assertThat(opp150PrivatArbgvrListe).hasSize(1).allSatisfy(opp150 -> {
            assertThat(opp150.getSats()).isEqualTo(Sats.på(1000));
            assertThat(opp150.gjelderOpphør()).isFalse();
        });
    }

    /**
     * Førstegangsbehandling med 2 perioder samme dagsatser til AG <br>
     * Revurdering med to perioder og bortfall av all ytelse<br
     * Ny revurdering med to ny oppfylte perioder med hhv refusjon og utbetaling til bruker<br
     */
    @Test
    void skalSendeOppdragMedOpphørNårAllInnvilgetYtelseBortfaller() {
        // Arrange
        var b10fom = LocalDate.of(I_ÅR, 7, 1);
        var b10tom = LocalDate.of(I_ÅR, 7, 31);
        var b11fom = LocalDate.of(I_ÅR, 8, 1);
        var b11tom = LocalDate.of(I_ÅR, 8, 15);
        var b12fom = LocalDate.of(I_ÅR, 9, 16);
        var b12tom = LocalDate.of(I_ÅR, 9, 30);
        var beregningsresultat = buildBeregningsresultatBrukerFP(List.of(0, 0, 0), List.of(0, 800, 800), b10fom, b10tom, b11fom, b11tom, b12fom,
            b12tom);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var b2p1fom = LocalDate.of(I_ÅR, 8, 1);
        var b2p1tom = LocalDate.of(I_ÅR, 8, 15);
        var b2p2fom = LocalDate.of(I_ÅR, 9, 16);
        var b2p2tom = LocalDate.of(I_ÅR, 9, 30);
        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(0, 0), List.of(0, 0), b2p1fom, b2p1tom, b2p2fom, b2p2tom);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();
        // AG + FP
        assertThat(opp150RevurderingListe).hasSize(2)
            .allSatisfy(linje -> assertThat(linje.gjelderOpphør()).isTrue())
            .anySatisfy(linje -> assertThat(linje.getDatoStatusFom()).isEqualTo(b2p1fom));

        // Arrange 2
        var b3p1fom = LocalDate.of(I_ÅR, 9, 1);
        var b3p1tom = LocalDate.of(I_ÅR, 9, 15);
        var b3p2fom = LocalDate.of(I_ÅR, 9, 16);
        var b3p2tom = LocalDate.of(I_ÅR, 9, 30);
        var beregningsresultatRevurderingFP2 = buildBeregningsresultatBrukerFP(List.of(0, 820), List.of(820, 0), b3p1fom, b3p1tom, b3p2fom, b3p2tom);
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP2);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering)));

        // Act
        var oppdragRevurdering2 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder3.build());

        //Assert 2
        var opp150RevurderingListe2 = oppdragRevurdering2.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        // AG + Bruker + 2 * FP
        assertThat(opp150RevurderingListe2).hasSize(4).noneSatisfy(linje -> assertThat(linje.gjelderOpphør()).isTrue());
    }

    /**
     * Prodscenario der ytelse omfordeles fra 1 ag til bruker, deretter omfordeles den andre ag til bruker. Oppretter ikke 110 for siste revurdering.
     */
    @Test
    void skalSendeOmfordeleFlereArbeidsgivereSerielt() {
        // Arrange
        var b10fom = LocalDate.of(I_ÅR - 1, 11, 2);
        var b10tom = LocalDate.of(I_ÅR - 1, 11, 19);
        var b20fom = LocalDate.of(I_ÅR - 1, 11, 20);
        var b20tom = LocalDate.of(I_ÅR - 1, 11, 30);
        var b21fom = LocalDate.of(I_ÅR - 1, 12, 1);
        var b21tom = LocalDate.of(I_ÅR - 1, 12, 31);
        var b30fom = LocalDate.of(I_ÅR, 1, 1);
        var b30tom = LocalDate.of(I_ÅR, 3, 4);
        var b40fom = LocalDate.of(I_ÅR, 3, 5);
        var b40tom = LocalDate.of(I_ÅR, 5, 28);
        var opptjeningsårFeriepenger = LocalDate.of(I_ÅR - 1, 12, 31);

        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("clob1").medRegelSporing("clob2").build();

        var feriepenger = buildBeregningsresultatFeriepenger();

        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, b10fom, b10tom.plusDays(1));
        buildBeregningsresultatAndel(brPeriode1, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode1, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB1P1Org1 = buildBeregningsresultatAndel(brPeriode1, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P1Org1, 1207L, opptjeningsårFeriepenger);
        var andelB1P1Org2 = buildBeregningsresultatAndel(brPeriode1, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P1Org2, 236L, opptjeningsårFeriepenger);

        var brPeriode2 = buildBeregningsresultatPeriode(beregningsresultat, b20fom.plusDays(3), b21tom.plusDays(2));
        buildBeregningsresultatAndel(brPeriode2, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode2, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB1P2Org1 = buildBeregningsresultatAndel(brPeriode2, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P2Org1, 2334L, opptjeningsårFeriepenger);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P2Org1, 80L, opptjeningsårFeriepenger.plusYears(1));
        var andelB1P2Org2 = buildBeregningsresultatAndel(brPeriode2, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P2Org2, 456L, opptjeningsårFeriepenger);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P2Org2, 16L, opptjeningsårFeriepenger.plusYears(1));

        var brPeriode3 = buildBeregningsresultatPeriode(beregningsresultat, b30fom.plusDays(2), b30tom.plusDays(1));
        buildBeregningsresultatAndel(brPeriode3, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode3, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB1P3Org1 = buildBeregningsresultatAndel(brPeriode3, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P3Org1, 1207L, opptjeningsårFeriepenger.plusYears(1));
        var andelB1P3Org2 = buildBeregningsresultatAndel(brPeriode3, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelB1P3Org2, 236L, opptjeningsårFeriepenger.plusYears(1));

        var brPeriode4 = buildBeregningsresultatPeriode(beregningsresultat, b40fom.plusDays(3), b40tom);
        buildBeregningsresultatAndel(brPeriode4, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode4, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatAndel(brPeriode4, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brPeriode4, true, 0, BigDecimal.valueOf(100), virksomhet2);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat, feriepenger);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        // To arbeidsgivere som mottakere ingen oppdrag til bruker
        assertThat(originaltOppdrag.getOppdrag110Liste()).hasSize(2);
        assertThat(originaltOppdrag.getOppdrag110Liste()
            .stream()
            .allMatch(oppdrag110 -> oppdrag110.getKodeFagomrade().equals(KodeFagområde.FPREF))).isTrue();

        // Arrange 1 - første revurdering2

        var beregningsresultat1 = BeregningsresultatEntitet.builder().medRegelInput("clob1").medRegelSporing("clob2").build();

        var feriepenger1 = buildBeregningsresultatFeriepenger();

        var brR0Periode1 = buildBeregningsresultatPeriode(beregningsresultat1, b10fom, b10tom);
        buildBeregningsresultatAndel(brR0Periode1, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR0Periode1, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB2P1Org1 = buildBeregningsresultatAndel(brR0Periode1, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P1Org1, 1127L, opptjeningsårFeriepenger);
        var andelB2P1Org2 = buildBeregningsresultatAndel(brR0Periode1, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P1Org2, 220L, opptjeningsårFeriepenger);

        var brR0Periode2 = buildBeregningsresultatPeriode(beregningsresultat1, b20fom, b21tom);
        buildBeregningsresultatAndel(brR0Periode2, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR0Periode2, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB2P2Org1 = buildBeregningsresultatAndel(brR0Periode2, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P2Org1, 2414L, opptjeningsårFeriepenger);
        var andelB2P2Org2 = buildBeregningsresultatAndel(brR0Periode2, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P2Org2, 471L, opptjeningsårFeriepenger);

        var brR0Periode3 = buildBeregningsresultatPeriode(beregningsresultat1, b30fom, b30tom);
        buildBeregningsresultatAndel(brR0Periode3, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR0Periode3, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB2P3Org1 = buildBeregningsresultatAndel(brR0Periode3, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P3Org1, 1288L, opptjeningsårFeriepenger.plusYears(1));
        var andelB2P3Org2 = buildBeregningsresultatAndel(brR0Periode3, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger1, andelB2P3Org2, 251L, opptjeningsårFeriepenger.plusYears(1));

        var brR0Periode4 = buildBeregningsresultatPeriode(beregningsresultat1, b40fom, b40tom);
        buildBeregningsresultatAndel(brR0Periode4, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR0Periode4, true, 0, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatAndel(brR0Periode4, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR0Periode4, false, 154, BigDecimal.valueOf(100), virksomhet2);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultat1, feriepenger1);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        // To arbeidsgivere som mottakere ingen oppdrag til bruker
        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSize(2);
        assertThat(oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .allMatch(oppdrag110 -> oppdrag110.getKodeFagomrade().equals(KodeFagområde.FPREF))).isTrue();


        // Arrange 2 - andre revurdering med omfordeling av 1 ag til bruker

        var beregningsresultat2 = BeregningsresultatEntitet.builder().medRegelInput("clob1").medRegelSporing("clob2").build();

        var feriepenger2 = buildBeregningsresultatFeriepenger();

        var brRPeriode1 = buildBeregningsresultatPeriode(beregningsresultat2, b10fom, b10tom);
        buildBeregningsresultatAndel(brRPeriode1, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brRPeriode1, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB3P1Org1 = buildBeregningsresultatAndel(brRPeriode1, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P1Org1, 1127L, opptjeningsårFeriepenger);
        var andelB3P1Org2 = buildBeregningsresultatAndel(brRPeriode1, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P1Org2, 220L, opptjeningsårFeriepenger);

        var brRPeriode2a = buildBeregningsresultatPeriode(beregningsresultat2, b20fom, b20tom);
        buildBeregningsresultatAndel(brRPeriode2a, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brRPeriode2a, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB3P2Org1 = buildBeregningsresultatAndel(brRPeriode2a, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P2Org1, 563L, opptjeningsårFeriepenger);
        var andelB3P2Org2 = buildBeregningsresultatAndel(brRPeriode2a, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P2Org2, 110L, opptjeningsårFeriepenger);

        var brRPeriode2b = buildBeregningsresultatPeriode(beregningsresultat2, b21fom, b21tom);
        var andelB3P3Org2 = buildBeregningsresultatAndel(brRPeriode2b, true, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P3Org2, 361L, opptjeningsårFeriepenger);
        buildBeregningsresultatAndel(brRPeriode2b, true, 0, BigDecimal.valueOf(100), virksomhet);
        var andelB3P3Org1 = buildBeregningsresultatAndel(brRPeriode2b, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P3Org1, 1851L, opptjeningsårFeriepenger);

        var brRPeriode3 = buildBeregningsresultatPeriode(beregningsresultat2, b30fom, b30tom);
        var andelB3P4Org2 = buildBeregningsresultatAndel(brRPeriode3, true, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P4Org2, 251L, opptjeningsårFeriepenger.plusYears(1));
        buildBeregningsresultatAndel(brRPeriode3, true, 0, BigDecimal.valueOf(100), virksomhet);
        var andelB3P4Org1 = buildBeregningsresultatAndel(brRPeriode3, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger2, andelB3P4Org1, 1288L, opptjeningsårFeriepenger.plusYears(1));

        var brRPeriode4 = buildBeregningsresultatPeriode(beregningsresultat2, b40fom, b40tom);
        buildBeregningsresultatAndel(brRPeriode4, true, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatAndel(brRPeriode4, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brRPeriode4, false, 789, BigDecimal.valueOf(100), virksomhet);

        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultat2, feriepenger2);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering)));

        // ActoppdragRevurdering = {Oppdragskontroll@3699} "Oppdragskontroll<behandlingId=123456, saksnummer=Saksnummer<101000>, venterKvittering=true, prosessTaskId=23, opprettetTs=null>"
        var oppdragRevurdering2 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder3.build());

        // To arbeidsgivere som mottakere og bruker
        assertThat(oppdragRevurdering2.getOppdrag110Liste()).hasSize(2);

        //Assert -- opphør av bruker
        var opp150RevurderingListe = oppdragRevurdering2.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        // Bruker + FP
        assertThat(opp150RevurderingListe).hasSize(6).anySatisfy(linje -> assertThat(linje.gjelderOpphør()).isTrue());


        // Arrange 3 - tredje revurdering med omfordeling av andre ag til bruker
        var beregningsresultat3 = BeregningsresultatEntitet.builder().medRegelInput("clob1").medRegelSporing("clob2").build();

        var feriepenger3 = buildBeregningsresultatFeriepenger();

        var brR2Periode1 = buildBeregningsresultatPeriode(beregningsresultat3, b10fom, b10tom);
        buildBeregningsresultatAndel(brR2Periode1, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR2Periode1, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB4P1Org1 = buildBeregningsresultatAndel(brR2Periode1, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P1Org1, 1127L, opptjeningsårFeriepenger);
        var andelB4P1Org2 = buildBeregningsresultatAndel(brR2Periode1, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P1Org2, 220L, opptjeningsårFeriepenger);

        var brR2Periode2a = buildBeregningsresultatPeriode(beregningsresultat3, b20fom, b20tom);
        buildBeregningsresultatAndel(brR2Periode2a, true, 0, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR2Periode2a, true, 0, BigDecimal.valueOf(100), virksomhet2);
        var andelB4P2Org1 = buildBeregningsresultatAndel(brR2Periode2a, false, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P2Org1, 563L, opptjeningsårFeriepenger);
        var andelB4P2Org2 = buildBeregningsresultatAndel(brR2Periode2a, false, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P2Org2, 110L, opptjeningsårFeriepenger);

        var brR2Periode2b = buildBeregningsresultatPeriode(beregningsresultat3, b21fom, b21tom);
        var andelB4P3Org1 = buildBeregningsresultatAndel(brR2Periode2b, true, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P3Org1, 1851L, opptjeningsårFeriepenger);
        var andelB4P3Org2 = buildBeregningsresultatAndel(brR2Periode2b, true, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P3Org2, 361L, opptjeningsårFeriepenger);

        var brR2Periode3 = buildBeregningsresultatPeriode(beregningsresultat3, b30fom, b30tom);
        var andelB4P4Org1 = buildBeregningsresultatAndel(brR2Periode3, true, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P4Org1, 1288L, opptjeningsårFeriepenger.plusYears(1));
        var andelB4P4Org2 = buildBeregningsresultatAndel(brR2Periode3, true, 154, BigDecimal.valueOf(100), virksomhet2);
        buildBeregningsresultatFeriepengerPrÅr(feriepenger3, andelB4P4Org2, 251L, opptjeningsårFeriepenger.plusYears(1));

        var brR2Periode4 = buildBeregningsresultatPeriode(beregningsresultat3, b40fom, b40tom);
        buildBeregningsresultatAndel(brR2Periode4, true, 789, BigDecimal.valueOf(100), virksomhet);
        buildBeregningsresultatAndel(brR2Periode4, true, 154, BigDecimal.valueOf(100), virksomhet2);

        var gruppertYtelse4 = mapper.fordelPåNøkler(beregningsresultat3, feriepenger3);
        var builder4 = getInputStandardBuilder(gruppertYtelse4).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering, oppdragRevurdering2)));

        // Act
        var oppdragRevurdering3 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder4.build());

        // Assert 3 -- opphør AG og endring for bruker
        assertThat(oppdragRevurdering3.getOppdrag110Liste()).hasSize(2);
    }


    /**
     * Prodscenario der bruker suksessivt mister ytelse. Til man til slutt står uten og det skal sendes opphørsoppdrag
     */
    @Test
    void skalSendeOppdragMedOpphørNårAllInnvilgetYtelseBortfallerBrukerErOpphørtTidligere() {
        // Arrange
        var bminfom = LocalDate.of(I_ÅR, 7, 13);
        var bmaxtom = LocalDate.of(I_ÅR, 10, 23);
        var bmax2tom = LocalDate.of(I_ÅR, 12, 4);

        var beregningsresultat = buildBeregningsresultatBrukerFP(List.of(2116), List.of(0), bminfom, bmaxtom);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(0), List.of(2143), bminfom, bmaxtom);
        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert -- opphør av bruker
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .filter(l -> l.getUtbetalesTilId() != null)
            .toList();

        // Bruker + FP
        assertThat(opp150RevurderingListe).hasSize(2).allSatisfy(linje -> assertThat(linje.gjelderOpphør()).isTrue());

        // Arrange 2 -- opphør deler av AG
        var b21fom = LocalDate.of(I_ÅR, 8, 24);
        var b20tom = LocalDate.of(I_ÅR, 8, 23);

        var beregningsresultatRevurderingFP2 = buildBeregningsresultatBrukerFP(List.of(0, 0), List.of(0, 2143), bminfom, b20tom, b21fom, bmax2tom);

        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP2);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering)));

        // Act
        var oppdragRevurdering2 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder3.build());

        // Arrange 3 -- opphør enda mer AG
        var b30tom = LocalDate.of(I_ÅR, 8, 23);
        var b31fom = LocalDate.of(I_ÅR, 8, 24);
        var b31tom = LocalDate.of(I_ÅR, 8, 31);
        var b32fom = LocalDate.of(I_ÅR, 9, 1);
        var b32tom = LocalDate.of(I_ÅR, 10, 18);
        var b33fom = LocalDate.of(I_ÅR, 10, 19);

        var beregningsresultatRevurderingFP3 = buildBeregningsresultatBrukerFP(List.of(0, 0, 0, 0), List.of(0, 0, 2143, 0), bminfom, b30tom, b31fom,
            b31tom, b32fom, b32tom, b33fom, bmax2tom);

        var gruppertYtelse4 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP3);
        var builder4 = getInputStandardBuilder(gruppertYtelse4).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering, oppdragRevurdering2)));

        // Act
        var oppdragRevurdering3 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder4.build());


        // Arrange 4 -- opphør enda mer AG
        var b41fom = LocalDate.of(I_ÅR, 9, 1);
        var b41tom = LocalDate.of(I_ÅR, 10, 2);
        var b42fom = LocalDate.of(I_ÅR, 10, 3);
        var b42tom = LocalDate.of(I_ÅR, 10, 18);
        var b43fom = LocalDate.of(I_ÅR, 10, 19);

        var beregningsresultatRevurderingFP4 = buildBeregningsresultatBrukerFP(List.of(0, 0, 0), List.of(0, 2143, 0), b41fom, b41tom, b42fom, b42tom,
            b43fom, bmax2tom);
        var gruppertYtelse5 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP4);
        var builder5 = getInputStandardBuilder(gruppertYtelse5).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering, oppdragRevurdering2, oppdragRevurdering3)));

        // Act
        var oppdragRevurdering4 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder5.build());


        // Arrange 5 -- opphør resten av AG
        var b51fom = LocalDate.of(I_ÅR, 10, 3);
        var b51tom = LocalDate.of(I_ÅR, 10, 18);
        var b52fom = LocalDate.of(I_ÅR, 10, 19);
        var b52tom = LocalDate.of(I_ÅR, 10, 23);
        var b53fom = LocalDate.of(I_ÅR, 10, 24);

        var beregningsresultatRevurderingFP5 = buildBeregningsresultatBrukerFP(List.of(0, 0, 0), List.of(0, 0, 0), b51fom, b51tom, b52fom, b52tom,
            b53fom, bmax2tom);
        var gruppertYtelse6 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP5);
        var builder6 = getInputStandardBuilder(gruppertYtelse6).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering, oppdragRevurdering2, oppdragRevurdering3, oppdragRevurdering4)));

        // Act
        var oppdragRevurdering5 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder6.build());

        //Assert 5 -- alt opphøres
        var opp150RevurderingListe5 = oppdragRevurdering5.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        // AG +  FP
        assertThat(opp150RevurderingListe5).hasSize(2).allSatisfy(l -> assertThat(l.gjelderOpphør()).isTrue());

    }

    /**
     * Prodscenario med omfordeling fra delvis ref til kun direkte utbetaling og så opphør
     */
    @Test
    void skalSendeOppdragMedOpphørNårAllInnvilgetYtelseBortfallerArbeidsgiverErOpphørtTidligere() {
        // Arrange
        var bminfom = LocalDate.of(I_ÅR, 3, 23);
        var bmaxtom = LocalDate.of(I_ÅR, 7, 3);

        var beregningsresultat = buildBeregningsresultatBrukerFP(List.of(897), List.of(1265), bminfom, bmaxtom);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse);
        var originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder.build());

        var beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(List.of(2162), List.of(0), bminfom, bmaxtom);

        var gruppertYtelse2 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP);
        var builder2 = getInputStandardBuilder(gruppertYtelse2).medTidligereOppdrag(mapTidligereOppdrag(List.of(originaltOppdrag)));

        // Act
        var oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder2.build());

        //Assert -- opphør av ag
        var opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .filter(l -> l.getUtbetalesTilId() == null)
            .toList();

        // AG + FP
        assertThat(opp150RevurderingListe).hasSize(2).allSatisfy(linje -> assertThat(linje.gjelderOpphør()).isTrue());

        // Arrange 2 -- opphør deler av AG

        var beregningsresultatRevurderingFP3 = buildBeregningsresultatBrukerFP(List.of(0), List.of(0), bminfom, bmaxtom);
        var gruppertYtelse3 = mapper.fordelPåNøkler(beregningsresultatRevurderingFP3);
        var builder3 = getInputStandardBuilder(gruppertYtelse3).medTidligereOppdrag(
            mapTidligereOppdrag(List.of(originaltOppdrag, oppdragRevurdering)));

        // Act
        var oppdragRevurdering2 = OppdragMedPositivKvitteringTestUtil.opprett(nyOppdragskontrollTjeneste, builder3.build());


        //Assert 2 -- alt opphøres
        var opp150RevurderingListe5 = oppdragRevurdering2.getOppdrag110Liste()
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .filter(l -> l.getUtbetalesTilId() != null)
            .toList();

        // AG +  FP
        assertThat(opp150RevurderingListe5).hasSize(2).allSatisfy(l -> assertThat(l.gjelderOpphør()).isTrue());

    }


    private List<Oppdragslinje150> sortOppdragslinj150Liste(List<Oppdragslinje150> opp150OpphForDPListe) {
        return opp150OpphForDPListe.stream().sorted(Comparator.comparing(Oppdragslinje150::getDelytelseId)).toList();
    }

    private void verifiserOpp150NårDetErFlereArbeidsgivereSomMottaker(LocalDate datoStatusFom, List<Oppdragslinje150> revurderingOpp150Arbgvr) {
        assertThat(revurderingOpp150Arbgvr).hasSize(5);
        var opp150ForOpph = revurderingOpp150Arbgvr.stream().filter(Oppdragslinje150::gjelderOpphør).findFirst().get();
        var opp150FomEndringsdato = revurderingOpp150Arbgvr.stream().filter(opp150 -> !opp150.gjelderOpphør()).toList();
        assertThat(opp150ForOpph.getDatoStatusFom()).isEqualTo(datoStatusFom);
        assertThat(opp150ForOpph.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
        assertThat(opp150ForOpph.getRefusjonsinfo156().getRefunderesId()).isIn(
            List.of(endreTilElleveSiffer(virksomhet), endreTilElleveSiffer(virksomhet2)));
        assertThat(opp150FomEndringsdato).hasSize(4);
        var delytelseIdList = opp150FomEndringsdato.stream().map(Oppdragslinje150::getDelytelseId).collect(Collectors.toList());
        delytelseIdList.add(opp150ForOpph.getDelytelseId());
        Set<Long> delytelseIdSet = Sets.newHashSet(delytelseIdList);
        assertThat(delytelseIdList).hasSize(delytelseIdSet.size());
    }

    private void verifiserKjeding(List<Oppdragslinje150> originaltOpp150Liste, List<Oppdragslinje150> opp150RevurdListe) {
        var opp150ForSNOriginalListe = getOpp150MedKodeklassifik(originaltOpp150Liste, KodeKlassifik.FPF_SELVSTENDIG);
        var opp150ForSNRevurdListe = getOpp150MedKodeklassifik(opp150RevurdListe, KodeKlassifik.FPF_SELVSTENDIG);
        var opp150ForFLRevurdListe = getOpp150MedKodeklassifik(opp150RevurdListe, KodeKlassifik.FPF_FRILANSER);

        assertThat(opp150ForFLRevurdListe).isEmpty();
        assertThat(opp150ForSNRevurdListe).hasSize(1);
        var opp150SN = opp150ForSNRevurdListe.get(0);
        assertThat(opp150SN.gjelderOpphør()).isTrue();
        assertThat(opp150ForSNOriginalListe).anySatisfy(opp150 -> assertThat(opp150.getDelytelseId()).isEqualTo(opp150SN.getDelytelseId()));
    }

    private List<Oppdragslinje150> getOpp150MedKodeklassifik(List<Oppdragslinje150> opp150RevurdListe, KodeKlassifik kodeKlassifik) {
        return opp150RevurdListe.stream().filter(opp150 -> kodeKlassifik.equals(opp150.getKodeKlassifik())).toList();
    }

    private void verifiserKodeklassifik(List<Oppdragslinje150> originaltOpp150Liste, List<Oppdragslinje150> opp150RevurdListe) {
        var kodeKlassifikForrigeListe = OppdragskontrollTestVerktøy.getKodeklassifikIOppdr150Liste(originaltOpp150Liste);
        var kodeKlassifikRevurderingListe = OppdragskontrollTestVerktøy.getKodeklassifikIOppdr150Liste(opp150RevurdListe);
        var kodeKlassifikRevurderingOpphListe = OppdragskontrollTestVerktøy.getKodeklassifikKunForOpp150MedOpph(opp150RevurdListe);
        assertThat(kodeKlassifikForrigeListe).containsAnyElementsOf(List.of(KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_ARBEIDSTAKER));
        assertThat(kodeKlassifikRevurderingListe).containsAnyElementsOf(List.of(KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_ARBEIDSTAKER));
        assertThat(kodeKlassifikRevurderingOpphListe).containsAnyElementsOf(List.of(KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_ARBEIDSTAKER));
    }

    private void verifiserKodeklassifikNårRevurderingHarNye(List<Oppdragslinje150> originaltOpp150Liste, List<Oppdragslinje150> opp150RevurdListe) {
        var kodeKlassifikForrigeListe = OppdragskontrollTestVerktøy.getKodeklassifikIOppdr150Liste(originaltOpp150Liste);
        var kodeKlassifikRevurderingListe = OppdragskontrollTestVerktøy.getKodeklassifikIOppdr150Liste(opp150RevurdListe);
        var kodeKlassifikRevurderingOpphListe = OppdragskontrollTestVerktøy.getKodeklassifikKunForOpp150MedOpph(opp150RevurdListe);
        assertThat(kodeKlassifikForrigeListe).containsAnyElementsOf(List.of(KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_SELVSTENDIG));
        assertThat(kodeKlassifikRevurderingListe).containsAnyElementsOf(List.of(KodeKlassifik.FPF_ARBEIDSTAKER, KodeKlassifik.FPF_SELVSTENDIG));
        assertThat(kodeKlassifikRevurderingOpphListe).containsExactly(KodeKlassifik.FPF_SELVSTENDIG);
    }


    private void verifiserOppdragslinje150_ENDR(Oppdragskontroll oppdragskontroll,
                                                List<Oppdragslinje150> originaltOpp150Liste,
                                                boolean medFeriepenger,
                                                boolean medFlereKlassekode,
                                                int gradering) {
        var opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragskontroll);

        verifiserOppdr150SomErOpphørt(opp150RevurdListe, originaltOpp150Liste, medFeriepenger, medFlereKlassekode, false);
        verifiserOppdr150SomErNy(opp150RevurdListe, originaltOpp150Liste, List.of(gradering));
    }

    private void verifiserOppdr150SomErUendret(Oppdragskontroll oppdrag) {
        var opp150RevurdListe = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdrag);
        var opp150VirksomhetListe = opp150RevurdListe.stream()
            .filter(oppdragslinje150 -> oppdragslinje150.getRefusjonsinfo156() != null)
            .filter(oppdragslinje150 -> oppdragslinje150.getRefusjonsinfo156()
                .getRefunderesId()
                .equals(OppdragskontrollTestVerktøy.endreTilElleveSiffer(virksomhet)))
            .filter(oppdragslinje150 -> oppdragslinje150.getKodeKlassifik().equals(KodeKlassifik.FPF_FERIEPENGER_AG))
            .toList();
        assertThat(opp150VirksomhetListe).isEmpty();
    }
}
