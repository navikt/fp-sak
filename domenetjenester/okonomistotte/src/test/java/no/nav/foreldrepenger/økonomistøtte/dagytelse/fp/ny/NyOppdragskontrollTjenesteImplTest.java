package no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.ny;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.IntervallUtil;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragskontrollTestVerktøy;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.Input;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.TilkjentYtelseMapper;

public class NyOppdragskontrollTjenesteImplTest extends NyOppdragskontrollTjenesteTestBase {

    public static final long PROSESS_TASK_ID = 23L;
    public static final String BRUKER_FNR = "12345678901";
    public static final Saksnummer SAKSNUMMER = Saksnummer.infotrygd("101000");
    public static final long BEHANDLING_ID = 123456L;
    public static final String ANSVARLIG_SAKSBEHANDLER = "Antonina";

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    public void skalSendeOppdragUtenFeriepenger() {
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
            varifiserMottakere110(mottakere110, gruppertYtelse.getNøkler().size(), List.of(ØkonomiKodeFagområde.FP.name(), ØkonomiKodeFagområde.FPREF.name()));

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
    public void skalSendeFørstegangsOppdragIRevurderingNårOriginalErInnvilgetOgFinnesIkkeTilkjentYtelseIOriginal() {
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
            varifiserMottakere110(mottakere110, gruppertYtelse.getNøkler().size(), List.of(ØkonomiKodeFagområde.FP.name()));

            // Verifiser utbetalinger
            verifiserUtbetalingene150(mottakere110, List.of(KodeKlassifik.FPF_ARBEIDSTAKER));

            var oppdragLinjer150 = mottakere110.stream()
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .collect(Collectors.toList());

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
    public void skalSendeFørstegangsOppdragIRevurderingNårOriginalErAvslagOgFinnesIkkeTilkjentYtelseIOriginal() {
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
            .medFagsakYtelseType(FagsakYtelseType.SVANGERSKAPSPENGER)
            .medFamilieYtelseType(FamilieYtelseType.SVANGERSKAPSPENGER);

        // Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(inputBuilder.build());

        // Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            verifiserOppdragGrunnlag(ok, PROSESS_TASK_ID);

            // Verifiser andeler
            var mottakere110 = ok.getOppdrag110Liste();
            varifiserMottakere110(mottakere110, gruppertYtelse.getNøkler().size(), List.of(ØkonomiKodeFagområde.SVP.name()));

            // Verifiser utbetalinger
            verifiserUtbetalingene150(mottakere110, List.of(KodeKlassifik.SVP_ARBEDISTAKER));

            var oppdragLinjer150 = mottakere110.stream()
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .collect(Collectors.toList());

            verifyOpp150NårFørstegangsoppdragBlirSendtIRevurdering(oppdragLinjer150);
        } else {
            fail();
        }
    }

    @Test
    public void skalSendeOppdragMedFlereInntektskategoriIFørstegangsbehandling() {
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
            varifiserMottakere110(mottakere110, gruppertYtelse.getBetalingsmottakere().size(), List.of(ØkonomiKodeFagområde.FP.name(), ØkonomiKodeFagområde.FPREF.name()));

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
    public void skalSendeOppdragMedFlereArbeidsgiverSomMottakerIFørstegangsbehandling() {
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
            varifiserMottakere110(mottakere110, gruppertYtelse.getBetalingsmottakere().size(), List.of(ØkonomiKodeFagområde.FP.name(), ØkonomiKodeFagområde.FPREF.name()));

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
    public void skalSendeFørstegangsoppdragHvorBrukerOgArbeidsgiverErMottakerOgBrukerHarFlereAndeler() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode brPeriode1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 7);
        buildBeregningsresultatAndel(brPeriode1, true, 1500, BigDecimal.valueOf(80), virksomhet);

        BeregningsresultatPeriode brPeriode3 = buildBeregningsresultatPeriode(beregningsresultat, 16, 22);
        buildBeregningsresultatAndel(brPeriode3, true, 0, BigDecimal.valueOf(80), virksomhet3);

        BeregningsresultatPeriode brPeriode4 = buildBeregningsresultatPeriode(beregningsresultat, 23, 30);
        buildBeregningsresultatAndel(brPeriode4, false, 2160, BigDecimal.valueOf(80), virksomhet3);
        buildBeregningsresultatAndel(brPeriode4, false, 0, BigDecimal.valueOf(80), virksomhet3);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();

            verifiserOppdragslinje150MedFlereKlassekode(ok);
            List<Oppdragslinje150> oppdragslinje150Liste = ok.getOppdrag110Liste().stream()
                .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
                .collect(Collectors.toList());
            assertThat(oppdragslinje150Liste).hasSize(2);
            assertThat(oppdragslinje150Liste.stream()
                .anyMatch(odl150 -> IntervallUtil.byggIntervall(odl150.getDatoVedtakFom(), odl150.getDatoVedtakTom())
                    .equals(IntervallUtil.byggIntervall(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(23), NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(30))))).isTrue();
            assertThat(oppdragslinje150Liste.stream()
                .anyMatch(odl150 -> IntervallUtil.byggIntervall(odl150.getDatoVedtakFom(), odl150.getDatoVedtakTom())
                    .equals(IntervallUtil.byggIntervall(NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(16), NyOppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(22))))).isFalse();
        }
    }

    @Test
    public void skalOppretteFørstegangsoppdragFP() {
        // Arrange
        GruppertYtelse gruppertYtelse = buildTilkjentYtelseFP();
        var builder = getInputStandardBuilder(gruppertYtelse);

        // Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        // Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            verifiserOppdragGrunnlag(ok, PROSESS_TASK_ID);

            // Assert
            assertThat(ok).isNotNull();

            List<Oppdrag110> oppdrag110Liste = ok.getOppdrag110Liste();
            assertThat(oppdrag110Liste).isNotNull();
            for (Oppdrag110 oppdrag110Lest : oppdrag110Liste) {
                assertThat(oppdrag110Lest.getOppdragslinje150Liste()).isNotNull();
                assertThat(oppdrag110Lest.getAvstemming()).isNotNull();
                assertThat(oppdrag110Lest.getOmpostering116()).isNotPresent();

                List<Oppdragslinje150> oppdrlinje150Liste = oppdrag110Lest.getOppdragslinje150Liste();
                for (Oppdragslinje150 oppdrlinje150 : oppdrlinje150Liste) {
                    assertThat(oppdrlinje150).isNotNull();
                    assertThat(oppdrlinje150.getOppdrag110()).isNotNull();
                }
            }

            // Verifiser andeler
            var mottakere110 = ok.getOppdrag110Liste();
            varifiserMottakere110(mottakere110, gruppertYtelse.getBetalingsmottakere().size(), List.of(ØkonomiKodeFagområde.FP.name(), ØkonomiKodeFagområde.FPREF.name()));

            // Verifiser utbetalinger
            verifiserUtbetalingene150(mottakere110, List.of(KodeKlassifik.FPF_ARBEIDSTAKER, KodeKlassifik.FPF_FRILANSER, KodeKlassifik.FPF_REFUSJON_AG, KodeKlassifik.FPF_SELVSTENDIG));

        } else {
            fail();
        }

    }

    @Test
    public void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdHosPrivatPersonOgUtbetalingGårTilBruker() {
        //Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 1000, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), null);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 1000, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), null);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            //Assert
            List<Oppdrag110> oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(1);
            assertThat(oppdrag110List.get(0).getOmpostering116()).isNotPresent();
            List<Oppdragslinje150> oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
            assertThat(oppdragslinje150List).hasSize(2);
            assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1500L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
        }
    }

    @Test
    public void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdHosPrivatPersonOgUtbetalingGårTilPrivatArbeidsgiver() {
        //Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 1000, BigDecimal.valueOf(100L), null);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 1000, BigDecimal.valueOf(100L), null);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            List<Oppdrag110> oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(1);
            List<Oppdragslinje150> oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
            assertThat(oppdragslinje150List).hasSize(2);
            assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1000L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
        }
    }

    @Test
    public void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdHosPrivatPersonOgUtbetalingGårTilBådePrivatArbeidsgiverOgBruker() {
        //Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), null);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), null);

        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            List<Oppdrag110> oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(1);
            List<Oppdragslinje150> oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
            assertThat(oppdragslinje150List).hasSize(2);
            assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1000L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
        }
    }

    @Test
    public void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdBådeHosPrivatArbeidsgiverOgEnOrganisasjonOgUtbetalingGårTilBruker() {
        //Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 0, BigDecimal.valueOf(100L), virksomhet);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, true, 500, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 0, BigDecimal.valueOf(100L), virksomhet);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();

            List<Oppdrag110> oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(1);
            List<Oppdragslinje150> oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
            assertThat(oppdragslinje150List).hasSize(2);
            assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1000L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
        }
    }

    @Test
    public void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdBådeHosEnPrivatArbeidsgiverOgEnOrganisasjonOgUtbetalingGårTilBeggeToArbeidsgivere() {
        //Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, true, 0, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), virksomhet);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 0, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, true, 0, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), virksomhet);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            List<Oppdrag110> oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(2);
            //Oppdrag110 for bruker/privat arbeidsgiver
            Oppdrag110 oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdrag110List);
            assertThat(oppdrag110Bruker).isNotNull();
            //Oppdrag110 for arbeidsgiver
            Oppdrag110 oppdrag110Virksomhet = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdrag110List, virksomhet);
            assertThat(oppdrag110Virksomhet).isNotNull();
            //Oppdragslinj150 for bruker/privat arbeidsgiver
            List<Oppdragslinje150> opp150ListPrivatArbgvr = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110List);
            assertThat(opp150ListPrivatArbgvr).hasSize(2);
            assertThat(opp150ListPrivatArbgvr).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(500L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
            //Oppdragslinj150 for arbeidsgiver
            List<Oppdragslinje150> opp150ListVirksomhet = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdrag110List, virksomhet);
            assertThat(opp150ListVirksomhet).hasSize(2);
            assertThat(opp150ListVirksomhet).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(500L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
            });
        }
    }

    @Test
    public void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdBådeHosEnPrivatArbeidsgiverOgEnOrganisasjonOgUtbetalingGårTilAlle() {
        //Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), virksomhet);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, true, 500, BigDecimal.valueOf(100L), virksomhet);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 500, BigDecimal.valueOf(100L), virksomhet);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.FØDSEL);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            List<Oppdrag110> oppdrag110List = ok.getOppdrag110Liste();
            assertThat(oppdrag110List).hasSize(2);
            //Oppdrag110 for bruker/privat arbeidsgiver
            Oppdrag110 oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdrag110List);
            assertThat(oppdrag110Bruker).isNotNull();
            //Oppdrag110 for arbeidsgiver
            Oppdrag110 oppdrag110Virksomhet = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdrag110List, virksomhet);
            assertThat(oppdrag110Virksomhet).isNotNull();
            //Oppdragslinj150 for bruker/privat arbeidsgiver
            List<Oppdragslinje150> opp150ListPrivatArbgvr = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110List);
            assertThat(opp150ListPrivatArbgvr).hasSize(2);
            assertThat(opp150ListPrivatArbgvr).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(1500L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_ARBEIDSTAKER);
            });
            //Oppdragslinj150 for arbeidsgiver
            List<Oppdragslinje150> opp150ListVirksomhet = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdrag110List, virksomhet);
            assertThat(opp150ListVirksomhet).hasSize(2);
            assertThat(opp150ListVirksomhet).allSatisfy(opp150 -> {
                assertThat(opp150.getSats()).isEqualTo(Sats.på(500L));
                assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FPF_REFUSJON_AG);
            });
        }
    }

    @Test
    public void skalSendeFørstegangsoppdragForAdopsjonMedTilsvarendeKlassekode() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), virksomhet);
        TilkjentYtelseMapper mapper = new TilkjentYtelseMapper(FamilieYtelseType.ADOPSJON);
        GruppertYtelse gruppertYtelse = mapper.fordelPåNøkler(beregningsresultat);

        var builder = getInputStandardBuilder(gruppertYtelse).medFamilieYtelseType(FamilieYtelseType.ADOPSJON);

        //Act
        var oppdragskontroll = nyOppdragskontrollTjeneste.opprettOppdrag(builder.build());

        //Assert
        if (oppdragskontroll.isPresent()) {
            var ok = oppdragskontroll.get();
            // Bruker
            List<Oppdragslinje150> opp150ListeForBruker = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(ok.getOppdrag110Liste());
            assertThat(opp150ListeForBruker).hasSize(1);
            assertThat(opp150ListeForBruker.get(0).getKodeKlassifik()).isEqualTo(KodeKlassifik.FPA_ARBEIDSTAKER);
            // Arbeidsgiver
            List<Oppdragslinje150> opp150ListeForArbgvr = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(ok.getOppdrag110Liste(), virksomhet);
            assertThat(opp150ListeForArbgvr).hasSize(1);
            assertThat(opp150ListeForArbgvr.get(0).getKodeKlassifik()).isEqualTo(KodeKlassifik.FPA_REFUSJON_AG);
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
        List<Oppdragslinje150> oppdr150ListeAT = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(oppdrag, KodeKlassifik.FPF_ARBEIDSTAKER);
        List<Oppdragslinje150> oppdr150ListeFL = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(oppdrag, KodeKlassifik.FPF_FRILANSER);

        OppdragskontrollTestVerktøy.verifiserKjedingForOppdragslinje150(oppdr150ListeAT, oppdr150ListeFL);
    }

    private void verifiserAvstemming(List<Oppdrag110> oppdrag110Liste) {
        assertThat(oppdrag110Liste).allSatisfy(oppdrag110 -> {
            Avstemming avstemming = oppdrag110.getAvstemming();
            assertThat(avstemming).isNotNull();
            assertThat(avstemming.getNøkkel()).isNotNull();
            assertEquals(avstemming.getNøkkel(), avstemming.getTidspunkt());
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
            .collect(Collectors.toList());

        assertThat(oppdragLinjer150.stream()).allSatisfy(periode150 -> {
            assertThat(periode150.getKodeKlassifik()).isIn(forventetKodeKlasifikk);
            assertThat(periode150.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.NY);
            assertThat(periode150.getTypeSats()).isEqualTo(periode150.getKodeKlassifik().gjelderFeriepenger() ? TypeSats.ENGANG : TypeSats.DAGLIG);
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
                Refusjonsinfo156 ref156 = periode150.getRefusjonsinfo156();
                assertThat(ref156.getRefunderesId()).isIn(
                    OppdragskontrollTestVerktøy.endreTilElleveSiffer(ARBEIDSFORHOLD_ID),
                    OppdragskontrollTestVerktøy.endreTilElleveSiffer(ARBEIDSFORHOLD_ID_2),
                    OppdragskontrollTestVerktøy.endreTilElleveSiffer(ARBEIDSFORHOLD_ID_3)
                );
            }
        });
    }

    private void varifiserMottakere110(List<Oppdrag110> mottakere110, int antallMottakere, List<String> kodeFagområder) {
        assertThat(mottakere110).isNotEmpty();
        assertThat(mottakere110.size()).isEqualTo(antallMottakere);

        assertThat(mottakere110.stream().map(Oppdrag110::getKodeFagomrade).distinct()).containsExactlyInAnyOrderElementsOf(kodeFagområder);
        assertThat(mottakere110).allMatch(oppdrag110 -> oppdrag110.getOppdragGjelderId().equals(BRUKER_FNR));
        assertThat(mottakere110).allMatch(oppdrag110 -> oppdrag110.getSaksbehId().equals(ANSVARLIG_SAKSBEHANDLER));
        assertThat(mottakere110.stream().map(Oppdrag110::getFagsystemId)).allSatisfy(fagsystemId ->
            assertThat(String.valueOf(fagsystemId).contains(SAKSNUMMER.getVerdi())).isTrue()
        );
        assertThat(mottakere110.stream().map(Oppdrag110::getFagsystemId).distinct().count()).isEqualTo(antallMottakere);
        verifiserAvstemming(mottakere110);
    }

    private Input.Builder getInputStandardBuilder(GruppertYtelse gruppertYtelse) {
        return Input.builder()
            .medTilkjentYtelse(gruppertYtelse)
            .medTidligereOppdrag(OverordnetOppdragKjedeOversikt.TOM)
            .medBrukerFnr(BRUKER_FNR)
            .medBehandlingId(BEHANDLING_ID)
            .medSaksnummer(SAKSNUMMER)
            .medFagsakYtelseType(FagsakYtelseType.FORELDREPENGER)
            .medFamilieYtelseType(FamilieYtelseType.FØDSEL)
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medVedtaksdato(VEDTAKSDATO)
            .medBrukInntrekk(true)
            .medProsessTaskId(PROSESS_TASK_ID);
    }

}
