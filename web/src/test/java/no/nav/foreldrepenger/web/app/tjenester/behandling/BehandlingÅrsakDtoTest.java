package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;

@CdiDbAwareTest
class BehandlingÅrsakDtoTest {

    @Inject
    private BehandlingDtoTjeneste behandlingDtoTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void skal_teste_at_behandlingÅrsakDto_får_korrekte_verdier() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultFordeling(LocalDate.now());
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now())
            .medOpprinneligEndringsdato(LocalDate.now())
            .build();
        scenario.medAvklarteUttakDatoer(avklarteUttakDatoer);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING).medManueltOpprettet(true);
        behandlingÅrsak.buildFor(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        var dto = behandlingDtoTjeneste.lagUtvidetBehandlingDto(behandling, null);

        var årsaker = dto.getBehandlingÅrsaker();

        assertThat(årsaker).isNotNull().hasSize(1);
        assertThat(årsaker.get(0).getBehandlingArsakType()).isEqualTo(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING);
        assertThat(årsaker.get(0).isManueltOpprettet()).isTrue();

    }
}
