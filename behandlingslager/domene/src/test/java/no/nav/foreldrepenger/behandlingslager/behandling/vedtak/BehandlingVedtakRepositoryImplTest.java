package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BehandlingVedtakRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final Repository repository = repoRule.getRepository();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    private final BehandlingVedtakRepository behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private Behandling behandling;
    
    private BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    
    private BehandlingsresultatRepository behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);

    @Before
    public void setup() {
        behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var resultat = Behandlingsresultat.builder().build();
        behandlingBuilder.lagreBehandlingsresultat(behandling.getId(), resultat);
    }

    @Test
    public void skalLagreVedtak() {
        // Arrange
        BehandlingVedtak behandlingVedtak = opprettBehandlingVedtak(behandling);

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);

        // Assert
        Long behandlingVedtakId = behandlingVedtak.getId();
        assertThat(behandlingVedtakId).isNotNull();
        BehandlingVedtak lagret = repository.hent(BehandlingVedtak.class, behandlingVedtakId);
        assertThat(lagret).isSameAs(behandlingVedtak);
    }

    @Test
    public void skalLagreOgHenteVedtak() {
        // Arrange
        BehandlingVedtak behandlingVedtak = opprettBehandlingVedtak(behandling);

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);
        Optional<BehandlingVedtak> lagretVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandling.getId());

        // Assert
        assertThat(lagretVedtakOpt).hasValueSatisfying(lagretVedtak -> {
            assertThat(lagretVedtak.getId()).isNotNull();
            assertThat(lagretVedtak).isSameAs(behandlingVedtak);
        });
    }

    private BehandlingVedtak opprettBehandlingVedtak(Behandling behandling) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.hent(behandling.getId());
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
            .medAnsvarligSaksbehandler("E2354345")
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .medBehandlingsresultat(behandlingsresultat)
            .build();
        return behandlingVedtak;
    }
}
