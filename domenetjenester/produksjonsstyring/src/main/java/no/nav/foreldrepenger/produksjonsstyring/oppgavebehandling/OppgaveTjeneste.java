package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.BEHANDLE_SAK;
import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.BEHANDLE_SAK_INFOTRYGD;
import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.REVURDER;
import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.SETT_ARENA_UTBET_VENT;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent.BehandlingAvsluttetEvent;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.Oppgavetyper;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.rest.OppgaveRestKlient;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.rest.OpprettOppgave;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.rest.Prioritet;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTaskProperties;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.meldinger.WSOpprettOppgaveResponse;
import no.nav.tjeneste.virksomhet.oppgave.v3.informasjon.oppgave.Oppgave;
import no.nav.tjeneste.virksomhet.oppgave.v3.meldinger.FinnOppgaveListeResponse;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.BehandleoppgaveConsumer;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.BrukerType;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.FagomradeKode;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.FerdigstillOppgaveRequestMal;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.PrioritetKode;
import no.nav.vedtak.felles.integrasjon.behandleoppgave.opprett.OpprettOppgaveRequest;
import no.nav.vedtak.felles.integrasjon.oppgave.FinnOppgaveListeFilterMal;
import no.nav.vedtak.felles.integrasjon.oppgave.FinnOppgaveListeRequestMal;
import no.nav.vedtak.felles.integrasjon.oppgave.FinnOppgaveListeSokMal;
import no.nav.vedtak.felles.integrasjon.oppgave.OppgaveConsumer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class OppgaveTjeneste {
    private static final int DEFAULT_OPPGAVEFRIST_DAGER = 1;
    private static final String DEFAULT_OPPGAVEBESKRIVELSE = "Må behandle sak i VL!";

    private static final String FORELDREPENGESAK_MÅ_FLYTTES_TIL_INFOTRYGD = "Foreldrepengesak må flyttes til Infotrygd";

    private static final String NØS_ANSVARLIG_ENHETID = "4151";
    private static final String NØS_FP_UNDERKATEGORI = "FORELDREPE_STO";
    private static final String NØS_BEH_TEMA = "ab0273";
    private static final String NØS_TEMA = "STO";


    // Gosys' kodeverk. Søk på confluence etter ENGANGSST_FOR og se regneark v3.x.y
    private static final String FP_UNDERKATEGORI = "FORELDREPE_FOR";
    private static final String ES_UNDERKATEGORI = "ENGANGSST_FOR";
    private static final String SVP_UNDERKATEGORI = "SVANGERSKAPSPE_FOR";

    private static final Map<OppgaveÅrsak, Oppgavetyper> ÅRSAK_TIL_OPPGAVETYPER = Map.ofEntries(
        Map.entry(OppgaveÅrsak.BEHANDLE_SAK, Oppgavetyper.BEHANDLE_SAK_VL),
        Map.entry(OppgaveÅrsak.BEHANDLE_SAK_INFOTRYGD, Oppgavetyper.BEHANDLE_SAK_IT),
        Map.entry(OppgaveÅrsak.SETT_ARENA_UTBET_VENT, Oppgavetyper.SETTVENT),
        Map.entry(OppgaveÅrsak.REGISTRER_SØKNAD, Oppgavetyper.REG_SOKNAD_VL),
        Map.entry(OppgaveÅrsak.GODKJENNE_VEDTAK, Oppgavetyper.GODKJENN_VEDTAK_VL),
        Map.entry(OppgaveÅrsak.REVURDER, Oppgavetyper.REVURDER_VL),
        Map.entry(OppgaveÅrsak.VURDER_DOKUMENT, Oppgavetyper.VURDER_DOKUMENT_VL),
        Map.entry(OppgaveÅrsak.VURDER_KONS_FOR_YTELSE, Oppgavetyper.VURDER_KONSEKVENS_YTELSE),
        Map.entry(OppgaveÅrsak.INNHENT_DOKUMENTASJON, Oppgavetyper.INNHENT_DOK)
    );

    private static final Map<PrioritetKode, Prioritet> PRIKODE_TIL_PRIORITET = Map.ofEntries(
        Map.entry(PrioritetKode.HOY_FOR, Prioritet.HOY),
        Map.entry(PrioritetKode.NORM_FOR, Prioritet.NORM),
        Map.entry(PrioritetKode.LAV_FOR, Prioritet.LAV),
        Map.entry(PrioritetKode.HOY_STO, Prioritet.HOY),
        Map.entry(PrioritetKode.NORM_STO, Prioritet.NORM)
    );

    private Logger logger = LoggerFactory.getLogger(OppgaveTjeneste.class);
    private FagsakRepository fagsakRepository;

    private BehandlingRepository behandlingRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private BehandleoppgaveConsumer service;
    private ProsessTaskRepository prosessTaskRepository;
    private TpsTjeneste tpsTjeneste;
    private OppgaveConsumer oppgaveConsumer;
    private OppgaveRestKlient restKlient;

    OppgaveTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OppgaveTjeneste(BehandlingRepositoryProvider repositoryProvider,
                           OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                           BehandleoppgaveConsumer service, OppgaveConsumer oppgaveConsumer,
                           OppgaveRestKlient restKlient,
                           ProsessTaskRepository prosessTaskRepository, TpsTjeneste tpsTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.service = service;
        this.oppgaveConsumer = oppgaveConsumer;
        this.restKlient = restKlient;
        this.prosessTaskRepository = prosessTaskRepository;
        this.tpsTjeneste = tpsTjeneste;
    }
    /*
     * Oppgave som lagres i OBK BEH_SAK_VL, RV_VL, GOD_VED_VL, REG_SOK_VL
     */
    public String opprettBasertPåBehandlingId(Long behandlingId, OppgaveÅrsak oppgaveÅrsak) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return opprettOppgave(behandling, oppgaveÅrsak, DEFAULT_OPPGAVEBESKRIVELSE, PrioritetKode.NORM_FOR, DEFAULT_OPPGAVEFRIST_DAGER);
    }

    public String opprettBehandleOppgaveForBehandling(Long behandlingId) {
        return opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(behandlingId, DEFAULT_OPPGAVEBESKRIVELSE, false, DEFAULT_OPPGAVEFRIST_DAGER);
    }

    public String opprettBehandleOppgaveForBehandlingMedPrioritetOgFrist(Long behandlingId, String beskrivelse, boolean høyPrioritet, int fristDager) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        OppgaveÅrsak oppgaveÅrsak = behandling.erRevurdering() ? REVURDER : BEHANDLE_SAK;
        return opprettOppgave(behandling, oppgaveÅrsak, beskrivelse, hentPrioritetKode(høyPrioritet), fristDager);
    }

    private String opprettOppgave(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, String beskrivelse, PrioritetKode prioritetKode, int fristDager) {
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        if (OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaveBehandlingKoblinger).isPresent()) {
            // skal ikke opprette oppgave med samme årsak når behandlingen allerede har en åpen oppgave med den årsaken knyttet til seg
            return null;
        }
        Fagsak fagsak = behandling.getFagsak();
        Personinfo personSomBehandles = hentPersonInfo(behandling.getAktørId());
        OpprettOppgaveRequest request = createRequest(fagsak, personSomBehandles, oppgaveÅrsak, behandling.getBehandlendeEnhet(),
            beskrivelse, prioritetKode, fristDager);
        try {
            var orequest = createRestRequestBuilder(fagsak, fagsak.getAktørId(), behandling.getBehandlendeEnhet(), beskrivelse,
                PRIKODE_TIL_PRIORITET.getOrDefault(prioritetKode, Prioritet.NORM), fristDager)
                .medBehandlingstema(BehandlingTema.fraFagsak(fagsak, null).getOffisiellKode())
                .medOppgavetype(ÅRSAK_TIL_OPPGAVETYPER.get(oppgaveÅrsak).getKode());
            var oppgave = restKlient.opprettetOppgave(orequest);
            if (oppgave == null || oppgave.getId() == null)
                throw new IllegalStateException("Gosys rest: kunne ikke opprette oppgave");
            logger.info("FPSAK GOSYS opprettet LOS oppgave {}", oppgave);
            return behandleRespons(behandling, oppgaveÅrsak, oppgave.getId().toString(), fagsak.getSaksnummer());
        } catch (Exception e) {
            logger.info("FPSAK GOSYS rest LOS - feil ved oppretting av oppgave",e );
        }
        WSOpprettOppgaveResponse response = service.opprettOppgave(request);
        return behandleRespons(behandling, oppgaveÅrsak, response.getOppgaveId(), fagsak.getSaksnummer());
    }

    /**
     * Observer endringer i BehandlingStatus og håndter oppgaver deretter.
     */
    public void observerBehandlingStatus(@Observes BehandlingAvsluttetEvent statusEvent) {
        Long behandlingId = statusEvent.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        opprettTaskAvsluttOppgave(behandling);
    }

    public String opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(Long fagsakId, OppgaveÅrsak oppgaveÅrsak, String enhetsId, String beskrivelse, boolean høyPrioritet) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        Personinfo personSomBehandles = hentPersonInfo(fagsak.getAktørId());

        OpprettOppgaveRequest request = createRequest(fagsak, personSomBehandles, oppgaveÅrsak, enhetsId, beskrivelse,
            hentPrioritetKode(høyPrioritet), DEFAULT_OPPGAVEFRIST_DAGER);
        try {
            var orequest = createRestRequestBuilder(fagsak, fagsak.getAktørId(), enhetsId, beskrivelse, høyPrioritet ? Prioritet.HOY : Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER)
                .medBehandlingstema(BehandlingTema.fraFagsak(fagsak, null).getOffisiellKode())
                .medOppgavetype(ÅRSAK_TIL_OPPGAVETYPER.get(oppgaveÅrsak).getKode());
            var oppgave = restKlient.opprettetOppgave(orequest);
            if (oppgave == null || oppgave.getId() == null)
                throw new IllegalStateException("Gosys rest: kunne ikke opprette oppgave");
            logger.info("FPSAK GOSYS opprettet VURDER VL oppgave {}", oppgave);
            return oppgave.getId().toString();
        } catch (Exception e) {
            logger.info("FPSAK GOSYS rest VURDER VL - feil ved oppretting av oppgave",e );
        }
        WSOpprettOppgaveResponse response = service.opprettOppgave(request);
        return response.getOppgaveId();
    }

    public String opprettMedPrioritetOgBeskrivelseBasertPåAktørId(String gjeldendeAktørId, Long fagsakId, OppgaveÅrsak oppgaveÅrsak, String enhetsId, String beskrivelse, boolean høyPrioritet) {

        AktørId aktørId = new AktørId(gjeldendeAktørId);
        Personinfo personSomBehandles = hentPersonInfo(aktørId);

        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        OpprettOppgaveRequest request = createRequest(null, personSomBehandles, oppgaveÅrsak, enhetsId, beskrivelse, hentPrioritetKode(høyPrioritet), DEFAULT_OPPGAVEFRIST_DAGER, finnUnderkategoriKode(fagsak.getYtelseType()));
        try {
            var orequest = createRestRequestBuilder(null, aktørId, enhetsId, beskrivelse, høyPrioritet ? Prioritet.HOY : Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER)
                .medBehandlingstema(BehandlingTema.fraFagsak(fagsak, null).getOffisiellKode())
                .medOppgavetype(ÅRSAK_TIL_OPPGAVETYPER.get(oppgaveÅrsak).getKode());
            var oppgave = restKlient.opprettetOppgave(orequest);
            if (oppgave == null || oppgave.getId() == null)
                throw new IllegalStateException("Gosys rest: kunne ikke opprette oppgave");
            logger.info("FPSAK GOSYS opprettet VURDER IT oppgave {}", oppgave);
            return oppgave.getId().toString();
        } catch (Exception e) {
            logger.info("FPSAK GOSYS rest VURDER IT - feil ved oppretting av oppgave",e );
        }
        WSOpprettOppgaveResponse response = service.opprettOppgave(request);
        return response.getOppgaveId();
    }

    public String opprettOppgaveStopUtbetalingAvARENAYtelse(long behandlingId, LocalDate førsteUttaksdato) {
        final String BESKRIVELSE = "Samordning arenaytelse. Vedtak foreldrepenger fra %s";
        var beskrivelse = String.format(BESKRIVELSE, førsteUttaksdato);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return opprettOkonomiSettPåVent(beskrivelse, behandling);
    }

    private String opprettOkonomiSettPåVent(String beskrivelse, Behandling behandling) {
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        OpprettOppgaveRequest request = OpprettOppgaveRequest.builder()
            .medBeskrivelse(beskrivelse)
            .medOpprettetAvEnhetId(Integer.parseInt(behandling.getBehandlendeEnhet()))
            .medAnsvarligEnhetId(NØS_ANSVARLIG_ENHETID)
            .medFagomradeKode(FagomradeKode.STO.getKode())
            .medOppgavetypeKode(SETT_ARENA_UTBET_VENT.getKode())
            .medUnderkategoriKode(NØS_FP_UNDERKATEGORI)
            .medPrioritetKode(PrioritetKode.HOY_STO.name())
            .medLest(false)
            .medAktivFra(LocalDate.now())
            .medAktivTil(helgeJustertFrist(LocalDate.now().plusDays(DEFAULT_OPPGAVEFRIST_DAGER)))
            .medBrukerTypeKode(BrukerType.PERSON)
            .medFnr(hentPersonInfo(behandling.getNavBruker().getAktørId()).getPersonIdent().getIdent())
            .medSaksnummer(saksnummer.getVerdi())
            .build();

        try {
            var fagsak = behandling.getFagsak();
            var orequest = createRestRequestBuilder(fagsak, fagsak.getAktørId(), behandling.getBehandlendeEnhet(), beskrivelse, Prioritet.HOY, DEFAULT_OPPGAVEFRIST_DAGER)
                .medTildeltEnhetsnr(NØS_ANSVARLIG_ENHETID)
                .medTemagruppe(null)
                .medTema(NØS_TEMA)
                .medBehandlingstema(NØS_BEH_TEMA)
                .medOppgavetype(Oppgavetyper.SETTVENT.getKode());
            var oppgave = restKlient.opprettetOppgave(orequest);
            if (oppgave == null || oppgave.getId() == null)
                throw new IllegalStateException("Gosys rest: kunne ikke opprette oppgave");
            logger.info("FPSAK GOSYS opprettet NØS oppgave {}", oppgave);
            return oppgave.getId().toString();
        } catch (Exception e) {
            logger.info("FPSAK GOSYS rest NØS - feil ved oppretting av oppgave",e );
        }

        WSOpprettOppgaveResponse response = service.opprettOppgave(request);
        return response.getOppgaveId();
    }

    public String opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(long behandlingId,
                                                                       LocalDate førsteUttaksdato,
                                                                       LocalDate vedtaksdato,
                                                                       AktørId arbeidsgiverAktørId) {

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        String arbeidsgiverIdent = hentPersonInfo(arbeidsgiverAktørId).getPersonIdent().getIdent();

        final String beskrivelse = String.format("Refusjon til privat arbeidsgiver," +
            "Saksnummer: %s," +
            "Vedtaksdato: %s," +
            "Dato for første utbetaling: %s," +
            "Fødselsnummer arbeidsgiver: %s", saksnummer.getVerdi(), vedtaksdato, førsteUttaksdato, arbeidsgiverIdent);

        return opprettOkonomiSettPåVent(beskrivelse, behandling);
    }

    public String opprettOppgaveSakSkalTilInfotrygd(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        OpprettOppgaveRequest request = OpprettOppgaveRequest.builder()
            .medOpprettetAvEnhetId(Integer.parseInt(behandling.getBehandlendeEnhet()))
            .medAnsvarligEnhetId(behandling.getBehandlendeEnhet())
            .medFagomradeKode(FagomradeKode.FOR.getKode())
            .medFnr(hentPersonInfo(behandling.getNavBruker().getAktørId()).getPersonIdent().getIdent())
            .medAktivFra(LocalDate.now())
            .medAktivTil(helgeJustertFrist(LocalDate.now().plusDays(DEFAULT_OPPGAVEFRIST_DAGER)))
            .medOppgavetypeKode(BEHANDLE_SAK_INFOTRYGD.getKode())
            .medSaksnummer(saksnummer.getVerdi())
            .medPrioritetKode(PrioritetKode.NORM_FOR.toString())
            .medBeskrivelse(FORELDREPENGESAK_MÅ_FLYTTES_TIL_INFOTRYGD)
            .medLest(false)
            .build();
        try {
            var fagsak = behandling.getFagsak();
            var orequest = createRestRequestBuilder(fagsak, fagsak.getAktørId(), behandling.getBehandlendeEnhet(), FORELDREPENGESAK_MÅ_FLYTTES_TIL_INFOTRYGD,
                Prioritet.NORM, DEFAULT_OPPGAVEFRIST_DAGER)
                .medBehandlingstema(BehandlingTema.fraFagsak(fagsak, null).getOffisiellKode())
                .medOppgavetype(Oppgavetyper.BEHANDLE_SAK_IT.getKode());
            var oppgave = restKlient.opprettetOppgave(orequest);
            if (oppgave == null || oppgave.getId() == null)
                throw new IllegalStateException("Gosys rest: kunne ikke opprette oppgave");
            logger.info("FPSAK GOSYS opprettet BEH/IT oppgave {}", oppgave);
            return oppgave.getId().toString();
        } catch (Exception e) {
            logger.info("FPSAK GOSYS rest BEH/IT - feil ved oppretting av oppgave",e );
        }
        WSOpprettOppgaveResponse response = service.opprettOppgave(request);
        return response.getOppgaveId();
    }

    private PrioritetKode hentPrioritetKode(boolean høyPrioritet) {
        return høyPrioritet ? PrioritetKode.HOY_FOR : PrioritetKode.NORM_FOR;
    }

    public void avslutt(Long behandlingId, OppgaveÅrsak oppgaveÅrsak) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        Optional<OppgaveBehandlingKobling> oppgave = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaveBehandlingKoblinger);
        if (oppgave.isPresent()) {
            avsluttOppgave(behandling, oppgave.get());
        } else {
            OppgaveFeilmeldinger.FACTORY.oppgaveMedÅrsakIkkeFunnet(oppgaveÅrsak.getKode(), behandlingId).log(logger);
        }
    }

    public void avslutt(Long behandlingId, String oppgaveId) {
        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (oppgave.isPresent()) {
            avsluttOppgave(behandling, oppgave.get());
        } else {
            OppgaveFeilmeldinger.FACTORY.oppgaveMedIdIkkeFunnet(oppgaveId, behandlingId).log(logger);
        }
    }

    private void avsluttOppgave(Behandling behandling, OppgaveBehandlingKobling aktivOppgave) {
        if (!aktivOppgave.isFerdigstilt()) {
            ferdigstillOppgaveBehandlingKobling(aktivOppgave);
        }
        try {
            restKlient.ferdigstillOppgave(aktivOppgave.getOppgaveId());
            var oppgv = restKlient.hentOppgave(aktivOppgave.getOppgaveId());
            logger.info("FPSAK GOSYS ferdigstilte oppgave {} svar {}", aktivOppgave.getOppgaveId(), oppgv);
            return;
        } catch (Exception e) {
            logger.info("FPSAK GOSYS rest ferdigstill - feil ved ferdigstilling av oppgave",e );
        }
        FerdigstillOppgaveRequestMal request = createFerdigstillRequest(behandling, aktivOppgave.getOppgaveId());
        service.ferdigstillOppgave(request);
    }

    public void ferdigstillOppgaveForForvaltning(Long behandlingId, String oppgaveId) {
        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (oppgave.isPresent()) {
            avsluttOppgave(behandling, oppgave.get());
        } else {
            try {
                restKlient.ferdigstillOppgave(oppgaveId);
                logger.info("FPSAK GOSYS ferdigstilte oppgave {}", oppgaveId);
                return;
            } catch (Exception e) {
                logger.info("FPSAK GOSYS rest ferdigstill - feil ved ferdigstilling av oppgave",e );
            }
            FerdigstillOppgaveRequestMal request = createFerdigstillRequest(behandling, oppgaveId);
            service.ferdigstillOppgave(request);
        }
    }

    public void feilregistrerOppgaveForForvaltning(Long behandlingId, String oppgaveId) {
        Optional<OppgaveBehandlingKobling> oppgave = oppgaveBehandlingKoblingRepository.hentOppgaveBehandlingKobling(oppgaveId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (oppgave.isPresent()) {
            avsluttOppgave(behandling, oppgave.get());
        } else {
            try {
                restKlient.feilregistrerOppgave(oppgaveId);
                logger.info("FPSAK GOSYS feilregistrerte oppgave {}", oppgaveId);
                return;
            } catch (Exception e) {
                logger.info("FPSAK GOSYS rest feilregistrering - feil ved feilregistrering av oppgave",e );
            }
            FerdigstillOppgaveRequestMal request = createFerdigstillRequest(behandling, oppgaveId);
            service.ferdigstillOppgave(request);
        }
    }

    private void ferdigstillOppgaveBehandlingKobling(OppgaveBehandlingKobling aktivOppgave) {
        aktivOppgave.ferdigstillOppgave(SubjectHandler.getSubjectHandler().getUid());
        oppgaveBehandlingKoblingRepository.lagre(aktivOppgave);
    }

    public void avsluttOppgaveOgStartTask(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, String taskType) {
        ProsessTaskGruppe taskGruppe = new ProsessTaskGruppe();
        taskGruppe.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        opprettTaskAvsluttOppgave(behandling, oppgaveÅrsak, false).ifPresent(taskGruppe::addNesteSekvensiell);
        taskGruppe.addNesteSekvensiell(opprettProsessTask(behandling, taskType));

        taskGruppe.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(taskGruppe);
    }

    public List<Oppgaveinfo> hentOppgaveListe(AktørId aktørId, List<String> oppgaveÅrsaker) {
        PersonIdent personIdent = hentPersonInfo(aktørId).getPersonIdent();
        FinnOppgaveListeRequestMal.Builder requestMalBuilder = new FinnOppgaveListeRequestMal.Builder();
        FinnOppgaveListeSokMal sokMal = FinnOppgaveListeSokMal.builder().medBrukerId(personIdent.getIdent()).build();
        FinnOppgaveListeFilterMal filterMal = FinnOppgaveListeFilterMal.builder().medOppgavetypeKodeListe(oppgaveÅrsaker).build();
        FinnOppgaveListeRequestMal requestMal = requestMalBuilder.medSok(sokMal).medFilter(filterMal).build();
        FinnOppgaveListeResponse finnOppgaveListeResponse = oppgaveConsumer.finnOppgaveListe(requestMal);
        List<Oppgave> oppgaveListe = finnOppgaveListeResponse.getOppgaveListe();
        var oppgaveInfoList = oppgaveListe.stream().map(ol -> new Oppgaveinfo(ol.getOppgavetype().getKode(), ol.getStatus().getKode())).collect(Collectors.toList());
        try {
            List<String> oppgavetyper = oppgaveÅrsaker.stream().map(aarsak -> ÅRSAK_TIL_OPPGAVETYPER.get(OppgaveÅrsak.fraKode(aarsak))).map(Oppgavetyper::getKode).collect(Collectors.toList());
            var oppgaver = restKlient.finnÅpneOppgaver(aktørId.getId(), Tema.FOR.getOffisiellKode(), oppgavetyper);
            if (oppgaver == null)
                throw new IllegalStateException("Gosys rest: kunne ikke opprette oppgave");
            logger.info("FPSAK GOSYS hentOppgaver fant oppgaver {}", oppgaver);
            var restOppgaveInfos = oppgaver.stream().map(o -> new Oppgaveinfo(o.getOppgavetype(), o.getStatus().name())).collect(Collectors.toList());
            var wsOppgaveInfos = oppgaveInfoList.stream()
                .filter(o -> oppgaveÅrsaker.contains(o.getOppgaveType()))
                .map(o -> new Oppgaveinfo(o.getOppgaveType(), o.getStatus())).collect(Collectors.toList());
            if (restOppgaveInfos.size() == wsOppgaveInfos.size())
                logger.info("FPSAK GOSYS rest hentOppgaver samme antall oppgaver");
            if (restOppgaveInfos.containsAll(wsOppgaveInfos) && wsOppgaveInfos.containsAll(restOppgaveInfos))
                logger.info("FPSAK GOSYS rest hentOppgaver samme oppgaver");
            else
                logger.info("FPSAK GOSYS rest hentOppgaver avvik oppgaver: rs {} ws {}", restOppgaveInfos, wsOppgaveInfos);
        } catch (Exception e) {
            logger.info("FPSAK GOSYS rest hentOppgaver - feil ved henting av oppgave",e );
        }
        return oppgaveInfoList;
    }

    public Optional<ProsessTaskData> opprettTaskAvsluttOppgave(Behandling behandling) {
        List<OppgaveBehandlingKobling> oppgaver = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        Optional<OppgaveBehandlingKobling> oppgave = oppgaver.stream().filter(kobling -> !kobling.isFerdigstilt()).findFirst();
        if (oppgave.isPresent()) {
            return opprettTaskAvsluttOppgave(behandling, oppgave.get().getOppgaveÅrsak());
        } else {
            return Optional.empty();
        }
    }

    public Optional<ProsessTaskData> opprettTaskAvsluttOppgave(Behandling behandling, OppgaveÅrsak oppgaveÅrsak) {
        return opprettTaskAvsluttOppgave(behandling, oppgaveÅrsak, true);
    }

    public Optional<ProsessTaskData> opprettTaskAvsluttOppgave(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, boolean skalLagres) {
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandling.getId());
        Optional<OppgaveBehandlingKobling> oppgave = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(oppgaveÅrsak, oppgaveBehandlingKoblinger);
        if (oppgave.isPresent()) {
            OppgaveBehandlingKobling aktivOppgave = oppgave.get();
            // skal ikke avslutte oppgave av denne typen
            if (BEHANDLE_SAK_INFOTRYGD.equals(aktivOppgave.getOppgaveÅrsak())) {
                return Optional.empty();
            }
            ferdigstillOppgaveBehandlingKobling(aktivOppgave);
            ProsessTaskData avsluttOppgaveTask = opprettProsessTask(behandling, AvsluttOppgaveTaskProperties.TASKTYPE);
            avsluttOppgaveTask.setOppgaveId(aktivOppgave.getOppgaveId());
            if (skalLagres) {
                avsluttOppgaveTask.setCallIdFraEksisterende();
                prosessTaskRepository.lagre(avsluttOppgaveTask);
            }
            return Optional.of(avsluttOppgaveTask);
        } else {
            return Optional.empty();
        }
    }

    private ProsessTaskData opprettProsessTask(Behandling behandling, String taskType) {
        ProsessTaskData prosessTask = new ProsessTaskData(taskType);
        prosessTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTask.setPrioritet(50);
        return prosessTask;
    }

    private FerdigstillOppgaveRequestMal createFerdigstillRequest(Behandling behandling, String oppgaveId) {
        FerdigstillOppgaveRequestMal.Builder builder = FerdigstillOppgaveRequestMal.builder().medOppgaveId(oppgaveId);
        if (behandling.getBehandlendeEnhet() != null) {
            builder.medFerdigstiltAvEnhetId(Integer.parseInt(behandling.getBehandlendeEnhet()));
        }
        return builder.build();
    }

    private String behandleRespons(Behandling behandling, OppgaveÅrsak oppgaveÅrsak, String oppgaveId,
                                   Saksnummer saksnummer) {

        OppgaveBehandlingKobling oppgaveBehandlingKobling = new OppgaveBehandlingKobling(oppgaveÅrsak, oppgaveId, saksnummer, behandling);
        oppgaveBehandlingKoblingRepository.lagre(oppgaveBehandlingKobling);
        return oppgaveId;
    }

    private Personinfo hentPersonInfo(AktørId aktørId) {
        return tpsTjeneste.hentBrukerForAktør(aktørId)
            .orElseThrow(() -> OppgaveFeilmeldinger.FACTORY.identIkkeFunnet(aktørId).toException());
    }

    private OpprettOppgaveRequest createRequest(Fagsak fagsak, Personinfo personinfo, OppgaveÅrsak oppgaveÅrsak,
                                                String enhetsId, String beskrivelse, PrioritetKode prioritetKode,
                                                int fristDager) {
        return createRequest(fagsak, personinfo, oppgaveÅrsak, enhetsId, beskrivelse, prioritetKode, fristDager, finnUnderkategoriKode(fagsak.getYtelseType()));
    }

    private String finnUnderkategoriKode(FagsakYtelseType fagsakYtelseType) {
        if (fagsakYtelseType.gjelderForeldrepenger()) {
            return FP_UNDERKATEGORI;
        } else if (fagsakYtelseType.gjelderEngangsstønad()) {
            return ES_UNDERKATEGORI;
        } else if (fagsakYtelseType.gjelderSvangerskapspenger()) {
            return SVP_UNDERKATEGORI;
        }
        throw OppgaveFeilmeldinger.FACTORY.underkategoriIkkeFunnetForFagsakYtelseType(fagsakYtelseType).toException();
    }

    private OpprettOppgaveRequest createRequest(Fagsak fagsak, Personinfo personinfo, OppgaveÅrsak oppgaveÅrsak,
                                                String enhetsId, String beskrivelse, PrioritetKode prioritetKode,
                                                int fristDager, String underkategoriKode) {

        String saksnummer = (fagsak == null || fagsak.getSaksnummer() == null) ? null : fagsak.getSaksnummer().getVerdi();
        OpprettOppgaveRequest.Builder builder = OpprettOppgaveRequest.builder();

        return builder
            .medOpprettetAvEnhetId(Integer.parseInt(enhetsId))
            .medAnsvarligEnhetId(enhetsId)
            .medFagomradeKode(FagomradeKode.FOR.getKode())
            .medFnr(personinfo.getPersonIdent().getIdent())
            .medBrukerTypeKode(BrukerType.PERSON)
            .medAktivFra(LocalDate.now())
            .medAktivTil(helgeJustertFrist(LocalDate.now().plusDays(fristDager)))
            .medOppgavetypeKode(oppgaveÅrsak.getKode())
            .medSaksnummer(saksnummer) // Mer iht PK-38815
            .medPrioritetKode(prioritetKode.toString())
            .medBeskrivelse(beskrivelse)
            .medLest(false)
            .medUnderkategoriKode(underkategoriKode)
            .build();
    }

    private OpprettOppgave.Builder createRestRequestBuilder(Fagsak fagsak, AktørId aktørId, String enhet, String beskrivelse, Prioritet prioritet, int fristDager) {
        return OpprettOppgave.getBuilder()
            .medAktoerId(aktørId.getId())
            .medSaksreferanse(fagsak != null ? fagsak.getSaksnummer().getVerdi() : null)
            .medTildeltEnhetsnr(enhet)
            .medOpprettetAvEnhetsnr(enhet)
            .medAktivDato(LocalDate.now())
            .medFristFerdigstillelse(helgeJustertFrist(LocalDate.now().plusDays(fristDager)))
            .medBeskrivelse(beskrivelse)
            .medTemagruppe("FMLI")
            .medTema(Tema.FOR.getOffisiellKode())
            .medPrioritet(prioritet);
    }

    // Sett frist til mandag hvis fristen er i helgen.
    private LocalDate helgeJustertFrist(LocalDate dato) {
        if (dato.getDayOfWeek().getValue() > DayOfWeek.FRIDAY.getValue()) {
            return dato.plusDays(1L + DayOfWeek.SUNDAY.getValue() - dato.getDayOfWeek().getValue());
        }
        return dato;
    }
}
