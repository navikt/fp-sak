package no.nav.foreldrepenger.domene.vedtak.intern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;

public class FagsakRelasjonAvsluttningsdatoOppdatererTest {

    private FagsakRelasjonAvsluttningsdatoOppdaterer fagsakRelasjonAvsluttningsdatoOppdaterer;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private UttakRepository uttakRepository;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;
    @Mock
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    @Mock
    private UttakInputTjeneste uttakInputTjeneste;

    private Fagsak fagsak;
    private Behandling behandling;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        stønadskontoSaldoTjeneste = spy(stønadskontoSaldoTjeneste);
        FagsakLåsRepository fagsakLåsRepository = mock(FagsakLåsRepository.class);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);
        when(repositoryProvider.getUttakRepository()).thenReturn(uttakRepository);
        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(fagsakLåsRepository);
        when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(familieHendelseRepository);

        fagsakRelasjonAvsluttningsdatoOppdaterer = new FagsakRelasjonAvsluttningsdatoOppdaterer(repositoryProvider, stønadskontoSaldoTjeneste, uttakInputTjeneste,fagsakRelasjonTjeneste);

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
        when(uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(lagUttakResultat(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)));
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.empty());

        // Act
        fagsakRelasjonAvsluttningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(), null, Optional.empty(), Optional.empty());

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
        when(uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(lagUttakResultat(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)));
        when(stønadskontoSaldoTjeneste.erSluttPåStønadsdager(any(UttakInput.class))).thenReturn(true);
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.empty());

        // Act
        fagsakRelasjonAvsluttningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(), null, Optional.empty(), Optional.empty());

        // Assert
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon, LocalDate.now().plusDays(10), null, Optional.empty(), Optional.empty());
    }

    @Test
    public void testStønadsdagerIgjen() {
        // Arrange
        FagsakRelasjon fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()))
            .thenReturn(lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT));
        when(uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(lagUttakResultat(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)));
        when(stønadskontoSaldoTjeneste.erSluttPåStønadsdager(any(UttakInput.class))).thenReturn(false);
        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = mock(FamilieHendelseGrunnlagEntitet.class);
        FamilieHendelseEntitet familieHendelse = mock(FamilieHendelseEntitet.class);
        when(familieHendelse.getFødselsdato()).thenReturn(Optional.of(LocalDate.now().minusDays(5)));
        when(familieHendelseGrunnlag.getGjeldendeVersjon()).thenReturn(familieHendelse);
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(familieHendelseGrunnlag));

        // Act
        fagsakRelasjonAvsluttningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(), null, Optional.empty(), Optional.empty());

        // Assert
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon, LocalDate.now().minusDays(5).plusYears(3), null, Optional.empty(), Optional.empty());
    }

    @Test
    public void testStønadsdagerIgjenPåTermin() {
        // Arrange
        FagsakRelasjon fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()))
            .thenReturn(lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT));
        when(uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(lagUttakResultat(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)));
        when(stønadskontoSaldoTjeneste.erSluttPåStønadsdager(any(UttakInput.class))).thenReturn(false);
        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = mock(FamilieHendelseGrunnlagEntitet.class);
        FamilieHendelseEntitet familieHendelse = mock(FamilieHendelseEntitet.class);
        TerminbekreftelseEntitet terminbekreftelse = mock(TerminbekreftelseEntitet.class);
        when(terminbekreftelse.getTermindato()).thenReturn(LocalDate.now().plusDays(5));
        when(familieHendelse.getTerminbekreftelse()).thenReturn(Optional.of(terminbekreftelse));
        when(familieHendelseGrunnlag.getGjeldendeVersjon()).thenReturn(familieHendelse);
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(familieHendelseGrunnlag));

        // Act
        fagsakRelasjonAvsluttningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(), null, Optional.empty(), Optional.empty());

        // Assert
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon, LocalDate.now().plusDays(5).plusYears(3), null, Optional.empty(), Optional.empty());
    }

    @Test
    public void testStønadsdagerIgjenPåAdopsjon() {
        // Arrange
        FagsakRelasjon fagsakRelasjon = mock(FagsakRelasjon.class);
        when(fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)).thenReturn(Optional.of(fagsakRelasjon));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(behandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()))
            .thenReturn(lagBehandlingsresultat(behandling, BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.UDEFINERT));
        when(uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(lagUttakResultat(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)));
        when(stønadskontoSaldoTjeneste.erSluttPåStønadsdager(any(UttakInput.class))).thenReturn(false);
        FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = mock(FamilieHendelseGrunnlagEntitet.class);
        FamilieHendelseEntitet familieHendelse = mock(FamilieHendelseEntitet.class);
        AdopsjonEntitet adopsjon = mock(AdopsjonEntitet.class);
        when(adopsjon.getOmsorgsovertakelseDato()).thenReturn(LocalDate.now().minusMonths(1));
        when(familieHendelse.getAdopsjon()).thenReturn(Optional.of(adopsjon));
        when(familieHendelseGrunnlag.getGjeldendeVersjon()).thenReturn(familieHendelse);
        when(familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(familieHendelseGrunnlag));

        // Act
        fagsakRelasjonAvsluttningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(fagsakRelasjon, fagsak.getId(), null, Optional.empty(), Optional.empty());

        // Assert
        verify(fagsakRelasjonTjeneste).oppdaterMedAvsluttningsdato(fagsakRelasjon, LocalDate.now().minusMonths(1).plusYears(3), null, Optional.empty(), Optional.empty());
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
        UttakResultatEntitet uttakResultatEntitet = new UttakResultatEntitet();
        UttakResultatPerioderEntitet uttakResultatPerioderEntitet = new UttakResultatPerioderEntitet();
        UttakResultatPeriodeEntitet uttakResultatPeriodeEntitet2 = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        uttakResultatPerioderEntitet.leggTilPeriode(uttakResultatPeriodeEntitet2);
        uttakResultatEntitet.setOpprinneligPerioder(uttakResultatPerioderEntitet);
        return Optional.of(uttakResultatEntitet);
    }
}
