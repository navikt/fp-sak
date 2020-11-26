package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@CdiDbAwareTest
public class AutomatiskNæringsdrivendeReguleringBatchTjenesteTest {

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

    private AutomatiskNæringsdrivendeReguleringBatchTjeneste tjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private long gammelSats;
    private long nySats;
    private LocalDate cutoff;

    @BeforeEach
    public void setUp() throws Exception {
        nySats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getVerdi();
        cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now())
                .getPeriode()
                .getFomDato();
        gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1))
                .getVerdi();
        tjeneste = new AutomatiskNæringsdrivendeReguleringBatchTjeneste(behandlingRevurderingRepository,
                beregningsresultatRepository, prosessTaskRepository);
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering(EntityManager em) {
        opprettRevurderingsKandidat(em, BehandlingStatus.UTREDES, cutoff.plusDays(5), gammelSats,
                gammelSats); // Har åpen behandling
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(5), gammelSats,
                gammelSats * 4); // Uttak før "1/5"
        String svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskNæringsdrivendeReguleringBatchTjeneste.BATCHNAME + "-0");
    }

    @Test
    public void skal_finne_tre_saker_til_revurdering(EntityManager em) {
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2), gammelSats,
                gammelSats); // Skal plukke
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(2), gammelSats,
                gammelSats * 8); // Skal plukke
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(2), gammelSats,
                gammelSats * 4); // Skal plukke
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(2), gammelSats,
                gammelSats * 2); // Uttak før "1/5"
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2), nySats,
                gammelSats * 7); // Har allerede ny G
        String svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskNæringsdrivendeReguleringBatchTjeneste.BATCHNAME + "-3");
    }

    private Behandling opprettRevurderingsKandidat(EntityManager em, BehandlingStatus status,
            LocalDate uttakFom,
            long sats,
            long brutto) {
        LocalDate terminDato = uttakFom.plusWeeks(3);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medSøknadDato(terminDato.minusDays(40));

        scenario.medBekreftetHendelse()
                .medFødselsDato(terminDato)
                .medAntallBarn(1);

        scenario.medBehandlingsresultat(
                Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        Behandling behandling = scenario.lagre(repositoryProvider);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medGrunnbeløp(BigDecimal.valueOf(sats))
                .medSkjæringstidspunkt(uttakFom)
                .build();
        BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3))
                .medBruttoPrÅr(BigDecimal.valueOf(brutto))
                .medAvkortetPrÅr(BigDecimal.valueOf(brutto))
                .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.oppdater(periode)
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
                .medDagsats(2300)
                .medDagsatsFraBg(1000)
                .medBrukerErMottaker(true)
                .medStillingsprosent(new BigDecimal(100))
                .medInntektskategori(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medUtbetalingsgrad(new BigDecimal(100))
                .build(brFPper);

        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, brFP);
        em.flush();
        em.clear();
        return em.find(Behandling.class, behandling.getId());
    }

}
