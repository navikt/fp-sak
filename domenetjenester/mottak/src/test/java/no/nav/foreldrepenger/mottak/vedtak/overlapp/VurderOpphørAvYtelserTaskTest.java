package no.nav.foreldrepenger.mottak.vedtak.overlapp;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
class VurderOpphørAvYtelserTaskTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private VurderOpphørAvYtelserTask vurderOpphørAvYtelserTask;
    @Mock
    private LoggOverlappEksterneYtelserTjeneste identifiserOverlappendeInfotrygdYtelser;
    @Mock
    private VurderOpphørAvYtelser vurderOpphørAvYtelser;

    @Test
    void vurderOpphørForFørstegangsbehandling() {
        var scenario = lagBehandlingFP(BehandlingType.FØRSTEGANGSSØKNAD);
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        vurderOpphørAvYtelserTask = new VurderOpphørAvYtelserTask(vurderOpphørAvYtelser,
            identifiserOverlappendeInfotrygdYtelser, repositoryProvider);
        var behandling = scenario.lagMocked();

        var prosessTaskData = ProsessTaskData.forProsessTask(VurderOpphørAvYtelserTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().toString());

        vurderOpphørAvYtelserTask.doTask(prosessTaskData);

        verify(identifiserOverlappendeInfotrygdYtelser, times(1)).loggOverlappForVedtakFPSAK(behandling);
        verify(vurderOpphørAvYtelser, times(1)).vurderOpphørAvYtelser(behandling);
    }

    public ScenarioMorSøkerForeldrepenger lagBehandlingFP(BehandlingType behandlingType) {
        ScenarioMorSøkerForeldrepenger scenarioFP;
        scenarioFP = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioFP.medBehandlingType(behandlingType);
        scenarioFP.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));

        return scenarioFP;
    }
}
