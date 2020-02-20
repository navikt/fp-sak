package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.task;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp.BeregningsgrunnlagInputTjeneste;
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
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.abakus.AbakusInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjenesteImpl;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class OpprettRegisterAktitviteterTaskTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now().minusDays(5);
    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private OpprettRegisterAktitviteterTask taskTjeneste = new OpprettRegisterAktitviteterTask(behandlingRepository, beregningsgrunnlagRepository, null);

    @Test
    public void skal_opprette_registeraktiviter_på_grunnlag_som_har_register_satt_på_aktivt_grunnlag() {
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        BeregningsgrunnlagGrunnlagBuilder grunnlagUtenAktiviteter = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty());
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagUtenAktiviteter, BeregningsgrunnlagTilstand.OPPRETTET);
        BeregningAktivitetAggregatEntitet registerAktiviteter = BeregningAktivitetAggregatEntitet.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
            .leggTilAktivitet(BeregningAktivitetEntitet.builder()
                .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(10)))
                .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING).build()).build();
        BeregningsgrunnlagGrunnlagBuilder grunnlagMedAktiviteter = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(registerAktiviteter);
        beregningsgrunnlagRepository.lagre(behandling.getId(), grunnlagMedAktiviteter, BeregningsgrunnlagTilstand.FASTSATT);

        ProsessTaskData prosessTaskData1 = new ProsessTaskData(OpprettRegisterAktitviteterTask.TASKNAME);
        prosessTaskData1.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        // Act
        taskTjeneste.doTask(prosessTaskData1);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> beregningsgrunnlagGrunnlagEntitet = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(behandling.getId(), BeregningsgrunnlagTilstand.OPPRETTET);
        assertThat(beregningsgrunnlagGrunnlagEntitet).isPresent();
        assertThat(beregningsgrunnlagGrunnlagEntitet.get().getRegisterAktiviteter()).isEqualTo(registerAktiviteter);
        assertThat(beregningsgrunnlagGrunnlagEntitet.get().erAktivt()).isEqualTo(false);
    }

}
