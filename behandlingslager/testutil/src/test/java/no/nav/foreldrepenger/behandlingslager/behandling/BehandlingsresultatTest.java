package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

class BehandlingsresultatTest extends EntityManagerAwareTest {


    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(getEntityManager());
    }

    @Test
    void skal_opprette_ny_behandlingsresultat() {
        var behandlingsresultatBuilder = Behandlingsresultat.builderForInngangsvilkår();
        var behandlingsresultat = behandlingsresultatBuilder.build();

        assertThat(behandlingsresultat).isNotNull();
        assertThat(behandlingsresultat.getVilkårResultat()).isNotNull();
    }

    @Test
    void skal_opprette_ny_behandlingsresultat_og_lagre_med_ikke_fastsatt_vilkårresultat() {
        var behandling = opprettBehandling();

        var behandlingsresultatBuilder = Behandlingsresultat.builderForInngangsvilkår();
        var behandlingsresultat = behandlingsresultatBuilder.buildFor(behandling);

        assertThat(behandling.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
        assertThat(behandlingsresultat.getBehandlingId()).isNotNull();

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);

        var id = behandling.getId();
        assertThat(id).isNotNull();

        var lagretBehandling = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(lagretBehandling).isEqualTo(behandling);
        assertThat(lagretBehandling.getBehandlingsresultat()).isEqualTo(behandlingsresultat);
    }

    private Behandling opprettBehandling() {
        var fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        new FagsakRepository(getEntityManager()).opprettNy(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, new BehandlingLåsRepository(getEntityManager()).taLås(behandling.getId()));
        return behandling;
    }
}
