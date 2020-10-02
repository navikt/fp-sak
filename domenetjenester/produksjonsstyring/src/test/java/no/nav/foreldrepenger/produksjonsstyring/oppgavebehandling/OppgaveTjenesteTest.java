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

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
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
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class OppgaveTjenesteTest {

    private static final String FNR = "00000000000";

    private static final Oppgave OPPGAVE = new Oppgave(99L, null, null, null, null,
        Tema.FOR.getOffisiellKode(), null, null, null, 1, "4806",
        LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET);

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private Repository repository = repoRule.getRepository();

    private OppgaveTjeneste tjeneste;
    private TpsTjeneste tpsTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private OppgaveRestKlient oppgaveRestKlient;

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    private Behandling behandling;

    @Before
    public void oppsett() {

        tpsTjeneste = mock(TpsTjeneste.class);
        prosessTaskRepository = mock(ProsessTaskRepository.class);
        oppgaveRestKlient = Mockito.mock(OppgaveRestKlient.class);
        oppgaveBehandlingKoblingRepository = spy(new OppgaveBehandlingKoblingRepository(entityManager));
        tjeneste = new OppgaveTjeneste(repositoryProvider, oppgaveBehandlingKoblingRepository, oppgaveRestKlient, prosessTaskRepository, tpsTjeneste);
        lagBehandling();
    }

    private void lagBehandling() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now()).medAntallBarn(1);
        behandling = scenario.lagre(repositoryProvider);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet("4802", null));
    }

    @Test
    public void skal_opprette_oppgave_når_det_ikke_finnes_fra_før() {
        // Arrange
        Long behandlingId = behandling.getId();

        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        // Act
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);

        // Assert
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaveBehandlingKoblinger).hasSize(1);
        OppgaveBehandlingKobling oppgaveBehandlingKobling = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.BEHANDLE_SAK, oppgaveBehandlingKoblinger).orElseThrow(
            () -> new IllegalStateException("Mangler AktivOppgaveMedÅrsak"));
        assertThat(oppgaveBehandlingKobling.getOppgaveÅrsak()).isEqualTo(OppgaveÅrsak.BEHANDLE_SAK);
        assertThat(oppgaveBehandlingKobling.getOppgaveId()).isEqualTo(OPPGAVE.getId().toString());

    }

    @Test
    public void skal_ikke_opprette_en_ny_oppgave_av_samme_type_når_det_finnes_fra_før_og_den_ikke_er_ferdigstilt() {
        // Arrange
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);
        List<OppgaveBehandlingKobling> oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaver).hasSize(1);
        oppgaver.get(0).setFerdigstilt(false);
        repository.lagre(oppgaver.get(0));

        // Act
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);

        // Assert
        oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaver).hasSize(1);
    }

    @Test
    public void skal_opprette_en_ny_oppgave_når_det_finnes_fra_før_og_den_er_ferdigstilt() {
        // Arrange
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);
        List<OppgaveBehandlingKobling> oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaver).hasSize(1);
        oppgaver.get(0).setFerdigstilt(true);
        repository.lagre(oppgaver.get(0));

        // Act
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.GODKJENNE_VEDTAK);

        // Assert
        oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        assertThat(oppgaver).hasSize(2);
    }

    @Test
    public void skal_kunne_opprette_en_ny_oppgave_med_en_annen_årsak_selv_om_det_finnes_en_aktiv_oppgave() {
        // Arrange
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        // Act
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.GODKJENNE_VEDTAK);
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.GODKJENNE_VEDTAK);

        // Assert
        List<OppgaveBehandlingKobling> aktiveOppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId).stream()
            .filter(oppgave -> !oppgave.isFerdigstilt())
            .collect(Collectors.toList());
        assertThat(aktiveOppgaver).hasSize(2);
    }

    @Test
    public void skal_avslutte_oppgave() {
        // Arrange
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);

        // Act
        tjeneste.avslutt(behandlingId, OppgaveÅrsak.BEHANDLE_SAK);

        // Assert
        List<OppgaveBehandlingKobling> oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        OppgaveBehandlingKobling behandlingKobling = oppgaver.get(0);
        assertThat(behandlingKobling.isFerdigstilt()).isTrue();
    }

    @Test
    public void skal_opprette_oppgave_basert_på_fagsakId() {
        // Arrange
        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        String oppgaveId = tjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), OppgaveÅrsak.VURDER_DOKUMENT, "2010", "bla bla", false);

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String)Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String)Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(Oppgavetyper.VURDER_DOKUMENT_VL.getKode());
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void skal_avslutte_oppgave_og_starte_task() {
        // Arrange
        String oppgaveId = "1";
        OppgaveBehandlingKobling kobling = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, oppgaveId, behandling.getFagsak().getSaksnummer(), behandling.getId());
        when(oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(anyLong())).thenReturn(Collections.singletonList(kobling));

        // Act
        tjeneste.avsluttOppgaveOgStartTask(behandling, OppgaveÅrsak.BEHANDLE_SAK, OpprettOppgaveGodkjennVedtakTask.TASKTYPE);

        // Assert
        ArgumentCaptor<ProsessTaskGruppe> captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskRepository).lagre(captor.capture());
        assertThat(captor.getAllValues()).hasSize(1);
        ProsessTaskGruppe gruppe = captor.getValue();
        List<ProsessTaskGruppe.Entry> tasks = gruppe.getTasks();
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
        // Arrange
        when(oppgaveRestKlient.finnÅpneOppgaver(any(), eq(Tema.FOR.getOffisiellKode()), eq(List.of(Oppgavetyper.VURDER_DOKUMENT_VL.getKode())))).thenReturn(List.of(OPPGAVE));
        when(oppgaveRestKlient.finnÅpneOppgaver(any(), eq(Tema.FOR.getOffisiellKode()), eq(List.of(Oppgavetyper.VURDER_KONSEKVENS_YTELSE.getKode())))).thenReturn(Collections.emptyList());

        // Act
        var harVurderDok = tjeneste.harÅpneOppgaverAvType(behandling.getAktørId(), Oppgavetyper.VURDER_DOKUMENT_VL);
        var harVurderKY = tjeneste.harÅpneOppgaverAvType(behandling.getAktørId(), Oppgavetyper.VURDER_KONSEKVENS_YTELSE);

        // Assert
        assertThat(harVurderDok).isTrue();
        assertThat(harVurderKY).isFalse();
    }

    @Test
    public void skal_opprette_oppgave_vurder_konsekvens_basert_på_fagsakId() {
        // Arrange

        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        String oppgaveId = tjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), OppgaveÅrsak.VURDER_KONS_FOR_YTELSE, "2010", "bla bla", false);

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String)Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String)Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(Oppgavetyper.VURDER_KONSEKVENS_YTELSE.getKode());
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void skal_lage_request_som_inneholder_verdier_i_forbindelse_med_manglende_regler() {
        // Arrange

        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        String oppgaveId = tjeneste.opprettOppgaveSakSkalTilInfotrygd(behandling.getId());

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String)Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String)Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(Oppgavetyper.BEHANDLE_SAK_IT.getKode());
        assertThat((String)Whitebox.getInternalState(request, "beskrivelse")).isEqualTo("Foreldrepengesak må flyttes til Infotrygd");
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void skal_opprette_oppgave_med_prioritet_og_beskrivelse() {
        // Arrange
        LocalDate forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        String oppgaveId = tjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), OppgaveÅrsak.GODKJENNE_VEDTAK,
            "4321", "noe tekst", true);

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String)Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String)Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(Oppgavetyper.GODKJENN_VEDTAK_VL.getKode());
        assertThat((LocalDate) Whitebox.getInternalState(request, "fristFerdigstillelse")).isEqualTo(forventetFrist);
        assertThat((Prioritet) Whitebox.getInternalState(request, "prioritet")).isEqualTo(Prioritet.HOY);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void opprettOppgaveStopUtbetalingAvARENAYtelse() {
        // Arrange

        LocalDate forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        LocalDate førsteAugust = LocalDate.of(2019, 8, 1);
        String oppgaveId = tjeneste.opprettOppgaveStopUtbetalingAvARENAYtelse(behandling.getId(), førsteAugust);

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String)Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String)Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(Oppgavetyper.SETTVENT.getKode());
        assertThat((String)Whitebox.getInternalState(request, "tema")).isEqualTo("STO");
        assertThat((LocalDate) Whitebox.getInternalState(request, "fristFerdigstillelse")).isEqualTo(forventetFrist);
        assertThat((String)Whitebox.getInternalState(request, "beskrivelse")).isEqualTo("Samordning arenaytelse. Vedtak foreldrepenger fra " + førsteAugust);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver() {
        // Arrange
        Personinfo personinfo = new Personinfo.Builder()
            .medAktørId(behandling.getAktørId())
            .medPersonIdent(new PersonIdent(FNR))
            .medNavn("Fornavn Etternavn")
            .medFødselsdato(LocalDate.of(1980,4,1))
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .build();
        when(tpsTjeneste.hentBrukerForAktør(behandling.getAktørId())).thenReturn(Optional.of(personinfo));
        LocalDate forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        LocalDate førsteUttaksdato = LocalDate.of(2019, 2, 1);
        LocalDate vedtaksdato = LocalDate.of(2019, 1, 15);
        PersonIdent personIdent = new PersonIdent(FNR);
        String beskrivelse = String.format("Refusjon til privat arbeidsgiver," +
                "Saksnummer: %s," +
                "Vedtaksdato: %s," +
                "Dato for første utbetaling: %s," +
                "Fødselsnummer arbeidsgiver: %s", behandling.getFagsak().getSaksnummer().getVerdi(),
            vedtaksdato, førsteUttaksdato, personIdent.getIdent());

        String oppgaveId = tjeneste.opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(behandling.getId(),
            førsteUttaksdato, vedtaksdato, behandling.getAktørId());

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String)Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String)Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(Oppgavetyper.SETTVENT.getKode());
        assertThat((String)Whitebox.getInternalState(request, "tema")).isEqualTo("STO");
        assertThat((LocalDate) Whitebox.getInternalState(request, "fristFerdigstillelse")).isEqualTo(forventetFrist);
        assertThat((String)Whitebox.getInternalState(request, "beskrivelse")).isEqualTo(beskrivelse);
        assertThat((Prioritet) Whitebox.getInternalState(request, "prioritet")).isEqualTo(Prioritet.HOY);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }
}
