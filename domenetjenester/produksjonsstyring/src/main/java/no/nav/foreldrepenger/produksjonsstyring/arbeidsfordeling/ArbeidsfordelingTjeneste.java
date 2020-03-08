package no.nav.foreldrepenger.produksjonsstyring.arbeidsfordeling;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.arbeidsfordeling.rest.ArbeidsfordelingRequest;
import no.nav.foreldrepenger.produksjonsstyring.arbeidsfordeling.rest.ArbeidsfordelingResponse;
import no.nav.foreldrepenger.produksjonsstyring.arbeidsfordeling.rest.ArbeidsfordelingRestKlient;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnAlleBehandlendeEnheterListeUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.binding.FinnBehandlendeEnhetListeUgyldigInput;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.ArbeidsfordelingKriterier;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Behandlingstema;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Behandlingstyper;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Diskresjonskoder;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Enhetsstatus;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Geografi;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Oppgavetyper;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Organisasjonsenhet;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Tema;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.informasjon.Temagrupper;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnAlleBehandlendeEnheterListeRequest;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnAlleBehandlendeEnheterListeResponse;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeRequest;
import no.nav.tjeneste.virksomhet.arbeidsfordeling.v1.meldinger.FinnBehandlendeEnhetListeResponse;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.klient.ArbeidsfordelingConsumer;

@Dependent
public class ArbeidsfordelingTjeneste {

    private static final OrganisasjonsEnhet KLAGE_ENHET =  new OrganisasjonsEnhet("4292", "NAV Klageinstans Midt-Norge");
    private static final Logger logger = LoggerFactory.getLogger(ArbeidsfordelingTjeneste.class);

    private static final String TEMAGRUPPE = "FMLI";//FMLI = Familie
    private static final String TEMA = "FOR"; //FOR = Foreldre- og svangerskapspenger
    private static final String OPPGAVETYPE = OppgaveÅrsak.BEHANDLE_SAK.getKode(); // BEH_SED = behandle sak
    private static final String BEHANDLINGTYPE = BehandlingType.REVURDERING.getOffisiellKode();

    private ArbeidsfordelingConsumer consumer;
    private ArbeidsfordelingRestKlient rest;

    @Inject
    public ArbeidsfordelingTjeneste(ArbeidsfordelingConsumer consumer, ArbeidsfordelingRestKlient rest) {
        this.consumer = consumer;
        this.rest = rest;
    }

    public OrganisasjonsEnhet finnBehandlendeEnhet(String geografiskTilknytning, String diskresjonskode, BehandlingTema behandlingTema) {
        FinnBehandlendeEnhetListeRequest request = lagRequestForHentBehandlendeEnhet(behandlingTema, diskresjonskode, geografiskTilknytning);

        try {
            FinnBehandlendeEnhetListeResponse response = consumer.finnBehandlendeEnhetListe(request);
            Organisasjonsenhet valgtEnhet = validerOgVelgBehandlendeEnhet(geografiskTilknytning, diskresjonskode, behandlingTema, response);
            var enhet = new OrganisasjonsEnhet(valgtEnhet.getEnhetId(), valgtEnhet.getEnhetNavn());
            restOgSammlign(List.of(enhet), geografiskTilknytning, diskresjonskode, behandlingTema);
            return enhet;
        } catch (FinnBehandlendeEnhetListeUgyldigInput e) {
            throw ArbeidsfordelingFeil.FACTORY.finnBehandlendeEnhetListeUgyldigInput(e).toException();
        }
    }

    public List<OrganisasjonsEnhet> finnAlleBehandlendeEnhetListe(BehandlingTema behandlingTema){
        // NORG2 og ruting diskriminerer på TEMA, for tiden ikke på BehandlingTEMA
        FinnAlleBehandlendeEnheterListeRequest request = lagRequestForHentAlleBehandlendeEnheter(behandlingTema);

        try {
            FinnAlleBehandlendeEnheterListeResponse response = consumer.finnAlleBehandlendeEnheterListe(request);
            var enheter = tilOrganisasjonsEnhetListe(response, behandlingTema, true);
            restOgSammlign(enheter, null, null, behandlingTema);
            return enheter;
        } catch (FinnAlleBehandlendeEnheterListeUgyldigInput e) {
            throw ArbeidsfordelingFeil.FACTORY.finnAlleBehandlendeEnheterListeUgyldigInput(e).toException();
        }
    }

    private FinnAlleBehandlendeEnheterListeRequest lagRequestForHentAlleBehandlendeEnheter(BehandlingTema behandlingTema){
        FinnAlleBehandlendeEnheterListeRequest request = new FinnAlleBehandlendeEnheterListeRequest();
        ArbeidsfordelingKriterier kriterier = new ArbeidsfordelingKriterier();

        Temagrupper temagruppe = new Temagrupper();
        temagruppe.setValue(TEMAGRUPPE);
        kriterier.setTemagruppe(temagruppe);

        Tema tema = new Tema();
        tema.setValue(TEMA);
        kriterier.setTema(tema);

        Behandlingstyper behandlingstyper = new Behandlingstyper();
        behandlingstyper.setValue(BEHANDLINGTYPE);
        kriterier.setBehandlingstype(behandlingstyper);

        Oppgavetyper oppgavetyper = new Oppgavetyper();
        oppgavetyper.setValue(OPPGAVETYPE);
        kriterier.setOppgavetype(oppgavetyper);

        if (!BehandlingTema.UDEFINERT.equals(behandlingTema)) {
            Behandlingstema behandlingstemaRequestObject = new Behandlingstema();
            behandlingstemaRequestObject.setValue(behandlingTema.getOffisiellKode());
            kriterier.setBehandlingstema(behandlingstemaRequestObject);
        }

        request.setArbeidsfordelingKriterier(kriterier);
        return request;
    }

    private FinnBehandlendeEnhetListeRequest lagRequestForHentBehandlendeEnhet(BehandlingTema behandlingTema, String diskresjonskode, String geografiskTilknytning) {
        FinnBehandlendeEnhetListeRequest request = new FinnBehandlendeEnhetListeRequest();
        ArbeidsfordelingKriterier kriterier = new ArbeidsfordelingKriterier();

        Temagrupper temagruppe = new Temagrupper();
        temagruppe.setValue(TEMAGRUPPE);
        kriterier.setTemagruppe(temagruppe);

        Tema tema = new Tema();
        tema.setValue(TEMA);
        kriterier.setTema(tema);

        Behandlingstyper behandlingstyper = new Behandlingstyper();
        behandlingstyper.setValue(BEHANDLINGTYPE);
        kriterier.setBehandlingstype(behandlingstyper);

        if (!BehandlingTema.UDEFINERT.equals(behandlingTema)) {
            Behandlingstema behandlingstemaRequestObject = new Behandlingstema();
            behandlingstemaRequestObject.setValue(behandlingTema.getOffisiellKode());
            kriterier.setBehandlingstema(behandlingstemaRequestObject);
        }

        if (diskresjonskode != null) {
            Diskresjonskoder diskresjonskoder = new Diskresjonskoder();
            diskresjonskoder.setValue(diskresjonskode);
            kriterier.setDiskresjonskode(diskresjonskoder);
        }

        if (geografiskTilknytning != null) {
            Geografi geografi = new Geografi();
            geografi.setValue(geografiskTilknytning);
            kriterier.setGeografiskTilknytning(geografi);
        }

        request.setArbeidsfordelingKriterier(kriterier);
        return request;
    }

    private Organisasjonsenhet validerOgVelgBehandlendeEnhet(String geografiskTilknytning, String diskresjonskode,
                                                             BehandlingTema behandlingTema, FinnBehandlendeEnhetListeResponse response) {
        List<Organisasjonsenhet> behandlendeEnheter = response.getBehandlendeEnhetListe();

        //Vi forventer å få én behandlende enhet.
        if (behandlendeEnheter == null || behandlendeEnheter.isEmpty()) {
            throw ArbeidsfordelingFeil.FACTORY.finnerIkkeBehandlendeEnhet(geografiskTilknytning, diskresjonskode, behandlingTema).toException();
        }

        //Vi forventer å få én behandlende enhet.
        Organisasjonsenhet valgtBehandlendeEnhet = behandlendeEnheter.get(0);
        if (behandlendeEnheter.size() > 1) {
            List<String> enheter = behandlendeEnheter.stream().map(Organisasjonsenhet::getEnhetId).collect(Collectors.toList());
            ArbeidsfordelingFeil.FACTORY.fikkFlereBehandlendeEnheter(geografiskTilknytning, diskresjonskode, behandlingTema, enheter,
                valgtBehandlendeEnhet.getEnhetId()).log(logger);
        }
        return valgtBehandlendeEnhet;
    }

    private List<OrganisasjonsEnhet> tilOrganisasjonsEnhetListe(FinnAlleBehandlendeEnheterListeResponse response,
                                                                BehandlingTema behandlingTema, boolean medKlage){
        List<Organisasjonsenhet> responsEnheter = response.getBehandlendeEnhetListe();

        if (responsEnheter == null || responsEnheter.isEmpty()) {
            throw ArbeidsfordelingFeil.FACTORY.finnerIkkeAlleBehandlendeEnheter( behandlingTema).toException();
        }

        List<OrganisasjonsEnhet> organisasjonsEnhetListe = responsEnheter.stream()
            .filter(e -> Enhetsstatus.AKTIV.equals(e.getStatus()))
            .map(responsOrgEnhet -> new OrganisasjonsEnhet(responsOrgEnhet.getEnhetId(), responsOrgEnhet.getEnhetNavn()))
            .collect(Collectors.toList());

        if (medKlage) {
            // Hardkodet inn for Klageinstans da den ikke kommer med i response fra NORG. Fjern dette når det er validert på plass.
            organisasjonsEnhetListe.add(KLAGE_ENHET);
        }

        return organisasjonsEnhetListe;
    }

    public OrganisasjonsEnhet hentEnhetForDiskresjonskode(String kode, BehandlingTema behandlingTema) {

        FinnBehandlendeEnhetListeRequest request = lagRequestForHentBehandlendeEnhet(behandlingTema, kode, null);

        try {
            FinnBehandlendeEnhetListeResponse response = consumer.finnBehandlendeEnhetListe(request);
            Organisasjonsenhet valgtEnhet = validerOgVelgBehandlendeEnhet(null, kode, behandlingTema, response);
            var enhet = new OrganisasjonsEnhet(valgtEnhet.getEnhetId(), valgtEnhet.getEnhetNavn());
            restOgSammlign(List.of(enhet), null, kode, behandlingTema);
            return enhet;
        } catch (FinnBehandlendeEnhetListeUgyldigInput e) {
            throw ArbeidsfordelingFeil.FACTORY.finnBehandlendeEnhetListeUgyldigInput(e).toException();
        }

    }

    public OrganisasjonsEnhet getKlageInstansEnhet() {
        return KLAGE_ENHET;
    }

    private void restOgSammlign(List<OrganisasjonsEnhet> enheter, String geografi, String diskresjon, BehandlingTema behandlingTema) {
        List<OrganisasjonsEnhet> wsEnheter = enheter.stream().filter(e -> !KLAGE_ENHET.getEnhetId().equals(e.getEnhetId())).collect(Collectors.toList());
        try {
            List<ArbeidsfordelingResponse> restenhet;
            var request = ArbeidsfordelingRequest.ny()
                .medTemagruppe(TEMAGRUPPE)
                .medTema(TEMA)
                .medOppgavetype(OPPGAVETYPE)
                .medBehandlingstype(BEHANDLINGTYPE)
                .medBehandlingstema(behandlingTema.getOffisiellKode())
                .medDiskresjonskode(diskresjon)
                .medGeografiskOmraade(geografi)
                .build();
            if (geografi == null && diskresjon == null) {
                restenhet = rest.hentAlleAktiveEnheter(request);
            } else {
                restenhet = rest.finnEnhet(request);
            }
            List<OrganisasjonsEnhet> mappedRest = restenhet.stream()
                .map(r -> new OrganisasjonsEnhet(r.getEnhetNr(), r.getEnhetNavn()))
                .collect(Collectors.toList());
            if (mappedRest.equals(wsEnheter)) {
                logger.info("NORG2 rest lik respons");
            } else {
                logger.info("NORG2 rest ulik respons: ws {} og rs {}", wsEnheter, restenhet);
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
