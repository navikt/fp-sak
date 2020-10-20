package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BehandlingVedtakRepositoryTest extends EntityManagerAwareTest {

    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
    }

    private Behandling opprettBehandling() {
        var behandlingBuilder = new BasicBehandlingBuilder(getEntityManager());
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var resultat = Behandlingsresultat.builder().build();
        behandlingBuilder.lagreBehandlingsresultat(behandling.getId(), resultat);
        return behandling;
    }

    @Test
    public void skalLagreVedtak() {
        // Arrange
        var behandling = opprettBehandling();
        BehandlingVedtak behandlingVedtak = opprettBehandlingVedtak(behandling);

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);

        // Assert
        Long behandlingVedtakId = behandlingVedtak.getId();
        assertThat(behandlingVedtakId).isNotNull();
        BehandlingVedtak lagret = behandlingVedtakRepository.hentForBehandling(behandling.getId());
        assertThat(lagret).isSameAs(behandlingVedtak);
    }

    @Test
    public void skalLagreOgHenteVedtak() {
        // Arrange
        var behandling = opprettBehandling();
        BehandlingVedtak behandlingVedtak = opprettBehandlingVedtak(behandling);

        // Act
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);
        Optional<BehandlingVedtak> lagretVedtakOpt = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId());

        // Assert
        assertThat(lagretVedtakOpt).hasValueSatisfying(lagretVedtak -> {
            assertThat(lagretVedtak.getId()).isNotNull();
            assertThat(lagretVedtak).isSameAs(behandlingVedtak);
        });
    }

    private BehandlingVedtak opprettBehandlingVedtak(Behandling behandling) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.hent(behandling.getId());
        return BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
            .medAnsvarligSaksbehandler("E2354345")
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medIverksettingStatus(IverksettingStatus.IVERKSATT)
            .medBehandlingsresultat(behandlingsresultat)
            .build();
    }
}
