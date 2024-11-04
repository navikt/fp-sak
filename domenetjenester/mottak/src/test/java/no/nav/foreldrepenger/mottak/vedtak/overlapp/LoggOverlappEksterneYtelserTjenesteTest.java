package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdPSGrunnlag;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdSPGrunnlag;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Periode;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Tema;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.TemaKode;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Vedtak;
import no.nav.vedtak.felles.integrasjon.spokelse.Spøkelse;
import no.nav.vedtak.felles.integrasjon.spokelse.SykepengeVedtak;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
class LoggOverlappEksterneYtelserTjenesteTest extends EntityManagerAwareTest {

    private static final Logger LOG = LoggerFactory.getLogger(LoggOverlappEksterneYtelserTjenesteTest.class);
    private LoggOverlappEksterneYtelserTjeneste overlappendeInfotrygdYtelseTjeneste;

    private BehandlingRepositoryProvider repositoryProvider;
    private BeregningsresultatRepository beregningsresultatRepository;
    private OverlappVedtakRepository overlappRepository;

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private InfotrygdPSGrunnlag infotrygdPSGrTjenesteMock;
    @Mock
    private InfotrygdSPGrunnlag infotrygdSPGrTjenesteMock;
    @Mock
    private Spøkelse spøkelseMock;
    @Mock
    private AbakusTjeneste abakusMock;
    @Mock
    private OppgaveTjeneste oppgaveTjenesteMock;

    private Behandling behandlingFP;
    private LocalDate førsteUttaksdatoFp;

    @BeforeEach
    public void oppsett() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        overlappRepository = new OverlappVedtakRepository(getEntityManager());
        var behandlingRepository = new BehandlingRepository(getEntityManager());
        beregningsresultatRepository = new BeregningsresultatRepository(getEntityManager());
        var overlappOppgaveTjeneste = new OverlappOppgaveTjeneste(oppgaveTjenesteMock);
        overlappendeInfotrygdYtelseTjeneste = new LoggOverlappEksterneYtelserTjeneste(null,
            beregningsresultatRepository, personinfoAdapter, infotrygdPSGrTjenesteMock, infotrygdSPGrTjenesteMock,
            abakusMock, spøkelseMock, overlappRepository, behandlingRepository, overlappOppgaveTjeneste);
        førsteUttaksdatoFp = LocalDate.now().minusMonths(4).minusWeeks(2);
        førsteUttaksdatoFp = VirkedagUtil.fomVirkedag(førsteUttaksdatoFp);

        var person = new PersonIdent("12345678901");
        when(personinfoAdapter.hentFnr(any())).thenReturn(Optional.of(person));
    }

    private ScenarioMorSøkerForeldrepenger avsluttetBehandlingMor() {
        var scenarioAvsluttetBehMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioAvsluttetBehMor.medSøknadHendelse().medFødselsDato(førsteUttaksdatoFp);
        scenarioAvsluttetBehMor.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvsluttetBehMor.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        return scenarioAvsluttetBehMor;
    }

    // CASE 1:
    // Løpende ytelse: Ja, infotrygd ytelse opphører samme dag som FP
    @Test
    void overlapp_når_Fp_starter_samme_dag_som_IT_opphører() {
        LOG.info("1");
        behandlingFP = avsluttetBehandlingMor().lagre(repositoryProvider);
        List<Vedtak> vedtakPeriode = new ArrayList<>();
        vedtakPeriode.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp, 100));
        var infotrygPSGrunnlag = lagGrunnlagPSIT(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp, vedtakPeriode);

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of(infotrygPSGrunnlag));
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any())).thenReturn(Collections.emptyList());

        var berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusWeeks(20));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        overlappendeInfotrygdYtelseTjeneste.loggOverlappForVedtakFPSAK(behandlingFP);

        // Assert
        var overlappIT = overlappRepository.hentForSaksnummer(behandlingFP.getFagsak().getSaksnummer());
        assertThat(overlappIT).hasSize(1);
        assertThat(overlappIT.getFirst().getPeriode().getTomDato()).isEqualTo(førsteUttaksdatoFp);
        verify(oppgaveTjenesteMock, times(1)).opprettVurderKonsekvensHosPleiepenger(any(), any());
        verifyNoMoreInteractions(oppgaveTjenesteMock);
    }

    @Test
    void overlapp_gradert_opphører() {
        LOG.info("2");
        behandlingFP = avsluttetBehandlingMor().lagre(repositoryProvider);
        List<Vedtak> vedtakPeriode = new ArrayList<>();
        vedtakPeriode.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.plusWeeks(4), 50));
        var infotrygPSGrunnlag = lagGrunnlagPSIT(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.plusWeeks(4),
            vedtakPeriode);

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of(infotrygPSGrunnlag));
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any())).thenReturn(Collections.emptyList());

        var berFp = lagGradertBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusWeeks(20));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        overlappendeInfotrygdYtelseTjeneste.loggOverlappForVedtakFPSAK(behandlingFP);

        // Assert
        var overlappIT = overlappRepository.hentForSaksnummer(behandlingFP.getFagsak().getSaksnummer());
        assertThat(overlappIT).isEmpty();

        // Arrange2
        List<Vedtak> vedtakPeriode2 = new ArrayList<>();
        vedtakPeriode2.add(
            lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.plusWeeks(4), 100));
        var infotrygPSGrunnlag2 = lagGrunnlagPSIT(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.plusWeeks(4),
            vedtakPeriode2);

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of(infotrygPSGrunnlag2));

        // Act
        overlappendeInfotrygdYtelseTjeneste.loggOverlappForVedtakFPSAK(behandlingFP);

        // Assert
        var overlappIT2 = overlappRepository.hentForSaksnummer(behandlingFP.getFagsak().getSaksnummer());
        assertThat(overlappIT2).hasSize(1);
        assertThat(overlappIT2.getFirst().getPeriode().getTomDato()).isEqualTo(førsteUttaksdatoFp.plusWeeks(4));
        verify(oppgaveTjenesteMock, times(1)).opprettVurderKonsekvensHosPleiepenger(any(), any());
        verifyNoMoreInteractions(oppgaveTjenesteMock);
    }

    @Test
    void flereOverlappIlisten() {
        LOG.info("3");

        behandlingFP = avsluttetBehandlingMor().lagre(repositoryProvider);
        List<Vedtak> vedtakPerioder = new ArrayList<>();
        vedtakPerioder.add(
            lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(4), førsteUttaksdatoFp.minusWeeks(3), 100));
        vedtakPerioder.add(
            lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(3), førsteUttaksdatoFp.minusWeeks(2), 100));
        vedtakPerioder.add(
            lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(3), førsteUttaksdatoFp.minusWeeks(2), 100));
        vedtakPerioder.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(1), førsteUttaksdatoFp, 100));

        var infotrygSPGrunnlag = lagGrunnlagSPIT(førsteUttaksdatoFp.minusWeeks(4), førsteUttaksdatoFp, vedtakPerioder);

        List<Vedtak> vedtakPerioderPS = new ArrayList<>();
        vedtakPerioderPS.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(10), førsteUttaksdatoFp, 100));

        var infotrygPSGrunnlag = lagGrunnlagPSIT(førsteUttaksdatoFp.minusDays(20), førsteUttaksdatoFp.plusWeeks(4),
            vedtakPerioderPS);

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of(infotrygPSGrunnlag));
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of(infotrygSPGrunnlag));

        var berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusMonths(5));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        overlappendeInfotrygdYtelseTjeneste.loggOverlappForVedtakFPSAK(behandlingFP);

        // Assert
        var overlappIT = overlappRepository.hentForSaksnummer(behandlingFP.getFagsak().getSaksnummer());
        assertThat(overlappIT).hasSize(2);
        verify(oppgaveTjenesteMock, times(1)).opprettVurderKonsekvensHosPleiepenger(any(), any());
        verify(oppgaveTjenesteMock, times(1)).opprettVurderKonsekvensHosSykepenger(any(), any());
        verifyNoMoreInteractions(oppgaveTjenesteMock);
    }

    @Test
    void nyeSykepengerOverlappIlisten() {
        LOG.info("4");

        behandlingFP = avsluttetBehandlingMor().lagre(repositoryProvider);
        List<Vedtak> vedtakPerioderIT = new ArrayList<>();
        vedtakPerioderIT.add(
            lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(4), førsteUttaksdatoFp.minusWeeks(3), 100));
        vedtakPerioderIT.add(
            lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(3), førsteUttaksdatoFp.minusWeeks(2), 100));
        vedtakPerioderIT.add(
            lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(3), førsteUttaksdatoFp.minusWeeks(2), 100));
        vedtakPerioderIT.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusWeeks(1), førsteUttaksdatoFp, 100));

        var infotrygSPGrunnlag = lagGrunnlagSPIT(førsteUttaksdatoFp.minusWeeks(4), førsteUttaksdatoFp, vedtakPerioderIT);

        List<SykepengeVedtak.SykepengeUtbetaling> utbetalingsperioderSyk = new ArrayList<>();

        utbetalingsperioderSyk.add(new SykepengeVedtak.SykepengeUtbetaling(førsteUttaksdatoFp.minusWeeks(3), førsteUttaksdatoFp, BigDecimal.valueOf(100)));
        utbetalingsperioderSyk.add(new SykepengeVedtak.SykepengeUtbetaling(førsteUttaksdatoFp.plusWeeks(1), førsteUttaksdatoFp.plusWeeks(2), BigDecimal.valueOf(100)));
        var sykepengerVedtak  = lagSykVedtak(utbetalingsperioderSyk, LocalDateTime.now());

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any())).thenReturn(Collections.emptyList());
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of(infotrygSPGrunnlag));
        when(spøkelseMock.hentGrunnlag(any(), any(), any())).thenReturn(List.of(sykepengerVedtak));

        var berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusMonths(5));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        overlappendeInfotrygdYtelseTjeneste.loggOverlappForVedtakFPSAK(behandlingFP);

        // Assert
        var overlappIT = overlappRepository.hentForSaksnummer(behandlingFP.getFagsak().getSaksnummer());
        assertThat(overlappIT).hasSize(3);
        verify(oppgaveTjenesteMock, times(2)).opprettVurderKonsekvensHosSykepenger(any(), any());
        verifyNoMoreInteractions(oppgaveTjenesteMock);
    }

    private SykepengeVedtak lagSykVedtak(List<SykepengeVedtak.SykepengeUtbetaling> sykUtbetalinger, LocalDateTime vedtattTid) {
        return new SykepengeVedtak( "ref", sykUtbetalinger, vedtattTid);
    }

    @Test
    void flereGrunnlagMenEttOverlappIlisten() {
        LOG.info("5");

        behandlingFP = avsluttetBehandlingMor().lagre(repositoryProvider);
        List<Vedtak> vedtakPerioder = new ArrayList<>();
        vedtakPerioder.add(
            lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.minusDays(5), 100));
        vedtakPerioder.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(4), førsteUttaksdatoFp.plusWeeks(3), 100));

        var infotrygPSGrunnlag = lagGrunnlagPSIT(førsteUttaksdatoFp, førsteUttaksdatoFp.plusDays(30), vedtakPerioder);

        List<Vedtak> vedtakPerioderSP = new ArrayList<>();
        var vedtakSP1 = lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(10), førsteUttaksdatoFp.minusDays(1), 100);
        vedtakPerioderSP.add(vedtakSP1);

        var infotrygSPGrunnlag = lagGrunnlagSPIT(førsteUttaksdatoFp.minusDays(20), førsteUttaksdatoFp,
            vedtakPerioderSP);

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of(infotrygPSGrunnlag));
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of(infotrygSPGrunnlag));

        var berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusWeeks(20));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        overlappendeInfotrygdYtelseTjeneste.loggOverlappForVedtakFPSAK(behandlingFP);

        // Assert
        var overlappIT = overlappRepository.hentForSaksnummer(behandlingFP.getFagsak().getSaksnummer());
        assertThat(overlappIT).hasSize(1);
        assertThat(overlappIT.getFirst().getPeriode().getTomDato()).isEqualTo(førsteUttaksdatoFp.plusWeeks(3));
        verify(oppgaveTjenesteMock, times(1)).opprettVurderKonsekvensHosPleiepenger(any(), any());
        verifyNoMoreInteractions(oppgaveTjenesteMock);
    }

    @Test
    void ingenOverlapp() {
        LOG.info("6");

        behandlingFP = avsluttetBehandlingMor().lagre(repositoryProvider);
        List<Vedtak> vedtakPeriode = new ArrayList<>();
        vedtakPeriode.add(lagVedtakForGrunnlag(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.minusDays(1), 100));
        var infotrygPSGrunnlag = lagGrunnlagPSIT(førsteUttaksdatoFp.minusDays(15), førsteUttaksdatoFp.minusDays(1),
            vedtakPeriode);

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of(infotrygPSGrunnlag));
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any())).thenReturn(Collections.emptyList());

        var berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusWeeks(20));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        overlappendeInfotrygdYtelseTjeneste.loggOverlappForVedtakFPSAK(behandlingFP);

        // Assert
        var overlappIT = overlappRepository.hentForSaksnummer(behandlingFP.getFagsak().getSaksnummer());
        assertThat(overlappIT).isEmpty();
        verifyNoInteractions(oppgaveTjenesteMock);
    }

    @Test
    void ingenGrunnlag() {
        LOG.info("7");
        behandlingFP = avsluttetBehandlingMor().lagre(repositoryProvider);
        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any())).thenReturn(Collections.emptyList());
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any())).thenReturn(Collections.emptyList());

        var berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusWeeks(20));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        overlappendeInfotrygdYtelseTjeneste.loggOverlappForVedtakFPSAK(behandlingFP);

        // Assert
        var overlappIT = overlappRepository.hentForSaksnummer(behandlingFP.getFagsak().getSaksnummer());
        assertThat(overlappIT).isEmpty();
        verifyNoInteractions(oppgaveTjenesteMock);
    }

    @Test
    void pleiepengerK9sak() {
        LOG.info("8");

        behandlingFP = avsluttetBehandlingMor().lagre(repositoryProvider);

        var psb = lagAbakusLagretVedtak(Ytelser.PLEIEPENGER_SYKT_BARN, "P1CDE", førsteUttaksdatoFp.plusWeeks(1), førsteUttaksdatoFp.plusWeeks(2).minusDays(1));
        var omp = lagAbakusLagretVedtak(Ytelser.OMSORGSPENGER, "O2CDE", førsteUttaksdatoFp.plusMonths(3), førsteUttaksdatoFp.plusMonths(3).plusDays(1));

        when(infotrygdPSGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of());
        when(infotrygdSPGrTjenesteMock.hentGrunnlag(any())).thenReturn(List.of());
        when(spøkelseMock.hentGrunnlag(any(), any(), any())).thenReturn(List.of());
        when(abakusMock.hentVedtakForAktørId(any())).thenReturn(List.of(psb, omp));

        var berFp = lagBeregningsresultatFP(førsteUttaksdatoFp, førsteUttaksdatoFp.plusMonths(5));
        beregningsresultatRepository.lagre(behandlingFP, berFp);

        // Act
        overlappendeInfotrygdYtelseTjeneste.loggOverlappForVedtakFPSAK(behandlingFP);

        // Assert
        var overlappIT = overlappRepository.hentForSaksnummer(behandlingFP.getFagsak().getSaksnummer());
        assertThat(overlappIT).hasSize(2);
        verify(oppgaveTjenesteMock, times(2)).opprettVurderKonsekvensHosPleiepenger(any(), any());
        verifyNoMoreInteractions(oppgaveTjenesteMock);
    }

    private YtelseV1 lagAbakusLagretVedtak(Ytelser ytelseType, String saksnummer, LocalDate fom, LocalDate tom) {
        var periode = new no.nav.abakus.vedtak.ytelse.Periode();
        periode.setFom(fom);
        periode.setTom(tom);
        var anvist = new Anvisning();
        anvist.setPeriode(periode);
        anvist.setDagsats(new Desimaltall(BigDecimal.valueOf(1000)));
        anvist.setUtbetalingsgrad(new Desimaltall(BigDecimal.valueOf(100)));
        anvist.setBeløp(new Desimaltall(BigDecimal.valueOf(1000)));
        var ytelse = new YtelseV1();
        ytelse.setYtelse(ytelseType);
        ytelse.setSaksnummer(saksnummer);
        ytelse.setKildesystem(Kildesystem.K9SAK);
        ytelse.setPeriode(periode);
        ytelse.setAnvist(List.of(anvist));
        return ytelse;
    }


    private Grunnlag lagGrunnlagPSIT(LocalDate fom, LocalDate tom, List<Vedtak> vedtakPerioder) {
        var periode = new Periode(fom, tom);
        var tema = new Tema(TemaKode.BS, "Pleiepenger");

        return new Grunnlag(null, tema, null, null, null, null, periode, null, null, null, tom, 0,
            LocalDate.now().minusMonths(1), LocalDate.now().minusMonths(1), "", vedtakPerioder);
    }

    private Vedtak lagVedtakForGrunnlag(LocalDate fom, LocalDate tom, int utbetGrad) {
        var periode = new Periode(fom, tom);
        return new Vedtak(periode, utbetGrad,"arbOrgnr", false, 100);
    }

    private Grunnlag lagGrunnlagSPIT(LocalDate fom, LocalDate tom, List<Vedtak> vedtakPerioder) {
        var periode = new Periode(fom, tom);
        var tema = new Tema(TemaKode.SP, "Sykepenger");

        return new Grunnlag(null, tema, null, null, null, null, periode, null, null, null, tom, 0,
            LocalDate.now().minusMonths(1), LocalDate.now().minusMonths(1), "", vedtakPerioder);
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP(LocalDate periodeFom, LocalDate periodeTom) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("sporing")
            .build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(200)
            .medDagsatsFraBg(200)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }

    private BeregningsresultatEntitet lagGradertBeregningsresultatFP(LocalDate periodeFom, LocalDate periodeTom) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("sporing")
            .build();
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(100)
            .medDagsatsFraBg(200)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(50))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultat;
    }
}
