package no.nav.foreldrepenger.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
public class DekningsgradTjenesteTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider.getFagsakRelasjonRepository(),
                null, repositoryProvider.getFagsakRepository());
    }

    @Test
    public void skal_gi_endret_dekningsgrad_hvis_behandlingen_har_endret_dekningsgrad() {
        var behandling = behandling(true);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._80);
        repositoryProvider.getFagsakRelasjonRepository().overstyrDekningsgrad(behandling.getFagsak(), Dekningsgrad._100);

        var tjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, repositoryProvider.getBehandlingsresultatRepository());

        assertThat(tjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(behandling))).isTrue();
    }

    @Test
    public void skal_gi_ikke_gi_endret_dekningsgrad_hvis_dekningsgrad_ikke_er_endret() {
        var behandling = behandling(false);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._80);

        var tjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, repositoryProvider.getBehandlingsresultatRepository());

        assertThat(tjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(behandling))).isFalse();
    }

    @Test
    public void skal_gi_ikke_gi_endret_dekningsgrad_hvis_dekningsgrad_er_endret_men_ikke_av_behandlingen() {
        var behandling = behandling(false);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._80);
        repositoryProvider.getFagsakRelasjonRepository().overstyrDekningsgrad(behandling.getFagsak(), Dekningsgrad._100);

        var tjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, repositoryProvider.getBehandlingsresultatRepository());

        assertThat(tjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(behandling))).isFalse();
    }

    @Test
    public void skal_gi_ikke_gi_endret_dekningsgrad_hvis_dekningsgrad_endret_til_samme_verdi() {
        var behandling = behandling(true);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._80);
        repositoryProvider.getFagsakRelasjonRepository().overstyrDekningsgrad(behandling.getFagsak(), Dekningsgrad._80);

        var tjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, repositoryProvider.getBehandlingsresultatRepository());

        assertThat(tjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(behandling))).isFalse();
    }

    private Behandling behandling(boolean endretDekningsgrad) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medEndretDekningsgrad(endretDekningsgrad));
        scenario.medBehandlingVedtak().medAnsvarligSaksbehandler("sdaw").medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.UDEFINERT);
        return scenario.lagre(repositoryProvider);
    }

}
