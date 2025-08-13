package no.nav.foreldrepenger.domene.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class BekreftFaktaForAleneomsorgAksjonspunktTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    private final YtelseFordelingTjeneste ytelseFordelingTjeneste = new YtelseFordelingTjeneste(ytelsesFordelingRepository);

    @Test
    void skal_lagre_ned_bekreftet_aksjonspunkt_aleneomsorg() {
        var behandling = opprettBehandling();
        var behandlingId = behandling.getId();
        ytelseFordelingTjeneste.avklarRettighet(behandlingId, Rettighetstype.BEGGE_RETT);

        var aleneomsorgAvklaring = ytelsesFordelingRepository.hentAggregat(behandlingId).getAleneomsorgAvklaring();
        assertThat(aleneomsorgAvklaring).isNotNull().isFalse();

        ytelseFordelingTjeneste.avklarRettighet(behandlingId, Rettighetstype.ALENEOMSORG);
        aleneomsorgAvklaring = ytelsesFordelingRepository.hentAggregat(behandlingId).getAleneomsorgAvklaring();
        assertThat(aleneomsorgAvklaring).isNotNull().isTrue();

        var annenForelderRettAvklaring = ytelsesFordelingRepository.hentAggregat(behandlingId).getAnnenForelderRettAvklaring();
        assertThat(annenForelderRettAvklaring).isNotNull().isFalse();
    }

    private Behandling opprettBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
    }
}
