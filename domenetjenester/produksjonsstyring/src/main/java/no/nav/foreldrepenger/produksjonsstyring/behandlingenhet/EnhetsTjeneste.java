package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.Temagrupper;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.nom.SkjermetPersonKlient;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.Arbeidsfordeling;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.ArbeidsfordelingRequest;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.ArbeidsfordelingResponse;

@ApplicationScoped
public class EnhetsTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(EnhetsTjeneste.class);

    private static final String TEMAGRUPPE = Temagrupper.FAMILIEYTELSER.getOffisiellKode(); // Kodeverk Temagrupper - dekker FOR + OMS
    private static final String TEMA = Tema.FOR.getOffisiellKode(); // Kodeverk Tema
    private static final String OPPGAVETYPE = "BEH_SAK"; // Kodeverk Oppgavetype - NFP , uten spesialenheter
    private static final String ENHET_TYPE_NFP = "FPY";  // Kodeverk EnhetstyperNORG - NFP , uten spesialenheter (alt dropp behtype og filter på denne)
    private static final String BEHANDLINGTYPE = BehandlingType.FØRSTEGANGSSØKNAD.getOffisiellKode(); // Kodeverk Behandlingstype, bruker søknad
    private static final String NK_ENHET_ID = "4292"; // Klageinstans
    private static final String EA_ENHET_ID = "4883"; // Egne ansatte mfl
    private static final String SF_ENHET_ID = "2103"; // Adressesperre
    private static final String UT_ENHET_ID = "4806"; // Utlandsenhet
    private static final Set<String> SPESIALENHETER = Set.of(NK_ENHET_ID, EA_ENHET_ID, SF_ENHET_ID); // Ta med UT_ENHET_ID ved nasjonal kø

    private static final OrganisasjonsEnhet KLAGE_ENHET =  new OrganisasjonsEnhet(NK_ENHET_ID, "NAV Klageinstans Midt-Norge");
    private static final OrganisasjonsEnhet SKJERMET_ENHET =  new OrganisasjonsEnhet(EA_ENHET_ID, "NAV Familie- og pensjonsytelser Egne ansatte");
    private static final OrganisasjonsEnhet UTLAND_ENHET =  new OrganisasjonsEnhet(UT_ENHET_ID, "NAV Familie- og pensjonsytelser Drammen");
    private static final OrganisasjonsEnhet KODE6_ENHET = new OrganisasjonsEnhet(SF_ENHET_ID, "NAV Vikafossen");

    private PersoninfoAdapter personinfoAdapter;
    private Arbeidsfordeling norgRest;
    private SkjermetPersonKlient skjermetPersonKlient;

    private LocalDate sisteInnhenting = LocalDate.MIN;
    private List<OrganisasjonsEnhet> alleBehandlendeEnheter = new ArrayList<>();

    public EnhetsTjeneste() {
        // For CDI proxy
    }

    @Inject
    public EnhetsTjeneste(PersoninfoAdapter personinfoAdapter,
                          Arbeidsfordeling arbeidsfordelingRestKlient,
                          SkjermetPersonKlient skjermetPersonKlient) {
        this.personinfoAdapter = personinfoAdapter;
        this.norgRest = arbeidsfordelingRestKlient;
        this.skjermetPersonKlient = skjermetPersonKlient;
    }


    List<OrganisasjonsEnhet> hentEnhetListe() {
        oppdaterEnhetCache();
        return alleBehandlendeEnheter;
    }

    OrganisasjonsEnhet hentEnhetSjekkKunAktør(AktørId aktørId, BehandlingTema behandlingTema) {
        oppdaterEnhetCache();
        if (harNoenDiskresjonskode6(Set.of(aktørId))) {
            return KODE6_ENHET;
        } else if (erNoenSkjermetPerson(Set.of(aktørId))) {
            return SKJERMET_ENHET;
        } else {
            var geografiskTilknytning = personinfoAdapter.hentGeografiskTilknytning(aktørId);
            if (geografiskTilknytning == null) {
                return UTLAND_ENHET;
            }
            var enheter = hentEnheterFor(geografiskTilknytning, behandlingTema);
            return enheter.isEmpty() ? tilfeldigEnhet() : enheter.get(0);
        }
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkOppgittePersoner(String enhetId, BehandlingTema behandlingTema, AktørId hovedAktør, Set<AktørId> alleAktører) {
        oppdaterEnhetCache();
        if (SPESIALENHETER.contains(enhetId)) {
            return Optional.empty();
        }
        if (harNoenDiskresjonskode6(alleAktører)) {
            return Optional.of(KODE6_ENHET);
        }
        if (erNoenSkjermetPerson(alleAktører)) {
            LOG.info("FPSAK enhettjeneste skjermet person funnet");
            return Optional.of(SKJERMET_ENHET);
        }
        if (finnOrganisasjonsEnhet(enhetId).isEmpty()) {
            return Optional.of(hentEnhetSjekkKunAktør(hovedAktør, behandlingTema));
        }
        return Optional.empty();
    }

    private boolean harNoenDiskresjonskode6(Set<AktørId> aktører) {
        return aktører.stream()
            .map(personinfoAdapter::hentDiskresjonskode)
            .anyMatch(Diskresjonskode.KODE6::equals);
    }

    private boolean erNoenSkjermetPerson(Set<AktørId> aktører) {
        return aktører.stream()
            .map(a -> personinfoAdapter.hentFnr(a))
            .flatMap(Optional::stream)
            .anyMatch(p -> skjermetPersonKlient.erSkjermet(p.getIdent()));
    }

    private void oppdaterEnhetCache() {
        if (sisteInnhenting.isBefore(LocalDate.now())) {
            alleBehandlendeEnheter.clear();
            alleBehandlendeEnheter.addAll(hentEnheterFor(null, BehandlingTema.FORELDREPENGER));
            alleBehandlendeEnheter.add(SKJERMET_ENHET);
            alleBehandlendeEnheter.add(KLAGE_ENHET);
            alleBehandlendeEnheter.add(KODE6_ENHET);
            sisteInnhenting = LocalDate.now();
        }
    }

    private OrganisasjonsEnhet tilfeldigEnhet() {
        oppdaterEnhetCache();
        var kanvelges = alleBehandlendeEnheter.stream().filter(e -> !SPESIALENHETER.contains(e.enhetId())).toList();
        if (kanvelges.isEmpty()) {
            throw new IllegalStateException("Ingen enheter å velge mellom");
        }
        return kanvelges.get(LocalDateTime.now().getNano() % kanvelges.size());
    }

    Optional<OrganisasjonsEnhet> finnOrganisasjonsEnhet(String enhetId) {
        oppdaterEnhetCache();
        return alleBehandlendeEnheter.stream().filter(e -> enhetId.equals(e.enhetId())).findFirst();
    }

    OrganisasjonsEnhet enhetsPresedens(OrganisasjonsEnhet enhetSak1, OrganisasjonsEnhet enhetSak2) {
        oppdaterEnhetCache();
        if (KODE6_ENHET.enhetId().equals(enhetSak1.enhetId()) || KODE6_ENHET.enhetId().equals(enhetSak2.enhetId())) {
            return KODE6_ENHET;
        }
        if (SKJERMET_ENHET.enhetId().equals(enhetSak1.enhetId()) || SKJERMET_ENHET.enhetId().equals(enhetSak2.enhetId())) {
            return SKJERMET_ENHET;
        }
        if (UTLAND_ENHET.enhetId().equals(enhetSak1.enhetId()) || UTLAND_ENHET.enhetId().equals(enhetSak2.enhetId())) {
            return UTLAND_ENHET;
        }
        return enhetSak1;
    }

    static OrganisasjonsEnhet getEnhetKlage() {
        return KLAGE_ENHET;
    }

    static OrganisasjonsEnhet getEnhetUtland() {
        return UTLAND_ENHET;
    }

    private List<OrganisasjonsEnhet> hentEnheterFor(String geografi, BehandlingTema behandlingTema) {
        var brukBTema = BehandlingTema.UDEFINERT.equals(behandlingTema) ? BehandlingTema.FORELDREPENGER : behandlingTema;
        List<ArbeidsfordelingResponse> restenhet;
        var request = ArbeidsfordelingRequest.ny()
            .medTemagruppe(TEMAGRUPPE)
            .medTema(TEMA)
            .medOppgavetype(OPPGAVETYPE)
            .medBehandlingstype(BEHANDLINGTYPE)
            .medBehandlingstema(brukBTema.getOffisiellKode())
            .medDiskresjonskode(null)
            .medGeografiskOmraade(geografi)
            .build();
        if (geografi == null) {
            restenhet = norgRest.hentAlleAktiveEnheter(request);
        } else {
            restenhet = norgRest.finnEnhet(request);
        }
        return restenhet.stream()
            .filter(r -> !SPESIALENHETER.contains(r.enhetNr()))
            .map(r -> new OrganisasjonsEnhet(r.enhetNr(), r.enhetNavn()))
            .toList();
    }

}
