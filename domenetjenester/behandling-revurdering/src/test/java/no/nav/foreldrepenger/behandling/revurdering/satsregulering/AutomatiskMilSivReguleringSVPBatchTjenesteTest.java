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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@CdiDbAwareTest
class AutomatiskMilSivReguleringSVPBatchTjenesteTest {

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    @Inject
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    private BehandlingRevurderingRepository behandlingRevurderingRepository;

    private AutomatiskMilSivReguleringSVPBatchTjeneste tjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private long gammelSats;
    private long nySats;
    private LocalDate cutoff;

    @BeforeEach
    public void setUp() {
        nySats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getVerdi();
        cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now())
                .getPeriode()
                .getFomDato();
        gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1))
                .getVerdi();
        tjeneste = new AutomatiskMilSivReguleringSVPBatchTjeneste(behandlingRevurderingRepository,
                beregningsresultatRepository, taskTjeneste);
    }

    @Test
    void skal_ikke_finne_saker_til_revurdering(EntityManager em) {
        opprettRevurderingsKandidat(em, BehandlingStatus.UTREDES, cutoff.plusDays(5), gammelSats,
                gammelSats * 3); // Har åpen behandling
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(5), gammelSats,
                gammelSats * 3); // Uttak før "1/5"
        var svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskMilSivReguleringSVPBatchTjeneste.BATCHNAME + "-0");
    }

    @Test
    void skal_finne_to_saker_til_revurdering(EntityManager em) {
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2), gammelSats,
                gammelSats * 3); // Skal finnes
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(2), gammelSats,
                gammelSats * 2); // Skal finnes
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(2), gammelSats,
                gammelSats * 4); // Over streken på 3G
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(2), gammelSats,
                gammelSats * 2); // Uttak før "1/5"
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2), nySats,
                gammelSats * 2); // Har allerede ny G
        var svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskMilSivReguleringSVPBatchTjeneste.BATCHNAME + "-2");
    }

    private Behandling opprettRevurderingsKandidat(EntityManager em, BehandlingStatus status,
            LocalDate uttakFom,
            long sats,
            long brutto) {
        var terminDato = uttakFom.plusWeeks(3);

        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger()
                .medSøknadDato(terminDato.minusDays(40));

        scenario.medBekreftetHendelse()
                .medFødselsDato(terminDato)
                .medAntallBarn(1);

        scenario.medBehandlingsresultat(
                Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var behandling = scenario.lagre(repositoryProvider);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medGrunnbeløp(BigDecimal.valueOf(sats))
                .medSkjæringstidspunkt(uttakFom)
                .build();
        BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(AktivitetStatus.MILITÆR_ELLER_SIVIL)
                .build(beregningsgrunnlag);
        var periode = BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3))
                .medBruttoPrÅr(BigDecimal.valueOf(brutto))
                .medAvkortetPrÅr(BigDecimal.valueOf(brutto))
                .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.oppdater(periode)
                .build(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);

        var brFP = BeregningsresultatEntitet.builder()
                .medRegelInput("clob1")
                .medRegelSporing("clob2")
                .build();
        var brFPper = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(uttakFom, uttakFom.plusMonths(3))
                .medBeregningsresultatAndeler(Collections.emptyList())
                .build(brFP);
        BeregningsresultatAndel.builder()
                .medDagsats(500)
                .medDagsatsFraBg(500)
                .medBrukerErMottaker(true)
                .medStillingsprosent(new BigDecimal(100))
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medUtbetalingsgrad(new BigDecimal(100))
                .build(brFPper);
        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, brFP);
        em.flush();
        em.clear();
        return em.find(Behandling.class, behandling.getId());
    }

}
