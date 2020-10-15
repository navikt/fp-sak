package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.Repository;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class BeregningsresultatRepositoryTest {

    private static final String ORGNR = "55";

    private static final LocalDate DAGENSDATO = LocalDate.now();

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final Repository repository = repoRule.getRepository();

    private final BeregningsresultatRepository beregningsresultatRepository = new BeregningsresultatRepository(repoRule.getEntityManager());
    private Behandling behandling;
    private AktørId aktørId;

    private final BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(repoRule.getEntityManager());

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        aktørId = AktørId.dummy();
        behandling = opprettBehandling();
    }

    private Behandling opprettBehandling() {
        return behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
    }

    @Test
    public void lagreOgHentBeregningsresultatFPAggregat() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(DAGENSDATO), false);

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
        BeregningsresultatEntitet bgBeregningsresultatFP = buildBeregningsresultatFP(Optional.of(DAGENSDATO), false);

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
        BeregningsresultatEntitet bgBeregningsresultatFP = buildBeregningsresultatFP(Optional.of(DAGENSDATO), false);
        BeregningsresultatEntitet utbetBeregningsresultatFP = buildBeregningsresultatFP(Optional.of(DAGENSDATO.plusDays(1)), false);

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
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(DAGENSDATO), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long id = beregningsresultat.getId();
        assertThat(id).isNotNull();

        repository.flushAndClear();
        Optional<BeregningsresultatEntitet> beregningsresultatFPLest = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());

        assertThat(beregningsresultatFPLest).isEqualTo(Optional.of(beregningsresultat));
    }

    @Test
    public void lagreOgHenteUtbetBeregningsresultatFP() {
        // Arrange
        BeregningsresultatEntitet bgBeregningsresultatFP = buildBeregningsresultatFP(Optional.of(DAGENSDATO), false);
        BeregningsresultatEntitet utbetBeregningsresultatFP = buildBeregningsresultatFP(Optional.of(DAGENSDATO.plusDays(1)), false);

        // Act
        beregningsresultatRepository.lagre(behandling, bgBeregningsresultatFP);
        beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBeregningsresultatFP);

        // Assert
        Long id = utbetBeregningsresultatFP.getId();
        assertThat(id).isNotNull();

        repository.flushAndClear();
        Optional<BeregningsresultatEntitet> utbetBeregningsresultatFPLest = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId());

        assertThat(utbetBeregningsresultatFPLest).isEqualTo(Optional.of(utbetBeregningsresultatFP));
    }

    @Test
    public void lagreOgHenteBeregningsresultatFPMedPrivatpersonSomArbeidsgiver() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(DAGENSDATO), true);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long id = beregningsresultat.getId();
        assertThat(id).isNotNull();

        repository.flushAndClear();
        Optional<BeregningsresultatEntitet> beregningsresultatFPLest = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        assertThat(beregningsresultatFPLest).isEqualTo(Optional.of(beregningsresultat));
        assertThat(beregningsresultatFPLest).isPresent();
        Arbeidsgiver arbeidsgiver = beregningsresultatFPLest.get().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList().get(0).getArbeidsgiver().get();//NOSONAR
        assertThat(arbeidsgiver.getAktørId()).isEqualTo(aktørId);
        assertThat(arbeidsgiver.getIdentifikator()).isEqualTo(aktørId.getId());
    }

    @Test
    public void lagreBeregningsresultatFPOgUnderliggendeTabellerMedEndringsdatoLikDagensDato() {
        // Arrange
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(DAGENSDATO), false);

        // Act
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Assert
        Long brId = beregningsresultat.getId();
        assertThat(brId).isNotNull();
        BeregningsresultatPeriode brPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        Long brPeriodeId = brPeriode.getId();
        assertThat(brPeriodeId).isNotNull();
        Long brAndelId = brPeriode.getBeregningsresultatAndelList().get(0).getId();

        repository.flushAndClear();
        BeregningsresultatEntitet beregningsresultatFPLest = repository.hent(BeregningsresultatEntitet.class, brId);
        BeregningsresultatPeriode brPeriodeLest = repository.hent(BeregningsresultatPeriode.class, brPeriodeId);
        BeregningsresultatAndel brAndelLest = repository.hent(BeregningsresultatAndel.class, brAndelId);

        assertThat(beregningsresultatFPLest.getId()).isNotNull();
        assertThat(beregningsresultatFPLest.getBeregningsresultatPerioder()).hasSize(1);
        assertThat(beregningsresultatFPLest.getRegelInput()).isEqualTo(beregningsresultat.getRegelInput());
        assertThat(beregningsresultatFPLest.getRegelSporing()).isEqualTo(beregningsresultat.getRegelSporing());
        assertThat(beregningsresultatFPLest.getEndringsdato()).isEqualTo(Optional.of(DAGENSDATO));
        assertBeregningsresultatPeriode(brPeriodeLest, brAndelLest, brPeriode);
    }

    @Test
    public void lagreBeregningsresultatFPOgUnderliggendeTabellerMedTomEndringsdato() {
        // Arrange
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

        repository.flushAndClear();
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
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(DAGENSDATO), false);
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
        repository.flushAndClear();
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
        Behandling behandling2 = opprettBehandling();
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(DAGENSDATO), false);

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
        BeregningsresultatEntitet beregningsresultat = buildBeregningsresultatFP(Optional.of(DAGENSDATO), false);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        Optional<BehandlingBeregningsresultatEntitet> koblingOpt = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());

        // Act
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), behandlingRepository.taSkriveLås(behandling));

        //Assert
        BeregningsresultatEntitet hentetBG = repoRule.getEntityManager().find(BeregningsresultatEntitet.class, beregningsresultat.getId());
        assertThat(hentetBG).isNotNull();

        BeregningsresultatPeriode beregningsresultatPeriode = beregningsresultat.getBeregningsresultatPerioder().get(0);
        BeregningsresultatPeriode hentetBGPeriode = repoRule.getEntityManager().find(BeregningsresultatPeriode.class, beregningsresultatPeriode.getId());
        assertThat(hentetBGPeriode).isNotNull();

        BeregningsresultatAndel beregningsresultatAndel = beregningsresultatPeriode.getBeregningsresultatAndelList().get(0);
        BeregningsresultatAndel hentetBRAndel = repoRule.getEntityManager().find(BeregningsresultatAndel.class, beregningsresultatAndel.getId());
        assertThat(hentetBRAndel).isNotNull();

        Optional<BeregningsresultatEntitet> deaktivertBeregningsresultatFP = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        Optional<BehandlingBeregningsresultatEntitet> deaktivertKobling = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getId());
        assertThat(deaktivertBeregningsresultatFP).isNotPresent();
        assertThat(deaktivertKobling).isNotPresent();
        assertThat(koblingOpt).hasValueSatisfying(kobling ->
            assertThat(kobling.erAktivt()).isFalse());
    }

    private BeregningsresultatAndel buildBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode, boolean medPrivatpersonArbeidsgiver) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(medPrivatpersonArbeidsgiver ? Arbeidsgiver.person(aktørId) : Arbeidsgiver.virksomhet(ORGNR))
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
