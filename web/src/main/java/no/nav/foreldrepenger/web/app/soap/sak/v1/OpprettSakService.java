package no.nav.foreldrepenger.web.app.soap.sak.v1;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.BehandleForeldrepengesakV1;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.feil.UgyldigInput;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.meldinger.OpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.meldinger.OpprettSakResponse;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.felles.integrasjon.felles.ws.SoapWebService;
import no.nav.vedtak.felles.integrasjon.felles.ws.VLFaultListenerUnntakKonfigurasjon;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ServiceType;

/**
 * Webservice for å opprette sak i VL ved manuelle journalføringsoppgaver.
 */

/*
 * @ Transaction HACK (uuh): Transaksjonsgrensen er for denne webservice'en
 * flyttet til javatjenesten OpprettGSakTjeneste Dette er ikke i henhold til
 * standard og kan ikke gjøres uten godkjenning fra sjefsarkitekt. Grunnen for
 * at det er gjort her er for å sikre at de tre kallene går i separate
 * transaksjoner. Se https://jira.adeo.no/browse/PKHUMLE-359 for detaljer.
 */
@Dependent
@WebService(wsdlLocation = "wsdl/no/nav/tjeneste/virksomhet/behandleForeldrepengesak/v1/behandleForeldrepengesak.wsdl", serviceName = "BehandleForeldrepengesak_v1", portName = "BehandleForeldrepengesak_v1Port", endpointInterface = "no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.BehandleForeldrepengesakV1")
@SoapWebService(endpoint = "/sak/opprettSak/v1", tjenesteBeskrivelseURL = "https://confluence.adeo.no/pages/viewpage.action?pageId=220529015")
public class OpprettSakService implements BehandleForeldrepengesakV1 {

    private static final Logger LOG = LoggerFactory.getLogger(OpprettSakService.class);

    private OpprettSakOrchestrator opprettSakOrchestrator;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    public OpprettSakService() {
        // cdi
    }

    @Inject
    public OpprettSakService(OpprettSakOrchestrator opprettSakOrchestrator,
                             DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.opprettSakOrchestrator = opprettSakOrchestrator;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    @Override
    public void ping() {
        LOG.debug("ping");
    }

    @Override
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.FAGSAK, serviceType = ServiceType.WEBSERVICE)
    public OpprettSakResponse opprettSak(
            @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) OpprettSakRequest opprettSakRequest)
            throws OpprettSakUgyldigInput {

        if (opprettSakRequest.getSakspart().getAktoerId() == null) {
            var faultInfo = lagUgyldigInput("AktørId", null);
            throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
        }
        var aktørId = new AktørId(opprettSakRequest.getSakspart().getAktoerId());
        var behandlingTema = hentBehandlingstema(opprettSakRequest.getBehandlingstema().getValue());
        var journalpostId = new JournalpostId(opprettSakRequest.getJournalpostId());
        validerJournalpostId(journalpostId, behandlingTema, aktørId);

        var saksnummer = opprettSakOrchestrator.opprettSak(journalpostId, behandlingTema, aktørId);

        return lagResponse(saksnummer);
    }

    private void validerJournalpostId(JournalpostId journalpostId, BehandlingTema behandlingTema, AktørId aktørId) throws OpprettSakUgyldigInput {

        var journalpost = dokumentArkivTjeneste.hentJournalpostForSak(journalpostId);
        var hoveddokument = journalpost.map(ArkivJournalPost::getHovedDokument);
        var journalpostYtelseType = FagsakYtelseType.UDEFINERT;
        if (hoveddokument.map(ArkivDokument::getDokumentType).filter(DokumentTypeId::erSøknadType).isPresent()) {
            var hovedtype = hoveddokument.map(ArkivDokument::getDokumentType).orElseThrow();
            journalpostYtelseType = switch (hovedtype) {
                case SØKNAD_ENGANGSSTØNAD_ADOPSJON -> FagsakYtelseType.ENGANGSTØNAD;
                case SØKNAD_ENGANGSSTØNAD_FØDSEL -> FagsakYtelseType.ENGANGSTØNAD;
                case SØKNAD_FORELDREPENGER_ADOPSJON -> FagsakYtelseType.FORELDREPENGER;
                case SØKNAD_FORELDREPENGER_FØDSEL -> FagsakYtelseType.FORELDREPENGER;
                case SØKNAD_SVANGERSKAPSPENGER ->  FagsakYtelseType.SVANGERSKAPSPENGER;
                default -> FagsakYtelseType.UDEFINERT;
            };
            if (behandlingTema.getFagsakYtelseType().equals(journalpostYtelseType)) return;
            if (journalpostYtelseType == null)
                return;
        } else if (hoveddokument.map(ArkivDokument::getDokumentType).filter(DokumentTypeId.INNTEKTSMELDING::equals).isPresent()) {
            var original = dokumentArkivTjeneste.hentStrukturertDokument(journalpostId, hoveddokument.map(ArkivDokument::getDokumentId).orElseThrow()).toLowerCase();
            if (original.contains("ytelse>foreldrepenger<")) {
                if (opprettSakOrchestrator.harAktivSak(aktørId, behandlingTema)) {
                    var faultInfo = lagUgyldigInput("Journalpost", "Inntektsmelding når det finnes åpen Foreldrepengesak");
                    throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
                }
                journalpostYtelseType = FagsakYtelseType.FORELDREPENGER;
            } else if (original.contains("ytelse>svangerskapspenger<")) {
                journalpostYtelseType = FagsakYtelseType.SVANGERSKAPSPENGER;
            }
        }
        LOG.info("FPSAK vurdering ytelsedok {} vs ytelseoppgitt {}", journalpostYtelseType, behandlingTema.getFagsakYtelseType());
        if (!behandlingTema.getFagsakYtelseType().equals(journalpostYtelseType)) {
            throw new FunksjonellException("FP-785356", "Dokument og valgt ytelsetype i uoverenstemmelse",
                "Velg ytelsetype som samstemmer med dokument");
        }
        if (FagsakYtelseType.UDEFINERT.equals(journalpostYtelseType)) {
            throw new FunksjonellException("FP-785354", "Kan ikke opprette sak basert på oppgitt dokument",
                "Journalføre dokument på annen sak");
        }
    }

    private BehandlingTema hentBehandlingstema(String behandlingstemaOffisiellKode) throws OpprettSakUgyldigInput {
        var behandlingTema = BehandlingTema.finnForKodeverkEiersKode(behandlingstemaOffisiellKode);
        if (BehandlingTema.UDEFINERT.equals(behandlingTema)) {
            var faultInfo = lagUgyldigInput("Behandlingstema", behandlingstemaOffisiellKode);
            throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
        }
        if (FagsakYtelseType.UDEFINERT.equals(behandlingTema.getFagsakYtelseType())) {
            var faultInfo = lagUgyldigInput("Behandlingstema", behandlingstemaOffisiellKode);
            throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
        }
        return behandlingTema;
    }

    private UgyldigInput lagUgyldigInput(String feltnavn, String value) {
        var faultInfo = new UgyldigInput();
        faultInfo.setFeilmelding(feltnavn + " med verdi " + (value != null ? value : "") + " er ugyldig input");
        faultInfo.setFeilaarsak("Ugyldig input");
        return faultInfo;
    }

    private OpprettSakResponse lagResponse(Saksnummer saksnummer) {
        var response = new OpprettSakResponse();
        response.setSakId(saksnummer.getVerdi());
        return response;
    }

    @ApplicationScoped
    public static class Unntak extends VLFaultListenerUnntakKonfigurasjon {
        public Unntak() {
            super(OpprettSakUgyldigInput.class);
        }
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (OpprettSakRequest) obj;
            var dataAttributter = AbacDataAttributter.opprett();
            if (req.getSakspart() != null) {
                dataAttributter = dataAttributter.leggTil(AppAbacAttributtType.AKTØR_ID, req.getSakspart().getAktoerId());
            }
            return dataAttributter;
        }
    }
}
