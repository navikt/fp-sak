package no.nav.foreldrepenger.domene.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class BekreftFaktaForAleneomsorgAksjonspunktTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    private final YtelseFordelingTjeneste ytelseFordelingTjeneste = new YtelseFordelingTjeneste(ytelsesFordelingRepository);

    @Test
    public void skal_lagre_ned_bekreftet_aksjonspunkt_aleneomsorg() {
        var behandling = opprettBehandling();
        var behandlingId = behandling.getId();
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForAleneomsorg(behandlingId, false);

        var perioderAleneOmsorgOptional = ytelsesFordelingRepository.hentAggregat(
            behandlingId).getPerioderAleneOmsorg();
        assertThat(perioderAleneOmsorgOptional).isPresent();
        var periodeAleneOmsorg = perioderAleneOmsorgOptional.get().getPerioder();
        assertThat(periodeAleneOmsorg).isEmpty();

        var perioderAnnenforelderHarRettOptional = ytelsesFordelingRepository.hentAggregat(
            behandlingId).getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRettOptional).isEmpty();

        //må legge inn etter endret til har aleneomsorg
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForAleneomsorg(behandlingId, true);
        perioderAleneOmsorgOptional = ytelsesFordelingRepository.hentAggregat(behandlingId).getPerioderAleneOmsorg();
        assertThat(perioderAleneOmsorgOptional).isPresent();
        periodeAleneOmsorg = perioderAleneOmsorgOptional.get().getPerioder();
        assertThat(periodeAleneOmsorg).hasSize(1);
        assertThat(periodeAleneOmsorg.get(0).getPeriode()).isEqualTo(
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()));

        perioderAnnenforelderHarRettOptional = ytelsesFordelingRepository.hentAggregat(behandlingId)
            .getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRettOptional).isPresent();
        var periodeAnnenforelderHarRett = perioderAnnenforelderHarRettOptional.get().getPerioder();
        assertThat(periodeAnnenforelderHarRett).isEmpty();
    }

    private Behandling opprettBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
    }
}
