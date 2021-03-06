package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.historikk.Oppgavetyper;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveGodkjennVedtakTask;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavestatus;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class OppgaveTjenesteTest extends EntityManagerAwareTest {

    private static final String FNR = "00000000000";

    private static final Oppgave OPPGAVE = new Oppgave(99L, null, null, null, null,
            Tema.FOR.getOffisiellKode(), null, null, null, 1, "4806",
            LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET);

    private OppgaveTjeneste tjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private ProsessTaskRepository prosessTaskRepository;
    private OppgaveRestKlient oppgaveRestKlient;

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        personinfoAdapter = mock(PersoninfoAdapter.class);
        prosessTaskRepository = mock(ProsessTaskRepository.class);
        oppgaveRestKlient = Mockito.mock(OppgaveRestKlient.class);
        oppgaveBehandlingKoblingRepository = spy(new OppgaveBehandlingKoblingRepository(entityManager));
        tjeneste = new OppgaveTjeneste(new FagsakRepository(entityManager), new BehandlingRepository(entityManager),
                oppgaveBehandlingKoblingRepository, oppgaveRestKlient, prosessTaskRepository, personinfoAdapter);
    }

    private Behandling lagBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now()).medAntallBarn(1);
        var behandling = scenario.lagre(repositoryProvider);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet("4802", null));
        return behandling;
    }

    @Test
    public void skal_opprette_oppgave_når_det_ikke_finnes_fra_før() {
        var behandling = lagBehandling();
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any(OpprettOppgave.Builder.class))).thenReturn(OPPGAVE);
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);
        var oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaveBehandlingKoblinger).hasSize(1);
        var oppgaveBehandlingKobling = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.BEHANDLE_SAK, oppgaveBehandlingKoblinger)
                .orElseThrow(() -> new IllegalStateException("Mangler AktivOppgaveMedÅrsak"));
        assertThat(oppgaveBehandlingKobling.getOppgaveÅrsak()).isEqualTo(OppgaveÅrsak.BEHANDLE_SAK);
        assertThat(oppgaveBehandlingKobling.getOppgaveId()).isEqualTo(OPPGAVE.getId().toString());

    }

    @Test
    public void skal_ikke_opprette_en_ny_oppgave_av_samme_type_når_det_finnes_fra_før_og_den_ikke_er_ferdigstilt() {
        var behandling = lagBehandling();
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any(OpprettOppgave.Builder.class))).thenReturn(OPPGAVE);

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);
        var oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaver).hasSize(1);
        oppgaver.get(0).setFerdigstilt(false);
        getEntityManager().persist(oppgaver.get(0));
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);
        oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaver).hasSize(1);
    }

    @Test
    public void skal_opprette_en_ny_oppgave_når_det_finnes_fra_før_og_den_er_ferdigstilt() {
        var behandling = lagBehandling();
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any(OpprettOppgave.Builder.class))).thenReturn(OPPGAVE);

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);
        var oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaver).hasSize(1);
        oppgaver.get(0).setFerdigstilt(true);
        getEntityManager().persist(oppgaver.get(0));

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.GODKJENNE_VEDTAK);

        oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaver).hasSize(2);
    }

    @Test
    public void skal_kunne_opprette_en_ny_oppgave_med_en_annen_årsak_selv_om_det_finnes_en_aktiv_oppgave() {
        var behandling = lagBehandling();
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any(OpprettOppgave.Builder.class))).thenReturn(OPPGAVE);

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.GODKJENNE_VEDTAK);
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.GODKJENNE_VEDTAK);

        var aktiveOppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId).stream()
                .filter(oppgave -> !oppgave.isFerdigstilt())
                .collect(Collectors.toList());
        assertThat(aktiveOppgaver).hasSize(2);
    }

    @Test
    public void skal_avslutte_oppgave() {
        var behandling = lagBehandling();
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any(OpprettOppgave.Builder.class))).thenReturn(OPPGAVE);

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);

        tjeneste.avslutt(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);

        var oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaver.get(0).isFerdigstilt()).isTrue();
    }

    @Test
    public void skal_opprette_oppgave_basert_på_fagsakId() {
        var captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);
        var behandling = lagBehandling();
        String oppgaveId = tjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), OppgaveÅrsak.VURDER_DOKUMENT, "2010",
                "bla bla", false);

        var request = captor.getValue().build();
        assertThat(request.getSaksreferanse()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(request.getOppgavetype()).isEqualTo(Oppgavetyper.VURDER_DOKUMENT_VL.getKode());
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void skal_avslutte_oppgave_og_starte_task() {
        String oppgaveId = "1";
        var behandling = lagBehandling();
        var kobling = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, oppgaveId, behandling.getFagsak().getSaksnummer(),
                behandling.getId());
        when(oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(anyLong())).thenReturn(Collections.singletonList(kobling));

        tjeneste.avsluttOppgaveOgStartTask(behandling, OppgaveÅrsak.BEHANDLE_SAK, OpprettOppgaveGodkjennVedtakTask.TASKTYPE);

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskRepository).lagre(captor.capture());
        assertThat(captor.getAllValues()).hasSize(1);
        var tasks = captor.getValue().getTasks();
        assertThat(tasks.get(0).getTask().getTaskType()).isEqualTo(AvsluttOppgaveTask.TASKTYPE);
        assertThat(tasks.get(0).getTask().getFagsakId()).isEqualTo(behandling.getFagsakId());
        assertThat(tasks.get(0).getTask().getBehandlingId()).isEqualTo(behandling.getId().toString());
        assertThat(String.valueOf(tasks.get(0).getTask().getAktørId())).isEqualTo(behandling.getAktørId().getId());
        assertThat(tasks.get(1).getTask().getTaskType()).isEqualTo(OpprettOppgaveGodkjennVedtakTask.TASKTYPE);
        assertThat(tasks.get(1).getTask().getFagsakId()).isEqualTo(behandling.getFagsakId());
        assertThat(tasks.get(1).getTask().getBehandlingId()).isEqualTo(behandling.getId().toString());
        assertThat(String.valueOf(tasks.get(1).getTask().getAktørId())).isEqualTo(behandling.getAktørId().getId());
    }

    @Test
    public void skal_hente_oppgave_liste() throws Exception {
        var behandling = lagBehandling();
        when(oppgaveRestKlient.finnÅpneOppgaver(any(), eq(Tema.FOR.getOffisiellKode()), eq(List.of(Oppgavetyper.VURDER_DOKUMENT_VL.getKode()))))
                .thenReturn(List.of(OPPGAVE));
        when(oppgaveRestKlient.finnÅpneOppgaver(any(), eq(Tema.FOR.getOffisiellKode()), eq(List.of(Oppgavetyper.VURDER_KONSEKVENS_YTELSE.getKode()))))
                .thenReturn(Collections.emptyList());

        var harVurderDok = tjeneste.harÅpneOppgaverAvType(behandling.getAktørId(), Oppgavetyper.VURDER_DOKUMENT_VL);
        var harVurderKY = tjeneste.harÅpneOppgaverAvType(behandling.getAktørId(), Oppgavetyper.VURDER_KONSEKVENS_YTELSE);

        assertThat(harVurderDok).isTrue();
        assertThat(harVurderKY).isFalse();
    }

    @Test
    public void skal_opprette_oppgave_vurder_konsekvens_basert_på_fagsakId() {
        var behandling = lagBehandling();
        var captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        String oppgaveId = tjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), OppgaveÅrsak.VURDER_KONS_FOR_YTELSE,
                "2010", "bla bla", false);

        var request = captor.getValue().build();
        assertThat(request.getSaksreferanse()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(request.getOppgavetype()).isEqualTo(Oppgavetyper.VURDER_KONSEKVENS_YTELSE.getKode());
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void skal_lage_request_som_inneholder_verdier_i_forbindelse_med_manglende_regler() {
        var behandling = lagBehandling();
        var captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        String oppgaveId = tjeneste.opprettOppgaveSakSkalTilInfotrygd(behandling.getId());

        var request = captor.getValue().build();
        assertThat(request.getSaksreferanse()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(request.getOppgavetype()).isEqualTo(Oppgavetyper.BEHANDLE_SAK_IT.getKode());
        assertThat(request.getBeskrivelse()).isEqualTo("Foreldrepengesak må flyttes til Infotrygd");
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void skal_opprette_oppgave_med_prioritet_og_beskrivelse() {
        var behandling = lagBehandling();
        var forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        var captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        String oppgaveId = tjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), OppgaveÅrsak.GODKJENNE_VEDTAK,
                "4321", "noe tekst", true);

        var request = captor.getValue().build();
        assertThat(request.getSaksreferanse()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(request.getOppgavetype()).isEqualTo(Oppgavetyper.GODKJENN_VEDTAK_VL.getKode());
        assertThat(request.getFristFerdigstillelse()).isEqualTo(forventetFrist);

        assertThat(request.getPrioritet()).isEqualTo(Prioritet.HOY);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void opprettOppgaveStopUtbetalingAvARENAYtelse() {
        var behandling = lagBehandling();

        var forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        var captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        var førsteAugust = LocalDate.of(2019, 8, 1);
        String oppgaveId = tjeneste.opprettOppgaveStopUtbetalingAvARENAYtelse(behandling.getId(), førsteAugust);

        var request = captor.getValue().build();
        assertThat(request.getSaksreferanse()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(request.getOppgavetype()).isEqualTo(Oppgavetyper.SETTVENT.getKode());

        assertThat(request.getTema()).isEqualTo("STO");
        assertThat(request.getFristFerdigstillelse()).isEqualTo(forventetFrist);
        assertThat(request.getBeskrivelse())
                .isEqualTo("Samordning arenaytelse. Vedtak foreldrepenger fra " + førsteAugust);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver() {
        var behandling = lagBehandling();
        when(personinfoAdapter.hentFnr(behandling.getAktørId())).thenReturn(Optional.of(new PersonIdent(FNR)));
        var forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        var captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        var førsteUttaksdato = LocalDate.of(2019, 2, 1);
        var vedtaksdato = LocalDate.of(2019, 1, 15);
        var personIdent = new PersonIdent(FNR);
        String beskrivelse = String.format("Refusjon til privat arbeidsgiver," +
                "Saksnummer: %s," +
                "Vedtaksdato: %s," +
                "Dato for første utbetaling: %s," +
                "Fødselsnummer arbeidsgiver: %s", behandling.getFagsak().getSaksnummer().getVerdi(),
                vedtaksdato, førsteUttaksdato, personIdent.getIdent());

        String oppgaveId = tjeneste.opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(behandling.getId(),
                førsteUttaksdato, vedtaksdato, behandling.getAktørId());

        var request = captor.getValue().build();
        assertThat(request.getSaksreferanse()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(request.getOppgavetype()).isEqualTo(Oppgavetyper.SETTVENT.getKode());
        assertThat(request.getTema()).isEqualTo("STO");
        assertThat(request.getFristFerdigstillelse()).isEqualTo(forventetFrist);
        assertThat(request.getBeskrivelse()).isEqualTo(beskrivelse);
        assertThat(request.getPrioritet()).isEqualTo(Prioritet.HOY);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }
}
