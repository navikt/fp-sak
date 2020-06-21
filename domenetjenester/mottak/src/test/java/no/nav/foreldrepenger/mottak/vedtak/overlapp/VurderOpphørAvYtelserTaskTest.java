package no.nav.foreldrepenger.mottak.vedtak.overlapp;


import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class VurderOpphørAvYtelserTaskTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    private VurderOpphørAvYtelserTask vurderOpphørAvYtelserTask;
    private IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelser = Mockito.mock(IdentifiserOverlappendeInfotrygdYtelseTjeneste.class);
    private VurderOpphørAvYtelser vurderOpphørAvYtelser = Mockito.mock(VurderOpphørAvYtelser.class);

    @Before
    public void setUp() {
        initMocks(this);
        vurderOpphørAvYtelserTask = new VurderOpphørAvYtelserTask(vurderOpphørAvYtelser, null, null, identifiserOverlappendeInfotrygdYtelser, behandlingRepository );

    }
    @Test
    public void vurderOpphørForFørstegangsbehandling() {
        Behandling behandling = lagBehandlingFP(BehandlingType.FØRSTEGANGSSØKNAD);

        ProsessTaskData prosessTaskData = new ProsessTaskData(VurderOpphørAvYtelserTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().toString());

        vurderOpphørAvYtelserTask.doTask(prosessTaskData);

        verify(identifiserOverlappendeInfotrygdYtelser, times(1)).vurderOglagreEventueltOverlapp(behandling.getId());
        verify(vurderOpphørAvYtelser, times(1)).vurderOpphørAvYtelser(behandling.getFagsak().getId(), behandling.getId());

    }
    @Test
    public void ikkeVurderOpphørForRevurderingsbehandling() {
        Behandling behandling = lagBehandlingFP(BehandlingType.REVURDERING);

        ProsessTaskData prosessTaskData = new ProsessTaskData(VurderOpphørAvYtelserTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().toString());

        vurderOpphørAvYtelserTask.doTask(prosessTaskData);

        verify(identifiserOverlappendeInfotrygdYtelser, times(1)).vurderOglagreEventueltOverlapp(behandling.getId());
        verify(vurderOpphørAvYtelser, times(0)).vurderOpphørAvYtelser(behandling.getFagsak().getId(), behandling.getId());

    }
    public Behandling lagBehandlingFP(BehandlingType behandlingType) {
        ScenarioMorSøkerForeldrepenger scenarioFP;
        scenarioFP = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioFP.medBehandlingType(behandlingType);
        scenarioFP.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioFP.medVilkårResultatType(VilkårResultatType.INNVILGET);

        Behandling behandling = scenarioFP.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }
}
