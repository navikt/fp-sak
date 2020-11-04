package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
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
import no.nav.vedtak.felles.testutilities.db.Repository;

@CdiDbAwareTest
public class IverksetteVedtakStegYtelseTest {


    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    private Behandling behandling;

    @Mock
    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;

    private Repository repository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private HistorikkRepository historikkRepository;

    @Mock
    private VurderBehandlingerUnderIverksettelse vurderBehandlingerUnderIverksettelse;

    private IverksetteVedtakStegFelles iverksetteVedtakSteg;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        repository = new Repository(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        historikkRepository = repositoryProvider.getHistorikkRepository();
        iverksetteVedtakSteg = new IverksetteVedtakStegFelles(repositoryProvider, opprettProsessTaskIverksett,
            vurderBehandlingerUnderIverksettelse);
        behandling = opprettBehandling();
    }

    @Test
    public void vurder_gitt_venterPåInfotrygd_venterTidligereBehandling_skal_VENT_TIDLIGERE_BEHANDLING() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(true);

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.STARTET);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        Historikkinnslag historikkinnslag = historikkRepository.hentHistorikk(behandling.getId()).get(0);
        assertThat(historikkinnslag.getHistorikkinnslagDeler()).hasSize(1);
        HistorikkinnslagDel del1 = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del1.getHendelse()).hasValueSatisfying(hendelse ->
            assertThat(hendelse.getNavn()).as("navn").isEqualTo(HistorikkinnslagType.IVERKSETTELSE_VENT.getKode()));
        assertThat(del1.getAarsak().get()).isEqualTo(Venteårsak.VENT_TIDLIGERE_BEHANDLING.getKode());
    }

    @Test
    public void vurder_gitt_ikkeVenterPåInfotrygd_ikkeVenterTidligereBehandling_skal_gi_empty() {
        // Arrange
        opprettBehandlingVedtak(VedtakResultatType.INNVILGET, IverksettingStatus.IKKE_IVERKSATT);
        when(vurderBehandlingerUnderIverksettelse.vurder(eq(behandling))).thenReturn(false);

        // Act
        BehandleStegResultat resultat = utførSteg(behandling);

        // Assert
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.SETT_PÅ_VENT);
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        return iverksetteVedtakSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), lås));
    }


    private Behandling opprettBehandling() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingStegStart(BehandlingStegType.IVERKSETT_VEDTAK);

        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);

        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        repository.lagre(behandlingsresultat);

        repository.flush();

        return behandling;
    }

    private BehandlingVedtak opprettBehandlingVedtak(VedtakResultatType resultatType, IverksettingStatus iverksettingStatus) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
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
