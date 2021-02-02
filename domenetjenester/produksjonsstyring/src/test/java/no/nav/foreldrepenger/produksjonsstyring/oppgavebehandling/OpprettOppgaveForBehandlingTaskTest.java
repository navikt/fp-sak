package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveForBehandlingTask;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavestatus;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@CdiDbAwareTest
public class OpprettOppgaveForBehandlingTaskTest {

    private static final String FNR = "00000000000";
    private static final Oppgave OPPGAVE = new Oppgave(99L, null, null, null, null,
        Tema.FOR.getOffisiellKode(), null, null, null, 1, "4806",
        LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET);

    private EntityManager entityManager;
    private OppgaveTjeneste tjeneste;

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    private Fagsak fagsak;

    @Mock
    private OppgaveRestKlient oppgaveRestKlient;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        this.entityManager = entityManager;
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        oppgaveBehandlingKoblingRepository = new OppgaveBehandlingKoblingRepository(entityManager);
        tjeneste = new OppgaveTjeneste(new FagsakRepository(entityManager), new BehandlingRepository(entityManager),
            oppgaveBehandlingKoblingRepository, oppgaveRestKlient, prosessTaskRepository, personinfoAdapter);

        // Bygg fagsak som gjenbrukes over testene
        fagsak = opprettOgLagreFagsak();

        // Sett opp default mock-oppførsel
        lenient().when(personinfoAdapter.hentFnr(fagsak.getNavBruker().getAktørId())).thenReturn(Optional.of(new PersonIdent(FNR)));
    }

    @Test
    public void skal_utføre_tasken_opprett_oppgave_for_behandling_av_førstegangsbehandling() throws Exception {
        // Arrange
        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak).medBehandlendeEnhet(new OrganisasjonsEnhet("0234", null));
        Behandling behandling = behandlingBuilder.build();
        lagreBehandling(behandling);

        ProsessTaskData taskData = new ProsessTaskData(OpprettOppgaveForBehandlingTask.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        OpprettOppgaveForBehandlingTask task = new OpprettOppgaveForBehandlingTask(tjeneste);

        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        List<OppgaveBehandlingKobling> oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        assertThat(OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.BEHANDLE_SAK, oppgaver)).isNotPresent();

        // Act
        task.doTask(taskData);
        entityManager.flush();
        entityManager.clear();

        // Assert
        behandling = behandlingRepository.hentBehandling(behandling.getId());
        oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        assertThat(OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.BEHANDLE_SAK, oppgaver)).isPresent();
    }

    @Test
    public void oppretter_oppgave_for_behandling_av_revurdering() throws Exception {
        // Arrange
        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak).medBehandlendeEnhet(new OrganisasjonsEnhet("0234", null));
        Behandling behandling = behandlingBuilder.build();
        lagreBehandling(behandling);

        Behandling revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING).build();
        lagreBehandling(revurdering);

        ProsessTaskData taskData = new ProsessTaskData(OpprettOppgaveForBehandlingTask.TASKTYPE);
        taskData.setBehandling(revurdering.getFagsakId(), revurdering.getId(), revurdering.getAktørId().getId());
        OpprettOppgaveForBehandlingTask task = new OpprettOppgaveForBehandlingTask(tjeneste);

        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        // Skal ikke ha en oppgave av typen revurder fra før
        List<OppgaveBehandlingKobling> oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        assertThat(OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.REVURDER, oppgaver)).isNotPresent();

        // Act
        task.doTask(taskData);
        entityManager.flush();
        entityManager.clear();

        // Assert
        oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(revurdering.getId());
        assertThat(OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.REVURDER, oppgaver)).isPresent();
    }


    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    private Fagsak opprettOgLagreFagsak() {
        Fagsak fagsak = FagsakBuilder.nyEngangstønadForMor()
            .medSaksnummer(new Saksnummer("124"))
            .build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        return fagsak;
    }
}
