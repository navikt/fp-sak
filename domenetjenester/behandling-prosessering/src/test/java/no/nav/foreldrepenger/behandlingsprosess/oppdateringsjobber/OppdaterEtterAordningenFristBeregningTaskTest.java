package no.nav.foreldrepenger.behandlingsprosess.oppdateringsjobber;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@CdiDbAwareTest
class OppdaterEtterAordningenFristBeregningTaskTest extends EntityManagerAwareTest {

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Test
    void kan_henlegge_behandling_uten_søknad_som_er_satt_på_vent() {
        var repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var oppdateringTask = new OppdaterEtterAordningFristBeregningTask(behandlingProsesseringTjeneste, getEntityManager(), prosessTaskTjeneste);

        var scenario = ScenarioMorSøkerForeldrepenger // Oppretter scenario uten søknad for å simulere sitausjoner som
            // f.eks der inntektsmelding kommer først.
            .forFødselUtenSøknad(AktørId.dummy())
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårUtfallType.OPPFYLT);
        var behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getBehandlingRepository().oppdaterSistOppdatertTidspunkt(behandling, LocalDateTime.now().minusWeeks(4).minusDays(1));
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);
        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling.getId());
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);

        oppdateringTask.doTask(ProsessTaskData.forProsessTask(OppdaterEtterAordningFristBeregningTask.class));

        verify(behandlingProsesseringTjeneste, times(1)).reposisjonerBehandlingTilbakeTil(eq(behandling), any(), eq(BehandlingStegType.DEKNINGSGRAD));
        verify(behandlingProsesseringTjeneste, times(1)).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), any());
        verify(prosessTaskTjeneste, times(1)).lagre(any(ProsessTaskData.class));
    }

}
