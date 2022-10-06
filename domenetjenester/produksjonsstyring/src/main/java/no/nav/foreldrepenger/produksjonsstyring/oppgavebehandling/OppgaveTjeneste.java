package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.BEHANDLE_SAK;
import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.BEHANDLE_SAK_INFOTRYGD;
import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.REVURDER;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent.BehandlingAvsluttetEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgaver;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class OppgaveTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(OppgaveTjeneste.class);
    private static final int DEFAULT_OPPGAVEFRIST_DAGER = 1;
    private static final String DEFAULT_OPPGAVEBESKRIVELSE = "Må behandle sak i VL!";

    private static final String NØS_ANSVARLIG_ENHETID = "4151";
    private static final String NØS_BEH_TEMA = "ab0273";
    private static final String NØS_TEMA = "STO";

    private static final String OPPGAVE_ID_TASK_KEY = "oppgaveId";

    private static final Map<OppgaveÅrsak, Oppgavetype> ÅRSAK_TIL_OPPGAVETYPER = Map.ofEntries(
        Map.entry(OppgaveÅrsak.BEHANDLE_SAK, Oppgavetype.BEHANDLE_SAK),
        Map.entry(OppgaveÅrsak.BEHANDLE_SAK_INFOTRYGD, Oppgavetype.BEHANDLE_SAK_INFOTRYGD),
        Map.entry(OppgaveÅrsak.SETT_ARENA_UTBET_VENT, Oppgavetype.SETT_UTBETALING_VENT),
        Map.entry(OppgaveÅrsak.REGISTRER_SØKNAD, Oppgavetype.REGISTRER_SØKNAD),
        Map.entry(OppgaveÅrsak.GODKJENNE_VEDTAK, Oppgavetype.GODKJENNE_VEDTAK),
        Map.entry(OppgaveÅrsak.REVURDER, Oppgavetype.REVURDER),
        Map.entry(OppgaveÅrsak.VURDER_DOKUMENT, Oppgavetype.VURDER_DOKUMENT),
        Map.entry(OppgaveÅrsak.VURDER_KONS_FOR_YTELSE, Oppgavetype.VURDER_KONSEKVENS_YTELSE),
        Map.entry(OppgaveÅrsak.INNHENT_DOKUMENTASJON, Oppgavetype.INNHENT_DOK));
    private FagsakRepository fagsakRepository;

    private BehandlingRepository behandlingRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private Oppgaver restKlient;

    OppgaveTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OppgaveTjeneste(FagsakRepository fagsakRepository,
                           BehandlingRepository behandlingRepository,
                           OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                           Oppgaver restKlient,
                           ProsessTaskTjeneste taskTjeneste,
                           PersoninfoAdapter personinfoAdapter) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.restKlient = restKlient;
        this.taskTjeneste = taskTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    /*
     * Oppgave som lagres i OBK: BEH_SAK_VL, RV_VL, GOD_VED_VL, REG_SOK_VL
     */
    public String opprettBasertPåBehandlingId(Long behandlingId, OppgaveÅrsak oppgaveÅrsak) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return opprettOppgave(behandling, oppgaveÅrsak, DEFAULT_OPPGAVEBESKRIVELSE, Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER);
    }

    public String opprettBehandleOppgaveForBehandling(Long behandlingId) {
        return opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(behandlingId, DEFAULT_OPPGAVEBESKRIVELSE, false, DEFAULT_OPPGAVEFRIST_DAGER);
    }

    public String opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(Long behandlingId, String beskrivelse, boolean høyPrioritet,
                                                                         int fristDager) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var oppgaveÅrsak = behandling.erRevurdering() ? REVURDER : BEHANDLE_SAK;
        return opprettOppgave(behandling, oppgaveÅrsak, beskrivelse, høyPrioritet ? Prioritet.HOY : Prioritet.NORM, fristDager);
    }

    private String opprettOppgave(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, String beskrivelse, Prioritet prioritet, int fristDager) {
        var oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository
            .hentOppgaverRelatertTilBehandling(behandling.getId());
        if (OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaveBehandlingKoblinger).isPresent()) {
            // skal ikke opprette oppgave med samme årsak når behandlingen allerede har en
            // åpen oppgave med den årsaken knyttet til seg
            return null;
        }
        var fagsak = behandling.getFagsak();
        var orequest = createRestRequestBuilder(ÅRSAK_TIL_OPPGAVETYPER.get(oppgaveÅrsak), fagsak.getSaksnummer(), fagsak.getAktørId(), behandling.getBehandlendeEnhet(), beskrivelse, prioritet,
            fristDager)
            .medBehandlingstema(BehandlingTema.fraFagsak(fagsak, null).getOffisiellKode());
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        LOG.info("FPSAK GOSYS opprettet LOS oppgave {}", oppgave.id());
        return behandleRespons(behandling.getId(), oppgaveÅrsak, oppgave.id().toString(), fagsak.getSaksnummer());
    }

    private String behandleRespons(Long behandlingId, OppgaveÅrsak oppgaveÅrsak, String oppgaveId,
                                   Saksnummer saksnummer) {
        var oppgaveBehandlingKobling = new OppgaveBehandlingKobling(oppgaveÅrsak, oppgaveId, saksnummer, behandlingId);
        oppgaveBehandlingKoblingRepository.lagre(oppgaveBehandlingKobling);
        return oppgaveId;
    }

    public void avslutt(Long behandlingId, OppgaveÅrsak oppgaveÅrsak) {
        var oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository
            .hentOppgaverRelatertTilBehandling(behandlingId);
        var oppgave = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaveBehandlingKoblinger);
        if (oppgave.isPresent()) {
            avsluttOppgave(oppgave.get());
        } else {
            LOG.warn("FP-395338 Fant ikke oppgave med årsak={}, som skulle vært avsluttet på behandlingId={}.", oppgaveÅrsak.getKode(), behandlingId);
        }
    }

    public void avslutt(Long behandlingId, String oppgaveId) {
        var oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(behandlingId, oppgaveId);
        if (oppgave.isPresent()) {
            avsluttOppgave(oppgave.get());
        } else {
            LOG.warn("FP-395339 Fant ikke oppgave med id={}, som skulle vært avsluttet på behandlingId={}.", oppgaveId, behandlingId);
        }
    }

    private void avsluttOppgave(OppgaveBehandlingKobling aktivOppgave) {
        if (!aktivOppgave.isFerdigstilt()) {
            ferdigstillOppgaveBehandlingKobling(aktivOppgave);
        }
        restKlient.ferdigstillOppgave(aktivOppgave.getOppgaveId());
        var oppgv = restKlient.hentOppgave(aktivOppgave.getOppgaveId());
        LOG.info("FPSAK GOSYS ferdigstilte oppgave {} svar {}", aktivOppgave.getOppgaveId(), oppgv.id());
    }

    private void ferdigstillOppgaveBehandlingKobling(OppgaveBehandlingKobling aktivOppgave) {
        aktivOppgave.ferdigstillOppgave(SubjectHandler.getSubjectHandler().getUid());
        oppgaveBehandlingKoblingRepository.lagre(aktivOppgave);
    }

    public void avsluttOppgaveOgStartTask(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, TaskType taskType) {
        var taskGruppe = new ProsessTaskGruppe();
        taskGruppe.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        opprettTaskAvsluttOppgave(behandling, oppgaveÅrsak, false).ifPresent(taskGruppe::addNesteSekvensiell);
        taskGruppe.addNesteSekvensiell(opprettProsessTask(behandling, taskType));

        taskGruppe.setCallIdFraEksisterende();

        taskTjeneste.lagre(taskGruppe);
    }

    public Optional<ProsessTaskData> opprettTaskAvsluttOppgave(Behandling behandling) {
        var oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        var oppgave = oppgaver.stream().filter(kobling -> !kobling.isFerdigstilt()).findFirst();
        if (oppgave.isPresent()) {
            return opprettTaskAvsluttOppgave(behandling, oppgave.get().getOppgaveÅrsak());
        }
        return Optional.empty();
    }

    public Optional<ProsessTaskData> opprettTaskAvsluttOppgave(Behandling behandling, OppgaveÅrsak oppgaveÅrsak) {
        return opprettTaskAvsluttOppgave(behandling, oppgaveÅrsak, true);
    }

    public Optional<ProsessTaskData> opprettTaskAvsluttOppgave(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, boolean skalLagres) {
        var oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository
            .hentOppgaverRelatertTilBehandling(behandling.getId());
        var oppgave = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaveBehandlingKoblinger);
        if (oppgave.isPresent()) {
            var aktivOppgave = oppgave.get();
            // skal ikke avslutte oppgave av denne typen
            if (BEHANDLE_SAK_INFOTRYGD.equals(aktivOppgave.getOppgaveÅrsak())) {
                return Optional.empty();
            }
            ferdigstillOppgaveBehandlingKobling(aktivOppgave);
            var avsluttOppgaveTask = opprettProsessTask(behandling, TaskType.forProsessTask(AvsluttOppgaveTask.class));
            setOppgaveId(avsluttOppgaveTask, aktivOppgave.getOppgaveId());
            if (skalLagres) {
                avsluttOppgaveTask.setCallIdFraEksisterende();
                taskTjeneste.lagre(avsluttOppgaveTask);
            }
            return Optional.of(avsluttOppgaveTask);
        }
        return Optional.empty();
    }

    public static void setOppgaveId(ProsessTaskData taskData, String oppgaveId) {
        taskData.setProperty(OPPGAVE_ID_TASK_KEY, oppgaveId);
    }

    public static Optional<String> getOppgaveId(ProsessTaskData taskData) {
        return Optional.ofNullable(taskData.getPropertyValue(OPPGAVE_ID_TASK_KEY));
    }

    private ProsessTaskData opprettProsessTask(Behandling behandling, TaskType taskType) {
        var prosessTask = ProsessTaskData.forTaskType(taskType);
        prosessTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTask.setPrioritet(50);
        return prosessTask;
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
    public boolean harÅpneOppgaverAvType(AktørId aktørId, Oppgavetype oppgavetype) {
        try {
            var oppgaver = restKlient.finnÅpneOppgaver(aktørId.getId(), Tema.FOR.getOffisiellKode(), List.of(oppgavetype.getKode()));
            LOG.info("FPSAK GOSYS fant {} oppgaver av type {}", oppgaver.size(), oppgavetype.getKode());
            return oppgaver != null && !oppgaver.isEmpty();
        } catch (Exception e) {
            throw new TekniskException("FP-395340", String.format("Feil ved henting av oppgaver for oppgavetype=%s.", oppgavetype.getKode()));
        }
    }

    public String opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(Long fagsakId, OppgaveÅrsak oppgaveÅrsak, String enhetsId, String beskrivelse,
                                                                   boolean høyPrioritet) {
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        var orequest = createRestRequestBuilder(ÅRSAK_TIL_OPPGAVETYPER.get(oppgaveÅrsak), fagsak.getSaksnummer(), fagsak.getAktørId(), enhetsId, beskrivelse,
            høyPrioritet ? Prioritet.HOY : Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER)
            .medBehandlingstema(BehandlingTema.fraFagsak(fagsak, null).getOffisiellKode());
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        LOG.info("FPSAK GOSYS opprettet VURDER VL oppgave {}", oppgave.id());
        return oppgave.id().toString();
    }

    public String opprettMedPrioritetOgBeskrivelseBasertPåAktørId(String gjeldendeAktørId, Long fagsakId, OppgaveÅrsak oppgaveÅrsak, String enhetsId,
                                                                  String beskrivelse, boolean høyPrioritet) {

        var aktørId = new AktørId(gjeldendeAktørId);

        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        var orequest = createRestRequestBuilder(ÅRSAK_TIL_OPPGAVETYPER.get(oppgaveÅrsak), null, aktørId, enhetsId, beskrivelse, høyPrioritet ? Prioritet.HOY : Prioritet.NORM,
            DEFAULT_OPPGAVEFRIST_DAGER)
            .medBehandlingstema(BehandlingTema.fraFagsak(fagsak, null).getOffisiellKode());
        var oppgave = restKlient.opprettetOppgave(orequest.build());
        LOG.info("FPSAK GOSYS opprettet VURDER IT oppgave {}", oppgave.id());
        return oppgave.id().toString();
    }

    /**
     * Observer endringer i BehandlingStatus og håndter oppgaver deretter.
     */
    public void observerBehandlingStatus(@Observes BehandlingAvsluttetEvent statusEvent) {
        var behandlingId = statusEvent.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        opprettTaskAvsluttOppgave(behandling);
    }

    public String opprettOppgaveStopUtbetalingAvARENAYtelse(long behandlingId, LocalDate førsteUttaksdato) {
        final var BESKRIVELSE = "Samordning arenaytelse. Vedtak foreldrepenger fra %s";
        var beskrivelse = String.format(BESKRIVELSE, førsteUttaksdato);

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

        final var beskrivelse = String.format("Refusjon til privat arbeidsgiver," +
            "Saksnummer: %s," +
            "Vedtaksdato: %s," +
            "Dato for første utbetaling: %s," +
            "Fødselsnummer arbeidsgiver: %s", saksnummer.getVerdi(), vedtaksdato, førsteUttaksdato, arbeidsgiverIdent);

        return opprettOkonomiSettPåVent(beskrivelse, behandling);
    }

    private PersonIdent hentPersonInfo(AktørId aktørId) {
        return personinfoAdapter.hentFnr(aktørId)
            .orElseThrow(() -> new TekniskException("FP-442142", String.format("Fant ingen ident for aktør %s.", aktørId)));
    }

    /*
     * Forvaltningsrelatert
     */
    public void ferdigstillOppgaveForForvaltning(Long behandlingId, String oppgaveId) {
        var oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(behandlingId, oppgaveId);
        oppgave.filter(o -> !o.isFerdigstilt()).ifPresent(this::ferdigstillOppgaveBehandlingKobling);
        restKlient.ferdigstillOppgave(oppgaveId);
        LOG.info("FPSAK GOSYS forvaltning ferdigstilte oppgave {}", oppgaveId);
    }

    public void feilregistrerOppgaveForForvaltning(Long behandlingId, String oppgaveId) {
        var oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(behandlingId, oppgaveId);
        oppgave.filter(o -> !o.isFerdigstilt()).ifPresent(this::ferdigstillOppgaveBehandlingKobling);
        restKlient.feilregistrerOppgave(oppgaveId);
        LOG.info("FPSAK GOSYS forvaltning feilregistrerte oppgave {}", oppgaveId);

    }

}
