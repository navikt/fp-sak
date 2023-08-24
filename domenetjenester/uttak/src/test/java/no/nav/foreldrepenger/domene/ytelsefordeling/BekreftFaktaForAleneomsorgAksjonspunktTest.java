package no.nav.foreldrepenger.domene.ytelsefordeling;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BekreftFaktaForAleneomsorgAksjonspunktTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    private final YtelseFordelingTjeneste ytelseFordelingTjeneste = new YtelseFordelingTjeneste(ytelsesFordelingRepository);

    @Test
    void skal_lagre_ned_bekreftet_aksjonspunkt_aleneomsorg() {
        var behandling = opprettBehandling();
        var behandlingId = behandling.getId();
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForAleneomsorg(behandlingId, false);

        var perioderAleneOmsorg = ytelsesFordelingRepository.hentAggregat(
            behandlingId).getAleneomsorgAvklaring();
        assertThat(perioderAleneOmsorg).isNotNull();
        assertThat(perioderAleneOmsorg).isFalse();

        var perioderAnnenforelderHarRettOptional = ytelsesFordelingRepository.hentAggregat(
            behandlingId).getAnnenForelderRettAvklaring();
        assertThat(perioderAnnenforelderHarRettOptional).isNull();

        //må legge inn etter endret til har aleneomsorg
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForAleneomsorg(behandlingId, true);
        perioderAleneOmsorg = ytelsesFordelingRepository.hentAggregat(behandlingId).getAleneomsorgAvklaring();
        assertThat(perioderAleneOmsorg).isNotNull();
        assertThat(perioderAleneOmsorg).isTrue();

        perioderAnnenforelderHarRettOptional = ytelsesFordelingRepository.hentAggregat(behandlingId)
            .getAnnenForelderRettAvklaring();
        assertThat(perioderAnnenforelderHarRettOptional).isNotNull();
        assertThat(perioderAnnenforelderHarRettOptional).isFalse();
    }

    private Behandling opprettBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
    }
}
