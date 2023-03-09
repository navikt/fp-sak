package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static java.time.format.DateTimeFormatter.ofPattern;
import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.AutomatiskArenaReguleringBatchArguments.DATE_PATTERN;
import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.AutomatiskGrunnbelopReguleringBatchTjenesteTest.lagPeriode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(JpaExtension.class)
class AutomatiskArenaReguleringBatchTjenesteTest {

    private BehandlingRepository behandlingRepository;
    private AutomatiskArenaReguleringBatchTjeneste tjeneste;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    private long gammelSats;
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
        var cutoffsats = repositoryProvider.getBeregningsresultatRepository()
            .finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now())
            .getPeriode()
            .getFomDato();
        gammelSats = repositoryProvider.getBeregningsresultatRepository()
            .finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoffsats.minusDays(1))
            .getVerdi();
        var nySatsDato = cutoff.plusWeeks(3).plusDays(2);
        var taskTjenesteMock = mock(ProsessTaskTjeneste.class);
        tjeneste = new AutomatiskArenaReguleringBatchTjeneste(new BehandlingRevurderingRepository(entityManager, behandlingRepository,
            new FagsakRelasjonRepository(entityManager, new YtelsesFordelingRepository(entityManager), new FagsakLåsRepository(entityManager)),
            new SøknadRepository(entityManager, behandlingRepository), new BehandlingLåsRepository(entityManager)), taskTjenesteMock);
        Map<String, String> arguments = new HashMap<>();
        arguments.put(AutomatiskArenaReguleringBatchArguments.REVURDER_KEY, "True");
        arguments.put(AutomatiskArenaReguleringBatchArguments.SATS_DATO_KEY, nySatsDato.format(ofPattern(DATE_PATTERN)));
        batchArgs = new AutomatiskArenaReguleringBatchArguments(arguments);
    }

    @Test
    void skal_ikke_finne_saker_til_revurdering() {
        var revurdering1 = opprettRevurderingsKandidat(BehandlingStatus.UTREDES, cutoff.minusDays(5));
        var revurdering2 = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, arenaDato.minusDays(5));
        assertThat(hentKandidatLik(revurdering1)).isEmpty();
        assertThat(hentKandidatLik(revurdering2)).isEmpty();
    }

    private Optional<Long> hentKandidatLik(Behandling revurdering1) {
        return tjeneste.hentKandidater(batchArgs)
            .stream()
            .map(longAktørIdTuple -> longAktørIdTuple.fagsakId())
            .filter(f -> f.equals(revurdering1.getFagsakId()))
            .findFirst();
    }

    @Test
    void skal_finne_tre_saker_til_revurdering() {
        var kandidat1 = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2));
        var kandidat2 = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusDays(2));
        var kandidat3 = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusMonths(2));
        var kandidat4 = opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, arenaDato.minusDays(5));

        assertThat(hentKandidatLik(kandidat1)).isPresent();
        assertThat(hentKandidatLik(kandidat2)).isPresent();
        assertThat(hentKandidatLik(kandidat3)).isPresent();
        assertThat(hentKandidatLik(kandidat4)).isEmpty();
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status, LocalDate uttakFom) {
        var terminDato = uttakFom.plusWeeks(3);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medSøknadDato(terminDato.minusDays(40));

        scenario.medBekreftetHendelse().medFødselsDato(terminDato).medAntallBarn(1);

        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var behandling = scenario.lagre(repositoryProvider);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny().medGrunnbeløp(BigDecimal.valueOf(gammelSats)).medSkjæringstidspunkt(uttakFom).build();
        BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER).build(beregningsgrunnlag);
        var periode = BeregningsgrunnlagPeriode.ny().medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3)).build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.oppdater(periode).build(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);

        var virksomhetForUttak = AutomatiskGrunnbelopReguleringBatchTjenesteTest.arbeidsgiver("456");
        var uttakAktivitet = AutomatiskGrunnbelopReguleringBatchTjenesteTest.lagUttakAktivitet(virksomhetForUttak);
        var uttakResultatPerioder = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioder, uttakAktivitet, uttakFom, uttakFom.plusWeeks(15).minusDays(1), StønadskontoType.MØDREKVOTE);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakResultatPerioder);

        return behandlingRepository.hentBehandling(behandling.getId());
    }

}
