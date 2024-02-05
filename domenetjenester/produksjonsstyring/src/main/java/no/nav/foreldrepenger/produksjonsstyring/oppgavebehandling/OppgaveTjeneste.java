package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgaver;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class OppgaveTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaveTjeneste.class);
    private static final int DEFAULT_OPPGAVEFRIST_DAGER = 1;

    private static final String NØS_ANSVARLIG_ENHETID = "4151";
    private static final String NØS_BEH_TEMA = "ab0273";
    private static final String NØS_TEMA = "STO";
    private static final String SAMHANDLING_BA_FA_SP = "ae0119";
    private static final String SYK_TEMA = "SYK";
    private static final String SYK_ANSVARLIG_ENHETID = "4488";
    private static final String FEILMELDING = "Feil ved henting av oppgaver for oppgavetype=";

    private FagsakRepository fagsakRepository;

    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private Oppgaver restKlient;

    OppgaveTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OppgaveTjeneste(FagsakRepository fagsakRepository,
                           BehandlingRepository behandlingRepository, Oppgaver restKlient,
                           ProsessTaskTjeneste taskTjeneste,
                           PersoninfoAdapter personinfoAdapter) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.restKlient = restKlient;
        this.taskTjeneste = taskTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    private OpprettOppgave.Builder createRestRequestBuilder(Oppgavetype oppgavetype, Saksnummer saksnummer, AktørId aktørId, String enhet, String beskrivelse,
                                                            Prioritet prioritet, int fristDager) {
        return OpprettOppgave.getBuilderTemaFOR(oppgavetype, prioritet, fristDager)
            .medAktoerId(aktørId.getId())
            .medSaksreferanse(saksnummer != null ? saksnummer.getVerdi() : null)
            .medTildeltEnhetsnr(enhet)
            .medOpprettetAvEnhetsnr(enhet)
            .medBeskrivelse(beskrivelse);
    }

    /**
     * Supplerende oppgaver: Vurder Dokument og Konsekvens for Ytelse
     */
    public boolean harÅpneVurderDokumentOppgaver(AktørId aktørId) {
        try {
            var oppgaver = restKlient.finnÅpneOppgaver(List.of(Oppgavetype.VURDER_DOKUMENT.getKode(), "VUR_VL"), aktørId.getId(), null, null);
            LOG.info("FPSAK GOSYS fant {} oppgaver av type {}", oppgaver.size(), Oppgavetype.VURDER_DOKUMENT.getKode());
            return oppgaver != null && !oppgaver.isEmpty();
        } catch (Exception e) {
            throw new TekniskException("FP-395340", String.format(FEILMELDING + "%s.", Oppgavetype.VURDER_DOKUMENT.getKode()));
        }
    }

    public boolean harÅpneVurderKonsekvensOppgaver(AktørId aktørId) {
        try {
            var oppgaver = restKlient.finnÅpneOppgaverAvType(Oppgavetype.VURDER_KONSEKVENS_YTELSE, aktørId.getId(), null, null);
            LOG.info("FPSAK GOSYS fant {} oppgaver av type {}", oppgaver.size(), Oppgavetype.VURDER_KONSEKVENS_YTELSE.getKode());
            return oppgaver != null && !oppgaver.isEmpty();
        } catch (Exception e) {
            throw new TekniskException("FP-395340", String.format(FEILMELDING + "%s.", Oppgavetype.VURDER_KONSEKVENS_YTELSE.getKode()));
        }
    }

    public String opprettVurderDokumentMedBeskrivelseBasertPåFagsakId(Long fagsakId, String journalpostId, String enhetsId, String beskrivelse) {
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        var orequest = createRestRequestBuilder(Oppgavetype.VURDER_DOKUMENT, fagsak.getSaksnummer(), fagsak.getAktørId(), enhetsId, beskrivelse,
            Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER)
            .medBehandlingstema(BehandlingTema.fraFagsak(fagsak, null).getOffisiellKode())
            .medJournalpostId(journalpostId);
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        LOG.info("FPSAK GOSYS opprettet VURDER VL oppgave {}", oppgave.id());
        return oppgave.id().toString();
    }

    public String opprettVurderKonsekvensBasertPåFagsakId(Long fagsakId, String enhetsId, String beskrivelse, boolean høyPrioritet) {
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        var orequest = createRestRequestBuilder(Oppgavetype.VURDER_KONSEKVENS_YTELSE, fagsak.getSaksnummer(), fagsak.getAktørId(), enhetsId, beskrivelse,
            høyPrioritet ? Prioritet.HOY : Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER)
            .medBehandlingstema(BehandlingTema.fraFagsak(fagsak, null).getOffisiellKode());
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        LOG.info("FPSAK GOSYS opprettet VURDER VL oppgave {}", oppgave.id());
        return oppgave.id().toString();
    }

    public void opprettVurderKonsekvensHosSykepenger(String enhetsId, String beskrivelse, AktørId aktørId) {
        var orequest = createRestRequestBuilder(Oppgavetype.VURDER_KONSEKVENS_YTELSE, null, aktørId, enhetsId, beskrivelse,
            Prioritet.HOY, DEFAULT_OPPGAVEFRIST_DAGER)
            .medBehandlingstype(SAMHANDLING_BA_FA_SP)
            .medTema(SYK_TEMA)
            .medTildeltEnhetsnr(SYK_ANSVARLIG_ENHETID);
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        LOG.info("FPSAK GOSYS opprettet SYK oppgave {}", oppgave.id());
    }

    public String opprettOppgaveStopUtbetalingAvARENAYtelse(long behandlingId, LocalDate førsteUttaksdato) {
        var beskrivelse = String.format("Samordning arenaytelse. Vedtak foreldrepenger fra %s", førsteUttaksdato);

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return opprettOkonomiSettPåVent(beskrivelse, behandling);
    }

    private String opprettOkonomiSettPåVent(String beskrivelse, Behandling behandling) {
        var fagsak = behandling.getFagsak();
        var orequest = createRestRequestBuilder(Oppgavetype.SETT_UTBETALING_VENT, fagsak.getSaksnummer(), fagsak.getAktørId(), behandling.getBehandlendeEnhet(), beskrivelse,
            Prioritet.HOY, DEFAULT_OPPGAVEFRIST_DAGER)
            .medTildeltEnhetsnr(NØS_ANSVARLIG_ENHETID)
            .medTemagruppe(null)
            .medTema(NØS_TEMA)
            .medBehandlingstema(NØS_BEH_TEMA);
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        LOG.info("FPSAK GOSYS opprettet NØS oppgave {}", oppgave.id());
        return oppgave.id().toString();
    }

    public String opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(long behandlingId,
                                                                       LocalDate førsteUttaksdato,
                                                                       LocalDate vedtaksdato,
                                                                       AktørId arbeidsgiverAktørId) {

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var saksnummer = behandling.getFagsak().getSaksnummer();
        var arbeidsgiverIdent = hentPersonInfo(arbeidsgiverAktørId).getIdent();

        var beskrivelse = String.format(
            "Refusjon til privat arbeidsgiver," + "Saksnummer: %s," + "Vedtaksdato: %s," + "Dato for første utbetaling: %s,"
                + "Fødselsnummer arbeidsgiver: %s", saksnummer.getVerdi(), vedtaksdato, førsteUttaksdato, arbeidsgiverIdent);

        return opprettOkonomiSettPåVent(beskrivelse, behandling);
    }

    private PersonIdent hentPersonInfo(AktørId aktørId) {
        return personinfoAdapter.hentFnr(aktørId)
            .orElseThrow(() -> new TekniskException("FP-442142", String.format("Fant ingen ident for aktør %s.", aktørId)));
    }

    /*
     * Forvaltningsrelatert
     */
    public List<Oppgave> alleÅpneOppgaver() {
        try {
            return restKlient.finnÅpneOppgaver(List.of(), null, null, null);
        } catch (Exception e) {
            throw new TekniskException("FP-395341", "Feil ved henting av alle åpne oppgaver.");
        }
    }

    public void ferdigstillOppgaveForForvaltning(String oppgaveId) {
        var avsluttOppgaveTask = ProsessTaskData.forProsessTask(AvsluttOppgaveTask.class);
        avsluttOppgaveTask.setPrioritet(50);
        AvsluttOppgaveTask.setOppgaveId(avsluttOppgaveTask, oppgaveId);
        avsluttOppgaveTask.setCallIdFraEksisterende();
        taskTjeneste.lagre(avsluttOppgaveTask);
    }

    public void avslutt(String oppgaveId) {
        restKlient.ferdigstillOppgave(oppgaveId);
        var oppgv = restKlient.hentOppgave(oppgaveId);
        LOG.info("FPSAK GOSYS ferdigstilte oppgave {} status {}", oppgaveId, Optional.ofNullable(oppgv).map(Oppgave::status).orElse(null));
    }


}
