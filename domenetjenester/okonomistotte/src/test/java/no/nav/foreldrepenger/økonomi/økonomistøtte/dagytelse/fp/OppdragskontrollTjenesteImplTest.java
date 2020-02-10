package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.IntervallUtil;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragsenhet120;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;

public class OppdragskontrollTjenesteImplTest extends OppdragskontrollTjenesteImplBaseTest {

    @Override
    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void skalSendeOppdragUtenFeriepenger() {
        // Arrange
        final Long prosessTaskId = 23L;
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP();
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Act
        Oppdragskontroll oppdragskontroll = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), prosessTaskId).get();

        // Assert
        verifiserOppdragskontroll(oppdragskontroll, prosessTaskId);
        List<Oppdrag110> oppdrag110Liste = verifiserOppdrag110(oppdragskontroll);
        verifiserAvstemming115(oppdrag110Liste);
        verifiserOppdragsenhet120(oppdrag110Liste);
        List<Oppdragslinje150> oppdragslinje150Liste = verifiserOppdragslinje150(oppdrag110Liste);
        verifiserGrad170(oppdragslinje150Liste);
        verifiserRefusjonInfo156(oppdragslinje150Liste);
        verifiserAttestant180(oppdragslinje150Liste);
    }

    /**
     * Førstegangsbehandling: BehandlingVedtak=Innvilget, BehandlingResultat=Innvilget, Finnes tilkjent ytelse=Nei(dvs. Stortingsansatt)
     * Revurdering: BehandlingVedtak=Innvilget, BehandlingResultat=Foreldrepenger endret, Finnes tilkjent ytelse=Ja
     */
    @Test
    public void skalSendeFørstegangsOppdragIRevurderingNårOriginalErInnvilgetOgFinnesIkkeTilkjentYtelseIOriginal() {
        // Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate endringsdato = OppdragskontrollTjenesteImplBaseTest.DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 4);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), virksomhet);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 5, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 2500, BigDecimal.valueOf(100L), virksomhet);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragskontroll = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), 32L).get();

        // Assert
        verifiserOppdragskontroll(oppdragskontroll, 32L);
        List<Oppdrag110> oppdrag110Liste = verifiserOppdrag110(oppdragskontroll);
        assertThat(oppdrag110Liste).hasSize(1);
        verifiserAvstemming115(oppdrag110Liste);
        verifiserOppdragsenhet120(oppdrag110Liste);
        List<Oppdragslinje150> oppdragslinje150List = oppdrag110Liste.get(0).getOppdragslinje150Liste();
        verifyOpp150NårFørstegangsoppdragBlirSendtIRevurdering(oppdragslinje150List);
    }

    /**
     * Førstegangsbehandling: BehandlingVedtak=Avslag, BehandlingResultat=Opphør, Finnes tilkjent ytelse=Nei
     * Revurdering: BehandlingVedtak=Innvilget, BehandlingResultat=Innvilget, Finnes tilkjent ytelse=Ja
     */
    @Test
    public void skalSendeFørstegangsOppdragIRevurderingNårOriginalErAvslagOgFinnesIkkeTilkjentYtelseIOriginal() {
        // Arrange
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, false);
        LocalDate endringsdato = OppdragskontrollTjenesteImplBaseTest.DAGENS_DATO.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 1, 4);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), virksomhet);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, 5, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 2500, BigDecimal.valueOf(100L), virksomhet);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        var oppdragskontroll = oppdragskontrollTjeneste.opprettOppdrag(revurdering.getId(), 33L).get();

        // Assert
        verifiserOppdragskontroll(oppdragskontroll, 33L);
        List<Oppdrag110> oppdrag110Liste = verifiserOppdrag110(oppdragskontroll);
        verifiserAvstemming115(oppdrag110Liste);
        verifiserOppdragsenhet120(oppdrag110Liste);
        assertThat(oppdrag110Liste).hasSize(1);
        verifiserAvstemming115(oppdrag110Liste);
        verifiserOppdragsenhet120(oppdrag110Liste);
        List<Oppdragslinje150> oppdragslinje150List = oppdrag110Liste.get(0).getOppdragslinje150Liste();
        verifyOpp150NårFørstegangsoppdragBlirSendtIRevurdering(oppdragslinje150List);
    }

    @Test
    public void skalSendeOppdragMedFlereInntektskategoriIFørstegangsbehandling() {
        // Arrange
        final Long prosessTaskId = 23L;
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Act
        Oppdragskontroll oppdrag = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), prosessTaskId).get();

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming115(oppdrag);
        OppdragskontrollTestVerktøy.verifiserOppdragsenhet120(oppdrag);
        verifiserOppdrag110(oppdrag);
        verifiserOppdragslinje150MedFlereKlassekode(oppdrag);
    }

    @Test
    public void skalSendeOppdragMedFlereArbeidsgiverSomMottakerIFørstegangsbehandling() {
        // Arrange
        final Long prosessTaskId = 23L;
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatMedFlereAndelerSomArbeidsgiver();
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Act
        Oppdragskontroll oppdrag = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), prosessTaskId).get();

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming115(oppdrag);
        OppdragskontrollTestVerktøy.verifiserOppdragsenhet120(oppdrag);
        verifiserOppdrag110(oppdrag);
    }

    @Test
    public void skalSendeFørstegangsoppdragHvorBrukerOgArbeidsgiverErMottakerOgBrukerHarFlereAndeler() {
        // Arrange
        final Long prosessTaskId = 23L;
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

        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Act
        Oppdragskontroll oppdrag = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), prosessTaskId).get();

        //Assert
        verifiserOppdragslinje150MedFlereKlassekode(oppdrag);
        List<Oppdragslinje150> oppdragslinje150Liste = oppdrag.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());
        assertThat(oppdragslinje150Liste).hasSize(2);
        assertThat(oppdragslinje150Liste.stream()
            .anyMatch(odl150 -> IntervallUtil.byggIntervall(odl150.getDatoVedtakFom(), odl150.getDatoVedtakTom())
                .equals(IntervallUtil.byggIntervall(OppdragskontrollTjenesteImplBaseTest.DAGENS_DATO.plusDays(23), OppdragskontrollTjenesteImplBaseTest.DAGENS_DATO.plusDays(30))))).isTrue();
        assertThat(oppdragslinje150Liste.stream()
            .anyMatch(odl150 -> IntervallUtil.byggIntervall(odl150.getDatoVedtakFom(), odl150.getDatoVedtakTom())
                .equals(IntervallUtil.byggIntervall(OppdragskontrollTjenesteImplBaseTest.DAGENS_DATO.plusDays(16), OppdragskontrollTjenesteImplBaseTest.DAGENS_DATO.plusDays(22))))).isFalse();
    }

    @Test
    public void skalOppretteFørstegangsoppdragFP() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP();
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Act
        Oppdragskontroll oppdrkontroll = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), 67L).get();

        // Assert
        assertThat(oppdrkontroll).isNotNull();
        assertThat(oppdrkontroll.getOppdrag110Liste()).hasSize(4);

        List<Oppdrag110> oppdrag110Liste = oppdrkontroll.getOppdrag110Liste();
        assertThat(oppdrag110Liste).isNotNull();
        for (Oppdrag110 oppdrag110Lest : oppdrag110Liste) {
            assertThat(oppdrag110Lest.getOppdragslinje150Liste()).isNotNull();
            assertThat(oppdrag110Lest.getOppdragsenhet120Liste()).isNotNull();
            assertThat(oppdrag110Lest.getAvstemming115()).isNotNull();
            assertThat(oppdrag110Lest.getOmpostering116()).isNotPresent();

            List<Oppdragslinje150> oppdrlinje150Liste = oppdrag110Lest.getOppdragslinje150Liste();
            for (Oppdragslinje150 oppdrlinje150 : oppdrlinje150Liste) {
                assertThat(oppdrlinje150).isNotNull();
                assertThat(oppdrlinje150.getOppdrag110()).isNotNull();
                assertThat(oppdrlinje150.getAttestant180Liste()).hasSize(1);
                assertThat(oppdrlinje150.getAttestant180Liste().get(0)).isNotNull();
            }
        }
    }

    @Test
    public void skalSendeFørstegangsoppdragHvorBrukerHarArbeidsforholdHosPrivatPersonOgUtbetalingGårTilBruker() {
        //Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 1000, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 0, BigDecimal.valueOf(100L), null);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 1000, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_2, false, 0, BigDecimal.valueOf(100L), null);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        //Act
        Oppdragskontroll oppdrag = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), 123L).get();

        //Assert
        assertThat(oppdrag).isNotNull();
        List<Oppdrag110> oppdrag110List = oppdrag.getOppdrag110Liste();
        assertThat(oppdrag110List).hasSize(1);
        assertThat(oppdrag110List.get(0).getOmpostering116()).isNotPresent();
        List<Oppdragslinje150> oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
        assertThat(oppdragslinje150List).hasSize(2);
        assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
            assertThat(opp150.getSats()).isEqualTo(1000L);
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        });
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
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        //Act
        Oppdragskontroll oppdrag = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), 123L).get();

        //Assert
        assertThat(oppdrag).isNotNull();
        List<Oppdrag110> oppdrag110List = oppdrag.getOppdrag110Liste();
        assertThat(oppdrag110List).hasSize(1);
        List<Oppdragslinje150> oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
        assertThat(oppdragslinje150List).hasSize(2);
        assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
            assertThat(opp150.getSats()).isEqualTo(1000L);
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        });
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
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        //Act
        Oppdragskontroll oppdrag = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), 123L).get();

        //Assert
        assertThat(oppdrag).isNotNull();
        List<Oppdrag110> oppdrag110List = oppdrag.getOppdrag110Liste();
        assertThat(oppdrag110List).hasSize(1);
        List<Oppdragslinje150> oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
        assertThat(oppdragslinje150List).hasSize(2);
        assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
            assertThat(opp150.getSats()).isEqualTo(1000L);
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        });
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
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        //Act
        Oppdragskontroll oppdrag = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), 123L).get();

        //Assert
        assertThat(oppdrag).isNotNull();
        List<Oppdrag110> oppdrag110List = oppdrag.getOppdrag110Liste();
        assertThat(oppdrag110List).hasSize(1);
        List<Oppdragslinje150> oppdragslinje150List = oppdrag110List.get(0).getOppdragslinje150Liste();
        assertThat(oppdragslinje150List).hasSize(2);
        assertThat(oppdragslinje150List).allSatisfy(opp150 -> {
            assertThat(opp150.getSats()).isEqualTo(1000L);
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        });
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
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        //Act
        Oppdragskontroll oppdrag = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), 123L).get();

        //Assert
        assertThat(oppdrag).isNotNull();
        List<Oppdrag110> oppdrag110List = oppdrag.getOppdrag110Liste();
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
            assertThat(opp150.getSats()).isEqualTo(500L);
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        });
        //Oppdragslinj150 for arbeidsgiver
        List<Oppdragslinje150> opp150ListVirksomhet = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdrag110List, virksomhet);
        assertThat(opp150ListVirksomhet).hasSize(2);
        assertThat(opp150ListVirksomhet).allSatisfy(opp150 -> {
            assertThat(opp150.getSats()).isEqualTo(500L);
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik());
        });
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
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        //Act
        Oppdragskontroll oppdrag = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), 123L).get();

        //Assert
        assertThat(oppdrag).isNotNull();
        List<Oppdrag110> oppdrag110List = oppdrag.getOppdrag110Liste();
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
            assertThat(opp150.getSats()).isEqualTo(1500L);
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        });
        //Oppdragslinj150 for arbeidsgiver
        List<Oppdragslinje150> opp150ListVirksomhet = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdrag110List, virksomhet);
        assertThat(opp150ListVirksomhet).hasSize(2);
        assertThat(opp150ListVirksomhet).allSatisfy(opp150 -> {
            assertThat(opp150.getSats()).isEqualTo(500L);
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik());
        });
    }

    @Test
    public void skalSendeFørstegangsoppdragForAdopsjonMedTilsvarendeKlassekode() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100L), null);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100L), virksomhet);
        Behandling behandlingAdopsjon = opprettOgLagreBehandling(FamilieYtelseType.ADOPSJON);
        beregningsresultatRepository.lagre(behandlingAdopsjon, beregningsresultat);

        // Act
        Oppdragskontroll oppdragskontroll = oppdragskontrollTjeneste.opprettOppdrag(behandlingAdopsjon.getId(), 23L).get();

        // Assert
        // Bruker
        List<Oppdragslinje150> opp150ListeForBruker = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdragskontroll.getOppdrag110Liste());
        assertThat(opp150ListeForBruker).hasSize(1);
        assertThat(opp150ListeForBruker.get(0).getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPADATORD.getKodeKlassifik());
        // Arbeidsgiver
        List<Oppdragslinje150> opp150ListeForArbgvr = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdragskontroll.getOppdrag110Liste(), virksomhet);
        assertThat(opp150ListeForArbgvr).hasSize(1);
        assertThat(opp150ListeForArbgvr.get(0).getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPADREFAG_IOP.getKodeKlassifik());
    }

    private void verifyOpp150NårFørstegangsoppdragBlirSendtIRevurdering(List<Oppdragslinje150> oppdragslinje150List) {
        assertThat(oppdragslinje150List).isNotEmpty();
        assertThat(oppdragslinje150List).allSatisfy(oppdragslinje150 -> {
            assertThat(oppdragslinje150.getKodeEndringLinje()).isEqualTo(ØkonomiKodeEndringLinje.NY.name());
            assertThat(oppdragslinje150.getRefusjonsinfo156()).isNull();
            assertThat(oppdragslinje150.getAttestant180Liste()).isNotEmpty();
            assertThat(oppdragslinje150.getGrad170Liste()).isNotEmpty();
        });
    }

    private void verifiserOppdragslinje150MedFlereKlassekode(Oppdragskontroll oppdrag) {
        List<Oppdragslinje150> oppdr150ListeArbeidsgiver = oppdrag.getOppdrag110Liste().stream().filter(opp110 -> opp110.getKodeFagomrade().equals(ØkonomiKodeFagområde.FPREF.name()))
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream()).filter(opp150 -> !opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPREFAGFER_IOP.getKodeKlassifik()))
            .collect(Collectors.toList());
        List<Oppdragslinje150> oppdr150ListeAT = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(oppdrag, ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        List<Oppdragslinje150> oppdr150ListeFL = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(oppdrag, ØkonomiKodeKlassifik.FPATFRI.getKodeKlassifik());
        List<BeregningsresultatAndel> andelersListe = hentAndeler();
        List<BeregningsresultatAndel> brukersandelerListeAT = andelersListe.stream().filter(BeregningsresultatAndel::erBrukerMottaker)
            .filter(andel -> andel.getInntektskategori().equals(Inntektskategori.ARBEIDSTAKER)).collect(Collectors.toList());
        List<BeregningsresultatAndel> brukersandelerListeFL = andelersListe.stream().filter(BeregningsresultatAndel::erBrukerMottaker)
            .filter(andel -> andel.getInntektskategori().equals(Inntektskategori.FRILANSER)).collect(Collectors.toList());
        List<BeregningsresultatAndel> arbeidsgiversandelerListe = andelersListe.stream().filter(andel -> !andel.erBrukerMottaker()).collect(Collectors.toList());

        verifiserOppdragslinje150MedFlereKlassekode(oppdr150ListeAT, brukersandelerListeAT);
        verifiserOppdragslinje150MedFlereKlassekode(oppdr150ListeFL, brukersandelerListeFL);
        verifiserOppdragslinje150MedFlereKlassekode(oppdr150ListeArbeidsgiver, arbeidsgiversandelerListe);
        OppdragskontrollTestVerktøy.verifiserKjedingForOppdragslinje150(oppdr150ListeAT, oppdr150ListeFL);
    }

    private void verifiserOppdragslinje150MedFlereKlassekode(List<Oppdragslinje150> oppdr150Liste, List<BeregningsresultatAndel> brukersandelerListe) {
        int ix150 = 0;
        for (Oppdragslinje150 opp150 : oppdr150Liste) {
            BeregningsresultatAndel andel = brukersandelerListe.get(ix150++);
            Boolean brukerErMottaker = andel.erBrukerMottaker();
            String kodeklassifik;
            if (brukerErMottaker) {
                kodeklassifik = andel.getInntektskategori().equals(Inntektskategori.ARBEIDSTAKER) ? ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik() : ØkonomiKodeKlassifik.FPATFRI.getKodeKlassifik();
            } else {
                kodeklassifik = ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik();
            }
            String utbetalesTilId = brukerErMottaker ? personInfo.getPersonIdent().getIdent() : andel.getArbeidsforholdIdentifikator();
            assertThat(opp150.getKodeEndringLinje()).isEqualTo(ØkonomiKodeEndringLinje.NY.name());
            assertThat(opp150.getVedtakId()).isEqualTo(behVedtak.getVedtaksdato().toString());
            assertThat(opp150.getKodeKlassifik()).isEqualTo(kodeklassifik);
            assertThat(opp150.getDatoVedtakFom()).isEqualTo(andel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeFom());
            assertThat(opp150.getDatoVedtakTom()).isEqualTo(andel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeTom());
            assertThat(opp150.getSats()).isEqualTo(andel.getDagsats());
            assertThat(opp150.getTypeSats()).isEqualTo(TYPE_SATS_FP_YTELSE);
            assertThat(opp150.getHenvisning()).isEqualTo(behandling.getId());
            assertThat(opp150.getSaksbehId()).isEqualTo(behVedtak.getAnsvarligSaksbehandler());
            assertThat(opp150.getBrukKjoreplan()).isEqualTo("N");
            assertThat(opp150.getAttestant180Liste()).hasSize(1);
            assertThat(opp150.getGrad170Liste()).hasSize(1);
            assertUtbetalesTilId(opp150, brukerErMottaker, utbetalesTilId);
        }
    }

    private void verifiserAttestant180(List<Oppdragslinje150> oppdragslinje150List) {
        assertThat(oppdragslinje150List).allSatisfy(oppdragslinje150 -> {
            var attestant180Liste = oppdragslinje150.getAttestant180Liste();
            assertThat(attestant180Liste).hasSize(1);
            var attestant180 = attestant180Liste.get(0);
            assertThat(attestant180.getAttestantId()).isEqualTo(behVedtak.getAnsvarligSaksbehandler());
        });
    }

    private List<Oppdragslinje150> verifiserOppdragslinje150(List<Oppdrag110> oppdrag110Liste) {
        List<Oppdragslinje150> oppdragslinje150List = oppdrag110Liste.stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());

        List<Long> delYtelseIdListe = new ArrayList<>();
        int jx = 0;
        for (Oppdrag110 oppdrag110 : oppdrag110Liste) {
            assertThat(oppdrag110.getOppdragslinje150Liste()).isNotEmpty();
            verifiserOppdragslinje150(oppdrag110.getOppdragslinje150Liste(), delYtelseIdListe, oppdrag110, jx++);
        }
        return oppdragslinje150List;
    }

    private void verifiserAvstemming115(List<Oppdrag110> oppdrag110Liste) {
        assertThat(oppdrag110Liste).allSatisfy(oppdrag110 -> {
            Avstemming115 avstemming115 = oppdrag110.getAvstemming115();
            assertThat(avstemming115).isNotNull();
            assertThat(avstemming115.getKodekomponent()).isEqualTo(ØkonomiKodekomponent.VLFP.getKodekomponent());
        });
    }

    private void verifiserOppdragsenhet120(List<Oppdrag110> oppdrag110Liste) {
        assertThat(oppdrag110Liste).allSatisfy(oppdrag110 -> {
            var oppdragsenhet120List = oppdrag110.getOppdragsenhet120Liste();
            assertThat(oppdragsenhet120List).hasSize(1);
            Oppdragsenhet120 oppdragsenhet120 = oppdragsenhet120List.get(0);
            assertThat(oppdragsenhet120.getTypeEnhet()).isEqualTo("BOS");
            assertThat(oppdragsenhet120.getEnhet()).isEqualTo("8020");
            assertThat(oppdragsenhet120.getDatoEnhetFom()).isEqualTo(LocalDate.of(1900, 1, 1));
        });
    }

    private void verifiserGrad170(List<Oppdragslinje150> oppdragslinje150List) {
        assertThat(oppdragslinje150List).allSatisfy(oppdragslinje150 -> {
            var grad170Liste = oppdragslinje150.getGrad170Liste();
            assertThat(grad170Liste).hasSize(1);
            var grad170 = grad170Liste.get(0);
            assertThat(grad170.getTypeGrad()).isEqualTo("UFOR");
            if (OppdragskontrollTestVerktøy.opp150MedGradering(oppdragslinje150)) {
                assertThat(grad170.getGrad()).isEqualTo(80);
            } else {
                assertThat(grad170.getGrad()).isEqualTo(100);
            }
        });
    }

    private void verifiserRefusjonInfo156(List<Oppdragslinje150> oppdragslinje150List) {
        List<BeregningsresultatAndel> andeler = hentAndeler();
        List<BeregningsresultatAndel> arbeidsgiverAndelListe = andeler.stream()
            .filter(andel -> !andel.erBrukerMottaker())
            .filter(andel -> andel.getDagsats() > 0)
            .collect(Collectors.toList());

        AtomicInteger ix156 = new AtomicInteger(0);
        assertThat(oppdragslinje150List).allSatisfy(oppdragslinje150 -> {
            if (oppdragslinje150.getOppdrag110().getKodeFagomrade().equals(ØkonomiKodeFagområde.FP.name())) {
                return;
            }
            var refusjonsinfo156 = oppdragslinje150.getRefusjonsinfo156();
            assertThat(refusjonsinfo156).isNotNull();

            BeregningsresultatAndel brAndel = arbeidsgiverAndelListe.get(ix156.getAndIncrement());
            String arbeidsforholdOrgnr = brAndel.getArbeidsforholdIdentifikator();
            String refunderesId = OppdragskontrollTestVerktøy.endreTilElleveSiffer(arbeidsforholdOrgnr);
            if (refunderesId.equals(OppdragskontrollTestVerktøy.endreTilElleveSiffer(ARBEIDSFORHOLD_ID))) {
                assertThat(refusjonsinfo156.getMaksDato()).isEqualTo(OppdragskontrollTjenesteImplBaseTest.DAGENS_DATO.plusDays(7));
            } else if (refunderesId.equals(OppdragskontrollTestVerktøy.endreTilElleveSiffer(ARBEIDSFORHOLD_ID_2))) {
                assertThat(refusjonsinfo156.getMaksDato()).isEqualTo(OppdragskontrollTjenesteImplBaseTest.DAGENS_DATO.plusDays(15));
            } else {
                assertThat(refusjonsinfo156.getMaksDato()).isEqualTo(OppdragskontrollTjenesteImplBaseTest.DAGENS_DATO.plusDays(22));
            }
            assertThat(refusjonsinfo156.getRefunderesId()).isEqualTo(refunderesId);
            assertThat(refusjonsinfo156.getDatoFom()).isEqualTo(behVedtak.getVedtaksdato());
        });
    }

    private void verifiserOppdragslinje150(List<Oppdragslinje150> oppdragslinje150List, List<Long> delYtelseIdListe, Oppdrag110 oppdrag110, int jx) {
        LocalDate vedtaksdatoFP = behVedtak.getVedtaksdato();
        Long fagsystemId = oppdrag110.getFagsystemId();

        List<List<BeregningsresultatAndel>> andelerSorted = sortAndelerSomListOfLists();
        assertThat(andelerSorted).isNotNull();

        long løpenummer = 100L;
        int ix150 = 0;
        List<BeregningsresultatAndel> andelerList = andelerSorted.get(jx);
        for (Oppdragslinje150 oppdragslinje150 : oppdragslinje150List) {
            BeregningsresultatAndel andel = andelerList.get(ix150++);
            Boolean brukerErMottaker = andel.erBrukerMottaker();
            delYtelseIdListe.add(oppdragslinje150.getDelytelseId());

            String utbetalesTilId = brukerErMottaker ? personInfo.getPersonIdent().getIdent() : andel.getArbeidsforholdIdentifikator();
            assertThat(oppdragslinje150.getKodeEndringLinje()).isEqualTo(ØkonomiKodeEndringLinje.NY.name());
            assertThat(oppdragslinje150.getVedtakId()).isEqualTo(vedtaksdatoFP.toString());
            assertThat(oppdragslinje150.getDelytelseId()).isEqualTo(concatenateValues(fagsystemId, løpenummer));
            assertThat(oppdragslinje150.getKodeKlassifik()).isEqualTo(brukerErMottaker ? ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik() : ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik());
            assertThat(oppdragslinje150.getDatoVedtakFom()).isEqualTo(andel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeFom());
            assertThat(oppdragslinje150.getDatoVedtakTom()).isEqualTo(andel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeTom());
            assertThat(oppdragslinje150.getSats()).isEqualTo(andel.getDagsats());
            assertThat(oppdragslinje150.getTypeSats()).isEqualTo(TYPE_SATS_FP_YTELSE);
            assertThat(oppdragslinje150.getHenvisning()).isEqualTo(behandling.getId());
            assertThat(oppdragslinje150.getSaksbehId()).isEqualTo(behVedtak.getAnsvarligSaksbehandler());
            assertThat(oppdragslinje150.getBrukKjoreplan()).isEqualTo("N");
            assertThat(oppdragslinje150.getAttestant180Liste()).hasSize(1);
            assertThat(oppdragslinje150.getGrad170Liste()).hasSize(1);
            assertUtbetalesTilId(oppdragslinje150, brukerErMottaker, utbetalesTilId);
            if (løpenummer > 100L) {
                int kx = (int) (løpenummer - 101);
                assertThat(oppdragslinje150.getRefFagsystemId()).isEqualTo(fagsystemId);
                assertThat(oppdragslinje150.getRefDelytelseId()).isEqualTo(delYtelseIdListe.get(kx));
            }
            løpenummer++;
        }
    }

    private void assertUtbetalesTilId(Oppdragslinje150 oppdragslinje150, Boolean brukerErMottaker, String utbetalesTilId) {
        if (brukerErMottaker) {
            assertThat(oppdragslinje150.getUtbetalesTilId()).isEqualTo(OppdragskontrollTestVerktøy.endreTilElleveSiffer(utbetalesTilId));
        } else {
            assertThat(oppdragslinje150.getUtbetalesTilId()).isNull();
            Refusjonsinfo156 ref156 = oppdragslinje150.getRefusjonsinfo156();
            assertThat(ref156.getRefunderesId()).isEqualTo(OppdragskontrollTestVerktøy.endreTilElleveSiffer(utbetalesTilId));
        }
    }

    private List<List<BeregningsresultatAndel>> sortAndelerSomListOfLists() {
        List<List<BeregningsresultatAndel>> andelerSorted = new ArrayList<>();

        List<BeregningsresultatAndel> beregningsresultatAndelListe = hentAndeler();
        List<BeregningsresultatAndel> brukersAndelListe = beregningsresultatAndelListe.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .filter(a -> a.getDagsats() > 0)
            .collect(Collectors.toList());

        List<BeregningsresultatAndel> arbeidsgiversAndelListe = beregningsresultatAndelListe.stream()
            .filter(a -> !a.erBrukerMottaker())
            .filter(a -> a.getDagsats() > 0)
            .collect(Collectors.toList());

        Map<String, List<BeregningsresultatAndel>> groupedById = arbeidsgiversAndelListe.stream()
            .collect(Collectors.groupingBy(
                BeregningsresultatAndel::getArbeidsforholdIdentifikator,
                LinkedHashMap::new,
                Collectors.mapping(Function.identity(), Collectors.toList())));

        andelerSorted.add(brukersAndelListe);
        andelerSorted.add(groupedById.get(ARBEIDSFORHOLD_ID));
        andelerSorted.add(groupedById.get(ARBEIDSFORHOLD_ID_2));
        andelerSorted.add(groupedById.get(ARBEIDSFORHOLD_ID_3));

        return andelerSorted;
    }

    private List<Oppdrag110> verifiserOppdrag110(Oppdragskontroll oppdragskontroll) {
        List<Oppdrag110> oppdrag110List = oppdragskontroll.getOppdrag110Liste();

        int ix110 = 0;
        long initialLøpenummer = 100L;
        for (Oppdrag110 oppdrag110 : oppdrag110List) {
            assertThat(oppdrag110.getKodeAksjon()).isEqualTo(ØkonomiKodeAksjon.EN.getKodeAksjon());
            assertThat(oppdrag110.getKodeEndring()).isEqualTo(ØkonomiKodeEndring.NY.name());
            boolean brukerErMottaker = ix110 == 0;
            assertThat(oppdrag110.getKodeFagomrade()).isEqualTo(brukerErMottaker ? ØkonomiKodeFagområde.FP.name() : ØkonomiKodeFagområde.FPREF.name());
            assertThat(oppdrag110.getFagsystemId()).isEqualTo(concatenateValues(Long.parseLong(fagsak.getSaksnummer().getVerdi()), initialLøpenummer++));
            assertThat(oppdrag110.getSaksbehId()).isEqualTo(behVedtak.getAnsvarligSaksbehandler());
            assertThat(oppdrag110.getUtbetFrekvens()).isEqualTo(ØkonomiUtbetFrekvens.MÅNED.getUtbetFrekvens());
            assertThat(oppdrag110.getOppdragGjelderId()).isEqualTo(personInfo.getPersonIdent().getIdent());
            assertThat(oppdrag110.getOppdragskontroll()).isEqualTo(oppdragskontroll);
            assertThat(oppdrag110.getAvstemming115()).isNotNull();
            ix110++;
        }

        return oppdrag110List;
    }

    private void verifiserOppdragskontroll(Oppdragskontroll oppdrskontroll, Long prosessTaskId) {
        assertThat(oppdrskontroll.getSaksnummer()).isEqualTo(fagsak.getSaksnummer());
        assertThat(oppdrskontroll.getVenterKvittering()).isEqualTo(Boolean.TRUE);
        assertThat(oppdrskontroll.getProsessTaskId()).isEqualTo(prosessTaskId);
    }

    private Long concatenateValues(Long... values) {
        List<Long> valueList = List.of(values);
        String result = valueList.stream().map(Object::toString).collect(Collectors.joining());

        return Long.valueOf(result);
    }

    private List<BeregningsresultatAndel> hentAndeler() {
        BeregningsresultatEntitet beregningsresultat = beregningsresultatRepository.hentBeregningsresultat(behandling.getId()).orElseThrow(() ->
            new IllegalStateException("Mangler Beregningsresultat for behandling " + behandling.getId()));
        List<BeregningsresultatPeriode> brPeriodeListe = beregningsresultat.getBeregningsresultatPerioder().stream()
            .sorted(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)).collect(Collectors.toList());
        List<BeregningsresultatAndel> andeler = brPeriodeListe.stream().map(BeregningsresultatPeriode::getBeregningsresultatAndelList).flatMap(List::stream).collect(Collectors.toList());

        return andeler.stream().filter(a -> a.getDagsats() > 0).collect(Collectors.toList());
    }
}
