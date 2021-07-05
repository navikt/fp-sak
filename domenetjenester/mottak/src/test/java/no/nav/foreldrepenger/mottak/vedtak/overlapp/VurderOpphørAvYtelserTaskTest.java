package no.nav.foreldrepenger.mottak.vedtak.overlapp;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class VurderOpphørAvYtelserTaskTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private VurderOpphørAvYtelserTask vurderOpphørAvYtelserTask;
    private final LoggOverlappEksterneYtelserTjeneste identifiserOverlappendeInfotrygdYtelser = Mockito.mock(
        LoggOverlappEksterneYtelserTjeneste.class);
    private final VurderOpphørAvYtelser vurderOpphørAvYtelser = Mockito.mock(VurderOpphørAvYtelser.class);

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        vurderOpphørAvYtelserTask = new VurderOpphørAvYtelserTask(vurderOpphørAvYtelser,
            identifiserOverlappendeInfotrygdYtelser, repositoryProvider);
    }

    @Test
    public void vurderOpphørForFørstegangsbehandling() {
        var behandling = lagBehandlingFP(BehandlingType.FØRSTEGANGSSØKNAD);

        var prosessTaskData = new ProsessTaskData(VurderOpphørAvYtelserTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().toString());

        vurderOpphørAvYtelserTask.doTask(prosessTaskData);

        verify(identifiserOverlappendeInfotrygdYtelser, times(1)).loggOverlappForVedtakFPSAK(behandling.getId(),
            behandling.getFagsak().getSaksnummer(), behandling.getAktørId());
        verify(vurderOpphørAvYtelser, times(1)).vurderOpphørAvYtelser(behandling.getFagsak().getId(),
            behandling.getId());
    }

    @Test
    public void ikkeVurderOpphørForRevurderingsbehandling() {
        var behandling = lagBehandlingFP(BehandlingType.REVURDERING);

        var prosessTaskData = new ProsessTaskData(VurderOpphørAvYtelserTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().toString());

        vurderOpphørAvYtelserTask.doTask(prosessTaskData);

        verify(identifiserOverlappendeInfotrygdYtelser, times(1)).loggOverlappForVedtakFPSAK(behandling.getId(),
            behandling.getFagsak().getSaksnummer(), behandling.getAktørId());
        verify(vurderOpphørAvYtelser, times(0)).vurderOpphørAvYtelser(behandling.getFagsak().getId(),
            behandling.getId());
    }

    public Behandling lagBehandlingFP(BehandlingType behandlingType) {
        ScenarioMorSøkerForeldrepenger scenarioFP;
        scenarioFP = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioFP.medBehandlingType(behandlingType);
        scenarioFP.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioFP.medVilkårResultatType(VilkårResultatType.INNVILGET);

        var behandling = scenarioFP.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }
}
