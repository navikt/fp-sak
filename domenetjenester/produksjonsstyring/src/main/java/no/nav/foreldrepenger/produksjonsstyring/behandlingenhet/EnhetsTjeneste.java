package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import java.util.List;
import java.util.Map;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
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
    private static final String SK_ENHET_ID = "4817"; // Sammensatt kontroll
    private static final String NY_ENHET_ID = "4867"; // Nasjonal enhet
    private static final Set<String> SPESIALENHETER = Set.of(NK_ENHET_ID, EA_ENHET_ID, SF_ENHET_ID);

    private static final OrganisasjonsEnhet KLAGE_ENHET =  new OrganisasjonsEnhet(NK_ENHET_ID, "NAV Klageinstans Midt-Norge");
    private static final OrganisasjonsEnhet SKJERMET_ENHET =  new OrganisasjonsEnhet(EA_ENHET_ID, "NAV Familie- og pensjonsytelser Egne ansatte");
    private static final OrganisasjonsEnhet UTLAND_ENHET =  new OrganisasjonsEnhet(UT_ENHET_ID, "NAV Familie- og pensjonsytelser Drammen");
    private static final OrganisasjonsEnhet KONTROLL_ENHET =  new OrganisasjonsEnhet(SK_ENHET_ID, "NAV Familie- og pensjonsytelser Steinkjer");
    private static final OrganisasjonsEnhet KODE6_ENHET = new OrganisasjonsEnhet(SF_ENHET_ID, "NAV Vikafossen");
    private static final OrganisasjonsEnhet NASJONAL_ENHET = new OrganisasjonsEnhet(NY_ENHET_ID, "NAV Familie- og pensjonsytelser Foreldrepenger");

    private static final OrganisasjonsEnhet DRAMMEN =  new OrganisasjonsEnhet("4806", "NAV Familie- og pensjonsytelser Drammen");
    private static final OrganisasjonsEnhet BERGEN =  new OrganisasjonsEnhet("4812", "NAV Familie- og pensjonsytelser Bergen");
    private static final OrganisasjonsEnhet STEINKJER =  new OrganisasjonsEnhet("4817", "NAV Familie- og pensjonsytelser Steinkjer");
    private static final OrganisasjonsEnhet OSLO =  new OrganisasjonsEnhet("4833", "NAV Familie- og pensjonsytelser Oslo 1");
    private static final OrganisasjonsEnhet STORD =  new OrganisasjonsEnhet("4842", "NAV Familie- og pensjonsytelser Stord");
    private static final OrganisasjonsEnhet TROMSØ =  new OrganisasjonsEnhet("4849", "NAV Familie- og pensjonsytelser Tromsø");

    // Oppdateres etterhvert som flytteprosessen foregår
    private static final Map<String, OrganisasjonsEnhet> FLYTTE_MAP = Map.ofEntries(
        Map.entry(NASJONAL_ENHET.enhetId(), NASJONAL_ENHET),
        Map.entry(KLAGE_ENHET.enhetId(), KLAGE_ENHET),
        Map.entry(SKJERMET_ENHET.enhetId(), SKJERMET_ENHET),
        Map.entry(KODE6_ENHET.enhetId(), KODE6_ENHET),
        Map.entry(DRAMMEN.enhetId(), NASJONAL_ENHET),
        Map.entry(BERGEN.enhetId(), BERGEN),
        Map.entry(STEINKJER.enhetId(), STEINKJER),
        Map.entry(OSLO.enhetId(), NASJONAL_ENHET),
        Map.entry(STORD.enhetId(), STORD),
        Map.entry(TROMSØ.enhetId(), NASJONAL_ENHET),
        Map.entry("4802", NASJONAL_ENHET),
        Map.entry("4847", NASJONAL_ENHET),
        Map.entry("4205", KLAGE_ENHET)
    );

    private static final Set<OrganisasjonsEnhet> ALLEBEHANDLENDEENHETER = Set.of(NASJONAL_ENHET, DRAMMEN, BERGEN, STEINKJER ,OSLO, STORD, TROMSØ, KLAGE_ENHET, SKJERMET_ENHET, KODE6_ENHET);

    private static final Set<OrganisasjonsEnhet> IKKE_MENY = Set.of(KLAGE_ENHET, DRAMMEN, OSLO, TROMSØ);

    private PersoninfoAdapter personinfoAdapter;
    private Arbeidsfordeling norgRest;
    private SkjermetPersonKlient skjermetPersonKlient;

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

    static OrganisasjonsEnhet velgEnhet(OrganisasjonsEnhet enhet, FagsakMarkering markering) {
        return enhet != null ? velgEnhet(enhet.enhetId(), markering) : null;
    }

    static OrganisasjonsEnhet velgEnhet(String enhetId, FagsakMarkering markering) {
        if (enhetId == null) {
            return null;
        }
        if (SPESIALENHETER.contains(enhetId)) {
            return FLYTTE_MAP.get(enhetId);
        }
        if (FagsakMarkering.BOSATT_UTLAND.equals(markering)) {
            return UTLAND_ENHET;
        }
        if (FagsakMarkering.SAMMENSATT_KONTROLL.equals(markering)) {
            return KONTROLL_ENHET;
        }
        return Optional.ofNullable(FLYTTE_MAP.get(enhetId)).orElse(NASJONAL_ENHET);
    }

    static List<OrganisasjonsEnhet> hentEnhetListe() {
        return ALLEBEHANDLENDEENHETER.stream().filter(e -> !IKKE_MENY.contains(e)).toList();
    }

    OrganisasjonsEnhet hentEnhetSjekkKunAktør(AktørId aktørId, BehandlingTema behandlingTema) {
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
            return enheter.isEmpty() ? NASJONAL_ENHET : velgEnhet(enheter.get(0), null);
        }
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkOppgittePersoner(String enhetId, BehandlingTema behandlingTema, AktørId hovedAktør,
                                                                    Set<AktørId> alleAktører, FagsakMarkering saksmarkering) {
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
        if (FagsakMarkering.BOSATT_UTLAND.equals(saksmarkering)) {
            return !UTLAND_ENHET.enhetId().equals(enhetId) ? Optional.of(UTLAND_ENHET) : Optional.empty();
        }
        if (FagsakMarkering.SAMMENSATT_KONTROLL.equals(saksmarkering)) {
            return !KONTROLL_ENHET.enhetId().equals(enhetId) ? Optional.of(KONTROLL_ENHET) : Optional.empty();
        }
        if (FLYTTE_MAP.get(enhetId) == null) {
            return Optional.of(hentEnhetSjekkKunAktør(hovedAktør, behandlingTema));
        }
        return Optional.of(FLYTTE_MAP.get(enhetId)).filter(ny -> !ny.enhetId().equals(enhetId));
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

    static OrganisasjonsEnhet enhetsPresedens(OrganisasjonsEnhet enhetSak1, OrganisasjonsEnhet enhetSak2) {
        if (KODE6_ENHET.enhetId().equals(enhetSak1.enhetId()) || KODE6_ENHET.enhetId().equals(enhetSak2.enhetId())) {
            return KODE6_ENHET;
        }
        if (SKJERMET_ENHET.enhetId().equals(enhetSak1.enhetId()) || SKJERMET_ENHET.enhetId().equals(enhetSak2.enhetId())) {
            return SKJERMET_ENHET;
        }
        return enhetSak1;
    }

    static OrganisasjonsEnhet getEnhetKlage() {
        return KLAGE_ENHET;
    }

    static OrganisasjonsEnhet getEnhetUtland() {
        return UTLAND_ENHET;
    }

    static OrganisasjonsEnhet getEnhetKontroll() {
        return KONTROLL_ENHET;
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
        restenhet = norgRest.finnEnhet(request);
        return restenhet.stream()
            .filter(r -> !SPESIALENHETER.contains(r.enhetNr()))
            .map(r -> new OrganisasjonsEnhet(r.enhetNr(), r.enhetNavn()))
            .toList();
    }

}
