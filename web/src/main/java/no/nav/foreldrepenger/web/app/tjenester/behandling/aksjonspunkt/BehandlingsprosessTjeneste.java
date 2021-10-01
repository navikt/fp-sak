package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBegrunnelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.FortsettBehandlingTask;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.ÅpneBehandlingForEndringerTask;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.web.app.tjenester.VurderProsessTaskStatusForPollingApi;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.util.LdapUtil;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.felles.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class BehandlingsprosessTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private String gruppenavnSaksbehandler;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private HistorikkRepository historikkRepository;

    BehandlingsprosessTjeneste() {
        // for CDI proxy
    }

    // test only
    BehandlingsprosessTjeneste(ProsesseringAsynkTjeneste prosesseringAsynkTjeneste) {
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
    }

    @Inject
    public BehandlingsprosessTjeneste(
                                                     BehandlingRepository behandlingRepository,
                                                     ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                                     BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                                     @KonfigVerdi(value = "bruker.gruppenavn.saksbehandler") String gruppenavnSaksbehandler,
                                                     HistorikkRepository historikkRepository) {

        Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.gruppenavnSaksbehandler = gruppenavnSaksbehandler;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.historikkRepository = historikkRepository;
    }

    /**
     * Kjører prosess, (henter ikke inn registeropplysninger på nytt selv)
     *
     * @return Prosess Task gruppenavn som kan brukes til å sjekke fremdrift
     */
    public void asynkKjørProsess(Behandling behandling) {
        if (behandlingProsesseringTjeneste.finnesTasksForPolling(behandling).isEmpty()) {
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        }
    }

    /**
     * Kjører prosess, (henter ikke inn registeropplysninger på nytt selv)
     *
     * @return Prosess Task gruppenavn som kan brukes til å sjekke fremdrift
     */
    public String asynkRegisteroppdateringKjørProsess(Behandling behandling) {
        behandlingProsesseringTjeneste.tvingInnhentingRegisteropplysninger(behandling);
        return asynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling);
    }

    /**
     * Kjør behandlingsprosess asynkront videre for nyopprettet behandling.
     *
     * @return ProsessTask gruppe
     */
    public String asynkStartBehandlingsprosess(Behandling behandling) {
        return prosesseringAsynkTjeneste.asynkStartBehandlingProsess(behandling);
    }

    /** Hvorvidt betingelser for å hente inn registeropplysninger på nytt er oppfylt. */
    private boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling) {
        var behandlingStatus = behandling.getStatus();
        return BehandlingStatus.UTREDES.equals(behandlingStatus)
            && !behandling.isBehandlingPåVent()
            && harRolleSaksbehandler()
            && behandlingProsesseringTjeneste.skalInnhenteRegisteropplysningerPåNytt(behandling);
    }

    /**
     * Betinget sjekk om innhent registeropplysninger (conditionally) og kjør prosess. Alt gjøres asynkront i form av prosess tasks.
     * Intern sjekk på om hvorvidt registeropplysninger må reinnhentes.
     *
     * @return optional Prosess Task gruppenavn som kan brukes til å sjekke fremdrift
     */
    public Optional<String> sjekkOgForberedAsynkInnhentingAvRegisteropplysningerOgKjørProsess(Behandling behandling) {
        if (!skalInnhenteRegisteropplysningerPåNytt(behandling)) {
            return Optional.empty();
        }
        // henter alltid registeropplysninger og kjører alltid prosess
        return Optional.of(asynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling));
    }

    /**
     * Innhent registeropplysninger og kjør prosess asynkront.
     *
     * @return Prosess Task gruppenavn som kan brukes til å sjekke fremdrift
     */
    private String asynkInnhentingAvRegisteropplysningerOgKjørProsess(Behandling behandling) {
        return behandlingProsesseringTjeneste.finnesTasksForPolling(behandling)
            .orElseGet(() -> {
                var gruppe = behandlingProsesseringTjeneste.lagOppdaterFortsettTasksForPolling(behandling);
                return prosesseringAsynkTjeneste.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), behandling.getId(), gruppe);
            });
    }

    /**
     * Gjenoppta behandling, start innhenting av registeropplysninger på nytt og kjør prosess hvis nødvendig.
     *
     * @return gruppenavn (prosesstask) hvis noe startet asynkront.
     */
    public Optional<String> gjenopptaBehandling(Behandling behandling) {
        opprettHistorikkinnslagForManueltGjenopptakelse(behandling, HistorikkinnslagType.BEH_MAN_GJEN);
        return Optional.of(asynkInnhentingAvRegisteropplysningerOgKjørProsess(behandling));
    }

    /** Sjekker om det pågår åpne prosess tasks (for angitt gruppe). Returnerer eventuelt task gruppe for eventuell åpen prosess task gruppe. */
    public Optional<AsyncPollingStatus> sjekkProsessTaskPågårForBehandling(Behandling behandling, String gruppe) {

        var behandlingId = behandling.getId();

        var nesteTask = prosesseringAsynkTjeneste.sjekkProsessTaskPågårForBehandling(behandling, gruppe);
        return new VurderProsessTaskStatusForPollingApi(behandlingId).sjekkStatusNesteProsessTask(gruppe, nesteTask);

    }

    private boolean harRolleSaksbehandler() {
        var ident = SubjectHandler.getSubjectHandler().getUid();
        var ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        var grupper = LdapUtil.filtrerGrupper(ldapBruker.getGroups());
        return grupper.contains(gruppenavnSaksbehandler);
    }

    public Behandling hentBehandling(Long behandlingsId) {
        return behandlingRepository.hentBehandling(behandlingsId);
    }

    public Behandling hentBehandling(UUID behandlingUuid) {
        return behandlingRepository.hentBehandling(behandlingUuid);
    }

    /**
     * Åpner behandlingen for endringer ved å reaktivere inaktive aksjonspunkter før startpunktet
     * og hopper til første startpunkt. Gjøres asynkront.
     *
     * @return ProsessTask gruppe
     */
    public String asynkTilbakestillOgÅpneBehandlingForEndringer(Behandling behandling) {
        if (behandlingProsesseringTjeneste.finnesTasksForPolling(behandling).isPresent()) {
            throw new FunksjonellException("FP-572345", "Finnes aktive tasks", "Vent til registeroppdatering er ferdig");
        }
        var gruppe = new ProsessTaskGruppe();

        var åpneBehandlingForEndringerTask = ProsessTaskData.forProsessTask(ÅpneBehandlingForEndringerTask.class);
        åpneBehandlingForEndringerTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        åpneBehandlingForEndringerTask.setCallIdFraEksisterende();
        gruppe.addNesteSekvensiell(åpneBehandlingForEndringerTask);
        var fortsettBehandlingTask = ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        fortsettBehandlingTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        fortsettBehandlingTask.setCallIdFraEksisterende();
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);

        opprettHistorikkinnslagForBehandlingStartetPåNytt(behandling);

        return prosesseringAsynkTjeneste.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), behandling.getId(), gruppe);
    }

    /**
     * På grunn av (nyinnført) async-prosessering videre nedover mister vi informasjon her om at det i dette tilfellet er saksbehandler som
     * ber om gjenopptakelse av behandlingen. Det kommer et historikkinnslag om dette (se
     * {@link no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent})
     * som eies av systembruker. Derfor velger vi her å legge på et innslag til med saksbehandler som eier slik at historikken blir korrekt.
     */
    private void opprettHistorikkinnslagForManueltGjenopptakelse(Behandling behandling,
                                                                 HistorikkinnslagType historikkinnslagType) {
        var builder = new HistorikkInnslagTekstBuilder();
        builder.medHendelse(historikkinnslagType);

        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    private void opprettHistorikkinnslagForBehandlingStartetPåNytt(Behandling behandling) {
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.BEH_STARTET_PÅ_NYTT);
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        var historikkInnslagTekstBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BEH_STARTET_PÅ_NYTT)
            .medBegrunnelse(HistorikkBegrunnelseType.BEH_STARTET_PA_NYTT);
        historikkInnslagTekstBuilder.build(historikkinnslag);
        historikkinnslag.setBehandling(behandling);
        historikkRepository.lagre(historikkinnslag);
    }
}
