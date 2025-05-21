package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.Etterkontroll;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class AutomatiskEtterkontrollBatchTjenesteTest {

    private static final int REVURDERING_DAGER_TILBAKE = 60;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private EtterkontrollRepository etterkontrollRepository;
    private BehandlingRepositoryProvider repositoryProvider;
    private AutomatiskEtterkontrollBatchTjeneste tjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        etterkontrollRepository = new EtterkontrollRepository(entityManager);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        tjeneste = new AutomatiskEtterkontrollBatchTjeneste(taskTjeneste, etterkontrollRepository, behandlingRepository);
    }

    @Test
    void skal_finne_kandidat_til_revurdering() {
        var behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE + 2);

        var etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false)
                .medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(REVURDERING_DAGER_TILBAKE))
                .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
                .build();
        etterkontrollRepository.lagre(etterkontroll);

        tjeneste.launch(new Properties());

        assertOpprettet(behandling, 1);
    }

    @Test
    void sjekk_at_behandlingen_blir_etterkontrollert_og_ikke_klagen() {
        var behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE + 2);
        var klage = Behandling.forKlage(behandling.getFagsak()).build();
        behandlingRepository.lagre(klage, behandlingRepository.taSkriveLås(klage));

        var etterkontroll = new Etterkontroll.Builder(klage.getFagsakId()).medErBehandlet(false)
                .medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(REVURDERING_DAGER_TILBAKE))
                .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
                .build();
        etterkontrollRepository.lagre(etterkontroll);

        tjeneste.launch(new Properties());

        assertOpprettet(behandling, 1);
        assertOpprettet(klage, 0);
    }

    @Test
    void behandling_som_har_vært_etterkontrollert_skal_ikke_være_kandidat_til_revurdering() {
        var behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE + 2);

        var etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false)
                .medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(REVURDERING_DAGER_TILBAKE))
                .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
                .build();
        etterkontrollRepository.lagre(etterkontroll);
        etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);

        tjeneste.launch(new Properties());

        assertIngenKandidater();
    }

    @Test
    void skal_ikke_velge_henlagt_behandling() {
        var behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE);

        var innvilget = new Behandlingsresultat.Builder().medBehandlingResultatType(
                BehandlingResultatType.INNVILGET).buildFor(behandling);
        behandling.setBehandlingresultat(innvilget);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false)
                .medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(REVURDERING_DAGER_TILBAKE))
                .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
                .build();
        etterkontrollRepository.lagre(etterkontroll);

        var henlagtBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.FØRSTEGANGSSØKNAD)
                .build();
        var henlagt = new Behandlingsresultat.Builder().medBehandlingResultatType(
                BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET).buildFor(henlagtBehandling);
        henlagtBehandling.setBehandlingresultat(henlagt);
        henlagtBehandling.avsluttBehandling();
        behandlingRepository.lagre(henlagtBehandling, behandlingRepository.taSkriveLås(henlagtBehandling));

        tjeneste.launch(new Properties());

        assertOpprettet(behandling, 1);
    }

    @Test
    void fagsak_som_har_eksisterende_etterkontrollsbehandling_skal_ikke_være_kandidat_til_revurdering() {
        var behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE + 2);

        var etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false)
                .medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(REVURDERING_DAGER_TILBAKE))
                .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
                .build();
        etterkontrollRepository.lagre(etterkontroll);

        var revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN))
                .build();

        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);

        tjeneste.launch(new Properties());

        assertIngenKandidater();
    }

    private void assertIngenKandidater() {
        verifyNoInteractions(taskTjeneste);
    }

    private void assertOpprettet(Behandling behandling, int antall) {
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(1)).lagre(captor.capture());
        var taskTypeExpected = TaskType.forProsessTask(AutomatiskEtterkontrollTask.class);
        assertThat(captor.getAllValues()).allMatch(t -> t.taskType().equals(taskTypeExpected));
        if (antall == 0) {
            assertThat(finnTaskFor(captor, behandling)).isEmpty();
        } else {
            assertThat(finnTaskFor(captor, behandling)).isPresent();
        }
    }

    static Optional<ProsessTaskData> finnTaskFor(ArgumentCaptor<ProsessTaskData> captor, Behandling behandling) {
        return captor.getAllValues().stream()
            .filter(t -> t.getBehandlingIdAsLong().equals(behandling.getId()))
            .findFirst();
    }

    @Test
    void skal_hente_ut_siste_vedtak_til_revurdering(EntityManager entityManager) {
        var grunnlagRepository = new FamilieHendelseRepository(entityManager);
        var behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE + 2);
        var terminDato = LocalDate.now().minusDays(REVURDERING_DAGER_TILBAKE + 2);

        var revurderingBuilder = Behandling.fraTidligereBehandling(behandling,
                BehandlingType.REVURDERING).medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ANNET));
        var revurderingsBehandling = revurderingBuilder.build();

        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(revurderingsBehandling);
        var behandlingVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("asdf")
            .build();
        revurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagre(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));
        grunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), revurderingsBehandling.getId());
        var oppdatere = grunnlagRepository.opprettBuilderForSøknad(revurderingsBehandling.getId());
        oppdatere.medTerminbekreftelse(oppdatere.getTerminbekreftelseBuilder()
                .medTermindato(terminDato)
                .medNavnPå("Lege Legsen")
                .medUtstedtDato(terminDato.minusDays(40))).medAntallBarn(1);
        grunnlagRepository.lagreSøknadHendelse(revurderingsBehandling.getId(), oppdatere);
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(revurderingsBehandling));

        var etterkontroll = new Etterkontroll.Builder(revurderingsBehandling.getFagsakId()).medErBehandlet(
                false)
                .medKontrollTidspunkt(LocalDate.now().atStartOfDay().minusDays(REVURDERING_DAGER_TILBAKE))
                .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
                .build();
        etterkontrollRepository.lagre(etterkontroll);

        tjeneste.launch(new Properties());

        assertOpprettet(revurderingsBehandling, 1);
        assertOpprettet(behandling, 0);
    }

    @Test
    void behandling_med_nyere_termindato_skal_ikke_være_kandidat_til_revurdering() {
        var behandling = opprettRevurderingsKandidat(0);

        var etterkontroll = new Etterkontroll.Builder(behandling.getFagsakId()).medErBehandlet(false)
                .medKontrollTidspunkt(LocalDate.now().plusDays(REVURDERING_DAGER_TILBAKE).atStartOfDay())
                .medKontrollType(KontrollType.MANGLENDE_FØDSEL)
                .build();
        etterkontrollRepository.lagre(etterkontroll);

        assertIngenKandidater();
    }

    private Behandling opprettRevurderingsKandidat(int dagerTilbake) {
        var terminDato = LocalDate.now().minusDays(dagerTilbake);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
                .medTerminbekreftelse(scenario.medSøknadHendelse()
                        .getTerminbekreftelseBuilder()
                        .medTermindato(terminDato)
                        .medUtstedtDato(LocalDate.now())
                        .medNavnPå("Lege Legesen"))
                .medAntallBarn(1);
        scenario.medBekreftetHendelse()
                .medTerminbekreftelse(scenario.medBekreftetHendelse()
                        .getTerminbekreftelseBuilder()
                        .medTermindato(terminDato)
                        .medNavnPå("LEGEN MIN")
                        .medUtstedtDato(terminDato.minusDays(40)))
                .medAntallBarn(1);

        var behandling = scenario.lagre(repositoryProvider);
        var behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        var behandlingVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
            .medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("asdf")
            .build();

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

    @Test
    void skal_finne_nyeste_innvilgete_avsluttede_behandling_som_ikke_er_henlagt() {
        var behandling = opprettRevurderingsKandidat(REVURDERING_DAGER_TILBAKE + 2);

        var innvilget = new Behandlingsresultat.Builder().medBehandlingResultatType(
                BehandlingResultatType.INNVILGET).buildFor(behandling);
        behandling.setBehandlingresultat(innvilget);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var henlagtBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.FØRSTEGANGSSØKNAD)
                .build();
        var henlagt = new Behandlingsresultat.Builder().medBehandlingResultatType(
                BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET).buildFor(henlagtBehandling);
        henlagtBehandling.setBehandlingresultat(henlagt);
        henlagtBehandling.avsluttBehandling();
        behandlingRepository.lagre(henlagtBehandling, behandlingRepository.taSkriveLås(henlagtBehandling));

        var resultatOpt = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(
                behandling.getFagsak().getId());
        assertThat(resultatOpt).hasValueSatisfying(
                resultat -> assertThat(resultat.getId()).isEqualTo(behandling.getId()));
    }

}
