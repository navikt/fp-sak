package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
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
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
public class AutomatiskGrunnbelopReguleringSVPBatchTjenesteTest {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;

    private AutomatiskGrunnbelopReguleringSVPBatchTjeneste tjeneste;
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
        tjeneste = new AutomatiskGrunnbelopReguleringSVPBatchTjeneste(behandlingRevurderingRepository,
                beregningsresultatRepository, taskTjeneste);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    public void skal_finne_en_sak_å_revurdere() {
        var cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now())
                .getPeriode().getFomDato();
        var gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1))
                .getVerdi();
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        Map<String, String> arguments = new HashMap<>();
        arguments.put(AutomatiskGrunnbelopReguleringBatchArguments.REVURDER_KEY, "True");
        var batchArgs = new AutomatiskGrunnbelopReguleringBatchArguments(
                arguments);
        var svar = tjeneste.launch(batchArgs);
        assertThat(svar).isEqualTo(AutomatiskGrunnbelopReguleringSVPBatchTjeneste.BATCHNAME + "-1");
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var taskTypeExpected = TaskType.forProsessTask(AutomatiskGrunnbelopReguleringTask.class);
        assertThat(captor.getValue().taskType()).isEqualTo(taskTypeExpected);
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering() {
        var cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now())
                .getPeriode().getFomDato();
        var gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1))
                .getVerdi();
        opprettRevurderingsKandidat(BehandlingStatus.UTREDES, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        var svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskGrunnbelopReguleringSVPBatchTjeneste.BATCHNAME + "-0");
        verifyNoInteractions(taskTjeneste);
    }

    @Test
    public void skal_finne_to_saker_å_revurdere_logg_ingen_task() {
        var nySats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now())
                .getVerdi();
        var cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now())
                .getPeriode().getFomDato();
        var gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1))
                .getVerdi();
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.plusDays(5),
                0); // Ikke uttak, bare utsettelse
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats,
                cutoff.minusDays(5)); // FØR
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 4 * gammelSats,
                cutoff.plusDays(5)); // Ikke avkortet
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, nySats, 6 * nySats, cutoff.plusDays(5)); // Ny sats
        var svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskGrunnbelopReguleringSVPBatchTjeneste.BATCHNAME + "-2");
        verifyNoInteractions(taskTjeneste);
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status,
            long sats,
            long avkortet,
            LocalDate uttakFom) {
        return opprettRevurderingsKandidat(status, sats, avkortet, uttakFom, 2300);
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status,
            long sats,
            long avkortet,
            LocalDate uttakFom,
            int dagsatsUtbet) {
        var terminDato = uttakFom.plusWeeks(3);

        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger()
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

        var brFP = BeregningsresultatEntitet.builder().medRegelInput("clob1")
                .medRegelSporing("clob2").build();
        var brFPper = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(uttakFom, uttakFom.plusMonths(3))
                .medBeregningsresultatAndeler(Collections.emptyList()).build(brFP);
        BeregningsresultatAndel.builder().medDagsats(dagsatsUtbet).medDagsatsFraBg(1000).medBrukerErMottaker(true)
                .medStillingsprosent(new BigDecimal(100)).medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medUtbetalingsgrad(new BigDecimal(100)).build(brFPper);
        beregningsresultatRepository.lagre(behandling, brFP);

        return behandlingRepository.hentBehandling(behandling.getId());
    }


}
