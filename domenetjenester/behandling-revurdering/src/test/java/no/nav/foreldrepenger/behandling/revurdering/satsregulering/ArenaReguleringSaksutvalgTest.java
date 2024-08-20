package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.SatsReguleringUtil.lagPeriode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.SatsReguleringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class ArenaReguleringSaksutvalgTest {

    private long gammelSats;
    private final LocalDate arenaDato = LocalDate.of(LocalDate.now().getYear(), 5, 1);;
    private LocalDate cutoff;
    private LocalDate arenaFerdigRegulertDato;
    private GrunnbeløpFinnSakerTask task;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        var satsRepo = new SatsRepository(entityManager);
        cutoff = arenaDato.isAfter(LocalDate.now()) ? arenaDato : LocalDate.now();
        var cutoffsats = satsRepo.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        gammelSats = satsRepo.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoffsats.minusDays(1)).getVerdi();
        arenaFerdigRegulertDato = cutoff.plusWeeks(3).plusDays(2);
        var satsReguleringRepository = new SatsReguleringRepository(entityManager);
        task = new GrunnbeløpFinnSakerTask(satsReguleringRepository, taskTjeneste, satsRepo);
    }

    @Test
    void skal_ikke_finne_saker_til_revurdering(EntityManager em) {
        opprettRevurderingsKandidat(em, BehandlingStatus.UTREDES, cutoff.minusDays(5));
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, arenaDato.minusDays(5));

        task.doTask(SatsReguleringUtil.lagFinnArenaSakerTask(cutoff, arenaFerdigRegulertDato));

        verifyNoInteractions(taskTjeneste);
    }

    @Test
    void skal_finne_tre_saker_til_revurdering(EntityManager em) {
        var kandidat1 = opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2));
        var kandidat2 = opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(2));
        var kandidat3 = opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusMonths(2));
        var kandidat4 = opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, arenaDato.minusDays(5));

        task.doTask(SatsReguleringUtil.lagFinnArenaSakerTask(cutoff, arenaFerdigRegulertDato));

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(3)).lagre(captor.capture());
        var taskTypeExpected = TaskType.forProsessTask(GrunnbeløpReguleringTask.class);
        assertThat(captor.getAllValues()).allMatch(t -> t.taskType().equals(taskTypeExpected));

        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat1)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat2)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat3)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kandidat4)).isEmpty();
    }

    private Behandling opprettRevurderingsKandidat(EntityManager em, BehandlingStatus status, LocalDate uttakFom) {
        var repositoryProvider = new BehandlingRepositoryProvider(em);
        var beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(em);

        var terminDato = uttakFom.plusWeeks(3);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medSøknadDato(terminDato.minusDays(40));

        scenario.medBekreftetHendelse().medFødselsDato(terminDato).medAntallBarn(1);

        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medVedtakstidspunkt(LocalDateTime.now()).build();
        var behandling = scenario.lagre(repositoryProvider);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);

        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny().medGrunnbeløp(BigDecimal.valueOf(gammelSats)).medSkjæringstidspunkt(uttakFom).build();
        BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER).build(beregningsgrunnlag);
        var periode = BeregningsgrunnlagPeriode.ny().medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3)).build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.oppdater(periode).build(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);

        var virksomhetForUttak = SatsReguleringUtil.arbeidsgiver("456");
        var uttakAktivitet = SatsReguleringUtil.lagUttakAktivitet(virksomhetForUttak);
        var uttakResultatPerioder = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioder, uttakAktivitet, uttakFom, uttakFom.plusWeeks(15).minusDays(1), UttakPeriodeType.MØDREKVOTE);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder);

        return repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
    }

}
