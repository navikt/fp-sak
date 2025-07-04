package no.nav.foreldrepenger.domene.vedtak.ekstern;

import static java.time.LocalDate.now;
import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@CdiDbAwareTest
class VurderOmSetteUtbetalingPåVentPrivatArbeidsgiverTest {

    private static final String SAKS_BEHANDLER = "Z1236525";
    private static final AktørId AKTØR_ID_PRIVAT_ARBEIDSGIVER = AktørId.dummy();
    private static final AktørId AKTØR_ID_PRIVAT_ARBEIDSGIVER2 = AktørId.dummy();
    private static final String ARBEIDSGIVER_ORGNR = KUNSTIG_ORG;

    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private ØkonomioppdragRepository økonomioppdragRepository;

    private Behandling behandling;
    private Oppdragskontroll oppdragskontroll;
    private BeregningsresultatEntitet beregningsresultat;

    private ArgumentCaptor<LocalDate> førsteUttaksdatoCaptor;
    private ArgumentCaptor<LocalDate> vedtaksdatoCaptor;
    private ArgumentCaptor<AktørId> aktørIdCaptor;

    private final OppgaveTjeneste oppgaveTjenesteMock = mock(OppgaveTjeneste.class);
    private VurderOmSetteUtbetalingPåVentPrivatArbeidsgiver testKlass;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        this.entityManager = entityManager;
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        var behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        økonomioppdragRepository = new ØkonomioppdragRepository(entityManager);
        testKlass = new VurderOmSetteUtbetalingPåVentPrivatArbeidsgiver(beregningsresultatRepository, økonomioppdragRepository,
            oppgaveTjenesteMock, behandlingVedtakRepository);
        behandling = opprettOgLagreBehandling();
        oppdragskontroll = byggOppdagskontroll();
        beregningsresultat = byggBeregningsresultatFP();

        førsteUttaksdatoCaptor = ArgumentCaptor.forClass(LocalDate.class);
        vedtaksdatoCaptor = ArgumentCaptor.forClass(LocalDate.class);
        aktørIdCaptor = ArgumentCaptor.forClass(AktørId.class);
        when(oppgaveTjenesteMock.opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(eq(behandling.getId()),
            førsteUttaksdatoCaptor.capture(),
            vedtaksdatoCaptor.capture(),
            aktørIdCaptor.capture())).thenReturn("OppgaveId");
    }

    @Test
    void skalAndelMedEnPrivatArbeidsgiverReturnereEnAktørId() {
        // Arrange
        var periode2Fom = now().minusMonths(6);
        var periode2Tom = now().minusMonths(3);
        var periode1Fom = now().minusMonths(10);
        var periode1Tom = now().minusMonths(7);

        var beregningResultatPeriode1 = byggBeregningsresultatPeriode(beregningsresultat, periode1Fom, periode1Tom);
        var beregningResultatPeriode2 = byggBeregningsresultatPeriode(beregningsresultat, periode2Fom, periode2Tom);
        byggBeregningsresultatAndel(beregningResultatPeriode1, true, 1000, ARBEIDSGIVER_ORGNR, null);
        byggBeregningsresultatAndel(beregningResultatPeriode1, false, 1000, ARBEIDSGIVER_ORGNR, null);
        byggBeregningsresultatAndel(beregningResultatPeriode2, true, 1000, null, AKTØR_ID_PRIVAT_ARBEIDSGIVER);
        byggBeregningsresultatAndel(beregningResultatPeriode2, false, 1000, null, AKTØR_ID_PRIVAT_ARBEIDSGIVER);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        byggOppdrag150(periode1Fom, periode1Tom, true);
        byggOppdrag150(periode1Fom, periode1Tom, false);
        byggOppdrag150(periode2Fom, periode2Tom, true);
        byggOppdrag150(periode2Fom, periode2Tom, false);
        økonomioppdragRepository.lagre(oppdragskontroll);

        // Act
        testKlass.opprettOppgave(behandling);

        //Assert
        assertThat(førsteUttaksdatoCaptor.getValue()).isEqualTo(periode2Fom);
        assertThat(vedtaksdatoCaptor.getValue()).isEqualTo(LocalDate.now());
        assertThat(aktørIdCaptor.getValue().getId()).isEqualTo(AKTØR_ID_PRIVAT_ARBEIDSGIVER.getId());
    }

    @Test
    void skalVurdereFlerePrivatArbeidsgiver() {
        // Arrange
        var periode1Fom = now().minusMonths(10);
        var periode1Tom = now().minusMonths(7);
        var periode2Fom = now().minusMonths(6);
        var periode2Tom = now().minusMonths(3);

        var beregningResultatPeriode1 = byggBeregningsresultatPeriode(beregningsresultat, periode1Fom, periode1Tom);
        var beregningResultatPeriode2 = byggBeregningsresultatPeriode(beregningsresultat, periode2Fom, periode2Tom);
        byggBeregningsresultatAndel(beregningResultatPeriode1, true, 1000, null, AKTØR_ID_PRIVAT_ARBEIDSGIVER);
        byggBeregningsresultatAndel(beregningResultatPeriode1, false, 1000, null, AKTØR_ID_PRIVAT_ARBEIDSGIVER);
        byggBeregningsresultatAndel(beregningResultatPeriode2, true, 1000, null, AKTØR_ID_PRIVAT_ARBEIDSGIVER2);
        byggBeregningsresultatAndel(beregningResultatPeriode2, false, 1000, null, AKTØR_ID_PRIVAT_ARBEIDSGIVER2);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        byggOppdrag150(periode1Fom, periode1Tom, true);
        byggOppdrag150(periode1Fom, periode1Tom, false);
        byggOppdrag150(periode2Fom, periode2Tom, true);
        byggOppdrag150(periode2Fom, periode2Tom, false);
        økonomioppdragRepository.lagre(oppdragskontroll);

        // Act
        testKlass.opprettOppgave(behandling);

        //Assert
        assertThat(førsteUttaksdatoCaptor.getAllValues()).containsOnly(periode1Fom, periode2Fom);
        assertThat(vedtaksdatoCaptor.getValue()).isEqualTo(LocalDate.now());
        assertThat(aktørIdCaptor.getAllValues()).containsOnly(AKTØR_ID_PRIVAT_ARBEIDSGIVER, AKTØR_ID_PRIVAT_ARBEIDSGIVER2);
    }

    @Test
    void skalVurdereVedtakPerioderMedIngenTilsvarendeAndeler() {
        // Arrange
        var periode1Fom = now().minusMonths(10);
        var periode1Tom = now().minusMonths(7);
        var periode2Fom = now().minusMonths(6);
        var periode2Tom = now().minusMonths(3);

        var beregningResultatPeriode2 = byggBeregningsresultatPeriode(beregningsresultat, periode2Fom, periode2Tom);
        byggBeregningsresultatAndel(beregningResultatPeriode2, false, 10000, null, AKTØR_ID_PRIVAT_ARBEIDSGIVER);
        byggBeregningsresultatAndel(beregningResultatPeriode2, true, 10000, ARBEIDSGIVER_ORGNR, null);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        byggOppdrag150(periode1Fom, periode1Tom, true);
        byggOppdrag150(periode1Fom, periode1Tom, false);
        økonomioppdragRepository.lagre(oppdragskontroll);

        // Act
        testKlass.opprettOppgave(behandling);

        //Assert
        verify(oppgaveTjenesteMock, never()).opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(eq(behandling.getId()),
            førsteUttaksdatoCaptor.capture(), vedtaksdatoCaptor.capture(), aktørIdCaptor.capture());
    }

    @Test
    void skalIgnorereOppdrag150MedKodeStatusLinjeOpphør() {
        // Arrange
        var periode1Fom = now().minusMonths(6);
        var periode1Tom = now().minusMonths(3);

        var beregningResultatPeriode1 = byggBeregningsresultatPeriode(beregningsresultat, periode1Fom, periode1Tom);
        byggBeregningsresultatAndel(beregningResultatPeriode1, true, 0, null, AKTØR_ID_PRIVAT_ARBEIDSGIVER);
        byggBeregningsresultatAndel(beregningResultatPeriode1, false, 0, null, AKTØR_ID_PRIVAT_ARBEIDSGIVER);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        byggOppdrag150(periode1Fom, periode1Tom, true, true);
        byggOppdrag150(periode1Fom, periode1Tom, false, true);
        økonomioppdragRepository.lagre(oppdragskontroll);

        // Act
        testKlass.opprettOppgave(behandling);

        //Assert
        verify(oppgaveTjenesteMock, never()).opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(eq(behandling.getId()),
            førsteUttaksdatoCaptor.capture(), vedtaksdatoCaptor.capture(), aktørIdCaptor.capture());
    }

    @Test
    void skalReturnereTomHvisBeregningsresultatFPFinnesIkke() {
        // Arrange
        var periode1Fom = now().minusMonths(6);
        var periode1Tom = now().minusMonths(3);

        byggOppdrag150(periode1Fom, periode1Tom, true);

        // Act
        testKlass.opprettOppgave(behandling);

        // Assert
        verify(oppgaveTjenesteMock, never()).opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(eq(behandling.getId()),
            førsteUttaksdatoCaptor.capture(), vedtaksdatoCaptor.capture(), aktørIdCaptor.capture());
    }

    @Test
    void skalReturnereTomHvisBeregningsperioderFinnesIkke() {
        // Arrange
        var periode1Fom = now().minusMonths(6);
        var periode1Tom = now().minusMonths(3);

        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        byggOppdrag150(periode1Fom, periode1Tom, true);
        økonomioppdragRepository.lagre(oppdragskontroll);

        // Act
        testKlass.opprettOppgave(behandling);

        // Assert
        verify(oppgaveTjenesteMock, never()).opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(eq(behandling.getId()),
            førsteUttaksdatoCaptor.capture(), vedtaksdatoCaptor.capture(), aktørIdCaptor.capture());
    }

    @Test
    void skalIkkeKasteExceptionHvisOppdragskontrollFinnesIkke() {
        // Act
        testKlass.opprettOppgave(behandling);

        // Assert
        verify(oppgaveTjenesteMock, never()).opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(eq(behandling.getId()),
            førsteUttaksdatoCaptor.capture(), vedtaksdatoCaptor.capture(), aktørIdCaptor.capture());
    }

    private Oppdragslinje150 byggOppdrag150(LocalDate datoVedtakFom, LocalDate datoVedtakTom, boolean erBruker) {
        return byggOppdrag150(datoVedtakFom, datoVedtakTom, erBruker, false);
    }

    private Oppdragslinje150 byggOppdrag150(LocalDate datoVedtakFom, LocalDate datoVedtakTom, boolean erBruker, boolean gjelderOpphør) {
        var builder = Oppdragslinje150.builder()
            .medKodeEndringLinje(KodeEndringLinje.NY)
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("123")
            .medDelytelseId(300L)
            .medKodeKlassifik(finnKodeKlassifik(erBruker))
            .medVedtakFomOgTom(datoVedtakFom, datoVedtakTom)
            .medSats(Sats.på(1000L))
            .medTypeSats(TypeSats.DAG)
            .medUtbetalesTilId("456")
            .medRefFagsystemId(678L)
            .medRefDelytelseId(789L)
            .medOppdrag110(byggOppdrag110());
        if (gjelderOpphør) {
            builder
                .medKodeEndringLinje(KodeEndringLinje.ENDR)
                .medKodeStatusLinje(KodeStatusLinje.OPPH);
        }
        return builder.build();
    }

    private KodeKlassifik finnKodeKlassifik(boolean erBruker) {
        return erBruker ? KodeKlassifik.FPF_ARBEIDSTAKER : KodeKlassifik.FPF_REFUSJON_AG;
    }

    private Oppdrag110 byggOppdrag110() {
        return Oppdrag110.builder()
            .medKodeEndring(KodeEndring.NY)
            .medKodeFagomrade(KodeFagområde.FP)
            .medFagSystemId(250L)
            .medOppdragGjelderId("1")
            .medSaksbehId(SAKS_BEHANDLER)
            .medAvstemming(Avstemming.ny())
            .medOppdragskontroll(oppdragskontroll)
            .build();
    }

    private Oppdragskontroll byggOppdagskontroll() {
        return Oppdragskontroll.builder()
            .medVenterKvittering(false)
            .medSaksnummer(new Saksnummer("12342234"))
            .medBehandlingId(behandling.getId())
            .medProsessTaskId(0L)
            .build();
    }

    private BeregningsresultatEntitet byggBeregningsresultatFP() {
        return BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("sporing")
            .build();
    }

    private BeregningsresultatPeriode byggBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom) {

        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }

    private BeregningsresultatAndel byggBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
                                                                Boolean brukerErMottaker, int dagsats,
                                                                String virksomhetOrgnr, AktørId aktørId) {
        var andelBuilder = BeregningsresultatAndel.builder()
            .medBrukerErMottaker(brukerErMottaker);

        if (virksomhetOrgnr != null) {
            andelBuilder.medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhetOrgnr));
        } else {
            andelBuilder.medArbeidsgiver(Arbeidsgiver.person(aktørId));
        }
        return andelBuilder
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(beregningsresultatPeriode);
    }

    private Behandling opprettOgLagreBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        var behandling = scenario.lagre(repositoryProvider);
        var lås = behandlingRepository.taSkriveLås(behandling);
        Behandlingsresultat.builderForInngangsvilkår()
            .leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING)
            .medRettenTil(RettenTil.HAR_RETT_TIL_FP)
            .medVedtaksbrev(Vedtaksbrev.INGEN)
            .buildFor(behandling);

        var behandlingsresultat = behandling.getBehandlingsresultat();
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        entityManager.persist(behandlingsresultat);

        var behandlingVedtak = byggBehandlingVedtak(LocalDateTime.now(), behandlingsresultat);
        repositoryProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, lås);

        entityManager.flush();

        return behandling;
    }

    private BehandlingVedtak byggBehandlingVedtak(LocalDateTime vedtakDato, Behandlingsresultat behandlingsresultat) {
        return BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(SAKS_BEHANDLER)
            .medVedtakstidspunkt(vedtakDato)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medBehandlingsresultat(behandlingsresultat)
            .build();
    }
}
