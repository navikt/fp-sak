package no.nav.foreldrepenger.mottak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class BehandlingsoppretterTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private Behandlingsoppretter behandlingsoppretter;

    @Test
    void skal_nullstille_ansvarlig_ved_merget_og_henlagt() {
        var opprinnelig = ScenarioMorSøkerEngangsstønad.forFødsel().lagre(repositoryProvider);
        opprinnelig.avsluttBehandling();
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(opprinnelig, behandlingRepository.taSkriveLås(opprinnelig));
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medOriginalBehandling(opprinnelig, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .lagre(repositoryProvider);
        behandling.setAnsvarligSaksbehandler("Z123456");
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        behandlingsoppretter.henleggBehandling(behandling);

        var henlagt = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(henlagt.getAnsvarligSaksbehandler()).isNull();
        assertThat(henlagt.erAvsluttet()).isTrue();
        assertThat(repositoryProvider.getBehandlingsresultatRepository().hent(henlagt.getId()).getBehandlingResultatType())
            .isEqualTo(BehandlingResultatType.MERGET_OG_HENLAGT);
    }

    @Test
    void skal_videreføre_ansvarlig_til_ny_manuell_revurdering_og_nullstille_på_henlagt_behandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now()).medVedtakResultatType(VedtakResultatType.INNVILGET);
        var opprinnelig = scenario.lagre(repositoryProvider);
        opprinnelig.avsluttBehandling();
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(opprinnelig, behandlingRepository.taSkriveLås(opprinnelig));
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medOriginalBehandling(opprinnelig, List.of(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER), true)
            .medBehandlendeEnhet("4833")
            .lagre(repositoryProvider);
        behandling.setAnsvarligSaksbehandler("Z123456");
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var nyRevurdering = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling);

        assertThat(behandlingRepository.hentBehandling(behandling.getId()).getAnsvarligSaksbehandler()).isNull();
        assertThat(repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getBehandlingResultatType())
            .isEqualTo(BehandlingResultatType.MERGET_OG_HENLAGT);
        assertThat(nyRevurdering.erManueltOpprettet()).isTrue();
        assertThat(behandlingRepository.hentBehandling(nyRevurdering.getId()).getAnsvarligSaksbehandler()).isEqualTo("Z123456");
    }
}
