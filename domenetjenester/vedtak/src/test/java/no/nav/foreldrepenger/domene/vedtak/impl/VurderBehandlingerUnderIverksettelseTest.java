package no.nav.foreldrepenger.domene.vedtak.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

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
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class VurderBehandlingerUnderIverksettelseTest {

    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private VurderBehandlingerUnderIverksettelse tjeneste;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        this.entityManager = entityManager;
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        tjeneste = new VurderBehandlingerUnderIverksettelse(repositoryProvider);
    }

    private Behandling lagreBehandling() {
        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        førstegangScenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        var førstegangBehandling = førstegangScenario.lagre(repositoryProvider);
        entityManager.persist(getBehandlingsresultat(førstegangBehandling));
        return førstegangBehandling;
    }

    @Test
    public void neiHvisIngenAnnenBehandling() {
        var førstegangBehandling = lagreBehandling();
        // Act
        boolean resultat = tjeneste.vurder(førstegangBehandling);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void neiHvisAnnenBehandlingErIverksatt() {
        // Arrange
        var førstegangBehandling = lagreBehandling();
        lagreBehandlingVedtak(førstegangBehandling, IverksettingStatus.IVERKSATT);
        Behandling revurdering = lagreRevurdering(førstegangBehandling);
        lagreBehandlingVedtak(revurdering, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        boolean resultat = tjeneste.vurder(revurdering);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void jaHvisAnnenBehandlingErIkkeIverksatt() {
        // Arrange
        var førstegangBehandling = lagreBehandling();
        lagreBehandlingVedtak(førstegangBehandling, IverksettingStatus.IKKE_IVERKSATT);
        Behandling revurdering = lagreRevurdering(førstegangBehandling);
        lagreBehandlingVedtak(revurdering, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        boolean resultat = tjeneste.vurder(revurdering);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void neiForFørstegangsbehandlingNårRevurderingErUnderIverksetting() {
        // Arrange
        var førstegangBehandling = lagreBehandling();
        lagreBehandlingVedtak(førstegangBehandling, IverksettingStatus.IKKE_IVERKSATT);
        Behandling revurdering = lagreRevurdering(førstegangBehandling);
        lagreBehandlingVedtak(revurdering, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        boolean resultat = tjeneste.vurder(førstegangBehandling);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void neiHvisInnsynOgAnnenBehandlingUnderIverksetting() {
        // Arrange
        ScenarioMorSøkerEngangsstønad førstegangScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioInnsynEngangsstønad scenarioInnsyn = ScenarioInnsynEngangsstønad.innsyn(førstegangScenario);
        Behandling innsyn = scenarioInnsyn.lagre(repositoryProvider);
        Behandling originalBehandling = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(
                innsyn.getFagsakId(), BehandlingType.FØRSTEGANGSSØKNAD).get();
        lagreBehandlingVedtak(originalBehandling, IverksettingStatus.IKKE_IVERKSATT);

        // Act
        boolean resultat = tjeneste.vurder(innsyn);

        // Assert
        assertThat(resultat).isFalse();
    }

    private Behandling lagreRevurdering(Behandling førstegangBehandling) {
        Behandling revurdering = Behandling.fraTidligereBehandling(førstegangBehandling, BehandlingType.REVURDERING)
                .build();
        BehandlingLås lås = new BehandlingLås(revurdering.getId());
        behandlingRepository.lagre(revurdering, lås);
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builderForInngangsvilkår().buildFor(revurdering);
        entityManager.persist(behandlingsresultat);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        return revurdering;
    }

    private BehandlingVedtak lagreBehandlingVedtak(Behandling behandling, IverksettingStatus iverksettingStatus) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
                .medVedtakstidspunkt(LocalDateTime.now().minusDays(3))
                .medAnsvarligSaksbehandler("E2354345")
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medIverksettingStatus(iverksettingStatus)
                .medBehandlingsresultat(behandlingsresultat)
                .build();
        LocalDateTime opprettetTidspunkt = behandling.erRevurdering() ? LocalDateTime.now()
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
