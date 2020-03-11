package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRequest;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingResponse;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRestKlient;

@ApplicationScoped
public class EnhetsTjeneste {

    private static final String TEMAGRUPPE = "FMLI"; // Kodeverk Temagrupper - dekker FOR + OMS
    private static final String TEMA = Tema.FOR.getOffisiellKode(); // Kodeverk Tema
    private static final String OPPGAVETYPE = OppgaveÅrsak.BEHANDLE_SAK.getKode(); // Kodeverk Oppgavetype - NFP , uten spesialenheter
    private static final String ENHET_TYPE_NFP = "FPY"; // NOSONAR Kodeverk EnhetstyperNORG - NFP , uten spesialenheter (alt dropp behtype og filter på denne)
    private static final String DISKRESJON_K6 = Diskresjonskode.KODE6.getOffisiellKode(); // Kodeverk Diskresjonskoder
    private static final String BEHANDLINGTYPE = BehandlingType.FØRSTEGANGSSØKNAD.getOffisiellKode(); // Kodeverk Behandlingstype, bruker søknad
    private static final String NK_ENHET_ID = "4292";

    private static final OrganisasjonsEnhet KLAGE_ENHET =  new OrganisasjonsEnhet(NK_ENHET_ID, "NAV Klageinstans Midt-Norge");

    private static final Logger logger = LoggerFactory.getLogger(EnhetsTjeneste.class);

    private TpsTjeneste tpsTjeneste;
    private ArbeidsfordelingRestKlient norgRest;

    private LocalDate sisteInnhenting = LocalDate.MIN;
    private OrganisasjonsEnhet enhetKode6;
    private List<OrganisasjonsEnhet> alleBehandlendeEnheter = new ArrayList<>();

    public EnhetsTjeneste() {
        // For CDI proxy
    }

    @Inject
    public EnhetsTjeneste(TpsTjeneste tpsTjeneste,
                          ArbeidsfordelingRestKlient arbeidsfordelingRestKlient) {
        this.tpsTjeneste = tpsTjeneste;
        this.norgRest = arbeidsfordelingRestKlient;
    }


    List<OrganisasjonsEnhet> hentEnhetListe() {
        oppdaterEnhetCache();
        return alleBehandlendeEnheter;
    }

    OrganisasjonsEnhet hentEnhetSjekkRegistrerteRelasjoner(AktørId aktørId, BehandlingTema behandlingTema) {
        oppdaterEnhetCache();
        PersonIdent fnr = tpsTjeneste.hentFnrForAktør(aktørId);

        GeografiskTilknytning geografiskTilknytning = tpsTjeneste.hentGeografiskTilknytning(fnr);
        String aktivDiskresjonskode = geografiskTilknytning.getDiskresjonskode();
        if (!DISKRESJON_K6.equals(aktivDiskresjonskode)) {
            boolean relasjonMedK6 = tpsTjeneste.hentDiskresjonskoderForFamilierelasjoner(fnr).stream()
                .anyMatch(geo -> DISKRESJON_K6.equals(geo.getDiskresjonskode()));
            if (relasjonMedK6) {
                aktivDiskresjonskode = DISKRESJON_K6;
            }
        }

        return hentEnheterFor(geografiskTilknytning.getTilknytning(), aktivDiskresjonskode, behandlingTema).get(0);
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkOppgitte(String enhetId, List<AktørId> relaterteAktører) {
        oppdaterEnhetCache();
        if (enhetKode6.getEnhetId().equals(enhetId) || NK_ENHET_ID.equals(enhetId)) {
            return Optional.empty();
        }

        return sjekkSpesifiserteRelaterte(relaterteAktører);
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkRegistrerteRelasjoner(String enhetId, BehandlingTema behandlingTema, AktørId aktørId, Optional<AktørId> kobletAktørId, List<AktørId> relaterteAktører) {
        oppdaterEnhetCache();
        if (enhetKode6.getEnhetId().equals(enhetId) || NK_ENHET_ID.equals(enhetId)) {
            return Optional.empty();
        }

        OrganisasjonsEnhet enhet = hentEnhetSjekkRegistrerteRelasjoner(aktørId, behandlingTema);
        if (enhetKode6.getEnhetId().equals(enhet.getEnhetId())) {
            return Optional.of(enhetKode6);
        }
        if (kobletAktørId.isPresent()) {
            OrganisasjonsEnhet enhetKoblet = hentEnhetSjekkRegistrerteRelasjoner(kobletAktørId.get(), behandlingTema);
            if (enhetKode6.getEnhetId().equals(enhetKoblet.getEnhetId())) {
                return Optional.of(enhetKode6);
            }
        }
        if (sjekkSpesifiserteRelaterte(relaterteAktører).isPresent()) {
            return Optional.of(enhetKode6);
        }
        if (!gyldigEnhetId(enhetId)) {
            return Optional.of(enhet);
        }

        return Optional.empty();
    }

    private Optional<OrganisasjonsEnhet> sjekkSpesifiserteRelaterte(List<AktørId> relaterteAktører) {
        for (AktørId relatert : relaterteAktører) {
            PersonIdent personIdent = tpsTjeneste.hentFnrForAktør(relatert);
            GeografiskTilknytning geo = tpsTjeneste.hentGeografiskTilknytning(personIdent);
            if (DISKRESJON_K6.equals(geo.getDiskresjonskode())) {
                return Optional.of(enhetKode6);
            }
        }
        return Optional.empty();
    }

    private void oppdaterEnhetCache() {
        if (sisteInnhenting.isBefore(LocalDate.now())) {
            enhetKode6 = hentEnheterFor(null, DISKRESJON_K6, BehandlingTema.UDEFINERT).get(0);
            alleBehandlendeEnheter.clear();
            alleBehandlendeEnheter.addAll(hentEnheterFor(null, null, BehandlingTema.UDEFINERT));
            alleBehandlendeEnheter.add(KLAGE_ENHET);
            sisteInnhenting = LocalDate.now();
        }
    }

    private boolean gyldigEnhetId(String enhetId) {
        return finnOrganisasjonsEnhet(enhetId).isPresent();
    }

    Optional<OrganisasjonsEnhet> finnOrganisasjonsEnhet(String enhetId) {
        oppdaterEnhetCache();
        return alleBehandlendeEnheter.stream().filter(e -> enhetId.equals(e.getEnhetId())).findFirst();
    }

    OrganisasjonsEnhet enhetsPresedens(OrganisasjonsEnhet enhetSak1, OrganisasjonsEnhet enhetSak2, boolean arverKlage) {
        oppdaterEnhetCache();
        if (arverKlage && NK_ENHET_ID.equals(enhetSak1.getEnhetId())) {
            return enhetSak1;
        }
        if (enhetKode6.getEnhetId().equals(enhetSak1.getEnhetId()) || enhetKode6.getEnhetId().equals(enhetSak2.getEnhetId())) {
            return enhetKode6;
        }
        return enhetSak1;
    }

    OrganisasjonsEnhet getEnhetKlage() {
        oppdaterEnhetCache();
        return KLAGE_ENHET;
    }

    private List<OrganisasjonsEnhet> hentEnheterFor(String geografi, String diskresjon, BehandlingTema behandlingTema) {
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
            restenhet = norgRest.hentAlleAktiveEnheter(request);
        } else {
            restenhet = norgRest.finnEnhet(request);
        }
        return restenhet.stream()
            .map(r -> new OrganisasjonsEnhet(r.getEnhetNr(), r.getEnhetNavn()))
            .collect(Collectors.toList());
    }

}
