package no.nav.foreldrepenger.domene.vedtak.intern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import javax.inject.Inject;

import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.fp.MaksDatoUttakTjenesteImpl;
import no.nav.foreldrepenger.domene.vedtak.intern.fp.FpFagsakRelasjonAvslutningsdatoOppdaterer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype;

public class FagsakRelasjonAvslutningsdatoOppdatererTest {

    private FagsakRelasjonAvslutningsdatoOppdaterer fagsakRelasjonAvslutningsdatoOppdaterer;

    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;

    @Mock
    private FpUttakRepository fpUttakRepository;

    @Mock
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;

    @Mock
    private SaldoUtregning saldoUtregning;

    @Mock
    private UttakInputTjeneste uttakInputTjeneste;

    @Mock
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    private Fagsak fagsak;
    private Behandling behandling;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(stønadskontoSaldoTjeneste.finnSaldoUtregning(any(UttakInput.class))).thenReturn(saldoUtregning);

        maksDatoUttakTjeneste = new MaksDatoUttakTjenesteImpl(fpUttakRepository, stønadskontoSaldoTjeneste);

        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        FagsakLåsRepository fagsakLåsRepository = mock(FagsakLåsRepository.class);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);
        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(fagsakLåsRepository);
        when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(familieHendelseRepository);

        fagsakRelasjonAvslutningsdatoOppdaterer = new FpFagsakRelasjonAvslutningsdatoOppdaterer(repositoryProvider, stønadskontoSaldoTjeneste,
            uttakInputTjeneste, maksDatoUttakTjeneste, fagsakRelasjonTjeneste, foreldrepengerUttakTjeneste);

        behandling = lagBehandling();
        fagsak = behandling.getFagsak();

        when(uttakInputTjeneste.lagInput(any(Behandling.class))).thenReturn(new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag()));
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
    }

    @Test
    public void testAvsluttningsdatoVedAvslag() {
        // Arrange
        FagsakRelasjon fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()))
            .thenReturn(lagBehandlingsresultat(behandling, BehandlingResultatType.AVSLÅTT, KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN));
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(lagUttakResultat(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)));

        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.empty());

        // Act
        fagsakRelasjonAvslutningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(), null, Optional.empty(), Optional.empty());

        // Assert
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon, LocalDate.now().plusDays(1), null, Optional.empty(), Optional.empty());
    }

    @Test
    public void testOppbruktStønadsdager() {
        // Arrange
        FagsakRelasjon fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()))
            .thenReturn(lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT));

        LocalDate periodeStartDato = LocalDate.now().minusDays(10);
        LocalDate periodeAvsluttetDato = LocalDate.now().plusDays(10);
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(lagUttakResultat(periodeStartDato, periodeAvsluttetDato));

        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.empty());
        when(saldoUtregning.saldo(any(Stønadskontotype.class))).thenReturn(0);
        when(stønadskontoSaldoTjeneste.finnStønadRest(any(UttakInput.class))).thenReturn(0);

        // Act
        fagsakRelasjonAvslutningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(), null, Optional.empty(), Optional.empty());

        // Assert
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon, VirkedagUtil.tomVirkedag(periodeAvsluttetDato).plusDays(1), null, Optional.empty(), Optional.empty());
    }

    @Test
    public void testStønadsdagerIgjen() {
        // Arrange
        FagsakRelasjon fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()))
            .thenReturn(lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT));

        LocalDate periodeStartDato = LocalDate.now().minusDays(10);
        LocalDate periodeAvsluttetDato = LocalDate.now().plusDays(10);
        when(fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(lagUttakResultat(periodeStartDato, periodeAvsluttetDato));

        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = mock(FamilieHendelseGrunnlagEntitet.class);
        FamilieHendelseEntitet familieHendelse = mock(FamilieHendelseEntitet.class);
        when(familieHendelse.getFødselsdato()).thenReturn(Optional.of(LocalDate.now().minusDays(5)));
        when(familieHendelseGrunnlag.getGjeldendeVersjon()).thenReturn(familieHendelse);
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(familieHendelseGrunnlag));

        int totalRest = 3;
        when(saldoUtregning.saldo(any(Stønadskontotype.class))).thenReturn(1);
        when(stønadskontoSaldoTjeneste.finnStønadRest(any(UttakInput.class))).thenReturn(totalRest); //summen for de tre stønadskotoene

        // Act
        fagsakRelasjonAvslutningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(), null, Optional.empty(), Optional.empty());

        LocalDate avslutningsdato = VirkedagUtil.tomVirkedag(Virkedager.plusVirkedager(periodeAvsluttetDato.plusDays(1),totalRest)).plusMonths(3).with(TemporalAdjusters.lastDayOfMonth());
        // Assert
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon, avslutningsdato, null, Optional.empty(), Optional.empty());
    }

    private Behandling lagBehandling() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        return scenario.lagMocked();
    }

    private Optional<Behandlingsresultat> lagBehandlingsresultat(Behandling behandling, BehandlingResultatType behandlingResultatType,
                                                                 KonsekvensForYtelsen konsekvensForYtelsen) {
        return Optional.of(Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType)
            .leggTilKonsekvensForYtelsen(konsekvensForYtelsen)
            .buildFor(behandling));
    }

    private Optional<UttakResultatEntitet> lagUttakResultat(LocalDate fom, LocalDate tom) {
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(fom,tom).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat.Builder().build();

        return Optional.of(new UttakResultatEntitet.Builder(behandlingsresultat).medOpprinneligPerioder(perioder).build());
    }
}
