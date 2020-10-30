package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
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

public class IverksetteVedtakStegFellesTest extends EntityManagerAwareTest {

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
    public void skalKasteFeilNårIngenBehandlingVedtak() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var behandling = opprettBehandling();

        // Act
        assertThrows(IllegalStateException.class, () -> utførSteg(behandling));
    }

    @Test
    public void skalOppretteHistorikkinnslagHvisVenterTidligereBehandling() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var behandling = opprettBehandling();
        @SuppressWarnings("unused")
        var vedtak = opprettBehandlingVedtak(VedtakResultatType.AVSLAG, IverksettingStatus.IKKE_IVERKSATT, behandling.getId());
        when(vurderBehandlingerUnderIverksettelse.vurder(any())).thenReturn(true);

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.STARTET);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();

        when(vurderBehandlingerUnderIverksettelse.vurder(any())).thenReturn(false);

        // Act
        BehandleStegResultat resultat2 = utførSteg(behandling);

        // Assert
        assertThat(resultat2.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat2.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skalOppretteIverksettingProsessTaskerHvisBehandlingKanIverksettes() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var behandling = opprettBehandling();
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT, behandling.getId());
        when(vurderBehandlingerUnderIverksettelse.vurder(any())).thenReturn(false);

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        verify(opprettProsessTaskIverksett).opprettIverksettingTasks(eq(behandling));
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());
        assertThat(behandlingVedtakOpt).hasValueSatisfying(behandlingVedtak ->
            assertThat(behandlingVedtak.getIverksettingStatus()).isEqualTo(IverksettingStatus.IKKE_IVERKSATT)
        );
    }

    @Test
    public void skalSettePåVentHvisVedtakErUnderIverksettelse() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var behandling = opprettBehandling();
        opprettBehandlingVedtak(VedtakResultatType.AVSLAG, IverksettingStatus.IKKE_IVERKSATT, behandling.getId());

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skalReturnereUtenAksjonspunkterNårVedtakErIverksatt() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var behandling = opprettBehandling();
        opprettBehandlingVedtak(VedtakResultatType.AVSLAG, IverksettingStatus.IVERKSATT, behandling.getId());

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        return iverksetteVedtakSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), lås));
    }

    private Behandling opprettBehandling() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        var behandling = scenario.lagre(repositoryProvider);
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling.getId());
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        behandlingsresultatRepository.lagre(behandling.getId(), behandlingsresultat);

        return behandling;
    }

    private BehandlingVedtak opprettBehandlingVedtak(VedtakResultatType resultatType,
                                                     IverksettingStatus iverksettingStatus,
                                                     Long behandlingId) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandlingId);
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandlingId);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
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
