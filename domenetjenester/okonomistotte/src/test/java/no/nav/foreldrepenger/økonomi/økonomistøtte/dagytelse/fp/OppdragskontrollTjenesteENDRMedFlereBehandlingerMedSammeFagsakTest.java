package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragMedPositivKvitteringTestUtil;

public class OppdragskontrollTjenesteENDRMedFlereBehandlingerMedSammeFagsakTest extends OppdragskontrollTjenesteTestBase {

    @Override
    @BeforeEach
    public void setUp() {
        setBrukNyOppdragTjeneste(true);
        super.setUp();
    }

    @Test
    public void skalKunneSendeOpphørPåOpphørNårEndringsdatoBlirEndretTilEnTidligereEndringsdatoISisteBehandling() {

        // Arrange
        LocalDate førsteEndringsdato = OppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(7);
        LocalDate andreEndringsdato = OppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(4);
        BigDecimal utbetalingsgrad = BigDecimal.valueOf(100);

        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 500, utbetalingsgrad, virksomhet);
        buildBeregningsresultatAndel(brPeriode_1, false, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 1000, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(behandling, beregningsresultatFP_1);
        Oppdragskontroll oppdragForFørstegangsbehandling = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Første Revurdering
        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet beregningsresultatFørsteRevurderingFP = buildBeregningsresultatFP(Optional.of(førsteEndringsdato));
        BeregningsresultatPeriode brPeriode_3 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 1, 6);
        buildBeregningsresultatAndel(brPeriode_3, true, 500, utbetalingsgrad, virksomhet);
        buildBeregningsresultatAndel(brPeriode_3, false, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode brPeriode_4 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 7, 20);
        buildBeregningsresultatAndel(brPeriode_4, false, 1000, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(førsteRevurdering, beregningsresultatFørsteRevurderingFP);
        Oppdragskontroll oppdragForFørsteRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, førsteRevurdering);

        // Andre Revurdering
        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet beregningsresultatAndreRevurderingFP = buildBeregningsresultatFP(Optional.of(andreEndringsdato));
        BeregningsresultatPeriode brPeriode_5 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 1, 3);
        buildBeregningsresultatAndel(brPeriode_5, true, 500, utbetalingsgrad, virksomhet);
        buildBeregningsresultatAndel(brPeriode_5, false, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode brPeriode_6 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 4, 20);
        buildBeregningsresultatAndel(brPeriode_6, false, 1500, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(andreRevurdering, beregningsresultatAndreRevurderingFP);

        // Act
        Oppdragskontroll oppdragForAndreRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andreRevurdering);

        // Assert : Førstegangsbehandling
        List<Oppdrag110> oppdrag110Liste_1 = oppdragForFørstegangsbehandling.getOppdrag110Liste();
        assertThat(oppdrag110Liste_1).hasSize(2);
        verifiserOppdrag110KodeEndring(oppdrag110Liste_1, List.of(ØkonomiKodeEndring.NY, ØkonomiKodeEndring.NY));
        verifiserOppdragslinje150KodeEndring(OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110Liste_1), List.of(ØkonomiKodeEndringLinje.NY, ØkonomiKodeEndringLinje.NY));
        verifiserOppdragslinje150KodeEndring(OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdrag110Liste_1, virksomhet), Collections.singletonList(ØkonomiKodeEndringLinje.NY));

        // Assert : Første revurdering
        List<Oppdrag110> oppdrag110Liste_2 = oppdragForFørsteRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste_2).hasSize(2);
        verifiserOppdrag110KodeEndring(oppdrag110Liste_2, List.of(ØkonomiKodeEndring.UEND, ØkonomiKodeEndring.UEND));
        verifiserOppdragslinje150KodeEndring(OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110Liste_2), Collections.singletonList(ØkonomiKodeEndringLinje.ENDR));
        verifiserOppdragslinje150KodeEndring(OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdrag110Liste_2, virksomhet), List.of(ØkonomiKodeEndringLinje.ENDR, ØkonomiKodeEndringLinje.NY));
        verifiserOpphørFom(OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110Liste_2), førsteEndringsdato);

        // Assert : Andre revurdering
        List<Oppdrag110> oppdrag110Liste_3 = oppdragForAndreRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste_3).hasSize(2);
        verifiserOppdrag110KodeEndring(oppdrag110Liste_3, List.of(ØkonomiKodeEndring.UEND, ØkonomiKodeEndring.UEND));
        verifiserOppdragslinje150KodeEndring(OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110Liste_3), Collections.singletonList(ØkonomiKodeEndringLinje.ENDR));
        verifiserOppdragslinje150KodeEndring(OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdrag110Liste_3, virksomhet), List.of(ØkonomiKodeEndringLinje.ENDR, ØkonomiKodeEndringLinje.NY));
        verifiserOpphørFom(OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110Liste_3), andreEndringsdato);

        // Assert : Kjeding
        verifiserKjedingNårDetErFlereBehandlingerMedSammeFagsak(oppdragForFørstegangsbehandling, oppdragForFørsteRevurdering, oppdragForAndreRevurdering, true);

    }

    @Test
    public void testEndringAvKodeNårEnFørstegangsbehandlingBlirOpphørtAvRevurderingOgEnNyFørstegangsbehandlingBlirOpprettetPåSammeFagsakMedEndringAvBruker() {

        // Arrange
        LocalDate endringsdato = OppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(8);
        BigDecimal utbetalingsgrad = BigDecimal.valueOf(100);

        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 1500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 1500, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(behandling, beregningsresultatFP_1);
        Oppdragskontroll oppdragForFørsteFørstegangsbehandling = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Revurdering
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.OPPHØR, true, false);
        Oppdragskontroll oppdragForRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, revurdering);

        // Ny førstegangsbehandling
        Behandling andregangsbehandling = opprettOgLagreBehandlingFPForSammeFagsak(behandling.getFagsak());
        BeregningsresultatEntitet beregningsresultatFP_2 = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode brPeriode_5 = buildBeregningsresultatPeriode(beregningsresultatFP_2, 1, 4);
        buildBeregningsresultatAndel(brPeriode_5, true, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode brPeriode_6 = buildBeregningsresultatPeriode(beregningsresultatFP_2, 5, 20);
        buildBeregningsresultatAndel(brPeriode_6, true, 2500, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(andregangsbehandling, beregningsresultatFP_2);

        // Act
        Oppdragskontroll oppdragForAndreFørstegangsbehandling = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andregangsbehandling);

        // Assert : Første førstegangsbehandling
        List<Oppdrag110> oppdrag110Liste_1 = oppdragForFørsteFørstegangsbehandling.getOppdrag110Liste();
        assertThat(oppdrag110Liste_1).hasSize(1);
        verifiserOppdrag110KodeEndring(oppdrag110Liste_1, Collections.singletonList(ØkonomiKodeEndring.NY));
        verifiserOppdragslinje150KodeEndring(oppdrag110Liste_1.get(0).getOppdragslinje150Liste(), List.of(ØkonomiKodeEndringLinje.NY, ØkonomiKodeEndringLinje.NY));

        // Assert : Revurdering
        List<Oppdrag110> oppdrag110Liste_2 = oppdragForRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste_2).hasSize(1);
        verifiserOppdrag110KodeEndring(oppdrag110Liste_2, Collections.singletonList(ØkonomiKodeEndring.UEND));
        verifiserOppdragslinje150KodeEndring(oppdrag110Liste_2.get(0).getOppdragslinje150Liste(), Collections.singletonList(ØkonomiKodeEndringLinje.ENDR));

        // Assert : Andre førstegangsbehandling
        List<Oppdrag110> oppdrag110Liste_3 = oppdragForAndreFørstegangsbehandling.getOppdrag110Liste();
        assertThat(oppdrag110Liste_3).hasSize(1);
        assertThat(oppdrag110Liste_3.get(0).getOmpostering116()).isNotPresent();
        verifiserOppdrag110KodeEndring(oppdrag110Liste_3, Collections.singletonList(ØkonomiKodeEndring.UEND));
        verifiserOppdragslinje150KodeEndring(oppdrag110Liste_3.get(0).getOppdragslinje150Liste(), Collections.singletonList(ØkonomiKodeEndringLinje.NY));

        // Assert : Kjeding
        verifiserKjedingNårDetErFlereBehandlingerMedSammeFagsak(oppdragForFørsteFørstegangsbehandling, oppdragForRevurdering, oppdragForAndreFørstegangsbehandling, false);

    }

    /**
     * Førstegangsbehandling: En mottaker bruker, periode1: 1-10, periode2: 11-20
     * Revurdering 1: Fullstendig opphør
     * Revurdering 2: Endret andel for bruker og arbeidsgiver som en ny mottaker, periode1:1-4, periode2: 5-20
     */
    @Test
    public void testEndringAvKodeNårEnFørstegangsbehandlingBlirOpphørtAvRevurderingOgEnNyFørstegangsbehandlingBlirOpprettetPåSammeFagsakMedEndringAvBrukerOgNyAndelForArbeidsgiver() {

        // Arrange
        LocalDate endringsdato = OppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(1);
        BigDecimal utbetalingsgrad = BigDecimal.valueOf(100);

        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 10);
        buildBeregningsresultatAndel(brPeriode_1, true, 1500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode brPeriode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 11, 20);
        buildBeregningsresultatAndel(brPeriode_2, true, 1500, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(behandling, beregningsresultatFP_1);
        Oppdragskontroll oppdragForFørsteFørstegangsbehandling = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Revurdering
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.OPPHØR, true, false);
        Oppdragskontroll oppdragForRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, revurdering);

        // Ny førstegangsbehandling
        Behandling andregangsbehandling = opprettOgLagreBehandlingFPForSammeFagsak(behandling.getFagsak());
        BeregningsresultatEntitet beregningsresultatFP_2 = buildBeregningsresultatFP(Optional.of(endringsdato));

        // Endre mottaker: Bruker
        BeregningsresultatPeriode brPeriode_5 = buildBeregningsresultatPeriode(beregningsresultatFP_2, 1, 4);
        buildBeregningsresultatAndel(brPeriode_5, true, 1000, utbetalingsgrad, virksomhet);
        buildBeregningsresultatAndel(brPeriode_5, false, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode brPeriode_6 = buildBeregningsresultatPeriode(beregningsresultatFP_2, 5, 20);
        buildBeregningsresultatAndel(brPeriode_6, true, 1000, utbetalingsgrad, virksomhet);
        buildBeregningsresultatAndel(brPeriode_6, false, 500, utbetalingsgrad, virksomhet);

        // Ny mottaker: Arbeidsgiver
        beregningsresultatRepository.lagre(andregangsbehandling, beregningsresultatFP_2);

        // Act
        Oppdragskontroll oppdragForAndreFørstegangsbehandling = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andregangsbehandling);

        // Assert : Første førstegangsbehandling
        List<Oppdrag110> oppdrag110Liste_1 = oppdragForFørsteFørstegangsbehandling.getOppdrag110Liste();
        assertThat(oppdrag110Liste_1).hasSize(1);
        verifiserOppdrag110KodeEndring(oppdrag110Liste_1, Collections.singletonList(ØkonomiKodeEndring.NY));
        verifiserOppdragslinje150KodeEndring(oppdrag110Liste_1.get(0).getOppdragslinje150Liste(), List.of(ØkonomiKodeEndringLinje.NY, ØkonomiKodeEndringLinje.NY));

        // Assert : Revurdering
        List<Oppdrag110> oppdrag110Liste_2 = oppdragForRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste_2).hasSize(1);
        verifiserOppdrag110KodeEndring(oppdrag110Liste_2, Collections.singletonList(ØkonomiKodeEndring.UEND));
        verifiserOppdragslinje150KodeEndring(oppdrag110Liste_2.get(0).getOppdragslinje150Liste(), Collections.singletonList(ØkonomiKodeEndringLinje.ENDR));

        // Assert : Andre førstegangsbehandling
        List<Oppdrag110> oppdrag110Liste_3 = oppdragForAndreFørstegangsbehandling.getOppdrag110Liste();
        assertThat(oppdrag110Liste_3).hasSize(2);
        verifiserOppdrag110KodeEndring(oppdrag110Liste_3, List.of(ØkonomiKodeEndring.UEND, ØkonomiKodeEndring.NY));
        verifiserOppdragslinje150KodeEndring(OppdragskontrollTestVerktøy.getOpp150ListeForBruker(oppdrag110Liste_3), List.of(ØkonomiKodeEndringLinje.NY, ØkonomiKodeEndringLinje.NY));
        verifiserOppdragslinje150KodeEndring(OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(oppdrag110Liste_3, virksomhet), List.of(ØkonomiKodeEndringLinje.NY, ØkonomiKodeEndringLinje.NY));

        // Assert : Kjeding
        verifiserKjedingNårDetErFlereBehandlingerMedSammeFagsak(oppdragForFørsteFørstegangsbehandling, oppdragForRevurdering, oppdragForAndreFørstegangsbehandling, false);

    }

    @Test
    public void skalTesteKjedingAvOppdragslinje150NårEnFørstegangsbehandlingBlirOpphørtAvEnRevurderingOgEnNyFørstegangsbehandlingBlirOpprettet() {

        // Arrange
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatEntenForBrukerEllerArbgvr(true, true);
        beregningsresultatRepository.lagre(behandling, beregningsresultatFP_1);
        Oppdragskontroll oppdragForFørstegangsbehandling = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.OPPHØR, true, false);
        Oppdragskontroll oppdragForRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, revurdering);

        Behandling andregangsbehandling = opprettOgLagreBehandlingFPForSammeFagsak(behandling.getFagsak());
        BeregningsresultatEntitet beregningsresultatFP_2 = buildBeregningsresultatEntenForBrukerEllerArbgvr(true, true);
        beregningsresultatRepository.lagre(andregangsbehandling, beregningsresultatFP_2);

        // Act
        Oppdragskontroll oppdragForAndregangsbehandling = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andregangsbehandling);

        // Assert
        verifiserKjedingNårDetErFlereBehandlingerMedSammeFagsak(oppdragForFørstegangsbehandling, oppdragForRevurdering, oppdragForAndregangsbehandling, false);
        verifiserFeriepengerNårDetErFlereBehandlingerMedSammeFagsak(oppdragForFørstegangsbehandling, oppdragForRevurdering, oppdragForAndregangsbehandling);

    }

    /**
     * Førstegangsbehandling: Både bruker og arbeidsgiver er mottaker
     * Revurdering#1: Arbeidsgiver er eneste mottaker
     * Revurdering#2: Både bruker og arbeidsgiver er mottaker
     * Forventet resultat: Skal ikke sende opphør for bruker i revurdering#2
     */
    @Test
    public void skalIkkeSendeOpphørForEnMottakerSomHaddeEnFullstendigOpphørIForrigeBehandling() {

        // Arrange

        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for bruker i periode#2
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultat, 11, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#2
        buildBeregningsresultatAndel(b1Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        //Act
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Første Revurdering
        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate førsteEndringsdato = b1Periode_1.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatFørsteRevurderingFP = buildBeregningsresultatFP(Optional.of(førsteEndringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 1, 10);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b2Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#2
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 11, 20);
        buildBeregningsresultatAndel(b2Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(førsteRevurdering, beregningsresultatFørsteRevurderingFP);
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, førsteRevurdering);

        // Andre Revurdering
        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.INNVILGET, false, true);
        LocalDate andreEndringsdato = b2Periode_1.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatAndreRevurderingFP = buildBeregningsresultatFP(Optional.of(andreEndringsdato));
        BeregningsresultatPeriode b3Periode_1 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b3Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b3Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        BeregningsresultatPeriode b3Periode_2 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 11, 20);
        //Andel for bruker i periode#2
        buildBeregningsresultatAndel(b3Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#2
        buildBeregningsresultatAndel(b3Periode_2, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(andreRevurdering, beregningsresultatAndreRevurderingFP);

        // Act
        Oppdragskontroll oppdragForAndreRevurdering =
            OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andreRevurdering);

        // Assert : Revurdering#2
        List<Oppdrag110> oppdrag110Liste = oppdragForAndreRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste).hasSize(2);
        //Oppdrag110 for Bruker
        Oppdrag110 oppdrag110ForBruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdrag110Liste);
        assertThat(oppdrag110ForBruker.getOmpostering116()).isNotPresent();
        assertThat(oppdrag110ForBruker.getKodeFagomrade()).isEqualTo(ØkonomiKodeFagområde.FP.name());
        assertThat(oppdrag110ForBruker.getKodeEndring()).isEqualTo(ØkonomiKodeEndring.UEND.name());
        //Oppdrag110 for Arbeidsgiver
        Oppdrag110 oppdrag110ForArbeidsgiver = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdrag110Liste, virksomhet);
        assertThat(oppdrag110ForArbeidsgiver.getKodeFagomrade()).isEqualTo(ØkonomiKodeFagområde.FPREF.name());
        assertThat(oppdrag110ForArbeidsgiver.getKodeEndring()).isEqualTo(ØkonomiKodeEndring.UEND.name());
        //Oppdragslinje150 for Bruker
        List<Oppdragslinje150> opp150ListeForBruker = oppdrag110ForBruker.getOppdragslinje150Liste();
        assertThat(opp150ListeForBruker).hasSize(2);
        assertThat(opp150ListeForBruker).allSatisfy(opp150 -> {
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
            assertThat(opp150.gjelderOpphør()).isFalse();
        });
        //Oppdragslinje150 for Arbeidsgiver
        List<Oppdragslinje150> opp150ListeForArbeidsgiver = oppdrag110ForArbeidsgiver.getOppdragslinje150Liste();
        assertThat(opp150ListeForArbeidsgiver).hasSize(3);
        assertThat(opp150ListeForArbeidsgiver).allSatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik()));
    }

    /**
     * Førstegangsbehandling: Både bruker og arbeidsgiver er mottaker
     * Revurdering#1: Fullstendig opphør
     * Revurdering#2: Både bruker og arbeidsgiver er mottaker
     */
    @Test
    public void skalSendeEndringsoppdragForAndreRevurderingNårFørsteRevurderingGjelderFullstendigOpphør() {

        // Arrange

        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        //Act
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Første Revurdering
        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.AVSLAG, true, false);
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, førsteRevurdering);

        // Andre Revurdering
        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet beregningsresultatAndreRevurderingFP = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b3Periode_1 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b3Periode_1, true, 1600, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b3Periode_1, false, 1600, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(andreRevurdering, beregningsresultatAndreRevurderingFP);

        // Act
        Oppdragskontroll oppdragForAndreRevurdering =
            OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andreRevurdering);

        // Assert : Revurdering#2
        List<Oppdrag110> oppdrag110Liste = oppdragForAndreRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste).hasSize(2);
        //Oppdrag110 for Bruker
        Oppdrag110 oppdrag110ForBruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdrag110Liste);
        assertThat(oppdrag110ForBruker.getOmpostering116()).isNotPresent();
        assertThat(oppdrag110ForBruker.getKodeFagomrade()).isEqualTo(ØkonomiKodeFagområde.FP.name());
        assertThat(oppdrag110ForBruker.getKodeEndring()).isEqualTo(ØkonomiKodeEndring.UEND.name());
        //Oppdrag110 for Arbeidsgiver
        Oppdrag110 oppdrag110ForArbeidsgiver = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdrag110Liste, virksomhet);
        assertThat(oppdrag110ForArbeidsgiver.getKodeFagomrade()).isEqualTo(ØkonomiKodeFagområde.FPREF.name());
        assertThat(oppdrag110ForArbeidsgiver.getKodeEndring()).isEqualTo(ØkonomiKodeEndring.UEND.name());
        //Oppdragslinje150 for Bruker
        List<Oppdragslinje150> opp150ListeForBruker = oppdrag110ForBruker.getOppdragslinje150Liste();
        assertThat(opp150ListeForBruker).hasSize(1);
        Oppdragslinje150 opp150ForBruker = opp150ListeForBruker.get(0);
        assertThat(opp150ForBruker.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        assertThat(opp150ForBruker.gjelderOpphør()).isFalse();
        assertThat(opp150ForBruker.getRefDelytelseId()).isNotNull();
        assertThat(opp150ForBruker.getRefFagsystemId()).isNotNull();
        //Oppdragslinje150 for Arbeidsgiver
        List<Oppdragslinje150> opp150ListeForArbeidsgiver = oppdrag110ForArbeidsgiver.getOppdragslinje150Liste();
        assertThat(opp150ListeForArbeidsgiver).hasSize(1);
        Oppdragslinje150 opp150ForArbeidsgiver = opp150ListeForArbeidsgiver.get(0);
        assertThat(opp150ForArbeidsgiver.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik());
        assertThat(opp150ForArbeidsgiver.gjelderOpphør()).isFalse();
        assertThat(opp150ForArbeidsgiver.getRefDelytelseId()).isNotNull();
        assertThat(opp150ForArbeidsgiver.getRefFagsystemId()).isNotNull();
    }

    /**
     * Førstegangsbehandling: To arbeidsgivere er mottaker (Virksomhet og virksomhet2)
     * Revurdering#1: Fullstendig opphør på virksomhet og virksomhet2 er fremdeles mottaker
     * Revurdering#2: Både virksomhet og virksomhet2 er mottakere
     */
    @Test
    public void skalIkkeSendeOpphørForEnAvDeArbeidsgivereneSomEnDelAvEndringsoppdragIAndreRevurderingNårFørsteRevurderingHaddeFullstendigOpphørForDenneArbeidsgiveren() {

        // Arrange

        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultat, 1, 10);
        //Andel for arbeidsgiver#1 i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver#2 i periode#1
        buildBeregningsresultatAndel(b1Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet2,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        //Act
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Første Revurdering
        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate førsteEndringsdato = b1Periode_1.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatFørsteRevurderingFP = buildBeregningsresultatFP(Optional.of(førsteEndringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 1, 10);
        //Andel for arbeidsgiver#2 i periode#1
        buildBeregningsresultatAndel(b2Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet2,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(førsteRevurdering, beregningsresultatFørsteRevurderingFP);
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, førsteRevurdering);

        // Andre Revurdering
        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.INNVILGET, false, true);
        LocalDate andreEndringsdato = b1Periode_1.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatAndreRevurderingFP = buildBeregningsresultatFP(Optional.of(andreEndringsdato));
        BeregningsresultatPeriode b3Periode_1 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 1, 10);
        //Andel for bruker i periode#1
        buildBeregningsresultatAndel(b3Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        //Andel for arbeidsgiver i periode#1
        buildBeregningsresultatAndel(b3Periode_1, false, 1500, BigDecimal.valueOf(100), virksomhet2,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(andreRevurdering, beregningsresultatAndreRevurderingFP);

        // Act
        Oppdragskontroll oppdragForAndreRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andreRevurdering);

        // Assert : Revurdering#2
        List<Oppdrag110> oppdrag110Liste = oppdragForAndreRevurdering.getOppdrag110Liste();
        assertThat(oppdrag110Liste).hasSize(2);
        //Oppdrag110 for Arbeidsgivere
        assertThat(oppdrag110Liste).allSatisfy(oppdrag110ForArbeidsgiver -> {
            assertThat(oppdrag110ForArbeidsgiver.getKodeFagomrade()).isEqualTo(ØkonomiKodeFagområde.FPREF.name());
            assertThat(oppdrag110ForArbeidsgiver.getKodeEndring()).isEqualTo(ØkonomiKodeEndring.UEND.name());
        });
        //Oppdragslinje150 for Arbeidsgiver#1
        Oppdrag110 oppdrag110ForArbeidsgiver_1 = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdrag110Liste, virksomhet);
        List<Oppdragslinje150> opp150ListeForArbeidsgiver_1_List = oppdrag110ForArbeidsgiver_1.getOppdragslinje150Liste();
        assertThat(opp150ListeForArbeidsgiver_1_List).hasSize(1);
        Oppdragslinje150 opp150OpphForArbeidsgiver_1 = opp150ListeForArbeidsgiver_1_List.get(0);
        assertThat(opp150OpphForArbeidsgiver_1.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik());
        assertThat(opp150OpphForArbeidsgiver_1.gjelderOpphør()).isFalse();
        //Oppdragslinje150 for Arbeidsgiver#2
        Oppdrag110 oppdrag110ForArbeidsgiver_2 = OppdragskontrollTestVerktøy.getOppdrag110ForArbeidsgiver(oppdrag110Liste, virksomhet2);
        List<Oppdragslinje150> opp150ListeForArbeidsgiver_2_List = oppdrag110ForArbeidsgiver_2.getOppdragslinje150Liste();
        assertThat(opp150ListeForArbeidsgiver_2_List).hasSize(2);
        List<Oppdragslinje150> opp150OpphListeForArbeidsgiver_2 = opp150ListeForArbeidsgiver_2_List.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .collect(Collectors.toList());
        assertThat(opp150OpphListeForArbeidsgiver_2).hasSize(1);
        Oppdragslinje150 opp150OpphForArbeidsgiver_2 = opp150ListeForArbeidsgiver_2_List.get(0);
        assertThat(opp150OpphForArbeidsgiver_2.getKodeKlassifik()).isEqualTo(ØkonomiKodeKlassifik.FPREFAG_IOP.getKodeKlassifik());
    }

    /**
     * Førstegangsbehandling(b1): Bruker er mottaker, Inntektskategori: AT, To perioder:b1Periode1-b1Periode2
     * Revurdering#1(b2): Bruker er mottaker, Inntektskategori: AT, To perioder: b2Periode1-b2Periode2, Endringsdato=b2Periode2.getTom().plusDays(1)
     * Revurdering#3(b3): Bruker er mottaker, Inntektskategori: AT, To perioder: b3Periode1-b3Periode2, Endringsdato=b3Periode2.getTom().plusDays(1)
     */
    @Test
    public void skalKunneSendeNyePerioderEtterEndringsdatoIAndreRevurderingNårFørsteRevurderingHarKunOpphør() {

        // Arrange
        BigDecimal utbetalingsgrad = BigDecimal.valueOf(100);

        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 8);
        BeregningsresultatAndel b1Andel = buildBeregningsresultatAndel(b1Periode_1, true, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 9, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1000, utbetalingsgrad, virksomhet);
        BeregningsresultatFeriepenger b1_feriepenger = buildBeregningsresultatFeriepenger(beregningsresultatFP_1);
        buildBeregningsresultatFeriepengerPrÅr(b1_feriepenger, b1Andel, 3000L, Collections.singletonList(OppdragskontrollTjenesteTestBase.DAGENS_DATO));
        beregningsresultatRepository.lagre(behandling, beregningsresultatFP_1);
        Oppdragskontroll førsteOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Første Revurdering
        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate førsteEndringsdato = b1Periode_2.getBeregningsresultatPeriodeTom().minusDays(3);
        BeregningsresultatEntitet beregningsresultatFørsteRevurderingFP = buildBeregningsresultatFP(Optional.of(førsteEndringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 1, 8);
        BeregningsresultatAndel b2Andel = buildBeregningsresultatAndel(b2Periode_1, true, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 9, 16);
        buildBeregningsresultatAndel(b2Periode_2, true, 1000, utbetalingsgrad, virksomhet);
        BeregningsresultatFeriepenger b2_feriepenger = buildBeregningsresultatFeriepenger(beregningsresultatFørsteRevurderingFP);
        buildBeregningsresultatFeriepengerPrÅr(b2_feriepenger, b2Andel, 3000L, Collections.singletonList(OppdragskontrollTjenesteTestBase.DAGENS_DATO));
        beregningsresultatRepository.lagre(førsteRevurdering, beregningsresultatFørsteRevurderingFP);
        Oppdragskontroll oppdragForFørsteRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, førsteRevurdering);

        // Andre Revurdering
        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.INNVILGET, false, true);
        LocalDate andreEndringsdato = b2Periode_1.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatAndreRevurderingFP = buildBeregningsresultatFP(Optional.of(andreEndringsdato));
        BeregningsresultatPeriode b3Periode_1 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 1, 8);
        BeregningsresultatAndel b3Andel = buildBeregningsresultatAndel(b3Periode_1, true, 600, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode b3Periode_2 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 9, 12);
        buildBeregningsresultatAndel(b3Periode_2, true, 900, utbetalingsgrad, virksomhet);
        BeregningsresultatFeriepenger b3_feriepenger = buildBeregningsresultatFeriepenger(beregningsresultatAndreRevurderingFP);
        buildBeregningsresultatFeriepengerPrÅr(b3_feriepenger, b3Andel, 3000L, Collections.singletonList(OppdragskontrollTjenesteTestBase.DAGENS_DATO));
        beregningsresultatRepository.lagre(andreRevurdering, beregningsresultatAndreRevurderingFP);

        // Act
        Oppdragskontroll oppdragForAndreRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andreRevurdering);

        //Assert
        //Første revurdering: Oppdrag110
        List<Oppdrag110> førsteRevurdOpp110Liste = oppdragForFørsteRevurdering.getOppdrag110Liste();
        assertThat(førsteRevurdOpp110Liste).hasSize(1);
        //Førsterevurdering: Oppdragslinjne150
        List<Oppdragslinje150> førsteRevurdOpp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(førsteRevurdOpp110Liste);
        assertThat(førsteRevurdOpp150Liste).hasSize(1);
        Oppdragslinje150 opp150ForOpphIFørsteRevurdering = førsteRevurdOpp150Liste.get(0);
        assertThat(opp150ForOpphIFørsteRevurdering.gjelderOpphør()).isTrue();
        assertThat(opp150ForOpphIFørsteRevurdering.getDatoStatusFom()).isEqualTo(førsteEndringsdato);
        //Andre revurdering: Oppdrag110
        List<Oppdrag110> andreRevurdOpp110Liste = oppdragForAndreRevurdering.getOppdrag110Liste();
        assertThat(førsteRevurdOpp110Liste).hasSize(1);
        //Andre revurdering: Oppdragslinjne150
        List<Oppdragslinje150> andreRevurdOpp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(andreRevurdOpp110Liste);
        assertThat(andreRevurdOpp150Liste).hasSize(3);
        Optional<Oppdragslinje150> opp150ForOpphIAndreRevurderingOpt = andreRevurdOpp150Liste.stream()
            .filter(Oppdragslinje150::gjelderOpphør).findFirst();
        assertThat(opp150ForOpphIAndreRevurderingOpt).isPresent();
        assertThat(opp150ForOpphIAndreRevurderingOpt.get().getDelytelseId()).isEqualTo(opp150ForOpphIFørsteRevurdering.getDelytelseId());
        assertThat(opp150ForOpphIAndreRevurderingOpt.get().getDatoStatusFom()).isEqualTo(andreEndringsdato);
        List<Oppdragslinje150> opp150AndreRevurdUtenOpphListe = andreRevurdOpp150Liste.stream()
            .filter(opp150 -> !opp150.gjelderOpphør()).collect(Collectors.toList());
        assertThat(opp150AndreRevurdUtenOpphListe).hasSize(2);
        verifiserKjedingNårDetErFlereBehandlingerMedSammeFagsak(førsteOppdrag, oppdragForFørsteRevurdering, oppdragForAndreRevurdering, true);
    }

    /**
     * <p>
     * Førstegangsbehandling(B1)
     * <ul>
     * <li>Mottaker: Bruker, Inntektskategori: AT og FL</li>
     * <li>To perioder: b1Periode1-b1Periode2</li>
     * </ul>
     * <p>
     * Revurdering#1(B2)
     * <ul>
     * <li>Mottaker: Bruker, Inntektskategori: AT og FL</li>
     * <li>To perioder: b2Periode1-b2Periode2</li>
     * <li>Endringsdato: b2Periode2.getTom().plusDays(1)</li>
     * </ul>
     * <p>
     * Revurdering#2(B3):
     * <ul>
     * * <li>Mottaker: Bruker, Inntektskategori: AT og FL</li>
     * * <li>To perioder: b3Periode1-b3Periode2</li>
     * * <li>Endringsdato: b3Periode2.getTom().plusDays(1)</li>
     * * </ul>
     */
    @Test
    public void skalKunneSendeOpphørPåForrigeOpphørNårEndringsdatoErEtterSisteTilkjentYtelseperiodeForBrukerMedFlereInntekskategoriIAndreRevurdering() {

        // Arrange
        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 8);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 9, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        beregningsresultatRepository.lagre(behandling, beregningsresultatFP_1);
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Første Revurdering
        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate førsteEndringsdato = b1Periode_2.getBeregningsresultatPeriodeTom().minusDays(3);
        BeregningsresultatEntitet beregningsresultatFørsteRevurderingFP = buildBeregningsresultatFP(Optional.of(førsteEndringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 1, 8);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 9, 16);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b2Periode_2, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        beregningsresultatRepository.lagre(førsteRevurdering, beregningsresultatFørsteRevurderingFP);
        Oppdragskontroll oppdragForFørsteRevurdering =
            OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, førsteRevurdering);

        // Andre Revurdering
        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.INNVILGET, false, true);
        LocalDate andreEndringsdato = b2Periode_2.getBeregningsresultatPeriodeTom().minusDays(3);
        BeregningsresultatEntitet beregningsresultatAndreRevurderingFP = buildBeregningsresultatFP(Optional.of(andreEndringsdato));
        BeregningsresultatPeriode b3Periode_1 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 1, 8);
        buildBeregningsresultatAndel(b3Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b3Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 9, 12);
        buildBeregningsresultatAndel(b3Periode_1, true, 1500, BigDecimal.valueOf(100), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b3Periode_1, true, 1500, BigDecimal.valueOf(100), null,
            AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER);
        beregningsresultatRepository.lagre(andreRevurdering, beregningsresultatAndreRevurderingFP);

        // Act
        Oppdragskontroll oppdragForAndreRevurdering =
            OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andreRevurdering);

        //Assert
        //Første revurdering: Oppdrag110
        List<Oppdrag110> førsteRevurdOpp110Liste = oppdragForFørsteRevurdering.getOppdrag110Liste();
        assertThat(førsteRevurdOpp110Liste).hasSize(1);
        //Førsterevurdering: Oppdragslinjne150
        List<Oppdragslinje150> førsteRevurdOpp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(førsteRevurdOpp110Liste);
        assertThat(førsteRevurdOpp150Liste).hasSize(2);
        assertThat(førsteRevurdOpp150Liste).allSatisfy(opp150 -> {
            assertThat(opp150.gjelderOpphør()).isTrue();
            assertThat(opp150.getDatoStatusFom()).isEqualTo(førsteEndringsdato);
        });
        List<Oppdragslinje150> opp150ForOpphPåATIFørsteRevurderingListe = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(førsteRevurdOpp150Liste,
            ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        assertThat(opp150ForOpphPåATIFørsteRevurderingListe).hasSize(1);
        List<Oppdragslinje150> opp150ForOpphPåFLIFørsteRevurderingListe = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(førsteRevurdOpp150Liste,
            ØkonomiKodeKlassifik.FPATFRI.getKodeKlassifik());
        assertThat(opp150ForOpphPåFLIFørsteRevurderingListe).hasSize(1);

        //Andre revurdering: Oppdrag110
        List<Oppdrag110> andreRevurdOpp110Liste = oppdragForAndreRevurdering.getOppdrag110Liste();
        assertThat(andreRevurdOpp110Liste).hasSize(1);
        //Andre revurdering: Oppdragslinjne150
        List<Oppdragslinje150> andreRevurdOpp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(andreRevurdOpp110Liste);
        assertThat(andreRevurdOpp150Liste).hasSize(2);
        assertThat(andreRevurdOpp150Liste).allSatisfy(opp150 -> {
            assertThat(opp150.gjelderOpphør()).isTrue();
            assertThat(opp150.getDatoStatusFom()).isEqualTo(andreEndringsdato);
        });
        List<Oppdragslinje150> opp150ForOpphPåATIAndreRevurderingListe = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(andreRevurdOpp150Liste,
            ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        assertThat(opp150ForOpphPåATIAndreRevurderingListe).hasSize(1);
        List<Oppdragslinje150> opp150ForOpphPåFLIAndreRevurderingListe = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(andreRevurdOpp150Liste,
            ØkonomiKodeKlassifik.FPATFRI.getKodeKlassifik());
        assertThat(opp150ForOpphPåFLIAndreRevurderingListe).hasSize(1);
    }
        /**
         * <p>
         * Førstegangsbehandling(B1)
         * <ul>
         * <li>Mottaker: Bruker, Inntektskategori: AT</li>
         * <li>To perioder: b1Periode1-b1Periode2</li>
         * </ul>
         * <p>
         * Revurdering#1(B2)
         * <ul>
         * <li>Mottaker: Bruker, Inntektskategori: AT</li>
         * <li>To perioder: b2Periode1-b2Periode2</li>
         * <li>Endringsdato: b2Periode2.getTom().plusDays(1)</li>
         * </ul>
         * <p>
         * Revurdering#2(B3):
         * <ul>
         * * <li>Mottaker: Bruker, Inntektskategori: AT</li>
         * * <li>To perioder: b3Periode1-b3Periode2</li>
         * * <li>Endringsdato: b3Periode2.getTom().plusDays(1)</li>
         * * </ul>
         */
    @Test
    public void skalKunneSendeOpphørPåForrigeOpphørNårEndringsdatoErEtterSisteTilkjentYtelseperiodeForBrukerIAndreRevurdering() {

        // Arrange
        BigDecimal utbetalingsgrad = BigDecimal.valueOf(100);

        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 8);
        buildBeregningsresultatAndel(b1Periode_1, true, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 9, 20);
        buildBeregningsresultatAndel(b1Periode_2, true, 1000, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(behandling, beregningsresultatFP_1);
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Første Revurdering
        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate førsteEndringsdato = b1Periode_2.getBeregningsresultatPeriodeTom().minusDays(3);
        BeregningsresultatEntitet beregningsresultatFørsteRevurderingFP = buildBeregningsresultatFP(Optional.of(førsteEndringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 1, 8);
        buildBeregningsresultatAndel(b2Periode_1, true, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 9, 16);
        buildBeregningsresultatAndel(b2Periode_2, true, 1000, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(førsteRevurdering, beregningsresultatFørsteRevurderingFP);
        Oppdragskontroll oppdragForFørsteRevurdering =
            OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, førsteRevurdering);

        // Andre Revurdering
        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.INNVILGET, false, true);
        LocalDate andreEndringsdato = b2Periode_2.getBeregningsresultatPeriodeTom().minusDays(3);
        BeregningsresultatEntitet beregningsresultatAndreRevurderingFP = buildBeregningsresultatFP(Optional.of(andreEndringsdato));
        BeregningsresultatPeriode b3Periode_1 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 1, 8);
        buildBeregningsresultatAndel(b3Periode_1, true, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode b3Periode_2 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 9, 12);
        buildBeregningsresultatAndel(b3Periode_2, true, 1000, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(andreRevurdering, beregningsresultatAndreRevurderingFP);

        // Act
        Oppdragskontroll oppdragForAndreRevurdering =
            OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andreRevurdering);

        //Assert
        //Første revurdering: Oppdrag110
        List<Oppdrag110> førsteRevurdOpp110Liste = oppdragForFørsteRevurdering.getOppdrag110Liste();
        assertThat(førsteRevurdOpp110Liste).hasSize(1);
        //Førsterevurdering: Oppdragslinjne150
        List<Oppdragslinje150> førsteRevurdOpp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(førsteRevurdOpp110Liste);
        assertThat(førsteRevurdOpp150Liste).hasSize(1);
        Oppdragslinje150 opp150ForOpphIFørsteRevurdering = førsteRevurdOpp150Liste.get(0);
        assertThat(opp150ForOpphIFørsteRevurdering.gjelderOpphør()).isTrue();
        assertThat(opp150ForOpphIFørsteRevurdering.getDatoStatusFom()).isEqualTo(førsteEndringsdato);
        //Andre revurdering: Oppdrag110
        List<Oppdrag110> andreRevurdOpp110Liste = oppdragForAndreRevurdering.getOppdrag110Liste();
        assertThat(andreRevurdOpp110Liste).hasSize(1);
        //Andre revurdering: Oppdragslinjne150
        List<Oppdragslinje150> andreRevurdOpp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(andreRevurdOpp110Liste);
        assertThat(andreRevurdOpp150Liste).hasSize(1);
        Oppdragslinje150 opp150ForOpphIAndreRevurdering = andreRevurdOpp150Liste.get(0);
        assertThat(opp150ForOpphIAndreRevurdering.gjelderOpphør()).isTrue();
        assertThat(opp150ForOpphIAndreRevurdering.getDatoStatusFom()).isEqualTo(andreEndringsdato);
    }

    /**
     * <p>
     * Førstegangsbehandling(B1)
     * <ul>
     * <li>Mottaker: Arbeidsgiver, Inntektskategori: AT</li>
     * <li>To perioder: b1Periode1-b1Periode2</li>
     * </ul>
     * <p>
     * Revurdering#1(B2)
     * <ul>
     * <li>Mottaker: Arbeidsgiver, Inntektskategori: AT</li>
     * <li>To perioder: b2Periode1-b2Periode2</li>
     * <li>Endringsdato: b2Periode2.getTom().plusDays(1)</li>
     * </ul>
     * <p>
     * Revurdering#2(B3):
     * <ul>
     * * <li>Mottaker: Arbeidsgiver, Inntektskategori: AT</li>
     * * <li>To perioder: b3Periode1-b3Periode2</li>
     * * <li>Endringsdato: b3Periode2.getTom().plusDays(1)</li>
     * * </ul>
     */
    @Test
    public void skalKunneSendeOpphørPåForrigeOpphørNårEndringsdatoErEtterSisteTilkjentYtelseperiodeForArbeidsgiverIAndreRevurdering() {

        // Arrange
        BigDecimal utbetalingsgrad = BigDecimal.valueOf(100);

        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 8);
        buildBeregningsresultatAndel(b1Periode_1, false, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 9, 20);
        buildBeregningsresultatAndel(b1Periode_2, false, 1000, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(behandling, beregningsresultatFP_1);
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Første Revurdering
        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate førsteEndringsdato = b1Periode_2.getBeregningsresultatPeriodeTom().minusDays(3);
        BeregningsresultatEntitet beregningsresultatFørsteRevurderingFP = buildBeregningsresultatFP(Optional.of(førsteEndringsdato));
        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 1, 8);
        buildBeregningsresultatAndel(b2Periode_1, false, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode b2Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 9, 16);
        buildBeregningsresultatAndel(b2Periode_2, false, 1000, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(førsteRevurdering, beregningsresultatFørsteRevurderingFP);
        Oppdragskontroll oppdragForFørsteRevurdering =
            OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, førsteRevurdering);

        // Andre Revurdering
        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.INNVILGET, false, true);
        LocalDate andreEndringsdato = b2Periode_2.getBeregningsresultatPeriodeTom().minusDays(3);
        BeregningsresultatEntitet beregningsresultatAndreRevurderingFP = buildBeregningsresultatFP(Optional.of(andreEndringsdato));
        BeregningsresultatPeriode b3Periode_1 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 1, 8);
        buildBeregningsresultatAndel(b3Periode_1, false, 500, utbetalingsgrad, virksomhet);
        BeregningsresultatPeriode b3Periode_2 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 9, 12);
        buildBeregningsresultatAndel(b3Periode_2, false, 1000, utbetalingsgrad, virksomhet);
        beregningsresultatRepository.lagre(andreRevurdering, beregningsresultatAndreRevurderingFP);

        // Act
        Oppdragskontroll oppdragForAndreRevurdering =
            OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andreRevurdering);

        //Assert
        //Første revurdering: Oppdrag110
        List<Oppdrag110> førsteRevurdOpp110Liste = oppdragForFørsteRevurdering.getOppdrag110Liste();
        assertThat(førsteRevurdOpp110Liste).hasSize(1);
        //Førsterevurdering: Oppdragslinjne150
        List<Oppdragslinje150> førsteRevurdOpp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(førsteRevurdOpp110Liste, virksomhet);
        assertThat(førsteRevurdOpp150Liste).hasSize(1);
        Oppdragslinje150 opp150ForOpphIFørsteRevurdering = førsteRevurdOpp150Liste.get(0);
        assertThat(opp150ForOpphIFørsteRevurdering.gjelderOpphør()).isTrue();
        assertThat(opp150ForOpphIFørsteRevurdering.getDatoStatusFom()).isEqualTo(førsteEndringsdato);
        //Andre revurdering: Oppdrag110
        List<Oppdrag110> andreRevurdOpp110Liste = oppdragForAndreRevurdering.getOppdrag110Liste();
        assertThat(andreRevurdOpp110Liste).hasSize(1);
        //Andre revurdering: Oppdragslinjne150
        List<Oppdragslinje150> andreRevurdOpp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForEnVirksomhet(andreRevurdOpp110Liste, virksomhet);
        assertThat(andreRevurdOpp150Liste).hasSize(1);
        Oppdragslinje150 opp150ForOpphIAndreRevurdering = andreRevurdOpp150Liste.get(0);
        assertThat(opp150ForOpphIAndreRevurdering.gjelderOpphør()).isTrue();
        assertThat(opp150ForOpphIAndreRevurdering.getDatoStatusFom()).isEqualTo(andreEndringsdato);
    }

    @Test
    public void skalTesteKjedingAvOppdragslinje150NårDetErFlereRevurderingerISammeSak() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(true, true);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        Oppdragskontroll oppdragForFørstegangsbehandling = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate endringsdato_1 = OppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(9);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingEntenForBrukerEllerArbgvr(true, false, endringsdato_1);
        beregningsresultatRepository.lagre(førsteRevurdering, beregningsresultatRevurderingFP);
        Oppdragskontroll oppdragForFørsteRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, førsteRevurdering);

        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.INNVILGET, false, true);
        LocalDate endringsdato_2 = OppdragskontrollTjenesteTestBase.DAGENS_DATO.plusDays(9);
        BeregningsresultatEntitet beregningsresultatRevurderingFP_2 = buildBeregningsresultatRevurderingEntenForBrukerEllerArbgvr(true, true, endringsdato_2);
        beregningsresultatRepository.lagre(andreRevurdering, beregningsresultatRevurderingFP_2);

        //Act
        Oppdragskontroll oppdragForAndreRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andreRevurdering);

        //Assert
        verifiserKjedingNårDetErFlereBehandlingerMedSammeFagsak(oppdragForFørstegangsbehandling, oppdragForFørsteRevurdering, oppdragForAndreRevurdering, false);
        verifiserFeriepengerNårDetErFlereBehandlingerMedSammeFagsak(oppdragForFørstegangsbehandling, oppdragForFørsteRevurdering, oppdragForAndreRevurdering);
    }

    @Test
    public void skalIkkeFeileMedManglerBeregningsresultatPeriodeHvisOpphørAvOpphør_TFP_1063() {
        // Arrange
        // Førstegangsbehandling
        BeregningsresultatEntitet beregningsresultatFP_1 = buildBeregningsresultatFP(Optional.empty());

        BeregningsresultatPeriode b1Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 1, 31);
        buildBeregningsresultatAndel(b1Periode_1, true, 224, BigDecimal.valueOf(0), null,
            AktivitetStatus.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER);
        buildBeregningsresultatAndel(b1Periode_1, false, 909, BigDecimal.valueOf(50), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        BeregningsresultatPeriode b1Periode_2 = buildBeregningsresultatPeriode(beregningsresultatFP_1, 32, 40);
        buildBeregningsresultatAndel(b1Periode_2, true, 224, BigDecimal.valueOf(0), null,
            AktivitetStatus.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER);
        buildBeregningsresultatAndel(b1Periode_2, true, 616, BigDecimal.valueOf(50), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b1Periode_2, false, 293, BigDecimal.valueOf(50), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        beregningsresultatRepository.lagre(behandling, beregningsresultatFP_1);
        OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, behandling);

        // Første Revurdering
        Behandling førsteRevurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate førsteEndringsdato = b1Periode_1.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatFørsteRevurderingFP = buildBeregningsresultatFP(Optional.of(førsteEndringsdato));

        BeregningsresultatPeriode b2Periode_1 = buildBeregningsresultatPeriode(beregningsresultatFørsteRevurderingFP, 1, 40);
        buildBeregningsresultatAndel(b2Periode_1, true, 452, BigDecimal.valueOf(0), null,
            AktivitetStatus.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER);
        buildBeregningsresultatAndel(b2Periode_1, false, 930, BigDecimal.valueOf(50), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        beregningsresultatRepository.lagre(førsteRevurdering, beregningsresultatFørsteRevurderingFP);

        Oppdragskontroll oppdragForFørsteRevurdering =
            OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, førsteRevurdering);

        // Andre Revurdering
        Behandling andreRevurdering = opprettOgLagreRevurdering(førsteRevurdering, VedtakResultatType.INNVILGET, false, true);
        LocalDate andreEndringsdato = b1Periode_1.getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatAndreRevurderingFP = buildBeregningsresultatFP(Optional.of(andreEndringsdato));

        BeregningsresultatPeriode b3Periode_1 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 1, 31);
        buildBeregningsresultatAndel(b3Periode_1, true, 452, BigDecimal.valueOf(0), null,
            AktivitetStatus.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER);
        buildBeregningsresultatAndel(b3Periode_1, false, 909, BigDecimal.valueOf(50), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        BeregningsresultatPeriode b3Periode_2 = buildBeregningsresultatPeriode(beregningsresultatAndreRevurderingFP, 32, 40);
        buildBeregningsresultatAndel(b3Periode_2, true, 452, BigDecimal.valueOf(0), null,
            AktivitetStatus.ARBEIDSAVKLARINGSPENGER, Inntektskategori.ARBEIDSAVKLARINGSPENGER);
        buildBeregningsresultatAndel(b3Periode_2, true, 616, BigDecimal.valueOf(50), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);
        buildBeregningsresultatAndel(b3Periode_2, false, 293, BigDecimal.valueOf(50), virksomhet,
            AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER);

        beregningsresultatRepository.lagre(andreRevurdering, beregningsresultatAndreRevurderingFP);

        // Act
        Oppdragskontroll oppdragForAndreRevurdering =
            OppdragMedPositivKvitteringTestUtil.opprett(oppdragskontrollTjeneste, andreRevurdering);

        List<Oppdrag110> førsteRevurdOpp110Liste = oppdragForFørsteRevurdering.getOppdrag110Liste();
        assertThat(førsteRevurdOpp110Liste).hasSize(2);
        //Førsterevurdering: Oppdragslinjne150
        List<Oppdragslinje150> førsteRevurdOpp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(førsteRevurdOpp110Liste);
        assertThat(førsteRevurdOpp150Liste).hasSize(3);

        List<Oppdragslinje150> opp150ForOpphPåATIFørsteRevurderingListe = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(førsteRevurdOpp150Liste,
            ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());

        assertThat(opp150ForOpphPåATIFørsteRevurderingListe).hasSize(1);
        assertThat(opp150ForOpphPåATIFørsteRevurderingListe).allSatisfy(opp150 -> {
            assertThat(opp150.gjelderOpphør()).isTrue();
        });

        List<Oppdragslinje150> opp150ForOpphPåFLIFørsteRevurderingListe = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(førsteRevurdOpp150Liste,
            ØkonomiKodeKlassifik.FPATAL.getKodeKlassifik());
        assertThat(opp150ForOpphPåFLIFørsteRevurderingListe).hasSize(2);


        //Andre revurdering: Oppdrag110
        List<Oppdrag110> andreRevurdOpp110Liste = oppdragForAndreRevurdering.getOppdrag110Liste();
        assertThat(andreRevurdOpp110Liste).hasSize(2);
        //Andre revurdering: Oppdragslinjne150
        List<Oppdragslinje150> andreRevurdOpp150Liste = OppdragskontrollTestVerktøy.getOpp150ListeForBruker(andreRevurdOpp110Liste);
        assertThat(andreRevurdOpp150Liste).hasSize(4);

        List<Oppdragslinje150> opp150ForOpphPåATIAndreRevurderingListe = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(andreRevurdOpp150Liste,
            ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik());
        assertThat(opp150ForOpphPåATIAndreRevurderingListe).hasSize(1);
        assertThat(opp150ForOpphPåATIAndreRevurderingListe).allSatisfy(opp150 -> {
            assertThat(opp150.gjelderOpphør()).isFalse();
        });

        List<Oppdragslinje150> opp150ForOpphPåFLIAndreRevurderingListe = OppdragskontrollTestVerktøy.getOppdragslinje150MedKlassekode(andreRevurdOpp150Liste,
            ØkonomiKodeKlassifik.FPATAL.getKodeKlassifik());
        assertThat(opp150ForOpphPåFLIAndreRevurderingListe).hasSize(3);
    }

    private void verifiserOppdrag110KodeEndring
        (List<Oppdrag110> oppdrag110List, List<ØkonomiKodeEndring> kode) {
        assertThat(oppdrag110List).hasSameSizeAs(kode);
        for (int i = 0; i < oppdrag110List.size(); i++) {
            assertThat(oppdrag110List.get(i).getKodeEndring()).isEqualTo(kode.get(i).name());
        }
    }

    private void verifiserOppdragslinje150KodeEndring
        (List<Oppdragslinje150> oppdragslinje150List, List<ØkonomiKodeEndringLinje> kode) {
        assertThat(oppdragslinje150List).hasSameSizeAs(kode);
        for (int i = 0; i < oppdragslinje150List.size(); i++) {
            assertThat(oppdragslinje150List.get(i).getKodeEndringLinje()).isEqualTo(kode.get(i).name());
        }
    }

    private void verifiserOpphørFom(List<Oppdragslinje150> oppdragslinje150Liste, LocalDate endringsdato) {
        List<Oppdragslinje150> opp150ForOpph = oppdragslinje150Liste.stream()
            .filter(Oppdragslinje150::gjelderOpphør)
            .collect(Collectors.toList());
        assertThat(oppdragslinje150Liste).hasSize(1);
        assertThat(opp150ForOpph.get(0).getDatoStatusFom()).isEqualTo(endringsdato);
    }

    private void verifiserKjedingNårDetErFlereBehandlingerMedSammeFagsak(Oppdragskontroll førsteOppdrag, Oppdragskontroll andreOppdrag,
                                                                         Oppdragskontroll tredjeOppdrag, boolean finnesOpphPåOpph) {
        List<Oppdragslinje150> førsteOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(førsteOppdrag);
        List<Oppdragslinje150> andreOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(andreOppdrag);
        List<Oppdragslinje150> tredjeOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(tredjeOppdrag);
        for (Oppdragslinje150 nyesteOpp150 : tredjeOpp150Liste) {
            verifiserTredje(førsteOpp150Liste, andreOpp150Liste, nyesteOpp150, finnesOpphPåOpph);
        }
        for (Oppdragslinje150 opp150FraFørsteRevurdering : andreOpp150Liste) {
            verifiserAndre(førsteOpp150Liste, opp150FraFørsteRevurdering);
        }
    }

    private void verifiserAndre(List<Oppdragslinje150> førsteOpp150Liste, Oppdragslinje150 opp150FraFørsteRevurdering) {
        if (opp150FraFørsteRevurdering.gjelderOpphør()) {
            assertThat(førsteOpp150Liste).anySatisfy(opp150 ->
                assertThat(opp150.getDelytelseId()).isEqualTo(opp150FraFørsteRevurdering.getDelytelseId()));
        } else {
            assertThat(førsteOpp150Liste).allSatisfy(opp150 ->
                assertThat(opp150.getDelytelseId()).isNotEqualTo(opp150FraFørsteRevurdering.getDelytelseId()));
        }
    }

    private void verifiserTredje(List<Oppdragslinje150> førsteOpp150Liste, List<Oppdragslinje150> andreOpp150Liste,
                                 Oppdragslinje150 nyesteOpp150, boolean finnesOpphPåOpph) {
        if (nyesteOpp150.gjelderOpphør()) {
            assertThat(andreOpp150Liste).anySatisfy(opp150 ->
                assertThat(opp150.getDelytelseId()).isEqualTo(nyesteOpp150.getDelytelseId()));
            if (!finnesOpphPåOpph) {
                assertThat(førsteOpp150Liste).allSatisfy(opp150 ->
                    assertThat(opp150.getDelytelseId()).isNotEqualTo(nyesteOpp150.getDelytelseId()));
            }
        } else {
            assertThat(andreOpp150Liste).allSatisfy(opp150 ->
                assertThat(opp150.getDelytelseId()).isNotEqualTo(nyesteOpp150.getDelytelseId()));
            assertThat(førsteOpp150Liste).allSatisfy(opp150 ->
                assertThat(opp150.getDelytelseId()).isNotEqualTo(nyesteOpp150.getDelytelseId()));
        }
    }

    private void verifiserFeriepengerNårDetErFlereBehandlingerMedSammeFagsak(Oppdragskontroll
                                                                                 oppdragForFørstegangsbehandling,
                                                                             Oppdragskontroll oppdragForFørsteRevurdering,
                                                                             Oppdragskontroll oppdragForAndreRevurdering) {
        List<Oppdragslinje150> førsteOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragForFørstegangsbehandling);
        List<Oppdragslinje150> andreOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragForFørsteRevurdering);
        List<Oppdragslinje150> tredjeOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragForAndreRevurdering);
        Optional<Oppdragslinje150> opp150OpphIFørsteRevurdering = andreOpp150Liste.stream()
            .filter(opp150 -> opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik()))
            .findFirst();
        assertThat(opp150OpphIFørsteRevurdering).hasValueSatisfying(opp150Revurdering -> {
            assertThat(førsteOpp150Liste).anySatisfy(førsteOpp150 -> assertThat(førsteOpp150.getDelytelseId()).isEqualTo(opp150Revurdering.getDelytelseId()));
            assertThat(tredjeOpp150Liste).allSatisfy(opp150FraSisteRevurd -> assertThat(opp150Revurdering.getDelytelseId()).isNotEqualTo(opp150FraSisteRevurd.getDelytelseId()));
        });
        Optional<Oppdragslinje150> opp150OpphISisteRevurdering = tredjeOpp150Liste.stream()
            .filter(opp150 -> opp150.getKodeKlassifik().equals(ØkonomiKodeKlassifik.FPATFER.getKodeKlassifik()))
            .findFirst();
        assertThat(opp150OpphISisteRevurdering).hasValueSatisfying(opp150Revurdering -> {
            assertThat(opp150Revurdering.getRefDelytelseId()).isNull();
            assertThat(opp150Revurdering.getRefFagsystemId()).isNull();
        });
    }
}
