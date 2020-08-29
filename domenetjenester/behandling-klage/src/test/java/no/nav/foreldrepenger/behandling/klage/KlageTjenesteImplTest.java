package no.nav.foreldrepenger.behandling.klage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

public class KlageTjenesteImplTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    @Mock
    private BehandlingskontrollTjeneste behandlingKontrollTjenesteMock;

    @Mock
    private HistorikkRepository historikkRepositoryMock;

    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjenesteMock;

    @Mock
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private BehandlingRepository behandlingRepositoryMock;

    @Mock
    private KlageRepository klageRepository;

    private ProsessTaskRepository prosessTaskRepository;

    @Before
    public void oppsett() {
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepositoryMock);
        prosessTaskRepository = spy(new ProsessTaskRepositoryImpl(entityManager, null, null));
    }

    @Test
    public void skal_opprette_klage_på_fagsak() {
        // Arrange
        Behandling opprinneligBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        opprinneligBehandling.avsluttBehandling();
        Fagsak fagsak = opprinneligBehandling.getFagsak();
        Behandling klagebehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.KLAGE).lagMocked();
        when(behandlingRepositoryMock.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(Optional.of(opprinneligBehandling));
        when(behandlingKontrollTjenesteMock.opprettNyBehandling(any(),any(),any())).thenReturn(klagebehandling);

        KlageHistorikkTjeneste klageHistorikkTjeneste = new KlageHistorikkTjeneste(historikkRepositoryMock);

        KlageTjeneste klageTjeneste = new KlageTjeneste(behandlingKontrollTjenesteMock, behandlingRepositoryMock, klageRepository, behandlendeEnhetTjenesteMock, prosessTaskRepository, klageHistorikkTjeneste);

        // Act
        Optional<Behandling> nyKlageBehandling = klageTjeneste.opprettKlageBehandling(fagsak);

        // Assert
        assertThat(nyKlageBehandling.isPresent()).isEqualTo(Boolean.TRUE);
        assertThat(nyKlageBehandling.get()).isEqualTo(klagebehandling);
        verify(prosessTaskRepository, times(1)).lagre(any(ProsessTaskData.class));
    }


}
