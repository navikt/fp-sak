package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.felles.testutilities.db.Repository;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BeregningsresultatRepositoryTest extends EntityManagerAwareTest {

    private static final String ORGNR = "55";
    private static final AktørId ARBEIDSGIVER_AKTØR_ID = AktørId.dummy();

    private BehandlingRepository behandlingRepository;

    private BeregningsresultatRepository beregningsresultatRepository;
    private Repository repository;

    @BeforeEach
    void setup() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        repository = new Repository(entityManager);
    }

    private Behandling opprettBehandling() {
        return new BasicBehandlingBuilder(getEntityManager())
            .opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    public void lagreOgHentBeregningsresultatFPAggregat() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(LocalDate.now()), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Optional<BehandlingBeregningsresultatEntitet> brKoblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        assertThat(brKoblingOpt).hasValueSatisfying(brKobling ->
            assertThat(brKobling.getBgBeregningsresultatFP()).isSameAs(beregningsresultat)
        );
    }

    @Test
    public void lagreOgHentUtbetBeregningsresultatFPAggregatNårUTBETIkkeEksisterer() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsresultatEntitet bgBeregningsresultatFP = buildBeregningsresultatFP(Optional.of(LocalDate.now()), false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultatFP);

        // Assert
        var utbetBROpt = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId());
        assertThat(utbetBROpt).hasValueSatisfying(beregningsresultat ->
            assertThat(beregningsresultat).isSameAs(bgBeregningsresultatFP)
        );
    }

    @Test
    public void lagreOgHentUtbetBeregningsresultatFPAggregatNårUTBETEksisterer() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsresultatEntitet bgBeregningsresultatFP = buildBeregningsresultatFP(Optional.of(LocalDate.now()), false);
        BeregningsresultatEntitet utbetBeregningsresultatFP = buildBeregningsresultatFP(Optional.of(LocalDate.now().plusDays(1)), false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultatFP);
        beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBeregningsresultatFP);

        // Assert
        Optional<BehandlingBeregningsresultatEntitet> brKoblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        assertThat(brKoblingOpt).hasValueSatisfying(brKobling ->
            assertThat(brKobling.getUtbetBeregningsresultatFP()).isSameAs(utbetBeregningsresultatFP)
        );
    }

    @Test
    public void lagreOgHenteBeregningsresultatFP() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(LocalDate.now()), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long id = beregningsresultat.getId();
        assertThat(id).isNotNull();

        Optional<BeregningsresultatEntitet> beregningsresultatFPLest = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());

        assertThat(beregningsresultatFPLest).isEqualTo(Optional.of(beregningsresultat));
    }

    @Test
    public void lagreOgHenteUtbetBeregningsresultatFP() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsresultatEntitet bgBeregningsresultatFP = buildBeregningsresultatFP(Optional.of(LocalDate.now()), false);
        BeregningsresultatEntitet utbetBeregningsresultatFP = buildBeregningsresultatFP(Optional.of(LocalDate.now().plusDays(1)), false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultatFP);
        beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBeregningsresultatFP);

        // Assert
        Long id = utbetBeregningsresultatFP.getId();
        assertThat(id).isNotNull();

        Optional<BeregningsresultatEntitet> utbetBeregningsresultatFPLest = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId());

        assertThat(utbetBeregningsresultatFPLest).isEqualTo(Optional.of(utbetBeregningsresultatFP));
    }

    @Test
    public void lagreOgHenteBeregningsresultatFPMedPrivatpersonSomArbeidsgiver() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(LocalDate.now()), true);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long id = beregningsresultat.getId();
        assertThat(id).isNotNull();

        Optional<BeregningsresultatEntitet> beregningsresultatFPLest = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        assertThat(beregningsresultatFPLest).isEqualTo(Optional.of(beregningsresultat));
        assertThat(beregningsresultatFPLest).isPresent();
        Arbeidsgiver arbeidsgiver = beregningsresultatFPLest.get().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList().get(0).getArbeidsgiver().get();//NOSONAR
        assertThat(arbeidsgiver.getAktørId()).isEqualTo(ARBEIDSGIVER_AKTØR_ID);
        assertThat(arbeidsgiver.getIdentifikator()).isEqualTo(ARBEIDSGIVER_AKTØR_ID.getId());
    }

    @Test
    public void lagreBeregningsresultatFPOgUnderliggendeTabellerMedEndringsdatoLikDagensDato() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(LocalDate.now()), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long brId = beregningsresultat.getId();
        assertThat(brId).isNotNull();
        BeregningsresultatPeriode brPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        Long brPeriodeId = brPeriode.getId();
        assertThat(brPeriodeId).isNotNull();
        Long brAndelId = brPeriode.getBeregningsresultatAndelList().get(0).getId();

        BeregningsresultatEntitet beregningsresultatFPLest = repository.hent(BeregningsresultatEntitet.class, brId);
        BeregningsresultatPeriode brPeriodeLest = repository.hent(BeregningsresultatPeriode.class, brPeriodeId);
        BeregningsresultatAndel brAndelLest = repository.hent(BeregningsresultatAndel.class, brAndelId);

        assertThat(beregningsresultatFPLest.getId()).isNotNull();
        assertThat(beregningsresultatFPLest.getBeregningsresultatPerioder()).hasSize(1);
        assertThat(beregningsresultatFPLest.getRegelInput()).isEqualTo(beregningsresultat.getRegelInput());
        assertThat(beregningsresultatFPLest.getRegelSporing()).isEqualTo(beregningsresultat.getRegelSporing());
        assertThat(beregningsresultatFPLest.getEndringsdato()).isEqualTo(Optional.of(LocalDate.now()));
        assertBeregningsresultatPeriode(brPeriodeLest, brAndelLest, brPeriode);
    }

    @Test
    public void lagreBeregningsresultatFPOgUnderliggendeTabellerMedTomEndringsdato() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.empty(), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long brId = beregningsresultat.getId();
        assertThat(brId).isNotNull();
        BeregningsresultatPeriode brPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        Long brPeriodeId = brPeriode.getId();
        assertThat(brPeriodeId).isNotNull();
        Long brAndelId = brPeriode.getBeregningsresultatAndelList().get(0).getId();

        BeregningsresultatEntitet beregningsresultatFPLest = repository.hent(BeregningsresultatEntitet.class, brId);
        BeregningsresultatPeriode brPeriodeLest = repository.hent(BeregningsresultatPeriode.class, brPeriodeId);
        BeregningsresultatAndel brAndelLest = repository.hent(BeregningsresultatAndel.class, brAndelId);

        assertThat(beregningsresultatFPLest.getId()).isNotNull();
        assertThat(beregningsresultatFPLest.getBeregningsresultatPerioder()).hasSize(1);
        assertThat(beregningsresultatFPLest.getRegelInput()).isEqualTo(beregningsresultat.getRegelInput());
        assertThat(beregningsresultatFPLest.getRegelSporing()).isEqualTo(beregningsresultat.getRegelSporing());
        assertThat(beregningsresultatFPLest.getEndringsdato()).isEmpty();
        assertBeregningsresultatPeriode(brPeriodeLest, brAndelLest, brPeriode);
    }

    @Test
    public void lagreBeregningsresultatFPOgFeriepenger() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(LocalDate.now()), false);
        BeregningsresultatFeriepenger feriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(LocalDate.now())
            .medFeriepengerPeriodeTom(LocalDate.now())
            .medFeriepengerRegelInput("-")
            .medFeriepengerRegelSporing("-")
            .build(beregningsresultat);

        BeregningsresultatAndel andel = beregningsresultat.getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList().get(0);
        BeregningsresultatFeriepengerPrÅr.builder()
            .medOpptjeningsår(LocalDate.now().withMonth(12).withDayOfMonth(31))
            .medÅrsbeløp(300L)
            .build(feriepenger, andel);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        BeregningsresultatEntitet hentetResultat = repository.hent(BeregningsresultatEntitet.class, beregningsresultat.getId());
        assertThat(hentetResultat).isNotNull();
        assertThat(hentetResultat.getBeregningsresultatFeriepenger()).isPresent();
        assertThat(hentetResultat.getBeregningsresultatFeriepenger()).hasValueSatisfying(this::assertFeriepenger);
    }

    private void assertFeriepenger(BeregningsresultatFeriepenger hentetFeriepenger) {
        List<BeregningsresultatFeriepengerPrÅr> prÅrListe = hentetFeriepenger.getBeregningsresultatFeriepengerPrÅrListe();
        assertThat(prÅrListe).hasOnlyOneElementSatisfying(beregningsresultatFeriepengerPrÅr -> {
            assertThat(beregningsresultatFeriepengerPrÅr.getBeregningsresultatAndel()).isNotNull();
            assertThat(beregningsresultatFeriepengerPrÅr.getOpptjeningsår()).isNotNull();
            assertThat(beregningsresultatFeriepengerPrÅr.getÅrsbeløp()).isNotNull();
        });
    }

    private void assertBeregningsresultatPeriode(BeregningsresultatPeriode brPeriodeLest, BeregningsresultatAndel brAndelLest, BeregningsresultatPeriode brPeriodeExpected) {
        assertThat(brPeriodeLest).isEqualTo(brPeriodeExpected);
        assertThat(brPeriodeLest.getBeregningsresultatAndelList()).hasSize(1);
        assertThat(brAndelLest).isEqualTo(brPeriodeExpected.getBeregningsresultatAndelList().get(0));
        assertThat(brPeriodeLest.getBeregningsresultatPeriodeFom()).isEqualTo(brPeriodeExpected.getBeregningsresultatPeriodeFom());
        assertThat(brPeriodeLest.getBeregningsresultatPeriodeTom()).isEqualTo(brPeriodeExpected.getBeregningsresultatPeriodeTom());
    }

    @Test
    public void toBehandlingerKanHaSammeBeregningsresultatFP() {
        // Arrange
        var behandling = opprettBehandling();
        var behandling2 = opprettBehandling();
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(LocalDate.now()), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);
        beregningsresultatRepository.lagre(behandling2, beregningsresultat);

        // Assert
        Optional<BeregningsresultatEntitet> beregningsresultatFP1 = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        Optional<BeregningsresultatEntitet> beregningsresultatFP2 = beregningsresultatRepository.hentBeregningsresultat(behandling2.getId());
        assertThat(beregningsresultatFP1).isPresent();
        assertThat(beregningsresultatFP2).isPresent();
        assertThat(beregningsresultatFP1).hasValueSatisfying(b -> assertThat(b).isSameAs(beregningsresultatFP2.get())); //NOSONAR
    }

    @Test
    public void slettBeregningsresultatFPOgKobling() {
        // Arrange
        var behandling = opprettBehandling();
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(LocalDate.now()), false);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        Optional<BehandlingBeregningsresultatEntitet> koblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Act
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), behandlingRepository.taSkriveLås(behandling));

        //Assert
        var entityManager = getEntityManager();
        BeregningsresultatEntitet hentetBG = entityManager.find(BeregningsresultatEntitet.class, beregningsresultat.getId());
        assertThat(hentetBG).isNotNull();

        BeregningsresultatPeriode beregningsresultatPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        BeregningsresultatPeriode hentetBGPeriode = entityManager.find(BeregningsresultatPeriode.class, beregningsresultatPeriode.getId());
        assertThat(hentetBGPeriode).isNotNull();

        BeregningsresultatAndel beregningsresultatAndel = beregningsresultatPeriode.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel hentetBRAndel = entityManager.find(BeregningsresultatAndel.class, beregningsresultatAndel.getId());
        assertThat(hentetBRAndel).isNotNull();

        Optional<BeregningsresultatEntitet> deaktivertBeregningsresultatFP = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        Optional<BehandlingBeregningsresultatEntitet> deaktivertKobling = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        assertThat(deaktivertBeregningsresultatFP).isNotPresent();
        assertThat(deaktivertKobling).isNotPresent();
        assertThat(koblingOpt).hasValueSatisfying(kobling ->
            assertThat(kobling.erAktivt()).isFalse());
    }

    private BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
                                                                 boolean medPrivatpersonArbeidsgiver) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(medPrivatpersonArbeidsgiver ? Arbeidsgiver.person(ARBEIDSGIVER_AKTØR_ID) : Arbeidsgiver.virksomhet(ORGNR))
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

    private BeregningsresultatEntitet buildBeregningsresultatFP(Optional<LocalDate> endringsdato, boolean medPrivatpersonArbeidsgiver) {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        endringsdato.ifPresent(builder::medEndringsdato);
        BeregningsresultatEntitet beregningsresultat = builder.build();
        BeregningsresultatPeriode brPeriode = buildBeregningsresultatPeriode(beregningsresultat);
        buildBeregningsresultatAndel(brPeriode, medPrivatpersonArbeidsgiver);
        return beregningsresultat;
    }

}
