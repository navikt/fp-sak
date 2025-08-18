package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

class IverksetteVedtakStegFellesTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private IverksetteVedtakStegFelles iverksetteVedtakSteg;

    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;

    private BehandlingVedtakRepository behandlingVedtakRepository;

    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;

    @BeforeEach
    void setup() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);

        opprettProsessTaskIverksett = mock(OpprettProsessTaskIverksett.class);
        vurderBehandlingerUnderIverksettelse = mock(VurderBehandlingerUnderIverksettelse.class);
        iverksetteVedtakSteg = new IverksetteVedtakStegFelles(repositoryProvider, opprettProsessTaskIverksett,
                vurderBehandlingerUnderIverksettelse);
    }

    @Test
    void skalKasteFeilNårIngenBehandlingVedtak() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var behandling = opprettBehandling();

        // Act
        assertThrows(IllegalStateException.class, () -> utførSteg(behandling));
    }

    @Test
    void skalOppretteHistorikkinnslagHvisVenterTidligereBehandling() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var behandling = opprettBehandling();
        opprettBehandlingVedtak(VedtakResultatType.AVSLAG, IverksettingStatus.IKKE_IVERKSATT, behandling.getId());
        when(vurderBehandlingerUnderIverksettelse.vurder(any())).thenReturn(true);

        // Act
        var resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.STARTET);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();

        when(vurderBehandlingerUnderIverksettelse.vurder(any())).thenReturn(false);

        // Act
        var resultat2 = utførSteg(behandling);

        // Assert
        assertThat(resultat2.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.SUSPENDERT);
        assertThat(resultat2.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void skalOppretteIverksettingProsessTaskerHvisBehandlingKanIverksettes() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var behandling = opprettBehandling();
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT, behandling.getId());
        when(vurderBehandlingerUnderIverksettelse.vurder(any())).thenReturn(false);

        // Act
        var resultat = utførSteg(behandling);

        // Assert
        verify(opprettProsessTaskIverksett).opprettIverksettingTasks(behandling);
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.SUSPENDERT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var behandlingVedtakOpt = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        assertThat(behandlingVedtakOpt).hasValueSatisfying(
                behandlingVedtak -> assertThat(behandlingVedtak.getIverksettingStatus()).isEqualTo(IverksettingStatus.IKKE_IVERKSATT));
    }

    @Test
    void skalSettePåVentHvisVedtakErUnderIverksettelse() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var behandling = opprettBehandling();
        opprettBehandlingVedtak(VedtakResultatType.AVSLAG, IverksettingStatus.IKKE_IVERKSATT, behandling.getId());

        // Act
        var resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.SUSPENDERT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void skalReturnereUtenAksjonspunkterNårVedtakErIverksatt() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var behandling = opprettBehandling();
        opprettBehandlingVedtak(VedtakResultatType.AVSLAG, IverksettingStatus.IVERKSATT, behandling.getId());

        // Act
        var resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        return iverksetteVedtakSteg.utførSteg(new BehandlingskontrollKontekst(behandling, lås));
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        var behandling = scenario.lagre(repositoryProvider);
        var lås = behandlingRepository.taSkriveLås(behandling);

        var behandlingsresultat = getBehandlingsresultat(behandling.getId());
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        behandlingsresultatRepository.lagre(behandling.getId(), behandlingsresultat);

        return behandling;
    }

    private BehandlingVedtak opprettBehandlingVedtak(VedtakResultatType resultatType,
            IverksettingStatus iverksettingStatus,
            Long behandlingId) {
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var behandlingsresultat = getBehandlingsresultat(behandlingId);
        var behandlingVedtak = BehandlingVedtak.builder()
                .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
                .medAnsvarligSaksbehandler("E123")
                .medVedtakResultatType(resultatType)
                .medIverksettingStatus(iverksettingStatus)
                .medBehandlingsresultat(behandlingsresultat)
                .build();
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);
        return behandlingVedtak;
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }
}
