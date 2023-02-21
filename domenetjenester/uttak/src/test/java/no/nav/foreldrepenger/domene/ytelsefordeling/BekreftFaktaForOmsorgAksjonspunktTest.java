package no.nav.foreldrepenger.domene.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class BekreftFaktaForOmsorgAksjonspunktTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    private final YtelseFordelingTjeneste ytelseFordelingTjeneste = new YtelseFordelingTjeneste(ytelsesFordelingRepository);

    @Test
    public void skal_lagre_ned_bekreftet_aksjonspunkt_omsorg() {
        var behandling = opprettBehandling();
        // simulerer svar fra GUI
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandling.getId(), false);

        var overstyrtOmsorg = ytelsesFordelingRepository.hentAggregat(behandling.getId()).getOverstyrtOmsorg();
        assertThat(overstyrtOmsorg).isNotNull();
        assertThat(overstyrtOmsorg).isFalse();

        //må nullstille etter endret til har omsorg
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandling.getId(), true);
        overstyrtOmsorg = ytelsesFordelingRepository.hentAggregat(behandling.getId()).getOverstyrtOmsorg();
        assertThat(overstyrtOmsorg).isNotNull();
        assertThat(overstyrtOmsorg).isTrue();
    }

    private Behandling opprettBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
    }
}
