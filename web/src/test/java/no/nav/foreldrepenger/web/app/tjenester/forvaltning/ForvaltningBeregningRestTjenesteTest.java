package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task.OpprettRegisterAktitviteterTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.prosesstask.impl.SubjectProvider;
import no.nav.vedtak.felles.testutilities.db.Repository;
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
        prosessTaskRepository);

    @Test
    public void opprettTaskerForRegisteraktiviteter() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        BeregningsgrunnlagGrunnlagBuilder grunnlagUtenAktiviteter = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty());
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagUtenAktiviteter, BeregningsgrunnlagTilstand.OPPRETTET);
        BeregningsgrunnlagGrunnlagBuilder grunnlagMedAktiviteter = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .leggTilAktivitet(BeregningAktivitetEntitet.builder()
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(10)))
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING).build()).build());
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagMedAktiviteter, BeregningsgrunnlagTilstand.FASTSATT);

        // Act
        forvaltningBeregningRestTjeneste.migrerRegisterAlleSaker();

        // Assert
        List<ProsessTaskData> prosessTaskData = prosessTaskRepository.finnIkkeStartet();
        List<ProsessTaskData> opprettTasker = prosessTaskData.stream().filter(task -> task.getTaskType().equals(OpprettRegisterAktitviteterTask.TASKNAME)).collect(Collectors.toList());
        assertThat(opprettTasker.size()).isEqualTo(1);
        assertThat(opprettTasker.get(0).getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    public void skal_ikkje_opprette_fleire_tasker_for_fleire_grunnlag_i_samme_behandling() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        BeregningsgrunnlagGrunnlagBuilder grunnlagUtenAktiviteter = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty());
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagUtenAktiviteter, BeregningsgrunnlagTilstand.OPPRETTET);
        BeregningsgrunnlagGrunnlagBuilder grunnlagUtenAktiviteter2 = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty());
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagUtenAktiviteter2, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        BeregningsgrunnlagGrunnlagBuilder grunnlagMedAktiviteter = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .leggTilAktivitet(BeregningAktivitetEntitet.builder()
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(10)))
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING).build()).build());
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagMedAktiviteter, BeregningsgrunnlagTilstand.FASTSATT);

        // Act
        forvaltningBeregningRestTjeneste.migrerRegisterAlleSaker();

        // Assert
        List<ProsessTaskData> prosessTaskData = prosessTaskRepository.finnIkkeStartet();
        List<ProsessTaskData> opprettTasker = prosessTaskData.stream().filter(task -> task.getTaskType().equals(OpprettRegisterAktitviteterTask.TASKNAME)).collect(Collectors.toList());
        assertThat(opprettTasker.size()).isEqualTo(1);
        assertThat(opprettTasker.get(0).getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    public void skal_ikkje_opprette_tasker_for_grunnlag_med_register_aktiviteter() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        BeregningsgrunnlagGrunnlagBuilder grunnlagMedAktiviteter = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .leggTilAktivitet(BeregningAktivitetEntitet.builder()
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(10)))
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING).build()).build());
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagMedAktiviteter, BeregningsgrunnlagTilstand.OPPRETTET);
        BeregningsgrunnlagGrunnlagBuilder grunnlagMedAktiviteter2 = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .leggTilAktivitet(BeregningAktivitetEntitet.builder()
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(10)))
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING).build()).build());
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagMedAktiviteter2, BeregningsgrunnlagTilstand.FASTSATT);

        // Act
        forvaltningBeregningRestTjeneste.migrerRegisterAlleSaker();

        // Assert
        List<ProsessTaskData> prosessTaskData = prosessTaskRepository.finnIkkeStartet();
        List<ProsessTaskData> opprettTasker = prosessTaskData.stream().filter(task -> task.getTaskType().equals(OpprettRegisterAktitviteterTask.TASKNAME)).collect(Collectors.toList());
        assertThat(opprettTasker.size()).isEqualTo(0);
    }

}
