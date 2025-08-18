package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ExtendWith(JpaExtension.class)
class BeregningsresultatRepositoryTest {

    private static final String ORGNR = "55";
    private static final AktørId ARBEIDSGIVER_AKTØR_ID = AktørId.dummy();

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private EntityManager entityManager;

    @BeforeEach
    void setup(EntityManager entityManager) {
        this.entityManager = entityManager;
        behandlingRepository = new BehandlingRepository(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
    }

    private Behandling opprettBehandling() {
        return new BasicBehandlingBuilder(entityManager).opprettOgLagreFørstegangssøknad(
            FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    void lagreOgHentBeregningsresultatFPAggregat() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsresultat = buildBeregningsresultatFP(false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        var brKoblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(
            behandling.getId());
        assertThat(brKoblingOpt).hasValueSatisfying(
            brKobling -> assertThat(brKobling.getBgBeregningsresultatFP()).isSameAs(beregningsresultat));
    }

    @Test
    void lagreOgHentUtbetBeregningsresultatFPAggregatNårUTBETIkkeEksisterer() {
        // Arrange
        var behandling = opprettBehandling();
        var bgBeregningsresultatFP = buildBeregningsresultatFP(false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultatFP);

        // Assert
        var utbetBROpt = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId());
        assertThat(utbetBROpt).hasValueSatisfying(
            beregningsresultat -> assertThat(beregningsresultat).isSameAs(bgBeregningsresultatFP));
    }

    @Test
    void lagreOgHentUtbetBeregningsresultatFPAggregatNårUTBETEksisterer() {
        // Arrange
        var behandling = opprettBehandling();
        var bgBeregningsresultatFP = buildBeregningsresultatFP(false);
        var utbetBeregningsresultatFP = buildBeregningsresultatFP(false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultatFP);
        beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBeregningsresultatFP, null);

        // Assert
        var brKoblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(
            behandling.getId());
        assertThat(brKoblingOpt).hasValueSatisfying(
            brKobling -> assertThat(brKobling.getUtbetBeregningsresultatFP())
                .hasValueSatisfying(utbet -> assertThat(utbet).isSameAs(utbetBeregningsresultatFP)));
    }

    @Test
    void lagreOgHenteBeregningsresultatFP() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsresultat = buildBeregningsresultatFP(false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        var id = beregningsresultat.getId();
        assertThat(id).isNotNull();

        var beregningsresultatFPLest = beregningsresultatRepository.hentBeregningsresultat(
            behandling.getId());

        assertThat(beregningsresultatFPLest).isEqualTo(Optional.of(beregningsresultat));
    }

    @Test
    void lagreOgHenteUtbetBeregningsresultatFP() {
        // Arrange
        var behandling = opprettBehandling();
        var bgBeregningsresultatFP = buildBeregningsresultatFP(false);
        var utbetBeregningsresultatFP = buildBeregningsresultatFP(false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultatFP);
        beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBeregningsresultatFP, null);

        // Assert
        var id = utbetBeregningsresultatFP.getId();
        assertThat(id).isNotNull();

        var utbetBeregningsresultatFPLest = beregningsresultatRepository.hentUtbetBeregningsresultat(
            behandling.getId());

        assertThat(utbetBeregningsresultatFPLest).isEqualTo(Optional.of(utbetBeregningsresultatFP));
    }

    @Test
    void lagreOgHenteBeregningsresultatFPMedPrivatpersonSomArbeidsgiver() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsresultat = buildBeregningsresultatFP(true);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        var id = beregningsresultat.getId();
        assertThat(id).isNotNull();

        var beregningsresultatFPLest = beregningsresultatRepository.hentBeregningsresultat(
            behandling.getId());
        assertThat(beregningsresultatFPLest)
            .isPresent()
            .isEqualTo(Optional.of(beregningsresultat));
        var arbeidsgiver = beregningsresultatFPLest.get()
            .getBeregningsresultatPerioder()
            .get(0)
            .getBeregningsresultatAndelList()
            .get(0)
            .getArbeidsgiver()
            .get();
        assertThat(arbeidsgiver.getAktørId()).isEqualTo(ARBEIDSGIVER_AKTØR_ID);
        assertThat(arbeidsgiver.getIdentifikator()).isEqualTo(ARBEIDSGIVER_AKTØR_ID.getId());
    }

    @Test
    void lagreBeregningsresultatFPOgUnderliggendeTabellerMedEndringsdatoLikDagensDato() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsresultat = buildBeregningsresultatFP(false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        var brId = beregningsresultat.getId();
        assertThat(brId).isNotNull();
        var brPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        var brPeriodeId = brPeriode.getId();
        assertThat(brPeriodeId).isNotNull();
        var brAndelId = brPeriode.getBeregningsresultatAndelList().get(0).getId();

        var beregningsresultatFPLest = entityManager.find(BeregningsresultatEntitet.class, brId);
        var brPeriodeLest = entityManager.find(BeregningsresultatPeriode.class, brPeriodeId);
        var brAndelLest = entityManager.find(BeregningsresultatAndel.class, brAndelId);

        assertThat(beregningsresultatFPLest.getId()).isNotNull();
        assertThat(beregningsresultatFPLest.getBeregningsresultatPerioder()).hasSize(1);
        assertThat(beregningsresultatFPLest.getRegelInput()).isEqualTo(beregningsresultat.getRegelInput());
        assertThat(beregningsresultatFPLest.getRegelSporing()).isEqualTo(beregningsresultat.getRegelSporing());
        assertBeregningsresultatPeriode(brPeriodeLest, brAndelLest, brPeriode);
    }

    @Test
    void lagreBeregningsresultatFPOgFeriepenger() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsresultat = buildBeregningsresultatFP(false);
        var feriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(LocalDate.now())
            .medFeriepengerPeriodeTom(LocalDate.now())
            .medFeriepengerRegelInput("-")
            .medFeriepengerRegelSporing("-")
            .build();

        var andel = beregningsresultat.getBeregningsresultatPerioder()
            .get(0)
            .getBeregningsresultatAndelList()
            .get(0);
        BeregningsresultatFeriepengerPrÅr.builder()
            .medAktivitetStatus(andel.getAktivitetStatus())
            .medBrukerErMottaker(andel.erBrukerMottaker())
            .medArbeidsgiver(andel.getArbeidsgiver().orElse(null))
            .medArbeidsforholdRef(andel.getArbeidsforholdRef())
            .medOpptjeningsår(LocalDate.now().withMonth(12).withDayOfMonth(31))
            .medÅrsbeløp(300L)
            .build(feriepenger);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat, feriepenger);

        // Assert
        var hentetResultat = beregningsresultatRepository.hentFeriepenger(behandling.getId());
        assertThat(hentetResultat)
            .isNotNull()
            .isPresent()
            .hasValueSatisfying(this::assertFeriepenger);
    }

    private void assertFeriepenger(BeregningsresultatFeriepenger hentetFeriepenger) {
        var prÅrListe = hentetFeriepenger.getBeregningsresultatFeriepengerPrÅrListe();
        assertThat(prÅrListe).satisfiesOnlyOnce(beregningsresultatFeriepengerPrÅr -> {
            assertThat(beregningsresultatFeriepengerPrÅr.getAktivitetStatus()).isNotNull();
            assertThat(beregningsresultatFeriepengerPrÅr.getOpptjeningsår()).isNotNull();
            assertThat(beregningsresultatFeriepengerPrÅr.getÅrsbeløp()).isNotNull();
        });
    }

    private void assertBeregningsresultatPeriode(BeregningsresultatPeriode brPeriodeLest,
                                                 BeregningsresultatAndel brAndelLest,
                                                 BeregningsresultatPeriode brPeriodeExpected) {
        assertThat(brPeriodeLest).isEqualTo(brPeriodeExpected);
        assertThat(brPeriodeLest.getBeregningsresultatAndelList()).hasSize(1);
        assertThat(brAndelLest).isEqualTo(brPeriodeExpected.getBeregningsresultatAndelList().get(0));
        assertThat(brPeriodeLest.getBeregningsresultatPeriodeFom()).isEqualTo(
            brPeriodeExpected.getBeregningsresultatPeriodeFom());
        assertThat(brPeriodeLest.getBeregningsresultatPeriodeTom()).isEqualTo(
            brPeriodeExpected.getBeregningsresultatPeriodeTom());
    }

    @Test
    void toBehandlingerKanHaSammeBeregningsresultatFP() {
        // Arrange
        var behandling = opprettBehandling();
        var behandling2 = opprettBehandling();
        var beregningsresultat = buildBeregningsresultatFP(false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        beregningsresultatRepository.lagre(behandling2, beregningsresultat);

        // Assert
        var beregningsresultatFP1 = beregningsresultatRepository.hentBeregningsresultat(
            behandling.getId());
        var beregningsresultatFP2 = beregningsresultatRepository.hentBeregningsresultat(
            behandling2.getId());
        assertThat(beregningsresultatFP1).isPresent();
        assertThat(beregningsresultatFP2).isPresent();
        assertThat(beregningsresultatFP1).hasValueSatisfying(
            b -> assertThat(b).isSameAs(beregningsresultatFP2.get()));
    }

    @Test
    void slettBeregningsresultatFPOgKobling() {
        // Arrange
        var behandling = opprettBehandling();
        var beregningsresultat = buildBeregningsresultatFP(false);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        var koblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(
            behandling.getId());

        // Act
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(),
            behandlingRepository.taSkriveLås(behandling));

        //Assert
        var hentetBG = entityManager.find(BeregningsresultatEntitet.class,
            beregningsresultat.getId());
        assertThat(hentetBG).isNotNull();

        var beregningsresultatPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        var hentetBGPeriode = entityManager.find(BeregningsresultatPeriode.class,
            beregningsresultatPeriode.getId());
        assertThat(hentetBGPeriode).isNotNull();

        var beregningsresultatAndel = beregningsresultatPeriode.getBeregningsresultatAndelList()
            .get(0);
        var hentetBRAndel = entityManager.find(BeregningsresultatAndel.class,
            beregningsresultatAndel.getId());
        assertThat(hentetBRAndel).isNotNull();

        var deaktivertBeregningsresultatFP = beregningsresultatRepository.hentBeregningsresultat(
            behandling.getId());
        var deaktivertKobling = beregningsresultatRepository.hentBeregningsresultatAggregat(
            behandling.getId());
        assertThat(deaktivertBeregningsresultatFP).isNotPresent();
        assertThat(deaktivertKobling).isNotPresent();
        assertThat(koblingOpt).hasValueSatisfying(kobling -> assertThat(kobling.erAktivt()).isFalse());
    }

    private BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
                                                                 boolean medPrivatpersonArbeidsgiver) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(
                medPrivatpersonArbeidsgiver ? Arbeidsgiver.person(ARBEIDSGIVER_AKTØR_ID) : Arbeidsgiver.virksomhet(
                    ORGNR))
            .medDagsats(2160)
            .medDagsatsFraBg(2160)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(beregningsresultatPeriode);
    }

    private BeregningsresultatPeriode buildBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.now().minusDays(20), LocalDate.now().minusDays(15))
            .build(beregningsresultat);
    }

    private BeregningsresultatEntitet buildBeregningsresultatFP(boolean medPrivatpersonArbeidsgiver) {
        var builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        var beregningsresultat = builder.build();
        var brPeriode = buildBeregningsresultatPeriode(beregningsresultat);
        buildBeregningsresultatAndel(brPeriode, medPrivatpersonArbeidsgiver);
        return beregningsresultat;
    }

}
