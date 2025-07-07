package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

@CdiDbAwareTest
class IverksetteVedtakStegYtelseTest {

    @Mock
    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;
    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock
    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;

    private IverksetteVedtakStegFelles iverksetteVedtakSteg;
    private Behandling behandling;

    @BeforeEach
    void setup() {
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        iverksetteVedtakSteg = new IverksetteVedtakStegFelles(repositoryProvider, opprettProsessTaskIverksett,
                vurderBehandlingerUnderIverksettelse);
        behandling = opprettBehandling();
    }

    @Test
    void vurder_gitt_venterPåInfotrygd_venterTidligereBehandling_skal_VENT_TIDLIGERE_BEHANDLING() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(true);

        // Act
        var resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.STARTET);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();

        var historikinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikinnslag.getTittel()).isEqualTo("Behandlingen venter på iverksettelse");
        assertThat(historikinnslag.getLinjer().getFirst().getTekst()).isEqualTo("Venter på iverksettelse av en tidligere behandling i denne saken.");
    }

    @Test
    void vurder_gitt_ikkeVenterPåInfotrygd_ikkeVenterTidligereBehandling_skal_gi_empty() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(false);

        // Act
        var resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.SUSPENDERT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        return iverksetteVedtakSteg.utførSteg(new BehandlingskontrollKontekst(behandling, lås));
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingStegStart(BehandlingStegType.IVERKSETT_VEDTAK);

        var behandling = scenario.lagre(repositoryProvider);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        var lås = behandlingRepository.taSkriveLås(behandling);

        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        var entityManager = repositoryProvider.getEntityManager();
        entityManager.persist(behandlingsresultat);

        entityManager.flush();

        return behandling;
    }

    private BehandlingVedtak opprettBehandlingVedtak(VedtakResultatType resultatType, IverksettingStatus iverksettingStatus) {
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        var behandlingVedtak = BehandlingVedtak.builder()
                .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
                .medAnsvarligSaksbehandler("E2354345")
                .medVedtakResultatType(resultatType)
                .medIverksettingStatus(iverksettingStatus)
                .medBehandlingsresultat(repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()))
                .build();
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);
        return behandlingVedtak;
    }

}
