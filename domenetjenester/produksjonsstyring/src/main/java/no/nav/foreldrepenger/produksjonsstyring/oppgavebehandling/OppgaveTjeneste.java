package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavestatus;

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
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class OppgaveTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaveTjeneste.class);
    private static final int DEFAULT_OPPGAVEFRIST_DAGER = 1;

    private static final String NØS_ANSVARLIG_ENHETID = "4151";
    private static final String NØS_BEH_TEMA = "ab0273";
    private static final String NØS_TEMA = "STO";
    private static final String SAMHANDLING_BS_FA_SP = "ae0119";
    private static final String SYK_TEMA = "SYK";
    private static final String SYK_ANSVARLIG_ENHETID = "4488";
    private static final String OMS_TEMA = "OMS";
    private static final String OMS_ANSVARLIG_ENHETID = "4487";

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
                           BehandlingRepository behandlingRepository,
                           Oppgaver restKlient,
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
        var erSystem = KontekstHolder.harKontekst() ? KontekstHolder.getKontekst().getIdentType().erSystem() :true;
        return OpprettOppgave.getBuilderTemaFOR(oppgavetype, prioritet, fristDager)
            .medAktoerId(aktørId.getId())
            .medSaksreferanse(saksnummer != null ? saksnummer.getVerdi() : null)
            .medTildeltEnhetsnr(enhet)
            .medOpprettetAvEnhetsnr(erSystem ? null : enhet)
            .medBeskrivelse(beskrivelse);
    }

    /**
     * Supplerende oppgaver: Vurder Dokument og Konsekvens for Ytelse
     */
    public List<Oppgave> hentÅpneVurderDokumentOgVurderKonsekvensOppgaver(AktørId aktørId) {
        var oppgaveTyper = List.of(Oppgavetype.VURDER_KONSEKVENS_YTELSE.getKode(), Oppgavetype.VURDER_DOKUMENT.getKode());
        try {
            var oppgaver = restKlient.finnÅpneOppgaver(oppgaveTyper, aktørId.getId(), null, null);
            LOG.debug("FPSAK GOSYS fant {} oppgaver av typer {}", oppgaver.size(), oppgaveTyper);
            return oppgaver;
        } catch (Exception e) {
            throw new TekniskException("FP-395340", String.format("Feil ved henting av oppgaver for oppgavetyper=%s.", oppgaveTyper));
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

    public void opprettVurderKonsekvensHosSykepenger(String beskrivelse, AktørId aktørId) {
        var orequest = createRestRequestBuilder(Oppgavetype.VURDER_KONSEKVENS_YTELSE, null, aktørId, SYK_ANSVARLIG_ENHETID, beskrivelse,
            Prioritet.HOY, DEFAULT_OPPGAVEFRIST_DAGER)
            .medBehandlingstype(SAMHANDLING_BS_FA_SP)
            .medTema(SYK_TEMA);
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        LOG.info("FPSAK GOSYS opprettet SYK oppgave {}", oppgave.id());
    }

    public void opprettVurderKonsekvensHosPleiepenger(String beskrivelse, AktørId aktørId) {
        var orequest = createRestRequestBuilder(Oppgavetype.VURDER_KONSEKVENS_YTELSE, null, aktørId, OMS_ANSVARLIG_ENHETID, beskrivelse,
            Prioritet.HOY, DEFAULT_OPPGAVEFRIST_DAGER)
            .medBehandlingstype(SAMHANDLING_BS_FA_SP)
            .medTema(OMS_TEMA)
            .medTildeltEnhetsnr(OMS_ANSVARLIG_ENHETID);
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        LOG.info("FPSAK GOSYS opprettet OMS oppgave {}", oppgave.id());
    }

    public String opprettOppgaveStopUtbetalingAvARENAYtelse(long behandlingId, LocalDate førsteUttaksdato) {
        var beskrivelse = String.format("Samordning arenaytelse. Vedtak foreldrepenger fra %s", førsteUttaksdato);

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return opprettOkonomiSettPåVent(beskrivelse, behandling);
    }

    private String opprettOkonomiSettPåVent(String beskrivelse, Behandling behandling) {
        var fagsak = behandling.getFagsak();
        var orequest = createRestRequestBuilder(Oppgavetype.SETT_UTBETALING_VENT, fagsak.getSaksnummer(), fagsak.getAktørId(), NØS_ANSVARLIG_ENHETID, beskrivelse,
            Prioritet.HOY, DEFAULT_OPPGAVEFRIST_DAGER)
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
        var saksnummer = behandling.getSaksnummer();
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

    /*
     * Forvaltningsrelatert
     */
    public List<Oppgave> hentOppgaver(List<String> oppgaveTyper, String aktørId, String enhetsNr, String limit) {
        try {
            return restKlient.finnÅpneOppgaver(oppgaveTyper, aktørId, enhetsNr, limit);
        } catch (Exception e) {
            throw new TekniskException("FP-395341", "Feil ved henting av alle åpne oppgaver.");
        }
    }

    public void ferdigstillOppgave(String oppgaveId) {
        var oppgave = hentOppgave(oppgaveId);
        if (!oppgave.status().equals(Oppgavestatus.FEILREGISTRERT) && !oppgave.status().equals(Oppgavestatus.FERDIGSTILT)) {
            try {
                avslutt(oppgaveId);
            } catch (Exception e) {
                LOG.warn("Kunne ikke ferdigstille oppgave med id {}", oppgaveId, e);
                throw new TekniskException("FP-395342", "Noe feilet ved ferdigstilling av oppgave", e);
            }
        }
    }

    public void ferdigstillOppgaveForForvaltning(String oppgaveId) {
        var avsluttOppgaveTask = ProsessTaskData.forProsessTask(AvsluttOppgaveTask.class);
        AvsluttOppgaveTask.setOppgaveId(avsluttOppgaveTask, oppgaveId);
        taskTjeneste.lagre(avsluttOppgaveTask);
    }

    public void avslutt(String oppgaveId) {
        restKlient.ferdigstillOppgave(oppgaveId);
        LOG.info("FPSAK GOSYS ferdigstilte oppgave {}", oppgaveId);
    }

    private Oppgave hentOppgave(String oppgaveId) {
        try {
            return restKlient.hentOppgave(oppgaveId);
        } catch (Exception e) {
            LOG.warn("Kunne ikke hente oppgave med id {}", oppgaveId, e);
            throw new TekniskException("FP-395343", "Noe feilet ved henting av oppgave", e);
        }
    }
}
