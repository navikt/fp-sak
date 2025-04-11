package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgaver;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavestatus;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class OppgaveTjenesteTest {

    private static final String FNR = "00000000000";
    private static final Oppgave OPPGAVE = new Oppgave(99L, null, null, null, null, Tema.FOR.getOffisiellKode(), null, null, null, 1, "4806",
        LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET, "beskrivelse", "null");

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private Oppgaver oppgaveRestKlient;

    private AbstractTestScenario<ScenarioMorSøkerEngangsstønad> lagScenario() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now()).medAntallBarn(1);
        return scenario;
    }

    private Behandling lagBehandling(AbstractTestScenario<?> scenario) {
        var behandling = scenario.lagMocked();
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet("4802", null));
        return behandling;
    }

    private OppgaveTjeneste lagTjeneste(AbstractTestScenario<?> scenario) {
        var provider = scenario.mockBehandlingRepositoryProvider();
        return new OppgaveTjeneste(provider.getFagsakRepository(), provider.getBehandlingRepository(), oppgaveRestKlient, taskTjeneste,
            personinfoAdapter);
    }

    @Test
    void skal_opprette_oppgave_basert_på_fagsakId() {
        var captor = ArgumentCaptor.forClass(OpprettOppgave.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);
        var scenario = lagScenario();
        var behandling = lagBehandling(scenario);
        var tjeneste = lagTjeneste(scenario);
        var oppgaveId = tjeneste.opprettVurderDokumentMedBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), null, "2010", "bla bla");

        var request = captor.getValue();
        assertThat(request.saksreferanse()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(request.oppgavetype()).isEqualTo(Oppgavetype.VURDER_DOKUMENT);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.id().toString());
    }

    @Test
    void skal_hente_oppgave_liste() {
        var scenario = lagScenario();
        var behandling = lagBehandling(scenario);
        var tjeneste = lagTjeneste(scenario);
        when(oppgaveRestKlient.finnÅpneOppgaver(eq(List.of(Oppgavetype.VURDER_KONSEKVENS_YTELSE.getKode(), Oppgavetype.VURDER_DOKUMENT.getKode())),
            any(), any(), any())).thenReturn(List.of(OPPGAVE));

        var oppgaver = tjeneste.hentÅpneVurderDokumentOgVurderKonsekvensOppgaver(behandling.getAktørId());

        assertThat(oppgaver).isNotEmpty().contains(OPPGAVE);
    }

    @Test
    void skal_opprette_oppgave_vurder_konsekvens_basert_på_fagsakId() {
        var scenario = lagScenario();
        var behandling = lagBehandling(scenario);
        var tjeneste = lagTjeneste(scenario);
        var captor = ArgumentCaptor.forClass(OpprettOppgave.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        var oppgaveId = tjeneste.opprettVurderKonsekvensBasertPåFagsakId(behandling.getFagsakId(), "2010", "bla bla", false);

        var request = captor.getValue();
        assertThat(request.saksreferanse()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(request.oppgavetype()).isEqualTo(Oppgavetype.VURDER_KONSEKVENS_YTELSE);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.id().toString());
    }

    @Test
    void opprettOppgaveStopUtbetalingAvARENAYtelse() {
        var scenario = lagScenario();
        var behandling = lagBehandling(scenario);
        var tjeneste = lagTjeneste(scenario);

        var forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        var captor = ArgumentCaptor.forClass(OpprettOppgave.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        var førsteAugust = LocalDate.of(2019, 8, 1);
        var oppgaveId = tjeneste.opprettOppgaveStopUtbetalingAvARENAYtelse(behandling.getId(), førsteAugust);

        var request = captor.getValue();
        assertThat(request.saksreferanse()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(request.oppgavetype()).isEqualTo(Oppgavetype.SETT_UTBETALING_VENT);

        assertThat(request.tema()).isEqualTo("STO");
        assertThat(request.fristFerdigstillelse()).isEqualTo(forventetFrist);
        assertThat(request.beskrivelse()).isEqualTo("Samordning arenaytelse. Vedtak foreldrepenger fra " + førsteAugust);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.id().toString());
    }

    @Test
    void opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver() {
        var scenario = lagScenario();
        var behandling = lagBehandling(scenario);
        var tjeneste = lagTjeneste(scenario);
        when(personinfoAdapter.hentFnr(behandling.getAktørId())).thenReturn(Optional.of(new PersonIdent(FNR)));
        var forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        var captor = ArgumentCaptor.forClass(OpprettOppgave.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        var førsteUttaksdato = LocalDate.of(2019, 2, 1);
        var vedtaksdato = LocalDate.of(2019, 1, 15);
        var personIdent = new PersonIdent(FNR);
        var beskrivelse = String.format(
            "Refusjon til privat arbeidsgiver," + "Saksnummer: %s," + "Vedtaksdato: %s," + "Dato for første utbetaling: %s,"
                + "Fødselsnummer arbeidsgiver: %s", behandling.getSaksnummer().getVerdi(), vedtaksdato, førsteUttaksdato, personIdent.getIdent());

        var oppgaveId = tjeneste.opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(behandling.getId(), førsteUttaksdato, vedtaksdato,
            behandling.getAktørId());

        var request = captor.getValue();
        assertThat(request.saksreferanse()).isEqualTo(behandling.getSaksnummer().getVerdi());
        assertThat(request.oppgavetype().getKode()).isEqualTo(Oppgavetype.SETT_UTBETALING_VENT.getKode());
        assertThat(request.tema()).isEqualTo("STO");
        assertThat(request.fristFerdigstillelse()).isEqualTo(forventetFrist);
        assertThat(request.beskrivelse()).isEqualTo(beskrivelse);
        assertThat(request.prioritet()).isEqualTo(Prioritet.HOY);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.id().toString());
    }

    @Test
    void ferdigstille_oppgave_kalles_videre_til_rest_klient() {
        var tjeneste = lagTjeneste(lagScenario());
        tjeneste.ferdigstillOppgave(OPPGAVE.id().toString());
        verify(oppgaveRestKlient, times(1)).ferdigstillOppgave(OPPGAVE.id().toString());
        verify(oppgaveRestKlient, times(1)).hentOppgave(OPPGAVE.id().toString());
    }

    @Test
    void ferdigstille_oppgave_feiler_for_oppgaveId_null() {
        var tjeneste = lagTjeneste(lagScenario());
        doThrow(new IllegalArgumentException("Feil")).when(oppgaveRestKlient).ferdigstillOppgave(null);

        assertThatThrownBy(() -> tjeneste.ferdigstillOppgave(null)).isInstanceOf(TekniskException.class)
            .hasMessageContaining("Noe feilet ved ferdigstilling av oppgave");
    }
}
