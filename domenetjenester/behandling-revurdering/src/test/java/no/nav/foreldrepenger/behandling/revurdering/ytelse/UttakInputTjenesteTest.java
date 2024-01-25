package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class UttakInputTjenesteTest {

    @Inject
    private UttakInputTjeneste tjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void skal_hente_behandlingsårsaker_fra_behandling() {
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultSøknadTerminbekreftelse()
                .lagre(repositoryProvider);
        var årsak = BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(originalBehandling, List.of(årsak), false)
                .medDefaultFordeling(LocalDate.of(2019, 11, 6)).medDefaultSøknadTerminbekreftelse()
                .lagre(repositoryProvider);

        var resultat = tjeneste.lagInput(revurdering);

        assertThat(resultat.isBehandlingManueltOpprettet()).isFalse();
        assertThat(resultat.harBehandlingÅrsak(årsak)).isTrue();
    }

    @Test
    void skal_sette_om_behandling_er_manuelt_behandlet() {
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultSøknadTerminbekreftelse()
                .lagre(repositoryProvider);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(originalBehandling, List.of(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT), true)
                .medDefaultFordeling(LocalDate.of(2019, 11, 6)).medDefaultSøknadTerminbekreftelse()
                .lagre(repositoryProvider);

        var resultat = tjeneste.lagInput(revurdering);

        assertThat(resultat.isBehandlingManueltOpprettet()).isTrue();
    }
}
