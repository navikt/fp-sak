package no.nav.foreldrepenger.web.app.soap.sak.v1;

import java.util.Set;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.arkiv.DokumentType;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.journal.JournalTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.BehandleForeldrepengesakV1;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.OpprettSakSakEksistererAllerede;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.OpprettSakSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.feil.UgyldigInput;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.meldinger.OpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.meldinger.OpprettSakResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.SoapWebService;
import no.nav.vedtak.felles.integrasjon.felles.ws.VLFaultListenerUnntakKonfigurasjon;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

/**
 * Webservice for å opprette sak i VL ved manuelle journalføringsoppgaver.
 */

/* @Transaction
 * HACK (u139158): Transaksjonsgrensen er for denne webservice'en flyttet til javatjenesten OpprettGSakTjeneste
 * Dette er ikke i henhold til standard og kan ikke gjøres uten godkjenning fra sjefsarkitekt.
 * Grunnen for at det er gjort her er for å sikre at de tre kallene går i separate transaksjoner.
 * Se https://jira.adeo.no/browse/PKHUMLE-359 for detaljer.
 */
@Dependent
@WebService(
    wsdlLocation = "wsdl/no/nav/tjeneste/virksomhet/behandleForeldrepengesak/v1/behandleForeldrepengesak.wsdl",
    serviceName = "BehandleForeldrepengesak_v1",
    portName = "BehandleForeldrepengesak_v1Port",
    endpointInterface = "no.nav.tjeneste.virksomhet.behandleforeldrepengesak.v1.binding.BehandleForeldrepengesakV1")
@SoapWebService(endpoint = "/sak/opprettSak/v1", tjenesteBeskrivelseURL = "https://confluence.adeo.no/pages/viewpage.action?pageId=220529015")
public class OpprettSakService implements BehandleForeldrepengesakV1 {

    private static final Logger logger = LoggerFactory.getLogger(OpprettSakService.class);

    private OpprettSakOrchestrator opprettSakOrchestrator;
    private JournalTjeneste journalTjeneste;
    private FpfordelRestKlient fordelKlient;

    public OpprettSakService() {
        // NOSONAR: cdi
    }

    @Inject
    public OpprettSakService(OpprettSakOrchestrator opprettSakOrchestrator,
                             JournalTjeneste journalTjeneste,
                             FpfordelRestKlient fordelKlient) {
        this.opprettSakOrchestrator = opprettSakOrchestrator;
        this.journalTjeneste = journalTjeneste;
        this.fordelKlient = fordelKlient;
    }

    @Override
    public void ping() {
        logger.debug("ping");
    }

    @Override
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, ressurs = BeskyttetRessursResourceAttributt.FAGSAK)
    public OpprettSakResponse opprettSak(
        @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) OpprettSakRequest opprettSakRequest)
        throws OpprettSakSakEksistererAllerede, OpprettSakSikkerhetsbegrensning, OpprettSakUgyldigInput {

        if (opprettSakRequest.getSakspart().getAktoerId() == null) {
            UgyldigInput faultInfo = lagUgyldigInput("AktørId", null);
            throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
        }
        AktørId aktørId = new AktørId(opprettSakRequest.getSakspart().getAktoerId());
        BehandlingTema behandlingTema = hentBehandlingstema(opprettSakRequest.getBehandlingstema().getValue());
        validerJournalpostId(opprettSakRequest.getJournalpostId(), behandlingTema, aktørId);
        JournalpostId journalpostId = new JournalpostId(opprettSakRequest.getJournalpostId());

        Saksnummer saksnummer = opprettSakOrchestrator.opprettSak(journalpostId, behandlingTema, aktørId);

        return lagResponse(saksnummer);
    }

    private void validerJournalpostId(String journalpostId, BehandlingTema behandlingTema, AktørId aktørId) throws OpprettSakUgyldigInput {
        final String feltnavnJournalpostId = "JournalpostId";
        if (!JournalpostId.erGyldig(journalpostId)) {
            UgyldigInput faultInfo = lagUgyldigInput(feltnavnJournalpostId, journalpostId);
            throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
        }

        var jpostId = new JournalpostId(journalpostId);
        try {
            FagsakYtelseType ytelsefraDokument = fordelKlient.utledYtelestypeFor(jpostId);
            logger.info("FPSAK vurdering FPFORDEL ytelsedok {} vs ytelseoppgitt {}", ytelsefraDokument, behandlingTema.getFagsakYtelseType());
            if (ytelsefraDokument.equals(behandlingTema.getFagsakYtelseType()))
                return;
            var ønsket = BehandlingTema.fraFagsakHendelse(behandlingTema.getFagsakYtelseType(), null);
            Boolean kanOpprette = fordelKlient.kanOppretteSakFra(jpostId, ønsket, opprettSakOrchestrator.aktiveBehandlingTema(aktørId));
            logger.info("FPSAK vurdering FPFORDEL er {} opprette", kanOpprette ? "kan" : "kan ikke");
            if (kanOpprette)
                return;
        } catch (Exception e) {
            logger.info("FPSAK vurdering FPFORDEL - noe gikk galt", e);
        }
        // Hindre at man oppretter sak basert på en klage - de skal journalføres på eksisterende sak

        ArkivJournalPost arkivJournalPost = journalTjeneste.hentInngåendeJournalpostHoveddokument(jpostId);
        if (arkivJournalPost == null || arkivJournalPost.getHovedDokument() == null) {
            throw OpprettSakServiceFeil.FACTORY.ikkeStøttetKunVedlegg().toException();
        }
        ArkivDokument dokument = arkivJournalPost.getHovedDokument();
        DokumentType dokumentTypeId = dokument.getDokumentType();
        validerIkkeSpesifiktForbudt(dokument, dokumentTypeId);
        if (DokumentTypeId.INNTEKTSMELDING.getKode().equals(dokumentTypeId.getKode())) {
            if (opprettSakOrchestrator.harAktivSak(aktørId, behandlingTema)) {
                throw OpprettSakServiceFeil.FACTORY.ikkeStøttetDokumentType(dokumentTypeId.getKode()).toException();
            }
            // Burde hatt sjekk på om valgt ytelsetype stemmer med im.ytelse. Dette sjekkes først ved journalføringservice
            return;
        }
        // Herfra og ned bør man kanskje hindre saksoppretting hvis det finnes en løpende eller åpen sak. Må vurderes ift SVP og erfaringer fra innstramming
        if (DokumentTypeId.erSøknadType(dokumentTypeId)) {
            if (!matchBehandlingtemaDokumenttypeSøknad(dokumentTypeId, behandlingTema)) {
                throw OpprettSakServiceFeil.FACTORY.inkonsistensTemaVsDokument(dokumentTypeId.getKode()).toException();
            }
            return;
        }
        if (DokumentKategori.SØKNAD.equals(dokument.getDokumentKategori())) {
            return;
        }
        throw OpprettSakServiceFeil.FACTORY.ikkeStøttetDokumentType(dokumentTypeId.getKode()).toException();
    }

    private void validerIkkeSpesifiktForbudt(ArkivDokument dokument, DokumentType dokumentTypeId) {
        if (DokumentTypeId.KLAGE_DOKUMENT.getKode().equals(dokumentTypeId.getKode()) || DokumentKategori.KLAGE_ELLER_ANKE.equals(dokument.getDokumentKategori())) {
            throw OpprettSakServiceFeil.FACTORY.ikkeStøttetDokumentType(dokumentTypeId.getKode()).toException();
        }
        if (DokumentTypeId.erEndringsSøknadType(dokumentTypeId)) {
            throw OpprettSakServiceFeil.FACTORY.ikkeStøttetDokumentType(dokumentTypeId.getKode()).toException();
        }
    }

    private BehandlingTema hentBehandlingstema(String behandlingstemaOffisiellKode) throws OpprettSakUgyldigInput {
        BehandlingTema behandlingTema = null;
        if (behandlingstemaOffisiellKode != null) {
            behandlingTema = BehandlingTema.finnForKodeverkEiersKode(behandlingstemaOffisiellKode);
        }
        if (behandlingTema == null || BehandlingTema.UDEFINERT.equals(behandlingTema)) {
            UgyldigInput faultInfo = lagUgyldigInput("Behandlingstema", behandlingstemaOffisiellKode);
            throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
        }
        if (FagsakYtelseType.UDEFINERT.equals(behandlingTema.getFagsakYtelseType())) {
            UgyldigInput faultInfo = lagUgyldigInput("Behandlingstema", behandlingstemaOffisiellKode);
            throw new OpprettSakUgyldigInput(faultInfo.getFeilmelding(), faultInfo);
        }
        return behandlingTema;
    }

    private boolean matchBehandlingtemaDokumenttypeSøknad(DokumentType dokumentTypeId, BehandlingTema behandlingTema) {

        DokumentTypeId dokId = DokumentTypeId.fraKode(dokumentTypeId.getKode());  // konverterer for
        if (BehandlingTema.gjelderEngangsstønad(behandlingTema) && Set.of(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL, DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON).contains(dokId)) {
            return true;
        }
        if (BehandlingTema.gjelderForeldrepenger(behandlingTema) && Set.of(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL, DokumentTypeId.SØKNAD_FORELDREPENGER_ADOPSJON).contains(dokId)) {
            return true;
        }
        return BehandlingTema.gjelderSvangerskapspenger(behandlingTema) && DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER.equals(dokId);
    }

    private UgyldigInput lagUgyldigInput(String feltnavn, String value) {
        UgyldigInput faultInfo = new UgyldigInput();
        faultInfo.setFeilmelding(feltnavn + " med verdi " + (value != null ? value : "") + " er ugyldig input");
        faultInfo.setFeilaarsak("Ugyldig input");
        return faultInfo;
    }

    private OpprettSakResponse lagResponse(Saksnummer saksnummer) {
        OpprettSakResponse response = new OpprettSakResponse();
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
            OpprettSakRequest req = (OpprettSakRequest) obj;
            AbacDataAttributter dataAttributter = AbacDataAttributter.opprett();
            if (req.getSakspart() != null) {
                dataAttributter = dataAttributter.leggTil(AppAbacAttributtType.AKTØR_ID, req.getSakspart().getAktoerId());
            }
            return dataAttributter;
        }
    }
}
