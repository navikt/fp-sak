package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static java.time.format.DateTimeFormatter.ofPattern;
import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.AutomatiskArenaReguleringBatchArguments.DATE_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AutomatiskArenaReguleringBatchTjenesteTest {

    private BehandlingRepository behandlingRepository;
    private AutomatiskArenaReguleringBatchTjeneste tjeneste;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    private LocalDate arenaDato;
    private LocalDate cutoff;
    private AutomatiskArenaReguleringBatchArguments batchArgs;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        beregningsgrunnlagRepository = new BeregningsgrunnlagRepository(entityManager);

        arenaDato = AutomatiskArenaReguleringBatchArguments.DATO;
        cutoff = arenaDato.isAfter(LocalDate.now()) ? arenaDato : LocalDate.now();
        var nySatsDato = cutoff.plusWeeks(3).plusDays(2);
        var prosessTaskRepositoryMock = mock(ProsessTaskRepository.class);
        tjeneste = new AutomatiskArenaReguleringBatchTjeneste(repositoryProvider,
                prosessTaskRepositoryMock);
        Map<String, String> arguments = new HashMap<>();
        arguments.put(AutomatiskArenaReguleringBatchArguments.REVURDER_KEY, "True");
        arguments.put(AutomatiskArenaReguleringBatchArguments.SATS_DATO_KEY,
                nySatsDato.format(ofPattern(DATE_PATTERN)));
        batchArgs = new AutomatiskArenaReguleringBatchArguments(arguments);
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering() {
        var revurdering1 = opprettRevurderingsKandidat(BehandlingStatus.UTREDES, cutoff.minusDays(5));
        var revurdering2 = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, arenaDato.minusDays(5));
        var kandidater = tjeneste.hentKandidater(batchArgs)
                .stream()
                .map(longAktørIdTuple -> longAktørIdTuple.getElement1())
                .collect(Collectors.toSet());
        assertThat(kandidater).doesNotContain(revurdering1.getFagsakId(), revurdering2.getFagsakId());
    }

    @Test
    public void skal_finne_tre_saker_til_revurdering() {
        var kandidat1 = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2));
        var kandidat2 = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusDays(2));
        var kandidat3 = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusMonths(2));
        var kandidat4 = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, arenaDato.minusDays(5));
        var kandidater = tjeneste.hentKandidater(batchArgs).stream()
                .map(fagsakAktørIdTuple -> fagsakAktørIdTuple.getElement1())
                .collect(Collectors.toSet());
        assertThat(kandidater).contains(kandidat1.getFagsakId(), kandidat2.getFagsakId(), kandidat3.getFagsakId());
        assertThat(kandidater).doesNotContain(kandidat4.getFagsakId());
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status, LocalDate uttakFom) {
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
                .medSkjæringstidspunkt(uttakFom)
                .build();
        BeregningsgrunnlagAktivitetStatus.builder()
                .medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)
                .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3))
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
                .medDagsats(1000)
                .medDagsatsFraBg(1000)
                .medBrukerErMottaker(true)
                .medStillingsprosent(new BigDecimal(100))
                .medInntektskategori(Inntektskategori.ARBEIDSAVKLARINGSPENGER)
                .medUtbetalingsgrad(new BigDecimal(100))
                .build(brFPper);
        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, brFP);
        return behandlingRepository.hentBehandling(behandling.getId());
    }

}
