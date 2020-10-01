package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task.OpprettGrunnbeløpTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.web.RepositoryAwareTest;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

public class ForvaltningBeregningRestTjenesteTest extends RepositoryAwareTest {

    private ForvaltningBeregningRestTjeneste forvaltningBeregningRestTjeneste;

    @BeforeEach
    public void before() {
        forvaltningBeregningRestTjeneste = new ForvaltningBeregningRestTjeneste(beregningsgrunnlagRepository,
                repositoryProvider,
                prosessTaskRepository, null, null);
    }

    @Test
    public void opprettTaskerForGrunnbeløp() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(LocalDate.now()).build();
        BeregningsgrunnlagGrunnlagBuilder grunnlagUtenGrunnbeløp = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(bg);
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagUtenGrunnbeløp, BeregningsgrunnlagTilstand.OPPRETTET);
        forvaltningBeregningRestTjeneste.opprettGrunnbeløp();
        List<ProsessTaskData> prosessTaskData = prosessTaskRepository.finnIkkeStartet();
        List<ProsessTaskData> opprettTasker = prosessTaskData.stream().filter(task -> task.getTaskType().equals(OpprettGrunnbeløpTask.TASKNAME))
                .collect(Collectors.toList());
        assertThat(opprettTasker.size()).isEqualTo(1);
        assertThat(opprettTasker.get(0).getBehandlingId()).isEqualTo(behandling.getId().toString());
    }

    @Test
    public void opprettGrunnbeløp() {
        final ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
        BeregningsgrunnlagEntitet bg = BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(LocalDate.now()).build();
        BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
                .medBeregningsgrunnlag(bg)
                .build(behandling.getId(), BeregningsgrunnlagTilstand.OPPRETTET);

    }
}
