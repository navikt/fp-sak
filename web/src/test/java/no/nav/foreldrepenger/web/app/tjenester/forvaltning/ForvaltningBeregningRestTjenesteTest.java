package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task.OpprettGrunnbeløpTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class ForvaltningBeregningRestTjenesteTest {


    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);
    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final ProsessTaskRepository prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, null);
    private ForvaltningBeregningRestTjeneste forvaltningBeregningRestTjeneste = new ForvaltningBeregningRestTjeneste(beregningsgrunnlagRepository, behandlingRepository,
        prosessTaskRepository, null, null);


    @Test
    public void opprettTaskerForGrunnbeløp() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(LocalDate.now()).build();
        BeregningsgrunnlagGrunnlagBuilder grunnlagUtenGrunnbeløp = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(bg);
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagUtenGrunnbeløp, BeregningsgrunnlagTilstand.OPPRETTET);

        // Act
        forvaltningBeregningRestTjeneste.opprettGrunnbeløp();

        // Assert
        List<ProsessTaskData> prosessTaskData = prosessTaskRepository.finnIkkeStartet();
        List<ProsessTaskData> opprettTasker = prosessTaskData.stream().filter(task -> task.getTaskType().equals(OpprettGrunnbeløpTask.TASKNAME)).collect(Collectors.toList());
        assertThat(opprettTasker.size()).isEqualTo(1);
        assertThat(opprettTasker.get(0).getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    public void opprettGrunnbeløp() {
        final ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(LocalDate.now()).build();
        BeregningsgrunnlagGrunnlagEntitet grunnlagUtenGrunnbeløp = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(bg)
            .build(behandling.getId(), BeregningsgrunnlagTilstand.OPPRETTET);




    }
}
