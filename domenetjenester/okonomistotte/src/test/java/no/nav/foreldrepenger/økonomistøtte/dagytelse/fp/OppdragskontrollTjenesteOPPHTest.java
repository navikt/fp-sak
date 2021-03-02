package no.nav.foreldrepenger.økonomistøtte.dagytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.økonomistøtte.OppdragMedPositivKvitteringTestUtil;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.OppdragskontrollConstants;

public class OppdragskontrollTjenesteOPPHTest extends OppdragskontrollTjenesteTestBase {

    @Override
    @BeforeEach
    public void setUp() {
        setBrukNyOppdragTjeneste(false);
        super.setUp();
    }

    @Test
    public void skalSendeOppdragForOpphør() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP();
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.AVSLAG, true, false);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        List<Oppdrag110> oppdrag110RevurderingList = verifiserOppdrag110_OPPH(oppdragRevurdering, originaltOppdrag);
        List<Oppdragslinje150> oppdragslinje150Liste = verifiserOppdragslinje150_OPPH(oppdragRevurdering, originaltOppdrag);
        OppdragskontrollTestVerktøy.verifiserGrad(oppdragslinje150Liste, originaltOppdrag);
        OppdragskontrollTestVerktøy.verifiserRefusjonInfo156(oppdrag110RevurderingList, originaltOppdrag);
    }

    @Test
    public void skalSendeOppdragForOpphørNårFørstegangsbehandlingHarFlereInntektskategori() {
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(false);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);
        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.AVSLAG, true, false);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110_OPPH(oppdragRevurdering, originaltOppdrag);
        verifiserOppdragslinje150MedFlereKategorier_OPPH(oppdragRevurdering, originaltOppdrag);
    }

    @Test
    public void skalSendeOpphørFørstSomEnDelAvEndringsoppdragForBruker() {
        // Arrange
        LocalDate fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate tom = LocalDate.of(I_ÅR, 8, 20);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, 1500, 500, fom, tom);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate endringsdato = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatPeriodeFom();
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingEntenForBrukerEllerArbgvr(false, true, endringsdato);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOPPHForBrukerIENDR(oppdragRevurdering, originaltOppdrag110Liste, endringsdato);
    }

    @Test
    public void opphørSkalIkkeSendesHvisEndringstidspunktErEtterAlleTidligereOppdragForBrukerMedFlereKlassekode() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(true);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate sistePeriodeTom = beregningsresultat.getBeregningsresultatPerioder().stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(Comparator.comparing(Function.identity())).get();
        LocalDate endringsdato = sistePeriodeTom.plusMonths(18);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, endringsdato.plusDays(1), endringsdato.plusDays(10));
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100), virksomhet);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        List<Oppdragslinje150> oppdragslinje150OpphørtListe = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(Oppdragslinje150::gjelderOpphør).collect(Collectors.toList());
        assertThat(oppdragslinje150OpphørtListe).isEmpty();
    }

    @Test
    public void opphørSkalIkkeSendesHvisEndringstidspunktErEtterAlleTidligereOppdrag() {
        // Arrange
        LocalDate fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate tom = LocalDate.of(I_ÅR, 8, 7);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, 1500, 500, fom, tom);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);

        LocalDate sistePeriodeTom = beregningsresultat.getBeregningsresultatPerioder().stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(Comparator.comparing(Function.identity())).get();
        LocalDate endringsdato = sistePeriodeTom.plusMonths(18);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, endringsdato.plusDays(1), endringsdato.plusDays(10));
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100), virksomhet);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        List<Oppdragslinje150> oppdragslinje150OpphørtListe = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(Oppdragslinje150::gjelderOpphør).collect(Collectors.toList());
        assertThat(oppdragslinje150OpphørtListe).isEmpty();
    }

    @Test
    public void opphørSkalIkkeSendesForYtelseHvisEndringsdatoErEtterSisteTomDatoITidligereOppdragForArbeidsgiver() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(false, true);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate sistePeriodeTom = beregningsresultat.getBeregningsresultatPerioder().stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(Comparator.comparing(Function.identity())).get();
        LocalDate endringsdato = sistePeriodeTom.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, endringsdato.plusDays(1), endringsdato.plusDays(10));
        buildBeregningsresultatAndel(brPeriode_1, false, 500, BigDecimal.valueOf(100), virksomhet);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110OgOppdragslinje150(oppdragRevurdering, originaltOppdrag110Liste, false);
    }

    @Test
    public void opphørSkalIkkeSendesHvisEndringstidspunktErEtterSisteDatoITidligereOppdrForBrukerMedFlereKlassekodeIForrigeBeh() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(false);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate sistePeriodeTom = beregningsresultat.getBeregningsresultatPerioder().stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(Comparator.comparing(Function.identity())).get();
        LocalDate endringsdato = sistePeriodeTom.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, virksomhet,
            virksomhet, endringsdato, true);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110OgOppdragslinje150(oppdragRevurdering, originaltOppdrag110Liste, true);
    }

    @Test
    public void opphørsDatoenMåSettesLikFørsteDatoVedtakFomNårDenneErSenereEnnEndringstdpktBruker() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(true, true);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate førsteDatoVedtakFom = beregningsresultat.getBeregningsresultatPerioder().stream().min(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom).get();
        LocalDate endringsdato = førsteDatoVedtakFom.minusDays(5);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingEntenForBrukerEllerArbgvr(true, true, endringsdato);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        Oppdragslinje150 oppdragslinje150Opphørt = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(Oppdragslinje150::gjelderOpphør).findFirst().get();
        assertThat(oppdragslinje150Opphørt.getDatoStatusFom()).isEqualTo(førsteDatoVedtakFom);
    }

    @Test
    public void opphørsDatoenMåSettesLikFørsteDatoVedtakFomNårDenneErSenereEnnEndringstdpktForBrukerMedFlereKlassekode() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatMedFlereInntektskategoriFP(false);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate førsteDatoVedtakFom = beregningsresultat.getBeregningsresultatPerioder().stream().min(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom).get();
        LocalDate endringsdato = førsteDatoVedtakFom.minusDays(5);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingFP(AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, virksomhet,
            virksomhet2, endringsdato, true);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        OppdragskontrollTestVerktøy.verifiserOpphørsdatoen(originaltOppdrag, oppdragRevurdering);
    }

    @Test
    public void opphørsDatoenMåSettesLikFørsteDatoVedtakFomAvForrigeOppdragNårDenneErSenereEnnEndringstdpktArbgvr() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(false, true);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate førsteDatoVedtakFom = beregningsresultat.getBeregningsresultatPerioder().stream().min(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom).get();
        LocalDate endringsdato = førsteDatoVedtakFom.minusDays(5);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatRevurderingEntenForBrukerEllerArbgvr(false, true, endringsdato);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        Oppdragslinje150 oppdragslinje150Opphørt = oppdragRevurdering.getOppdrag110Liste().stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste()
            .stream()).filter(Oppdragslinje150::gjelderOpphør).findFirst().get();
        assertThat(oppdragslinje150Opphørt.getDatoStatusFom()).isEqualTo(førsteDatoVedtakFom);
    }

    @Test
    public void opphørSkalIkkeSendesHvisEndringstidspunktErEtterSisteDatoITidligereOppdragForBruker() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatEntenForBrukerEllerArbgvr(true, false);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        Oppdragskontroll originaltOppdrag = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        LocalDate sistePeriodeTom = beregningsresultat.getBeregningsresultatPerioder().stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(Comparator.comparing(Function.identity())).get();
        LocalDate endringsdato = sistePeriodeTom.plusDays(1);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(endringsdato));
        BeregningsresultatPeriode brPeriode_1 = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, endringsdato.plusDays(1), endringsdato.plusDays(10));
        buildBeregningsresultatAndel(brPeriode_1, true, 500, BigDecimal.valueOf(100), virksomhet);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        OppdragskontrollTestVerktøy.verifiserAvstemming(oppdragRevurdering);
        verifiserOppdrag110OgOppdragslinje150(oppdragRevurdering, originaltOppdrag110Liste, false);
    }

    // Førstegangsbehandling: VedtakResultat=Innvilget, Behandlingsresultat=Innvilget, FinnnesTilkjentYtelse=Ja
    // Revurdering: VedtakResultat=Innvilget, Behandlingsresultat=Foreldrepenger endret, FinnesTilkjentYtelse=Nei(Utbetalingsgrad=0)
    @Test
    public void skalSendeOpphørNårForrigeBehandlingHarTilkjentYtelseOgRevurderingHarIngenTilkjentYtelsePgaNullUtbetalingsgradSomBlirSattIUttak() {
        // Arrange
        LocalDate fom = LocalDate.of(I_ÅR, 8, 1);
        LocalDate tom = LocalDate.of(I_ÅR, 8, 15);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty());
        BeregningsresultatPeriode brPeriode = buildBeregningsresultatPeriode(beregningsresultat, fom, tom);
        buildBeregningsresultatAndel(brPeriode, true, 2000, BigDecimal.valueOf(100), virksomhet);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatFP(Optional.of(fom));
        BeregningsresultatPeriode brPeriodeRevurdering = buildBeregningsresultatPeriode(beregningsresultatRevurderingFP, fom, tom);
        buildBeregningsresultatAndel(brPeriodeRevurdering, true, 0, BigDecimal.valueOf(0), virksomhet);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        //Assert
        LocalDate forrigeYtelseStartDato = brPeriode.getBeregningsresultatPeriodeFom();
        List<Oppdragslinje150> opp150RevurderingListe = oppdragRevurdering.getOppdrag110Liste().stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());
        assertThat(opp150RevurderingListe).hasSize(1);
        assertThat(opp150RevurderingListe).allSatisfy(oppdragslinje150 -> {
                assertThat(oppdragslinje150.gjelderOpphør()).isTrue();
                assertThat(oppdragslinje150.getDatoStatusFom()).isEqualTo(forrigeYtelseStartDato);
            }
        );
    }

    @Test
    public void skalSendeOppdragUtenOmposteringHvisFullstendigOpphørPåBruker() {
        // Arrange
        LocalDate b1fom = LocalDate.of(I_ÅR, 1, 1);
        LocalDate b1tom = LocalDate.of(I_ÅR, 8, 20);
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatBrukerFP(null, 400, 400, b1fom, b1tom);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), behandling);

        Behandling revurdering = opprettOgLagreRevurdering(behandling, VedtakResultatType.INNVILGET, false, true);
        BeregningsresultatEntitet beregningsresultatRevurderingFP = buildBeregningsresultatBrukerFP(b1fom, 0, 300, b1fom, b1tom);
        beregningsresultatRepository.lagre(revurdering, beregningsresultatRevurderingFP);

        // Act
        Oppdragskontroll oppdragRevurdering = OppdragMedPositivKvitteringTestUtil.opprett(getOppdragTjeneste(), revurdering);

        // Assert
        Oppdrag110 oppdrag110Bruker = OppdragskontrollTestVerktøy.getOppdrag110ForBruker(oppdragRevurdering.getOppdrag110Liste());
        assertThat(oppdrag110Bruker.getKodeEndring()).isEqualTo(OppdragskontrollConstants.KODE_ENDRING_UENDRET);
        assertThat(oppdrag110Bruker.getOmpostering116()).isNotPresent();
    }

    private void verifiserOppdrag110OgOppdragslinje150(Oppdragskontroll oppdragskontroll, List<Oppdrag110> originaltOpp110Liste, boolean medFlereKlassekode) {
        List<Oppdrag110> nyOppdr110Liste = oppdragskontroll.getOppdrag110Liste();
        for (Oppdrag110 oppdr110Revurd : nyOppdr110Liste) {
            assertThat(oppdr110Revurd.getKodeEndring()).isEqualTo(gjelderFagområdeBruker(oppdr110Revurd)
                ? KodeEndring.ENDRING
                : KodeEndring.UENDRET);
            assertThat(oppdr110Revurd.getOppdragslinje150Liste()).isNotEmpty();
            assertThat(originaltOpp110Liste).anySatisfy(oppdrag110 ->
                assertThat(oppdrag110.getFagsystemId()).isEqualTo(oppdr110Revurd.getFagsystemId()));
        }
        List<Oppdragslinje150> opp150RevurderingListe = nyOppdr110Liste.stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream()).collect(Collectors.toList());
        List<Oppdragslinje150> opp150OriginalListe = originaltOpp110Liste.stream().flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream()).collect(Collectors.toList());
        assertThat(opp150RevurderingListe).anySatisfy(opp150 -> assertThat(opp150.getKodeStatusLinje()).isNull());
        if (medFlereKlassekode) {
            OppdragskontrollTestVerktøy.verifiserDelYtelseOgFagsystemIdForEnKlassekode(opp150RevurderingListe, opp150OriginalListe);
        } else {
            OppdragskontrollTestVerktøy.verifiserDelYtelseOgFagsystemIdForFlereKlassekode(opp150RevurderingListe, opp150OriginalListe);
        }
        for (Oppdragslinje150 opp150Revurdering : opp150RevurderingListe) {
            assertThat(opp150OriginalListe).allSatisfy(opp150 ->
                assertThat(opp150.getDelytelseId()).isNotEqualTo(opp150Revurdering.getDelytelseId()));
            if (!gjelderFagområdeBruker(opp150Revurdering.getOppdrag110())) {
                assertThat(opp150Revurdering.getRefusjonsinfo156()).isNotNull();
            }
            if (!OppdragskontrollTestVerktøy.erOpp150ForFeriepenger(opp150Revurdering)) {
                assertThat(opp150Revurdering.getUtbetalingsgrad()).isNotNull();
            } else {
                assertThat(opp150Revurdering.getUtbetalingsgrad()).isNull();
            }
        }
    }

    private void verifiserOPPHForBrukerIENDR(Oppdragskontroll oppdragRevurdering, List<Oppdrag110> originaltOpp110Liste, LocalDate endringsdato) {
        Optional<Oppdrag110> nyOppdr110Bruker = oppdragRevurdering.getOppdrag110Liste()
            .stream()
            .filter(this::gjelderFagområdeBruker)
            .findFirst();
        assertThat(nyOppdr110Bruker).isPresent();
        assertThat(nyOppdr110Bruker).hasValueSatisfying(opp110 ->
        {
            assertThat(opp110.getKodeEndring()).isEqualTo(KodeEndring.UENDRET);
            assertThat(opp110.getOppdragslinje150Liste()).isNotEmpty();
            assertThat(originaltOpp110Liste).anySatisfy(oppdrag110 ->
                assertThat(oppdrag110.getFagsystemId()).isEqualTo(nyOppdr110Bruker.get().getFagsystemId()));
        });
        verifiserOppdragslinje150_OPPH_Bruker_I_ENDR(originaltOpp110Liste, nyOppdr110Bruker.get(), endringsdato);
    }

    private void verifiserOppdragslinje150_OPPH_Bruker_I_ENDR(List<Oppdrag110> opp110OriginalListe, Oppdrag110 nyOpp110Bruker, LocalDate endringsdato) {
        List<Oppdragslinje150> originaltOpp150BrukerListe = opp110OriginalListe.stream()
            .filter(this::gjelderFagområdeBruker)
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.toList());
        List<Oppdragslinje150> revurderingOpp150BrukerListe = nyOpp110Bruker.getOppdragslinje150Liste();

        assertThat(revurderingOpp150BrukerListe).anySatisfy(opp150 ->
            assertThat(opp150.getKodeKlassifik()).isEqualTo(KodeKlassifik.FERIEPENGER_BRUKER));

        for (int ix = 0; ix < revurderingOpp150BrukerListe.size(); ix++) {
            Oppdragslinje150 revurderingOpp150Bruker = revurderingOpp150BrukerListe.get(ix);
            Oppdragslinje150 originaltOpp150Bruker = originaltOpp150BrukerListe.get(ix);
            assertThat(revurderingOpp150Bruker.getDelytelseId()).isEqualTo(originaltOpp150Bruker.getDelytelseId());
            assertThat(revurderingOpp150Bruker.getRefDelytelseId()).isNull();
            assertThat(revurderingOpp150Bruker.getRefFagsystemId()).isNull();
            assertThat(revurderingOpp150Bruker.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.ENDRING);
            assertThat(revurderingOpp150Bruker.getKodeStatusLinje()).isEqualTo(KodeStatusLinje.OPPHØR);
            LocalDate førsteDatoVedtakFom = OppdragskontrollTestVerktøy.finnFørsteDatoVedtakFom(originaltOpp150BrukerListe, originaltOpp150Bruker);
            LocalDate datoStatusFom = førsteDatoVedtakFom.isAfter(endringsdato) ? førsteDatoVedtakFom : endringsdato;
            assertThat(revurderingOpp150Bruker.getDatoStatusFom()).isEqualTo(revurderingOpp150Bruker.getKodeKlassifik().equals(KodeKlassifik.FERIEPENGER_BRUKER)
                ? LocalDate.of(I_ÅR + 1, 5, 1) : datoStatusFom);
            assertThat(revurderingOpp150Bruker.getSats()).isEqualTo(originaltOpp150Bruker.getSats());
        }
    }

    private List<Oppdrag110> verifiserOppdrag110_OPPH(Oppdragskontroll oppdragRevurdering, Oppdragskontroll originaltOppdrag) {
        List<Oppdrag110> originaltOppdrag110Liste = originaltOppdrag.getOppdrag110Liste();
        List<Oppdrag110> nyOppdr110Liste = oppdragRevurdering.getOppdrag110Liste();
        verifiserAlleOppdragOpphørt(originaltOppdrag110Liste, nyOppdr110Liste);

        assertThat(oppdragRevurdering.getOppdrag110Liste()).hasSameSizeAs(originaltOppdrag110Liste);
        for (int ix110 = 0; ix110 < nyOppdr110Liste.size(); ix110++) {
            assertThat(nyOppdr110Liste.get(ix110).getKodeEndring()).isEqualTo(KodeEndring.UENDRET);
            assertThat(nyOppdr110Liste.get(ix110).getFagsystemId()).isEqualTo(originaltOppdrag110Liste.get(ix110).getFagsystemId());
            assertThat(nyOppdr110Liste.get(ix110).getOppdragslinje150Liste()).isNotEmpty();
        }
        return nyOppdr110Liste;
    }

    private void verifiserAlleOppdragOpphørt(List<Oppdrag110> originaltOpp110Liste, List<Oppdrag110> nyOppdr110Liste) {
        for (Oppdrag110 originalt : originaltOpp110Liste) {
            Oppdrag110 nyttOppdrag = nyOppdr110Liste.stream()
                .filter(nytt -> originalt.getFagsystemId() == nytt.getFagsystemId())
                .findFirst().orElse(null);
            assertThat(nyttOppdrag).isNotNull();
            List<KodeKlassifik> klassifikasjoner = originalt.getOppdragslinje150Liste().stream()
                .map(Oppdragslinje150::getKodeKlassifik)
                .distinct()
                .collect(Collectors.toList());
            for (KodeKlassifik klassifikasjon : klassifikasjoner) {
                Optional<Oppdragslinje150> opphørslinje = nyttOppdrag.getOppdragslinje150Liste().stream()
                    .filter(opp150 -> klassifikasjon.equals(opp150.getKodeKlassifik()))
                    .filter(opp150 -> KodeEndringLinje.ENDRING.equals(opp150.getKodeEndringLinje()))
                    .filter(opp150 -> KodeStatusLinje.OPPHØR.equals(opp150.getKodeStatusLinje()))
                    .findFirst();
                assertThat(opphørslinje)
                    .as("Mangler oppdragslinje med opphør for klassifikasjon %s i oppdrag %s", klassifikasjon,
                        nyttOppdrag.getFagsystemId())
                    .isNotEmpty();
            }
        }
    }

    private void verifiserOppdragslinje150MedFlereKategorier_OPPH(Oppdragskontroll oppdragRevurdering, Oppdragskontroll originaltOppdrag) {
        Map<KodeKlassifik, List<Oppdragslinje150>> originaltOppLinjePerKodeKl = originaltOppdrag.getOppdrag110Liste().stream()
            .filter(this::gjelderFagområdeBruker)
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.groupingBy(Oppdragslinje150::getKodeKlassifik));
        Map<KodeKlassifik, List<Oppdragslinje150>> nyOpp150LinjePerKodeKl = oppdragRevurdering.getOppdrag110Liste().stream()
            .filter(this::gjelderFagområdeBruker)
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .collect(Collectors.groupingBy(Oppdragslinje150::getKodeKlassifik));

        for (KodeKlassifik kodeKlassifik : originaltOppLinjePerKodeKl.keySet()) {
            Oppdragslinje150 sisteOriginaltOppdragsLinje = originaltOppLinjePerKodeKl.get(kodeKlassifik).stream()
                .max(Comparator.comparing(Oppdragslinje150::getDelytelseId)).get();
            Oppdragslinje150 sisteNyOppdragsLinje = nyOpp150LinjePerKodeKl.get(kodeKlassifik).get(0);
            assertThat(sisteOriginaltOppdragsLinje.getDelytelseId()).isEqualTo(sisteNyOppdragsLinje.getDelytelseId());
            assertThat(sisteNyOppdragsLinje.getRefDelytelseId()).isNull();
            assertThat(sisteNyOppdragsLinje.getRefFagsystemId()).isNull();
            assertThat(sisteNyOppdragsLinje.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.ENDRING);
            assertThat(sisteNyOppdragsLinje.getKodeStatusLinje()).isEqualTo(KodeStatusLinje.OPPHØR);
        }
    }

    private List<Oppdragslinje150> verifiserOppdragslinje150_OPPH(Oppdragskontroll oppdragRevurdering, Oppdragskontroll originaltOppdrag) {
        List<Oppdragslinje150> originaltOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(originaltOppdrag);
        List<Oppdragslinje150> nyOpp150Liste = OppdragskontrollTestVerktøy.getOppdragslinje150Liste(oppdragRevurdering);

        for (Oppdragslinje150 nyOpp150 : nyOpp150Liste) {
            Oppdragslinje150 originaltOpp150 = originaltOpp150Liste.stream()
                .filter(oppdragslinje150 -> oppdragslinje150.getDatoVedtakFom().equals(nyOpp150.getDatoVedtakFom())
                    && oppdragslinje150.getDatoVedtakTom().equals(nyOpp150.getDatoVedtakTom())
                    && oppdragslinje150.getOppdrag110().getKodeFagomrade().equals(nyOpp150.getOppdrag110().getKodeFagomrade()))
                .findFirst().get();
            assertThat(nyOpp150.getDelytelseId()).isEqualTo(originaltOpp150.getDelytelseId());
            assertThat(nyOpp150.getRefDelytelseId()).isNull();
            assertThat(nyOpp150.getRefFagsystemId()).isNull();
            assertThat(nyOpp150.getKodeEndringLinje()).isEqualTo(KodeEndringLinje.ENDRING);
            assertThat(nyOpp150.getKodeStatusLinje()).isEqualTo(KodeStatusLinje.OPPHØR);
            assertThat(nyOpp150.getSats()).isEqualTo(originaltOpp150.getSats());
        }
        return nyOpp150Liste;
    }

    private boolean gjelderFagområdeBruker(Oppdrag110 oppdrag110) {
        return KodeFagområde.FORELDREPENGER_BRUKER.equals(oppdrag110.getKodeFagomrade());
    }
}
