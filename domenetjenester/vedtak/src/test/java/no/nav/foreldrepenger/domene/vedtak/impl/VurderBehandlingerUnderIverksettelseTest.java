package no.nav.foreldrepenger.domene.vedtak.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioInnsynEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
class VurderBehandlingerUnderIverksettelseTest {

    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private VurderBehandlingerUnderIverksettelse tjeneste;

    @BeforeEach
    void setup(EntityManager entityManager) {
        this.entityManager = entityManager;
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        tjeneste = new VurderBehandlingerUnderIverksettelse(repositoryProvider);
    }

    private Behandling lagreBehandling() {
        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        førstegangScenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var førstegangBehandling = førstegangScenario.lagre(repositoryProvider);
        entityManager.persist(getBehandlingsresultat(førstegangBehandling));
        return førstegangBehandling;
    }

    @Test
    void neiHvisIngenAnnenBehandling() {
        var førstegangBehandling = lagreBehandling();
        // Act
        var resultat = tjeneste.vurder(førstegangBehandling);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void neiHvisAnnenBehandlingErIverksatt() {
        // Arrange
        var førstegangBehandling = lagreBehandling();
        lagreBehandlingVedtak(førstegangBehandling, IverksettingStatus.IVERKSATT);
        var revurdering = lagreRevurdering(førstegangBehandling);
        lagreBehandlingVedtak(revurdering, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        var resultat = tjeneste.vurder(revurdering);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void jaHvisAnnenBehandlingErIkkeIverksatt() {
        // Arrange
        var førstegangBehandling = lagreBehandling();
        lagreBehandlingVedtak(førstegangBehandling, IverksettingStatus.IKKE_IVERKSATT);
        var revurdering = lagreRevurdering(førstegangBehandling);
        lagreBehandlingVedtak(revurdering, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        var resultat = tjeneste.vurder(revurdering);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void neiForFørstegangsbehandlingNårRevurderingErUnderIverksetting() {
        // Arrange
        var førstegangBehandling = lagreBehandling();
        lagreBehandlingVedtak(førstegangBehandling, IverksettingStatus.IKKE_IVERKSATT);
        var revurdering = lagreRevurdering(førstegangBehandling);
        lagreBehandlingVedtak(revurdering, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        var resultat = tjeneste.vurder(førstegangBehandling);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void neiHvisInnsynOgAnnenBehandlingUnderIverksetting() {
        // Arrange
        var førstegangScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenarioInnsyn = ScenarioInnsynEngangsstønad.innsyn(førstegangScenario);
        var innsyn = scenarioInnsyn.lagre(repositoryProvider);
        var originalBehandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
                innsyn.getFagsakId(), BehandlingType.FØRSTEGANGSSØKNAD).get();
        lagreBehandlingVedtak(originalBehandling, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        var resultat = tjeneste.vurder(innsyn);

        // Assert
        assertThat(resultat).isFalse();
    }

    private Behandling lagreRevurdering(Behandling førstegangBehandling) {
        var revurdering = Behandling.fraTidligereBehandling(førstegangBehandling, BehandlingType.REVURDERING)
                .build();
        var lås = new BehandlingLås(revurdering.getId());
        behandlingRepository.lagre(revurdering, lås);
        var behandlingsresultat = Behandlingsresultat.builderForInngangsvilkår().buildFor(revurdering);
        entityManager.persist(behandlingsresultat);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        return revurdering;
    }

    private BehandlingVedtak lagreBehandlingVedtak(Behandling behandling, IverksettingStatus iverksettingStatus) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        var behandlingsresultat = getBehandlingsresultat(behandling);
        var behandlingVedtak = BehandlingVedtak.builder()
                .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
                .medAnsvarligSaksbehandler("E2354345")
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medIverksettingStatus(iverksettingStatus)
                .medBehandlingsresultat(behandlingsresultat)
                .build();
        var opprettetTidspunkt = behandling.erRevurdering() ? LocalDateTime.now()
                .plusSeconds(1) : LocalDateTime.now();
        behandling.setOpprettetTidspunkt(opprettetTidspunkt);
        behandlingVedtakRepository.lagre(behandlingVedtak, lås);
        if (IverksettingStatus.IKKE_IVERKSATT.equals(iverksettingStatus)) {
            behandling.setStatus(BehandlingStatus.IVERKSETTER_VEDTAK);
        }
        return behandlingVedtak;
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }
}
