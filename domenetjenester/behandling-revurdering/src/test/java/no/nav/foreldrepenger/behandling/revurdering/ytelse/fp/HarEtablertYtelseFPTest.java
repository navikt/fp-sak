package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagUttakResultatPlanTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@CdiDbAwareTest
public class HarEtablertYtelseFPTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private UttakInputTjeneste uttakInputTjeneste;
    @Inject
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    @Mock
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;

    private HarEtablertYtelseFP harEtablertYtelse;

    @BeforeEach
    void setUp() {
        harEtablertYtelse = new HarEtablertYtelseFP(stønadskontoSaldoTjeneste, uttakInputTjeneste,
                relatertBehandlingTjeneste,
                new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()),
                repositoryProvider.getBehandlingVedtakRepository());
    }

    private Behandling opprettBehandling() {
        var fødselsdato = LocalDate.of(2017, 10, 10);
        var førstegangsScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100())
                .medDefaultFordeling(fødselsdato);
        førstegangsScenario.medSøknadHendelse().medFødselsDato(fødselsdato);
        var originalBehandling = førstegangsScenario
                .lagre(repositoryProvider);
        var scenarioRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var behandling = scenarioRevurdering.lagre(repositoryProvider);
        new BeregningRevurderingTestUtil(repositoryProvider).avsluttBehandling(behandling);
        return behandling;
    }

    @Test
    public void skal_gi_etablert_ytelse_med_tom_for_innvilget_periode_etter_dagens_dato() {
        // Arrange
        var dagensDato = LocalDate.now();

        var behandling = opprettBehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandling,
                List.of(new LocalDateInterval(dagensDato.minusDays(10), dagensDato.plusDays(10))),
                List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true),
                List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var etablertYtelse = vurder(uttakResultatOriginal, true, Optional.empty(), false, behandling);

        // Assert
        assertThat(etablertYtelse).isTrue();
    }

    private boolean vurder(UttakResultatEntitet uttakResultatOriginal,
            boolean finnesInnvilgetIkkeOpphørtVedtak,
            Optional<UttakResultatEntitet> uttakAnnenpart,
            boolean sluttPåTrekkdager,
            Behandling behandling) {
        lenient().when(stønadskontoSaldoTjeneste.erSluttPåStønadsdager(any())).thenReturn(sluttPåTrekkdager);
        uttakAnnenpart.ifPresent(uttak -> {
            var scenarioAnnenpart = ScenarioFarSøkerForeldrepenger.forFødsel();
            scenarioAnnenpart.medBehandlingVedtak()
                    .medVedtakResultatType(VedtakResultatType.INNVILGET)
                    .medVedtakstidspunkt(LocalDateTime.now())
                    .medAnsvarligSaksbehandler(" ");
            var behandlingAnnenpart = scenarioAnnenpart
                    .lagre(repositoryProvider);
            repositoryProvider.getFagsakRepository()
                    .oppdaterFagsakStatus(behandlingAnnenpart.getFagsakId(), FagsakStatus.LØPENDE);
            var originalBehandling = finnOriginalBehandling(behandling);
            repositoryProvider.getFagsakRelasjonRepository()
                    .kobleFagsaker(originalBehandling.getFagsak(), behandlingAnnenpart.getFagsak(), originalBehandling);
            repositoryProvider.getFpUttakRepository()
                    .lagreOpprinneligUttakResultatPerioder(behandlingAnnenpart.getId(),
                            uttakAnnenpart.get().getOpprinneligPerioder());
            behandlingAnnenpart.avsluttBehandling();
            repositoryProvider.getBehandlingRepository()
                    .lagre(behandlingAnnenpart,
                            repositoryProvider.getBehandlingLåsRepository().taLås(behandlingAnnenpart.getId()));
        });

        var behandlingsresultat = uttakResultatOriginal.getBehandlingsresultat();
        var behandlingVedtak = repositoryProvider.getBehandlingVedtakRepository()
                .hentForBehandlingHvisEksisterer(behandlingsresultat.getBehandlingId());
        return harEtablertYtelse.vurder(behandling, finnesInnvilgetIkkeOpphørtVedtak,
                new UttakResultatHolderFP(Optional.of(ForeldrepengerUttakTjeneste.map(uttakResultatOriginal)),
                        behandlingVedtak.orElse(null)));
    }

    private Behandling finnOriginalBehandling(Behandling behandling) {
        return repositoryProvider.getBehandlingRepository().hentBehandling(
                behandling.getOriginalBehandlingId().orElseThrow());
    }

    @Test
    public void skal_ikke_gi_etablert_ytelse_hvis_finnesInnvilgetIkkeOpphørtVedtak_er_false() {
        // Arrange
        var dagensDato = LocalDate.now();

        var behandling = opprettBehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandling,
                List.of(new LocalDateInterval(dagensDato.minusDays(10), dagensDato.plusDays(10))),
                List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true),
                List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));
        var finnesInnvilgetIkkeOpphørtVedtak = false;
        // Act
        var etablertYtelse = vurder(uttakResultatOriginal, finnesInnvilgetIkkeOpphørtVedtak, Optional.empty(),
                false, behandling);

        // Assert
        assertThat(etablertYtelse).isFalse();
    }

    @Test
    public void skal_gi_etablert_ytelse_med_tom_for_innvilget_periode_på_dagens_dato() {
        // Arrange
        var dagensDato = LocalDate.now();

        var behandling = opprettBehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandling,
                List.of(new LocalDateInterval(dagensDato.minusDays(10), dagensDato)),
                List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true),
                List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));
        // Act
        var etablertYtelse = vurder(uttakResultatOriginal, true, Optional.empty(), false, behandling);

        // Assert
        assertThat(etablertYtelse).isTrue();
    }

    @Test
    public void skal_ikkje_gi_etablert_ytelse_med_tom_for_avslått_periode_etter_dagens_dato() {
        // Arrange
        var dagensDato = LocalDate.now();

        var behandling = opprettBehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandling,
                List.of(new LocalDateInterval(dagensDato.minusDays(10), dagensDato.plusDays(5))),
                List.of(false), List.of(PeriodeResultatType.AVSLÅTT), List.of(PeriodeResultatÅrsak.UKJENT),
                List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
                List.of(StønadskontoType.FORELDREPENGER));
        // Act
        var etablertYtelse = vurder(uttakResultatOriginal, true, Optional.empty(), true, behandling);

        // Assert
        assertThat(etablertYtelse).isFalse();
    }

    @Test
    public void skal_ikkje_gi_etablert_ytelse_med_tom_for_innvilget_periode_før_dagens_dato() {
        // Arrange
        var dagensDato = LocalDate.now();

        var behandling = opprettBehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandling,
                List.of(new LocalDateInterval(dagensDato.minusDays(10), dagensDato.minusDays(5))),
                List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT),
                List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
                List.of(StønadskontoType.FORELDREPENGER));
        // Act
        var etablertYtelse = vurder(uttakResultatOriginal, true, Optional.empty(), true, behandling);

        // Assert
        assertThat(etablertYtelse).isFalse();
    }

    @Test
    public void skal_gi_etablert_ytelse_selv_med_tom_for_innvilget_periode_før_dagens_dato_så_lenge_saldo_ikke_er_0() {
        // Arrange
        var dagensDato = LocalDate.now();

        var behandling = opprettBehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandling,
                List.of(new LocalDateInterval(dagensDato.minusDays(10), dagensDato.minusDays(5))),
                List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT),
                List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
                List.of(StønadskontoType.FORELDREPENGER));
        // Act
        var etablertYtelse = vurder(uttakResultatOriginal, true, Optional.empty(), false, behandling);

        // Assert
        assertThat(etablertYtelse).isTrue();
    }

    @Test
    public void skal_gi_etablert_ytelse_selvom_tom_for_innvilget_periode_er_før_dagens_dato_hvis_annen_part_har_etter() {
        // Arrange
        var dagensDato = LocalDate.now();

        var behandling = opprettBehandling();
        var uttakResultatOriginal = lagUttakResultatPlanForBehandling(behandling,
                List.of(new LocalDateInterval(dagensDato.minusDays(10), dagensDato.minusDays(5))),
                List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true),
                List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        var uttakResultatAnnenPart = lagUttakResultatPlanForBehandling(behandling,
                List.of(new LocalDateInterval(dagensDato.minusDays(4), dagensDato.plusDays(10))),
                List.of(false), List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true),
                List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Act
        var etablertYtelse = vurder(uttakResultatOriginal, true, Optional.of(uttakResultatAnnenPart), true,
                behandling);

        // Assert
        assertThat(etablertYtelse).isTrue();
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
            List<LocalDateInterval> perioder,
            List<Boolean> samtidigUttak,
            List<PeriodeResultatType> periodeResultatTyper,
            List<PeriodeResultatÅrsak> periodeResultatÅrsak,
            List<Boolean> graderingInnvilget,
            List<Integer> andelIArbeid,
            List<Integer> utbetalingsgrad,
            List<Trekkdager> trekkdager,
            List<StønadskontoType> stønadskontoTyper) {
        var uttakresultat = LagUttakResultatPlanTjeneste.lagUttakResultatPlanTjeneste(behandling,
                perioder, samtidigUttak, periodeResultatTyper, periodeResultatÅrsak, graderingInnvilget, andelIArbeid,
                utbetalingsgrad, trekkdager, stønadskontoTyper);
        repositoryProvider.getFpUttakRepository()
                .lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakresultat.getGjeldendePerioder());
        return uttakresultat;

    }
}
