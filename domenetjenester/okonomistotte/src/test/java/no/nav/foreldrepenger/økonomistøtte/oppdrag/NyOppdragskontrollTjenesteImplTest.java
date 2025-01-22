package no.nav.foreldrepenger.økonomistøtte.oppdrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.TilkjentYtelseMapper;

class NyOppdragskontrollTjenesteImplTest extends NyOppdragskontrollTjenesteTestBase {

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void skalSendeOppdragUtenFeriepenger() {
        // Arrange
        var gruppertYtelse = buildTilkjentYtelseFP();

        var input = getInputStandardBuilder(gruppertYtelse).build();

        // Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(input);

        // Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            verifiserOppdragGrunnlag(ok, PROSESS_TASK_ID);

            // Verifiser andeler
            var mottakere110 = ok.getOppdrag110Liste();
            varifiserMottakere110(mottakere110, gruppertYtelse.getNøkler().size(), List.of(KodeFagområde.FP, KodeFagområde.FPREF));

            // Verifiser utbetalinger
            verifiserUtbetalingene150(mottakere110, List.of(KodeKlassifik.FPF_ARBEIDSTAKER, KodeKlassifik.FPF_REFUSJON_AG));

        } else {
            fail();
        }
    }

    /**
     * Førstegangsbehandling: BehandlingVedtak=Innvilget, BehandlingResultat=Innvilget, Finnes tilkjent ytelse=Nei(dvs. Stortingsansatt)
     * Revurdering: BehandlingVedtak=Innvilget, BehandlingResultat=Foreldrepenger endret, Finnes tilkjent ytelse=Ja
     */
    @Test
    void skalSendeFørstegangsOppdragIRevurderingNårOriginalErInnvilgetOgFinnesIkkeTilkjentYtelseIOriginal() {
        // Arrange
        var gruppertYtelse = GruppertYtelse.builder()
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.FPF_ARBEIDSTAKER, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 3, Satsen.dagsats(500), 100))
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 4, 20, Satsen.dagsats(2500), 100))
                    .build())
            .build();

        var inputBuilder = getInputStandardBuilder(gruppertYtelse);

        // Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        // Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            verifiserOppdragGrunnlag(ok, PROSESS_TASK_ID);

            // Verifiser andeler
            var mottakere110 = ok.getOppdrag110Liste();
            varifiserMottakere110(mottakere110, gruppertYtelse.getNøkler().size(), List.of(KodeFagområde.FP));

            // Verifiser utbetalinger
            verifiserUtbetalingene150(mottakere110, List.of(KodeKlassifik.FPF_ARBEIDSTAKER));

            var oppdragLinjer150 = mottakere110.stream()
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .toList();

            verifyOpp150NårFørstegangsoppdragBlirSendtIRevurdering(oppdragLinjer150);
        } else {
            fail();
        }
    }

    /**
     * Førstegangsbehandling: BehandlingVedtak=Avslag, BehandlingResultat=Opphør, Finnes tilkjent ytelse=Nei
     * Revurdering: BehandlingVedtak=Innvilget, BehandlingResultat=Innvilget, Finnes tilkjent ytelse=Ja
     */
    @Test
    void skalSendeFørstegangsOppdragIRevurderingNårOriginalErAvslagOgFinnesIkkeTilkjentYtelseIOriginal() {
        // Arrange
        var gruppertYtelse = GruppertYtelse.builder()
            .leggTilKjede(
                KjedeNøkkel.lag(KodeKlassifik.SVP_ARBEDISTAKER, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 0, 3, Satsen.dagsats(500), 100))
                    .leggTilPeriode(lagPeriode(VEDTAKSDATO, 4, 20, Satsen.dagsats(2500), 100))
                    .build())
            .build();

        var inputBuilder = getInputStandardBuilder(gruppertYtelse)
            .medFagsakYtelseType(FagsakYtelseType.SVANGERSKAPSPENGER);

        // Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        // Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            verifiserOppdragGrunnlag(ok, PROSESS_TASK_ID);

            // Verifiser andeler
            var mottakere110 = ok.getOppdrag110Liste();
            varifiserMottakere110(mottakere110, gruppertYtelse.getNøkler().size(), List.of(KodeFagområde.SVP));

            // Verifiser utbetalinger
            verifiserUtbetalingene150(mottakere110, List.of(KodeKlassifik.SVP_ARBEDISTAKER));

            var oppdragLinjer150 = mottakere110.stream()
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .toList();

            verifyOpp150NårFørstegangsoppdragBlirSendtIRevurdering(oppdragLinjer150);
        } else {
            fail();
        }
    }

    @Test
    void skalSendeOppdragMedFlereInntektskategoriIFørstegangsbehandling() {
        // Arrange
        var gruppertYtelse = buildTilkjentYtelseMedFlereInntektskategoriFP(true);
        var inputBuilder = getInputStandardBuilder(gruppertYtelse);

        // Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        // Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            verifiserOppdragGrunnlag(ok, PROSESS_TASK_ID);

            // Verifiser andeler
            var mottakere110 = ok.getOppdrag110Liste();
            varifiserMottakere110(mottakere110, gruppertYtelse.getBetalingsmottakere().size(), List.of(KodeFagområde.FP, KodeFagområde.FPREF));

            // Verifiser utbetalinger
            verifiserUtbetalingene150(mottakere110, List.of(KodeKlassifik.FPF_ARBEIDSTAKER, KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_REFUSJON_AG, KodeKlassifik.FERIEPENGER_BRUKER, KodeKlassifik.FPF_FERIEPENGER_AG));

            //Assert
            OppdragskontrollTestVerktøy.verifiserAvstemming(ok);
            verifiserOppdragslinje150MedFlereKlassekode(ok);
        } else {
            fail();
        }
    }

    @Test
    void skalSendeOppdragMedFlereArbeidsgiverSomMottakerIFørstegangsbehandling() {
        // Arrange
        var gruppertYtelse = buildTilkjentYtelseMedFlereAndelerSomArbeidsgiver();
        var inputBuilder = getInputStandardBuilder(gruppertYtelse);

        // Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        // Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            verifiserOppdragGrunnlag(ok, PROSESS_TASK_ID);

            // Verifiser andeler
            var mottakere110 = ok.getOppdrag110Liste();
            varifiserMottakere110(mottakere110, gruppertYtelse.getBetalingsmottakere().size(), List.of(KodeFagområde.FP, KodeFagområde.FPREF));

            // Verifiser utbetalinger
            verifiserUtbetalingene150(mottakere110, List.of(KodeKlassifik.FPF_ARBEIDSTAKER, KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_REFUSJON_AG, KodeKlassifik.FPF_SELVSTENDIG));

            //Assert
            OppdragskontrollTestVerktøy.verifiserAvstemming(ok);
            verifiserOppdragslinje150MedFlereKlassekode(ok);
        } else {
            fail();
        }
    }

    @Test
    void skalSendeFørstegangsoppdragHvorBrukerOgArbeidsgiverErMottakerOgBrukerHarFlereAndeler() {
        // Arrange
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        var brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 7);
        buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(80), virksomhet);

        var brPeriode3 = buildBeregningsresultatPeriode(beregningsresultat, 16, 22);
        buildBeregningsresultatAndel(brPeriode3, true, 0, BigDecimal.valueOf(80), virksomhet3);

        var brPeriode4 = buildBeregningsresultatPeriode(beregningsresultat, 23, 30);
        buildBeregningsresultatAndel(brPeriode4, false, 2160, BigDecimal.valueOf(80), virksomhet3);
        buildBeregningsresultatAndel(brPeriode4, false, 0, BigDecimal.valueOf(80), virksomhet3);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();

            verifiserOppdragslinje150MedFlereKlassekode(ok);
            var oppdragslinje150Liste = ok.getOppdrag110Liste().stream()
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .toList();
            assertThat(oppdragslinje150Liste).hasSize(2);
            assertThat(oppdragslinje150Liste.stream()
                .anyMatch(odl150 -> SimpleLocalDateInterval.fraOgMedTomNotNull(odl150.getDatoVedtakFom(), odl150.getDatoVedtakTom())
                    .equals(SimpleLocalDateInterval.fraOgMedTomNotNull(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(23), NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(30))))).isTrue();
            assertThat(oppdragslinje150Liste.stream()
                .anyMatch(odl150 -> SimpleLocalDateInterval.fraOgMedTomNotNull(odl150.getDatoVedtakFom(), odl150.getDatoVedtakTom())
                    .equals(SimpleLocalDateInterval.fraOgMedTomNotNull(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(16), NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(22))))).isFalse();
        }
    }

    @Test
    void skalOppretteFørstegangsoppdragFP() {
        // Arrange
        var gruppertYtelse = buildTilkjentYtelseFP();
        var builder = getInputStandardBuilder(gruppertYtelse);

        // Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        // Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            verifiserOppdragGrunnlag(ok, PROSESS_TASK_ID);

            // Assert
            assertThat(ok).isNotNull();

            var oppdrag110Liste = ok.getOppdrag110Liste();
            assertThat(oppdrag110Liste).isNotNull();
            for (var oppdrag110Lest : oppdrag110Liste) {
                assertThat(oppdrag110Lest.getOppdragslinje150Liste()).isNotNull();
                assertThat(oppdrag110Lest.getAvstemming()).isNotNull();
                assertThat(oppdrag110Lest.getOmpostering116()).isNotPresent();

                var oppdrlinje150Liste = oppdrag110Lest.getOppdragslinje150Liste();
                for (var oppdrlinje150 : oppdrlinje150Liste) {
                    assertThat(oppdrlinje150).isNotNull();
                    assertThat(oppdrlinje150.getOppdrag110()).isNotNull();
                }
            }

            // Verifiser andeler
            var mottakere110 = ok.getOppdrag110Liste();
            varifiserMottakere110(mottakere110, gruppertYtelse.getBetalingsmottakere().size(), List.of(KodeFagområde.FP, KodeFagområde.FPREF));

            // Verifiser utbetalinger
            verifiserUtbetalingene150(mottakere110, List.of(KodeKlassifik.FPF_ARBEIDSTAKER, KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_REFUSJON_AG, KodeKlassifik.FPF_SELVSTENDIG));

        } else {
            fail();
        }

    }

    @Test
    void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdHosPrivatPersonOgUtbetalingGårTilBruker() {
        //Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 1000, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), null);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 1000, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), null);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            //Assert
            var oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(1);
            assertThat(oppdrag110List.get(0).getOmpostering116()).isNotPresent();
            var oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
            assertThat(oppdragslinje150List).hasSize(1);
            assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1500L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
        }
    }

    @Test
    void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdHosPrivatPersonOgUtbetalingGårTilPrivatArbeidsgiver() {
        //Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 1000, BigDecimal.valueOf(100L), null);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 20, 28);
        buildBeregningsresultatAndel(brPeriode_2, true, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100L), null);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            var oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(1);
            var oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
            assertThat(oppdragslinje150List).hasSize(2);
            assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1000L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
        }
    }

    @Test
    void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdHosPrivatPersonOgUtbetalingGårTilBådePrivatArbeidsgiverOgBruker() {
        //Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), null);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), null);

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            var oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(1);
            var oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
            assertThat(oppdragslinje150List).hasSize(1);
            assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1000L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
        }
    }

    @Test
    void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdBådeHosPrivatArbeidsgiverOgEnOrganisasjonOgUtbetalingGårTilBruker() {
        //Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 0, BigDecimal.valueOf(100L), virksomhet);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, true, 500, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 0, BigDecimal.valueOf(100L), virksomhet);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();

            var oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(1);
            var oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
            assertThat(oppdragslinje150List).hasSize(1);
            assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1000L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
        }
    }

    @Test
    void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdBådeHosEnPrivatArbeidsgiverOgEnOrganisasjonOgUtbetalingGårTilBeggeToArbeidsgivere() {
        //Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, true, 0, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), virksomhet);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 20, 28);
        buildBeregningsresultatAndel(brPeriode_2, true, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, true, 0, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), virksomhet);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            var oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(2);
            //Oppdrag110 for bruker/privat arbeidsgiver
            var oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdrag110List);
            assertThat(oppdrag110Bruker).isNotNull();
            //Oppdrag110 for arbeidsgiver
            var oppdrag110Virksomhet = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdrag110List, virksomhet);
            assertThat(oppdrag110Virksomhet).isNotNull();
            //Oppdragslinj150 for bruker/privat arbeidsgiver
            var opp150ListPrivatArbgvr = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110List);
            assertThat(opp150ListPrivatArbgvr).hasSize(2);
            assertThat(opp150ListPrivatArbgvr).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(500L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
            //Oppdragslinj150 for arbeidsgiver
            var opp150ListVirksomhet = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdrag110List, virksomhet);
            assertThat(opp150ListVirksomhet).hasSize(2);
            assertThat(opp150ListVirksomhet).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(500L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
            });
        }
    }

    @Test
    void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdBådeHosEnPrivatArbeidsgiverOgEnOrganisasjonOgUtbetalingGårTilAlle() {
        //Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), virksomhet);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, true, 500, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), virksomhet);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            var oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(2);
            //Oppdrag110 for bruker/privat arbeidsgiver
            var oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdrag110List);
            assertThat(oppdrag110Bruker).isNotNull();
            //Oppdrag110 for arbeidsgiver
            var oppdrag110Virksomhet = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdrag110List, virksomhet);
            assertThat(oppdrag110Virksomhet).isNotNull();
            //Oppdragslinj150 for bruker/privat arbeidsgiver
            var opp150ListPrivatArbgvr = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110List);
            assertThat(opp150ListPrivatArbgvr).hasSize(1);
            assertThat(opp150ListPrivatArbgvr).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1500L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
            //Oppdragslinj150 for arbeidsgiver
            var opp150ListVirksomhet = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdrag110List, virksomhet);
            assertThat(opp150ListVirksomhet).hasSize(1);
            assertThat(opp150ListVirksomhet).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(500L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
            });
        }
    }

    @Test
    void skalSendeFørstegangsoppdragForAdopsjonMedTilsvarendeKlassekode() {
        // Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), virksomhet);
        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.ADOPSJON);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            // Bruker
            var opp150ListeForBruker = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(ok.getOppdrag110Liste());
            assertThat(opp150ListeForBruker).hasSize(1);
            assertThat(opp150ListeForBruker.get(0).getKodeKlassifik()).isEqualTo(KodeKlassifik.FPA_ARBEIDSTAKER);
            // Arbeidsgiver
            var opp150ListeForArbgvr = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(ok.getOppdrag110Liste(), virksomhet);
            assertThat(opp150ListeForArbgvr).hasSize(1);
            assertThat(opp150ListeForArbgvr.get(0).getKodeKlassifik()).isEqualTo(KodeKlassifik.FPA_REFUSJON_AG);
        }
    }

    @Test
    void skal_sende_oppdrag_for_svangerskapspenger() {
        //Arrange
        var beregningsresultat = buildEmptyBeregningsresultatFP();
        var brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        var andelBruker_1 = buildBeregningsresultatAndel(brPeriode_1, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        var andelArbeidsgiver_1 = buildBeregningsresultatAndel(brPeriode_1, false, 1000, BigDecimal.valueOf(100L), virksomhet);
        var brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 1000, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100L), virksomhet);
        var feriepenger = buildBeregningsresultatFeriepenger();
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelBruker_1, 10000L, LocalDate.of(2018, 12, 31));
        buildBeregningsresultatFeriepengerPrÅr(feriepenger, andelArbeidsgiver_1, 10000L, LocalDate.of(2018, 12, 31));

        var mapper = new TilkjentYtelseMapper(FamilieYtelseType.SVANGERSKAPSPENGER);
        var gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);
        var builder = getInputStandardBuilder(gruppertYtelse)
            .medFagsakYtelseType(FagsakYtelseType.SVANGERSKAPSPENGER);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();

            assertThat(oppdragskontroll).isNotNull();
            var oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(2);
            //Oppdrag110 - Bruker
            var oppdrag110_Bruker = oppdrag110List.stream()
                .filter(o110 -> !o110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver())
                .findFirst();
            assertThat(oppdrag110_Bruker).isPresent();
            //Oppdrag110 - Arbeidsgiver
            var oppdrag110_Arbeidsgiver = oppdrag110List.stream()
                .filter(o110 -> o110.getKodeFagomrade().gjelderRefusjonTilArbeidsgiver())
                .findFirst();
            assertThat(oppdrag110_Arbeidsgiver).isPresent();
            //Oppdragslinje150 - Bruker
            var opp150List_Bruker = oppdrag110_Bruker.get().getOppdragslinje150Liste();
            assertThat(opp150List_Bruker).anySatisfy(opp150 ->
                assertThat(opp150.getKodeKlassifik()).isIn(Arrays.asList(
                    KodeKlassifik.SVP_ARBEDISTAKER,
                    KodeKlassifik.FERIEPENGER_BRUKER.getKode())
                ));
            //Oppdragslinje150 - Arbeidsgiver
            var opp150List_Arbeidsgiver = oppdrag110_Arbeidsgiver.get().getOppdragslinje150Liste();
            assertThat(opp150List_Arbeidsgiver).anySatisfy(opp150 ->
                assertThat(opp150.getKodeKlassifik()).isIn(Arrays.asList(
                    KodeKlassifik.SVP_REFUSJON_AG,
                    KodeKlassifik.SVP_FERIEPENGER_AG)
                ));
        }
    }

    private void verifyOpp150NårFørstegangsoppdragBlirSendtIRevurdering(List<Oppdragslinje150> oppdragslinje150List) {
        assertThat(oppdragslinje150List).isNotEmpty();
        assertThat(oppdragslinje150List).allSatisfy(oppdragslinje150 -> {
            assertThat(oppdragslinje150.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.NY);
            assertThat(oppdragslinje150.getRefusjonsinfo156()).isNull();
            assertThat(oppdragslinje150.getUtbetalingsgrad()).isNotNull();
        });
    }

    private void verifiserOppdragslinje150MedFlereKlassekode(Oppdragskontroll oppdrag) {
        var oppdr150ListeAT = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(oppdrag, KodeKlassifik.FPF_ARBEIDSTAKER);
        var oppdr150ListeFL = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(oppdrag, KodeKlassifik.FPF_FRILANSER);

        OppdragskontrollTestVerktøy.verifiserKjedingForOppdragslinje150(oppdr150ListeAT, oppdr150ListeFL);
    }

    private void verifiserAvstemming(List<Oppdrag110> oppdrag110Liste) {
        assertThat(oppdrag110Liste).isNotEmpty().allSatisfy(oppdrag110 -> {
            var avstemming = oppdrag110.getAvstemming();
            assertThat(avstemming).isNotNull();
            assertThat(avstemming.getNøkkel()).isNotNull();
            assertThat(avstemming.getNøkkel()).isEqualTo(avstemming.getTidspunkt());
        });
    }

    private void verifiserOppdragGrunnlag(Oppdragskontroll oppdragGrunnlag, Long prosessTaskId) {
        assertThat(oppdragGrunnlag.getSaksnummer()).isEqualTo(SAKSNUMMER);
        assertThat(oppdragGrunnlag.getVenterKvittering()).isEqualTo(Boolean.TRUE);
        assertThat(oppdragGrunnlag.getProsessTaskId()).isEqualTo(prosessTaskId);
    }


    private void verifiserUtbetalingene150(List<Oppdrag110> mottakere110, List<KodeKlassifik> forventetKodeKlasifikk) {
        var oppdragLinjer150 = mottakere110.stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .toList();

        assertThat(oppdragLinjer150.stream()).isNotEmpty().allSatisfy(periode150 -> {
            assertThat(periode150.getKodeKlassifik()).isIn(forventetKodeKlasifikk);
            assertThat(periode150.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.NY);
            assertThat(periode150.getTypeSats()).isEqualTo(periode150.getKodeKlassifik().gjelderFeriepenger() ? TypeSats.ENG : TypeSats.DAG);
            assertThat(periode150.getVedtakId()).isEqualTo(VEDTAKSDATO.toString());
            if (!periode150.getKodeKlassifik().gjelderFeriepenger()) {
                assertThat(periode150.getUtbetalingsgrad()).isNotNull();
            }
            assertThat(periode150.getDelytelseId()).isNotNull();
            assertThat(periode150.getDatoVedtakFom()).isNotNull();
            assertThat(periode150.getDatoVedtakTom()).isNotNull();
            assertThat(periode150.getSats()).isNotNull();
            if (null != periode150.getUtbetalesTilId()) {
                assertThat(periode150.getRefusjonsinfo156()).isNull();
                assertThat(periode150.getUtbetalesTilId()).isEqualTo(OppdragskontrollTestVerktøy.endreTilElleveSiffer(BRUKER_FNR));
            } else {
                assertThat(periode150.getUtbetalesTilId()).isNull();
                var ref156 = periode150.getRefusjonsinfo156();
                assertThat(ref156.getRefunderesId()).isIn(
                    OppdragskontrollTestVerktøy.endreTilElleveSiffer(ARBEIDSFORHOLD_ID),
                    OppdragskontrollTestVerktøy.endreTilElleveSiffer(ARBEIDSFORHOLD_ID_2),
                    OppdragskontrollTestVerktøy.endreTilElleveSiffer(ARBEIDSFORHOLD_ID_3)
                );
            }
        });
    }

    private void varifiserMottakere110(List<Oppdrag110> mottakere110, int antallMottakere, List<KodeFagområde> kodeFagområder) {
        assertThat(mottakere110).isNotEmpty();
        assertThat(mottakere110).hasSize(antallMottakere);

        assertThat(mottakere110.stream().map(Oppdrag110::getKodeFagomrade).distinct()).containsExactlyInAnyOrderElementsOf(kodeFagområder);
        assertThat(mottakere110).allMatch(oppdrag110 -> oppdrag110.getOppdragGjelderId().equals(BRUKER_FNR));
        assertThat(mottakere110).allMatch(oppdrag110 -> oppdrag110.getSaksbehId().equals(ANSVARLIG_SAKSBEHANDLER));
        assertThat(mottakere110.stream().map(Oppdrag110::getFagsystemId)).allSatisfy(fagsystemId ->
            assertThat(String.valueOf(fagsystemId)).contains(SAKSNUMMER.getVerdi())
        );
        assertThat(mottakere110.stream().map(Oppdrag110::getFagsystemId).distinct().count()).isEqualTo(antallMottakere);
        verifiserAvstemming(mottakere110);
    }

}
