package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgaver;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavestatus;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

class OppgaveTjenesteTest extends EntityManagerAwareTest {

    private static final String FNR = "00000000000";
    private static final Oppgave OPPGAVE = new Oppgave(99L, null, null, null, null,
            Tema.FOR.getOffisiellKode(), null, null, null, 1, "4806",
            LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET, "beskrivelse", "null");

    private OppgaveTjeneste tjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private ProsessTaskTjeneste taskTjeneste;
    private Oppgaver oppgaveRestKlient;

    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        personinfoAdapter = mock(PersoninfoAdapter.class);
        taskTjeneste = mock(ProsessTaskTjeneste.class);
        oppgaveRestKlient = Mockito.mock(Oppgaver.class);
        tjeneste = new OppgaveTjeneste(new FagsakRepository(entityManager), new BehandlingRepository(entityManager),
                oppgaveRestKlient, taskTjeneste, personinfoAdapter);
    }

    private Behandling lagBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now()).medAntallBarn(1);
        var behandling = scenario.lagre(repositoryProvider);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet("4802", null));
        return behandling;
    }

    @Test
    void skal_opprette_oppgave_basert_på_fagsakId() {
        var captor = ArgumentCaptor.forClass(OpprettOppgave.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);
        var behandling = lagBehandling();
        var oppgaveId = tjeneste.opprettVurderDokumentMedBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), null, "2010",
                "bla bla");

        var request = captor.getValue();
        assertThat(request.saksreferanse()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(request.oppgavetype()).isEqualTo(Oppgavetype.VURDER_DOKUMENT);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.id().toString());
    }

    @Test
    void skal_hente_oppgave_liste() {
        var behandling = lagBehandling();
        when(oppgaveRestKlient.finnÅpneOppgaver(eq(List.of(Oppgavetype.VURDER_DOKUMENT.getKode(), "VUR_VL")), any(), any(), any())).thenReturn(List.of(OPPGAVE));
        when(oppgaveRestKlient.finnÅpneOppgaverAvType(eq(Oppgavetype.VURDER_KONSEKVENS_YTELSE), any(), any(), any())).thenReturn(List.of());

        var harVurderDok = tjeneste.harÅpneVurderDokumentOppgaver(behandling.getAktørId());
        var harVurderKY = tjeneste.harÅpneVurderKonsekvensOppgaver(behandling.getAktørId());

        assertThat(harVurderDok).isTrue();
        assertThat(harVurderKY).isFalse();
    }

    @Test
    void skal_opprette_oppgave_vurder_konsekvens_basert_på_fagsakId() {
        var behandling = lagBehandling();
        var captor = ArgumentCaptor.forClass(OpprettOppgave.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        var oppgaveId = tjeneste.opprettVurderKonsekvensBasertPåFagsakId(behandling.getFagsakId(), "2010", "bla bla", false);

        var request = captor.getValue();
        assertThat(request.saksreferanse()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(request.oppgavetype()).isEqualTo(Oppgavetype.VURDER_KONSEKVENS_YTELSE);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.id().toString());
    }

    @Test
    void opprettOppgaveStopUtbetalingAvARENAYtelse() {
        var behandling = lagBehandling();

        var forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        var captor = ArgumentCaptor.forClass(OpprettOppgave.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        var førsteAugust = LocalDate.of(2019, 8, 1);
        var oppgaveId = tjeneste.opprettOppgaveStopUtbetalingAvARENAYtelse(behandling.getId(), førsteAugust);

        var request = captor.getValue();
        assertThat(request.saksreferanse()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(request.oppgavetype()).isEqualTo(Oppgavetype.SETT_UTBETALING_VENT);

        assertThat(request.tema()).isEqualTo("STO");
        assertThat(request.fristFerdigstillelse()).isEqualTo(forventetFrist);
        assertThat(request.beskrivelse())
                .isEqualTo("Samordning arenaytelse. Vedtak foreldrepenger fra " + førsteAugust);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.id().toString());
    }

    @Test
    void opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver() {
        var behandling = lagBehandling();
        when(personinfoAdapter.hentFnr(behandling.getAktørId())).thenReturn(Optional.of(new PersonIdent(FNR)));
        var forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        var captor = ArgumentCaptor.forClass(OpprettOppgave.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        var førsteUttaksdato = LocalDate.of(2019, 2, 1);
        var vedtaksdato = LocalDate.of(2019, 1, 15);
        var personIdent = new PersonIdent(FNR);
        var beskrivelse = String.format("Refusjon til privat arbeidsgiver," +
                "Saksnummer: %s," +
                "Vedtaksdato: %s," +
                "Dato for første utbetaling: %s," +
                "Fødselsnummer arbeidsgiver: %s", behandling.getFagsak().getSaksnummer().getVerdi(),
                vedtaksdato, førsteUttaksdato, personIdent.getIdent());

        var oppgaveId = tjeneste.opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(behandling.getId(),
                førsteUttaksdato, vedtaksdato, behandling.getAktørId());

        var request = captor.getValue();
        assertThat(request.saksreferanse()).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat(request.oppgavetype().getKode()).isEqualTo(Oppgavetype.SETT_UTBETALING_VENT.getKode());
        assertThat(request.tema()).isEqualTo("STO");
        assertThat(request.fristFerdigstillelse()).isEqualTo(forventetFrist);
        assertThat(request.beskrivelse()).isEqualTo(beskrivelse);
        assertThat(request.prioritet()).isEqualTo(Prioritet.HOY);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.id().toString());
    }
}
