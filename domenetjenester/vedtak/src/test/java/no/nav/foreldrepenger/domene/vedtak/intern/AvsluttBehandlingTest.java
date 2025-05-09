package no.nav.foreldrepenger.domene.vedtak.intern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

@ExtendWith(MockitoExtension.class)
class AvsluttBehandlingTest {

    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Mock
    private BehandlingEventPubliserer behandlingVedtakEventPubliserer;

    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Mock
    private OppdatereFagsakRelasjonVedVedtak oppdatereFagsakRelasjonVedVedtak;

    private AvsluttBehandling avsluttBehandling;
    private Behandling behandling;

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    private Fagsak fagsak;

    @BeforeEach
    public void setUp() {
        behandling = lagBehandling(LocalDateTime.now().minusHours(1), LocalDateTime.now());
        fagsak = behandling.getFagsak();

        var vurderBehandlingerUnderIverksettelse = new VurderBehandlingerUnderIverksettelse(
                repositoryProvider);

        avsluttBehandling = new AvsluttBehandling(repositoryProvider, behandlingskontrollTjeneste, behandlingVedtakEventPubliserer,
            vurderBehandlingerUnderIverksettelse, behandlingProsesseringTjeneste, oppdatereFagsakRelasjonVedVedtak);

        when(behandlingskontrollTjeneste.initBehandlingskontroll(Mockito.any(Behandling.class), Mockito.any(BehandlingLås.class)))
            .thenAnswer(invocation -> {
                    Behandling beh = invocation.getArgument(0);
                    BehandlingLås lås = invocation.getArgument(1);
                    return new BehandlingskontrollKontekst(beh, lås);
                });
    }

    @Test
    void testAvsluttBehandlingUtenAndreBehandlingerISaken() {
        // Arrange
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())).thenReturn(
                Collections.singletonList(behandling));

        // Act
        avsluttBehandling.avsluttBehandling(behandling.getId());

        // Assert
        verifiserIverksatt(behandling);
        verifiserKallTilProsesserBehandling(behandling);
    }

    private void verifiserIverksatt(Behandling behandling) {
        var vedtak = repositoryProvider.getBehandlingVedtakRepository()
                .hentForBehandling(behandling.getId());
        verify(vedtak).setIverksettingStatus(IverksettingStatus.IVERKSATT);
        verify(repositoryProvider.getBehandlingVedtakRepository()).lagre(Mockito.eq(vedtak), any(BehandlingLås.class));
    }

    @Test
    void testAvsluttBehandlingMedAnnenBehandlingSomIkkeVenter() {
        // Arrange
        var behandling2 = ScenarioMorSøkerEngangsstønad.forAdopsjon().lagMocked();
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())).thenReturn(
                List.of(behandling, behandling2));

        // Act
        avsluttBehandling.avsluttBehandling(behandling.getId());

        verifiserIverksatt(behandling);
        verifiserKallTilProsesserBehandling(behandling);
        verifyNoInteractions(behandlingProsesseringTjeneste);
    }

    @Test
    void testAvsluttBehandlingMedAnnenBehandlingSomVenter() {
        // Arrange
        var annenBehandling = lagBehandling(LocalDateTime.now().minusDays(1), LocalDateTime.now());
        var tilstand = new BehandlingStegTilstand(annenBehandling,
                BehandlingStegType.IVERKSETT_VEDTAK, BehandlingStegStatus.STARTET);
        annenBehandling.setStatus(BehandlingStatus.IVERKSETTER_VEDTAK);
        annenBehandling.setBehandlingStegTilstander(List.of(tilstand));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())).thenReturn(
                List.of(behandling, annenBehandling));
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(behandlingRepository.hentBehandling(annenBehandling.getId())).thenReturn(annenBehandling);
        // Act
        avsluttBehandling.avsluttBehandling(behandling.getId());

        verifiserIverksatt(behandling);
        verifiserKallTilProsesserBehandling(behandling);
        verifiserKallTilFortsettBehandling(annenBehandling);
    }

    @Test
    void testAvsluttBehandlingMedAnnenBehandlingSomErUnderIverksetting() {
        // Arrange
        var annenBehandling = lagBehandling(LocalDateTime.now().minusDays(1), LocalDateTime.now());
        var tilstand = new BehandlingStegTilstand(annenBehandling,
                BehandlingStegType.IVERKSETT_VEDTAK, BehandlingStegStatus.VENTER);
        annenBehandling.setStatus(BehandlingStatus.IVERKSETTER_VEDTAK);
        annenBehandling.setBehandlingStegTilstander(List.of(tilstand));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())).thenReturn(
                List.of(behandling, annenBehandling));

        // Act
        avsluttBehandling.avsluttBehandling(behandling.getId());

        verifiserIverksatt(behandling);
        verifiserKallTilProsesserBehandling(behandling);
        verifyNoInteractions(behandlingProsesseringTjeneste);
    }

    @Test
    void testAvsluttBehandlingMedToAndreBehandlingerSomVenterEldsteFørst() {
        // Arrange
        var now = LocalDateTime.now();
        var annenBehandling = lagBehandling(now.minusDays(2), now);
        var tredjeBehandling = lagBehandling(now, now);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())).thenReturn(
                List.of(behandling, annenBehandling, tredjeBehandling));
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(behandlingRepository.hentBehandling(annenBehandling.getId())).thenReturn(annenBehandling);

        // Act
        avsluttBehandling.avsluttBehandling(behandling.getId());

        verifiserIverksatt(behandling);
        verifiserKallTilProsesserBehandling(behandling);
        verifiserKallTilFortsettBehandling(annenBehandling);
        verifiserIkkeKallTilFortsettBehandling(tredjeBehandling);
    }

    private void verifiserKallTilProsesserBehandling(Behandling behandling) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, new BehandlingLås(behandling.getId()));
        verify(behandlingskontrollTjeneste).prosesserBehandlingGjenopptaHvisStegVenter(kontekst,
                BehandlingStegType.IVERKSETT_VEDTAK);
    }

    private void verifiserKallTilFortsettBehandling(Behandling behandling) {
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(eq(behandling));

    }

    private void verifiserIkkeKallTilFortsettBehandling(Behandling behandling) {
        verify(behandlingProsesseringTjeneste, times(0)).opprettTasksForFortsettBehandling(eq(behandling));
    }


    @Test
    void testAvsluttBehandlingMedToAndreBehandlingerSomVenterEldsteSist() {
        // Arrange
        var now = LocalDateTime.now();
        var annenBehandling = lagBehandling(now, now);
        var tredjeBehandling = lagBehandling(now.minusDays(1), now);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())).thenReturn(
                List.of(behandling, annenBehandling, tredjeBehandling));
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(behandlingRepository.hentBehandling(tredjeBehandling.getId())).thenReturn(tredjeBehandling);

        // Act
        avsluttBehandling.avsluttBehandling(behandling.getId());

        verifiserIverksatt(behandling);
        verifiserKallTilProsesserBehandling(behandling);
        verifiserKallTilFortsettBehandling(tredjeBehandling);
        verifiserIkkeKallTilFortsettBehandling(annenBehandling);
    }

    private Behandling lagBehandling(LocalDateTime opprettet, LocalDateTime vedtaksdato) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        if (fagsak != null) {
            scenario.medFagsakId(fagsak.getId());
            scenario.medSaksnummer(fagsak.getSaksnummer());
        }
        if (repositoryProvider == null) {
            repositoryProvider = scenario.mockBehandlingRepositoryProvider();
            behandlingRepository = repositoryProvider.getBehandlingRepository();
        }
        var behandling = scenario.lagMocked();
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);

        if (vedtaksdato != null) {
            var vedtak = lagMockedBehandlingVedtak(opprettet, vedtaksdato, behandling);
            lenient().when(repositoryProvider.getBehandlingVedtakRepository().hentForBehandlingHvisEksisterer(behandling.getId()))
                    .thenReturn(Optional.of(vedtak));
            lenient().when(repositoryProvider.getBehandlingVedtakRepository().hentForBehandling(behandling.getId())).thenReturn(
                    vedtak);
            behandling.setAvsluttetDato(vedtaksdato);
        }
        behandling.setStatus(BehandlingStatus.IVERKSETTER_VEDTAK);
        behandling.setBehandlingStegTilstander(List.of(
                new BehandlingStegTilstand(behandling, BehandlingStegType.IVERKSETT_VEDTAK, BehandlingStegStatus.STARTET)));
        return behandling;
    }

    private BehandlingVedtak lagMockedBehandlingVedtak(LocalDateTime opprettet,
            LocalDateTime vedtaksdato,
            Behandling behandling) {
        var vedtak = Mockito.spy(BehandlingVedtak.builder()
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medBehandlingsresultat(behandling.getBehandlingsresultat())
                .medAnsvarligSaksbehandler("Severin Saksbehandler")
                .medIverksettingStatus(IverksettingStatus.IKKE_IVERKSATT)
                .medVedtakstidspunkt(vedtaksdato)
                .build());
        vedtak.setOpprettetTidspunkt(opprettet);
        behandling.setOpprettetTidspunkt(opprettet);
        return vedtak;
    }

}
