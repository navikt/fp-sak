package no.nav.foreldrepenger.behandling.revurdering.satsregulering;


import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.AutomatiskGrunnbelopReguleringTask.TASKTYPE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AutomatiskGrunnbelopReguleringBatchTjenesteTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    @Inject
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    private BehandlingRevurderingRepository behandlingRevurderingRepository;

    private AutomatiskGrunnbelopReguleringBatchTjeneste tjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private long gammelSats;
    private long nySats;
    private LocalDate cutoff;


    @Before
    public void setUp() throws Exception {
        nySats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getVerdi();
        cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1)).getVerdi();
        tjeneste = new AutomatiskGrunnbelopReguleringBatchTjeneste(behandlingRevurderingRepository, beregningsresultatRepository, prosessTaskRepository);
    }

    @Test
    public void skal_finne_en_sak_å_revurdere() {
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6*gammelSats, cutoff.plusDays(5));
        Map<String, String> arguments = new HashMap<>();
        arguments.put(AutomatiskGrunnbelopReguleringBatchArguments.REVURDER_KEY, "True");
        AutomatiskGrunnbelopReguleringBatchArguments batchArgs = new AutomatiskGrunnbelopReguleringBatchArguments(arguments);
        String svar = tjeneste.launch(batchArgs);
        assertThat(svar).isEqualTo(AutomatiskGrunnbelopReguleringBatchTjeneste.BATCHNAME+"-1");
        assertThat(prosessTaskRepository.finnIkkeStartet().stream().anyMatch(task -> task.getTaskType().equals(TASKTYPE))).isTrue();
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering() {
        opprettRevurderingsKandidat(BehandlingStatus.UTREDES, gammelSats, 6*gammelSats, cutoff.plusDays(5));
        String svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskGrunnbelopReguleringBatchTjeneste.BATCHNAME+"-0");
    }

    @Test
    public void skal_finne_to_saker_å_revurdere() {
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6*gammelSats, cutoff.plusDays(5));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6*gammelSats, cutoff.plusDays(5), 0); // Ikke uttak, bare utsettelse
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6*gammelSats, cutoff.minusDays(5));  // FØR
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6*gammelSats, cutoff.plusDays(5));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 4*gammelSats, cutoff.plusDays(5)); // Ikke avkortet
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, nySats, 6*nySats, cutoff.plusDays(5)); // Ny sats
        String svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskGrunnbelopReguleringBatchTjeneste.BATCHNAME+"-2");
        assertThat(prosessTaskRepository.finnIkkeStartet().stream().anyMatch(task -> task.getTaskType().equals(TASKTYPE))).isFalse();
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status, long sats, long avkortet, LocalDate uttakFom) {
        return opprettRevurderingsKandidat(status, sats, avkortet, uttakFom, 2300);
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status, long sats, long avkortet, LocalDate uttakFom, int dagsatsUtbet) {
        LocalDate terminDato = uttakFom.plusWeeks(3);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medSøknadDato(terminDato.minusDays(40));

        scenario.medBekreftetHendelse()
            .medFødselsDato(LocalDate.now())
            .medAntallBarn(1);

        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        Behandling behandling = scenario.lagre(repositoryProvider);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(terminDato.minusWeeks(3L))
            .medGrunnbeløp(BigDecimal.valueOf(sats))
            .build();
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3))
            .medBruttoPrÅr(BigDecimal.valueOf(avkortet))
            .medAvkortetPrÅr(BigDecimal.valueOf(avkortet))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.builder(periode)
            .build(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);

        BeregningsresultatEntitet brFP = BeregningsresultatEntitet.builder()
                .medRegelInput("clob1")
                .medRegelSporing("clob2")
                .build();
        BeregningsresultatPeriode brFPper = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(uttakFom, uttakFom.plusMonths(3))
            .medBeregningsresultatAndeler(Collections.emptyList())
            .build(brFP);
        BeregningsresultatAndel.builder()
            .medDagsats(dagsatsUtbet)
            .medDagsatsFraBg(1000)
            .medBrukerErMottaker(true)
            .medStillingsprosent(new BigDecimal(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medUtbetalingsgrad(new BigDecimal(100))
            .build(brFPper);
        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, brFP);
        repoRule.getRepository().flushAndClear();
        return repoRule.getEntityManager().find(Behandling.class, behandling.getId());
    }

}
