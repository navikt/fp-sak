package no.nav.foreldrepenger.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
class DekningsgradTjenesteTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
    }

    @Test
    void skal_gi_endret_dekningsgrad_hvis_behandlingen_har_endret_dekningsgrad() {
        var behandling = førstegangsbehandling(Dekningsgrad._80);
        var revurdering = revurdering(behandling);
        overstyrDekningsgrad(revurdering, Dekningsgrad._100);

        var tjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, repositoryProvider.getBehandlingsresultatRepository(),
            repositoryProvider.getYtelsesFordelingRepository());

        assertThat(tjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(revurdering))).isTrue();
    }

    private Behandling revurdering(Behandling behandling) {
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING)
            .lagre(repositoryProvider);
        repositoryProvider.getYtelsesFordelingRepository().kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), revurdering);
        return revurdering;
    }

    private void overstyrDekningsgrad(Behandling behandling, Dekningsgrad dekningsgrad) {
        fagsakRelasjonTjeneste.overstyrDekningsgrad(behandling.getFagsak(), dekningsgrad);
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        ytelsesFordelingRepository.lagre(behandling.getId(),
            ytelsesFordelingRepository.opprettBuilder(behandling.getId()).medSakskompleksDekningsgrad(dekningsgrad).build());
    }

    @Test
    void skal_gi_ikke_gi_endret_dekningsgrad_hvis_dekningsgrad_ikke_er_endret() {
        var behandling = førstegangsbehandling(Dekningsgrad._80);

        var tjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, repositoryProvider.getBehandlingsresultatRepository(),
            repositoryProvider.getYtelsesFordelingRepository());

        assertThat(tjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(behandling))).isFalse();
    }

    @Test
    void skal_gi_ikke_gi_endret_dekningsgrad_hvis_dekningsgrad_er_endret_men_ikke_av_behandlingen() {
        var behandling = førstegangsbehandling(Dekningsgrad._80);
        overstyrDekningsgrad(behandling, Dekningsgrad._100);
        var revurdering = revurdering(behandling);

        var tjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, repositoryProvider.getBehandlingsresultatRepository(),
            repositoryProvider.getYtelsesFordelingRepository());

        assertThat(tjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(revurdering))).isFalse();
    }

    @Test
    void skal_gi_ikke_gi_endret_dekningsgrad_hvis_dekningsgrad_endret_til_samme_verdi() {
        var behandling = førstegangsbehandling(Dekningsgrad._80);
        overstyrDekningsgrad(behandling, Dekningsgrad._80);

        var tjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, repositoryProvider.getBehandlingsresultatRepository(),
            repositoryProvider.getYtelsesFordelingRepository());

        assertThat(tjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(behandling))).isFalse();
    }

    private Behandling førstegangsbehandling(Dekningsgrad dekningsgrad) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOppgittDekningsgrad(dekningsgrad);
        scenario.medBehandlingVedtak().medAnsvarligSaksbehandler("sdaw").medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.UDEFINERT);
        var behandling = scenario.lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak(), dekningsgrad);
        return behandling;
    }

}
