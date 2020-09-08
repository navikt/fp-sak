package no.nav.foreldrepenger.behandling.revurdering.satsregulering;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

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
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
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
public class AutomatiskMilSivReguleringBatchTjenesteTest {

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

    private AutomatiskMilSivReguleringBatchTjeneste tjeneste;

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
        tjeneste = new AutomatiskMilSivReguleringBatchTjeneste(behandlingRevurderingRepository, beregningsresultatRepository, prosessTaskRepository);
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering() {
        opprettRevurderingsKandidat(BehandlingStatus.UTREDES, cutoff.plusDays(5), gammelSats, gammelSats * 3); // Har åpen behandling
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.minusDays(5), gammelSats, gammelSats * 3); // Uttak før "1/5"
        String svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskMilSivReguleringBatchTjeneste.BATCHNAME+"-0");
    }

    @Test
    public void skal_finne_to_saker_til_revurdering() {
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2), gammelSats, gammelSats * 3); // Skal finnes
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusDays(2), gammelSats, gammelSats * 2);  // Skal finnes
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusDays(2), gammelSats, gammelSats * 4); // Over streken på 3G
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.minusDays(2), gammelSats, gammelSats * 2); // Uttak før "1/5"
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2), nySats, gammelSats * 2); // Har allerede ny G
        String svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskMilSivReguleringBatchTjeneste.BATCHNAME+"-2");
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status, LocalDate uttakFom, long sats, long brutto) {
        LocalDate terminDato = uttakFom.plusWeeks(3);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medSøknadDato(terminDato.minusDays(40));

        scenario.medBekreftetHendelse()
            .medFødselsDato(terminDato)
            .medAntallBarn(1);

        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        Behandling behandling = scenario.lagre(repositoryProvider);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(BigDecimal.valueOf(sats))
            .medSkjæringstidspunkt(uttakFom)
            .build();
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.MILITÆR_ELLER_SIVIL)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3))
            .medBruttoPrÅr(BigDecimal.valueOf(brutto))
            .medAvkortetPrÅr(BigDecimal.valueOf(brutto))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.builder(periode)
            .build(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);

        BeregningsresultatEntitet brFP = BeregningsresultatEntitet.builder()
                .medRegelInput("clob1")
                .medRegelSporing("clob2")
                .build();
        BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(uttakFom, uttakFom.plusMonths(3))
            .medBeregningsresultatAndeler(Collections.emptyList())
            .build(brFP);
        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, brFP);
        repoRule.getRepository().flushAndClear();
        return repoRule.getEntityManager().find(Behandling.class, behandling.getId());
    }

}
