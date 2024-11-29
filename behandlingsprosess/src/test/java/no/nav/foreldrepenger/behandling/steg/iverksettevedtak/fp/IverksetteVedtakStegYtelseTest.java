package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
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

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    private Behandling behandling;

    @Mock
    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;

    private EntityManager entityManager;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private HistorikkRepository historikkRepository;

    @Mock
    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;

    private IverksetteVedtakStegFelles iverksetteVedtakSteg;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        this.entityManager = entityManager;
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        historikkRepository = repositoryProvider.getHistorikkRepository();
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
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.STARTET);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        var historikkinnslag = historikkRepository.hentHistorikk(behandling.getId()).get(0);
        assertThat(historikkinnslag.getHistorikkinnslagDeler()).hasSize(1);
        var del1 = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del1.getHendelse()).hasValueSatisfying(
            hendelse -> assertThat(hendelse.getNavn()).as("navn").isEqualTo(HistorikkinnslagType.IVERKSETTELSE_VENT.getKode()));
        assertThat(del1.getAarsak()).contains(Venteårsak.VENT_TIDLIGERE_BEHANDLING.getKode());
    }

    @Test
    void vurder_gitt_ikkeVenterPåInfotrygd_ikkeVenterTidligereBehandling_skal_gi_empty() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(false);

        // Act
        var resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        return iverksetteVedtakSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getSaksnummer(), behandling.getFagsakId(), lås));
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingStegStart(BehandlingStegType.IVERKSETT_VEDTAK);

        var behandling = scenario.lagre(repositoryProvider);
        var lås = behandlingRepository.taSkriveLås(behandling);

        var behandlingsresultat = getBehandlingsresultat(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        entityManager.persist(behandlingsresultat);

        entityManager.flush();

        return behandling;
    }

    private BehandlingVedtak opprettBehandlingVedtak(VedtakResultatType resultatType, IverksettingStatus iverksettingStatus) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        var behandlingsresultat = getBehandlingsresultat(behandling);
        var behandlingVedtak = BehandlingVedtak.builder()
                .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
                .medAnsvarligSaksbehandler("E2354345")
                .medVedtakResultatType(resultatType)
                .medIverksettingStatus(iverksettingStatus)
                .medBehandlingsresultat(behandlingsresultat)
                .build();
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);
        return behandlingVedtak;
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }
}
