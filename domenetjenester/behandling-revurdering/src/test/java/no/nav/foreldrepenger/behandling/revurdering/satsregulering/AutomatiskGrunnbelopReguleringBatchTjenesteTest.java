package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Periode;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
public class AutomatiskGrunnbelopReguleringBatchTjenesteTest {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;

    private AutomatiskGrunnbelopReguleringBatchTjeneste tjeneste;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        var ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        var fagsakLåsRepository = new FagsakLåsRepository(entityManager);
        var fagsakRelasjonRepository = new FagsakRelasjonRepository(entityManager, ytelsesFordelingRepository,
                fagsakLåsRepository);
        var søknadRepository = new SøknadRepository(entityManager, behandlingRepository);
        var behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        var behandlingRevurderingRepository = new BehandlingRevurderingRepository(
                entityManager, behandlingRepository, fagsakRelasjonRepository, søknadRepository, behandlingLåsRepository);
        tjeneste = new AutomatiskGrunnbelopReguleringBatchTjeneste(behandlingRevurderingRepository,
                beregningsresultatRepository, taskTjeneste);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    public void skal_finne_en_sak_å_revurdere() {
        var cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1)).getVerdi();
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        Map<String, String> arguments = new HashMap<>();
        arguments.put(AutomatiskGrunnbelopReguleringBatchArguments.REVURDER_KEY, "True");
        var batchArgs = new AutomatiskGrunnbelopReguleringBatchArguments(arguments);

        var svar = tjeneste.launch(batchArgs);

        assertThat(svar).isEqualTo(AutomatiskGrunnbelopReguleringBatchTjeneste.BATCHNAME + "-1");
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var taskTypeExpected = TaskType.forProsessTask(AutomatiskGrunnbelopReguleringTask.class);
        assertThat(captor.getValue().taskType()).isEqualTo(taskTypeExpected);
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering() {
        var cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1)).getVerdi();
        opprettRevurderingsKandidat(BehandlingStatus.UTREDES, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        var svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskGrunnbelopReguleringBatchTjeneste.BATCHNAME + "-0");
        verifyNoInteractions(taskTjeneste);
    }

    @Test
    public void skal_finne_to_saker_å_revurdere_logg_ikke_task() {
        var nySats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getVerdi();
        var cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        var gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1)).getVerdi();
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.minusDays(5)); // FØR
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 4 * gammelSats, cutoff.plusDays(5)); // Ikke avkortet
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, nySats, 6 * nySats, cutoff.plusDays(5)); // Ny sats
        var svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskGrunnbelopReguleringBatchTjeneste.BATCHNAME + "-2");
        verifyNoInteractions(taskTjeneste);
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status,
            long sats,
            long avkortet,
            LocalDate uttakFom) {
        var terminDato = uttakFom.plusWeeks(3);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medSøknadDato(terminDato.minusDays(40));

        scenario.medBekreftetHendelse().medFødselsDato(LocalDate.now()).medAntallBarn(1);

        scenario.medBehandlingsresultat(
                Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var behandling = scenario.lagre(repositoryProvider);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(terminDato.minusWeeks(3L)).medGrunnbeløp(BigDecimal.valueOf(sats)).build();
        BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .build(beregningsgrunnlag);
        var periode = BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3)).medBruttoPrÅr(BigDecimal.valueOf(avkortet))
                .medAvkortetPrÅr(BigDecimal.valueOf(avkortet)).build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.oppdater(periode).build(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);

        var virksomhetForUttak = arbeidsgiver("456");
        var uttakAktivitet = lagUttakAktivitet(virksomhetForUttak);
        var uttakResultatPerioder = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioder, uttakAktivitet, uttakFom,
            uttakFom.plusWeeks(15).minusDays(1), StønadskontoType.MØDREKVOTE);

        repositoryProvider.getFpUttakRepository()
            .lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder);

        return behandlingRepository.hentBehandling(behandling.getId());
    }

    static Arbeidsgiver arbeidsgiver(String arbeidsgiverIdentifikator) {
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator);
    }

    static UttakAktivitetEntitet lagUttakAktivitet(Arbeidsgiver arbeidsgiver) {
        return new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
    }

    static void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            UttakAktivitetEntitet uttakAktivitet,
                            LocalDate fom, LocalDate tom,
                            StønadskontoType stønadskontoType) {
        lagPeriode(uttakResultatPerioder, fom, tom, stønadskontoType, uttakAktivitet);
    }

    static void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            LocalDate fom,
                            LocalDate tom,
                            StønadskontoType stønadskontoType,
                            UttakAktivitetEntitet uttakAktivitetEntitet) {

        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medSamtidigUttak(false)
            .medFlerbarnsdager(false)
            .build();
        uttakResultatPerioder.leggTilPeriode(periode);

        var trekkdager = new Trekkdager(TrekkdagerUtregningUtil.trekkdagerFor(new Periode(periode.getFom(), periode.getTom()),
                false, BigDecimal.ZERO, null).decimalValue());

        var aktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitetEntitet)
            .medTrekkdager(trekkdager)
            .medTrekkonto(stønadskontoType)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        periode.leggTilAktivitet(aktivitet);
    }

}
