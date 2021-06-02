package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.AutomatiskGrunnbelopReguleringBatchTjenesteTest.lagPeriode;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@CdiDbAwareTest
public class AutomatiskMilSivReguleringBatchTjenesteTest {

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

    @BeforeEach
    public void setUp() throws Exception {
        nySats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getVerdi();
        cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now())
                .getPeriode()
                .getFomDato();
        gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1))
                .getVerdi();
        tjeneste = new AutomatiskMilSivReguleringBatchTjeneste(behandlingRevurderingRepository,
                beregningsresultatRepository, prosessTaskRepository);
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering(EntityManager em) {
        opprettRevurderingsKandidat(em, BehandlingStatus.UTREDES, cutoff.plusDays(5), gammelSats,
                gammelSats * 3); // Har åpen behandling
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(5), gammelSats,
                gammelSats * 3); // Uttak før "1/5"
        var svar = tjeneste.launch(null);
        assertThat(svar).isEqualTo(AutomatiskMilSivReguleringBatchTjeneste.BATCHNAME + "-0");
    }

    @Test
    public void skal_finne_to_saker_til_revurdering(EntityManager em) {
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
        assertThat(svar).isEqualTo(AutomatiskMilSivReguleringBatchTjeneste.BATCHNAME + "-2");
    }

    private Behandling opprettRevurderingsKandidat(EntityManager em, BehandlingStatus status,
            LocalDate uttakFom,
            long sats,
            long brutto) {
        var terminDato = uttakFom.plusWeeks(3);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
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

        var virksomhetForUttak = AutomatiskGrunnbelopReguleringBatchTjenesteTest.arbeidsgiver("456");
        var uttakAktivitet = AutomatiskGrunnbelopReguleringBatchTjenesteTest.lagUttakAktivitet(virksomhetForUttak);
        var uttakResultatPerioder = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioder, uttakAktivitet, uttakFom,
            uttakFom.plusWeeks(15).minusDays(1), StønadskontoType.MØDREKVOTE);

        repositoryProvider.getFpUttakRepository()
            .lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder);

        em.flush();
        em.clear();
        return em.find(Behandling.class, behandling.getId());
    }

}
