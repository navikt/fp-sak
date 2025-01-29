package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.Temagrupper;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
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
    private static final String MI_ENHET_ID = "4863"; // Midlertidig spesialisert enhet
    private static final String NY_ENHET_ID = "4867"; // Nasjonal enhet
    private static final Set<String> SPESIALENHETER = Set.of(NK_ENHET_ID, EA_ENHET_ID, SF_ENHET_ID);

    private static final OrganisasjonsEnhet KLAGE_ENHET =  new OrganisasjonsEnhet(NK_ENHET_ID, "Nav klageinstans Midt-Norge");
    private static final OrganisasjonsEnhet SKJERMET_ENHET =  new OrganisasjonsEnhet(EA_ENHET_ID, "Nav familie- og pensjonsytelser Egne ansatte");
    private static final OrganisasjonsEnhet UTLAND_ENHET =  new OrganisasjonsEnhet(UT_ENHET_ID, "Nav familie- og pensjonsytelser Drammen");
    private static final OrganisasjonsEnhet KONTROLL_ENHET =  new OrganisasjonsEnhet(SK_ENHET_ID, "Nav familie- og pensjonsytelser Steinkjer");
    public static final OrganisasjonsEnhet MIDLERTIDIG_ENHET =  new OrganisasjonsEnhet(MI_ENHET_ID, "Nav familie- og pensjonsytelser midlertidig enhet");

    private static final OrganisasjonsEnhet KODE6_ENHET = new OrganisasjonsEnhet(SF_ENHET_ID, "Nav Vikafossen");
    private static final OrganisasjonsEnhet NASJONAL_ENHET = new OrganisasjonsEnhet(NY_ENHET_ID, "Nav familie- og pensjonsytelser foreldrepenger");

    private static final OrganisasjonsEnhet DRAMMEN =  new OrganisasjonsEnhet("4806", "Nav familie- og pensjonsytelser Drammen");
    private static final OrganisasjonsEnhet BERGEN =  new OrganisasjonsEnhet("4812", "Nav familie- og pensjonsytelser Bergen");
    private static final OrganisasjonsEnhet STEINKJER =  new OrganisasjonsEnhet("4817", "Nav familie- og pensjonsytelser Steinkjer");
    private static final OrganisasjonsEnhet OSLO =  new OrganisasjonsEnhet("4833", "Nav familie- og pensjonsytelser Oslo 1");
    private static final OrganisasjonsEnhet STORD =  new OrganisasjonsEnhet("4842", "Nav familie- og pensjonsytelser Stord");
    private static final OrganisasjonsEnhet TROMSØ =  new OrganisasjonsEnhet("4849", "Nav familie- og pensjonsytelser Tromsø");

    // Oppdateres etterhvert som flytteprosessen foregår. Behold så lenge evaluering av nasjonal enhet foregår
    private static final Map<String, OrganisasjonsEnhet> FLYTTE_MAP = Map.ofEntries(
        Map.entry(NASJONAL_ENHET.enhetId(), NASJONAL_ENHET),
        Map.entry(KLAGE_ENHET.enhetId(), KLAGE_ENHET),
        Map.entry(SKJERMET_ENHET.enhetId(), SKJERMET_ENHET),
        Map.entry(KODE6_ENHET.enhetId(), KODE6_ENHET),
        Map.entry(DRAMMEN.enhetId(), NASJONAL_ENHET),
        Map.entry(BERGEN.enhetId(), NASJONAL_ENHET),
        Map.entry(STEINKJER.enhetId(), NASJONAL_ENHET),
        Map.entry(OSLO.enhetId(), NASJONAL_ENHET),
        Map.entry(STORD.enhetId(), NASJONAL_ENHET),
        Map.entry(TROMSØ.enhetId(), NASJONAL_ENHET),
        Map.entry("4802", NASJONAL_ENHET),
        Map.entry("4847", NASJONAL_ENHET),
        Map.entry("4205", KLAGE_ENHET)
    );

    private static final Set<OrganisasjonsEnhet> ALLEBEHANDLENDEENHETER = Set.of(NASJONAL_ENHET, DRAMMEN, BERGEN, STEINKJER, OSLO,
        STORD, TROMSØ, KLAGE_ENHET, SKJERMET_ENHET, KODE6_ENHET, MIDLERTIDIG_ENHET);

    private static final Set<OrganisasjonsEnhet> IKKE_MENY = Set.of(KLAGE_ENHET, DRAMMEN, BERGEN, STEINKJER, OSLO, STORD, TROMSØ, MIDLERTIDIG_ENHET);

    private PersoninfoAdapter personinfoAdapter;
    private Arbeidsfordeling norgRest;
    private SkjermetPersonKlient skjermetPersonKlient;
    private RutingKlient rutingKlient;

    public EnhetsTjeneste() {
        // For CDI proxy
    }

    @Inject
    public EnhetsTjeneste(PersoninfoAdapter personinfoAdapter,
                          Arbeidsfordeling arbeidsfordelingRestKlient,
                          SkjermetPersonKlient skjermetPersonKlient,
                          RutingKlient rutingKlient) {
        this.personinfoAdapter = personinfoAdapter;
        this.norgRest = arbeidsfordelingRestKlient;
        this.skjermetPersonKlient = skjermetPersonKlient;
        this.rutingKlient = rutingKlient;
    }

    static OrganisasjonsEnhet velgEnhet(OrganisasjonsEnhet enhet, Collection<FagsakMarkering> markering) {
        return enhet != null ? velgEnhet(enhet.enhetId(), markering) : null;
    }

    static OrganisasjonsEnhet velgEnhet(String enhetId, Collection<FagsakMarkering> markering) {
        if (enhetId == null) {
            return null;
        }
        if (SPESIALENHETER.contains(enhetId)) {
            return FLYTTE_MAP.get(enhetId);
        }
        if (markering.contains(FagsakMarkering.SAMMENSATT_KONTROLL)) {
            return KONTROLL_ENHET;
        }
        if (markering.contains(FagsakMarkering.PRAKSIS_UTSETTELSE)) {
            return MIDLERTIDIG_ENHET;
        }
        if (markering.contains(FagsakMarkering.BOSATT_UTLAND)) {
            return UTLAND_ENHET;
        }
        return Optional.ofNullable(FLYTTE_MAP.get(enhetId)).orElse(NASJONAL_ENHET);
    }

    static List<OrganisasjonsEnhet> hentEnhetListe() {
        return ALLEBEHANDLENDEENHETER.stream().filter(e -> !IKKE_MENY.contains(e)).toList();
    }

    OrganisasjonsEnhet hentEnhetSjekkKunAktør(AktørId aktørId, FagsakYtelseType ytelseType) {
        var rutingResultater = finnRuting(Set.of(aktørId));
        if (harNoenDiskresjonskode6(ytelseType, Set.of(aktørId))) {
            LOG.info("RUTING {}", rutingResultater.contains(RutingResultat.STRENGTFORTROLIG) ? "ok1" : "diff sf");
            return KODE6_ENHET;
        } else if (erNoenSkjermetPerson(Set.of(aktørId))) {
            LOG.info("RUTING {}", rutingResultater.contains(RutingResultat.SKJERMING) ? "ok skjerm" : "diff skjerm");
            return SKJERMET_ENHET;
        } else if (personinfoAdapter.hentGeografiskTilknytning(ytelseType, aktørId) == null) {
            LOG.info("RUTING {}", rutingResultater.contains(RutingResultat.UTLAND) ? "ok utland" : "diff utland");
            return UTLAND_ENHET;
        } else {
            if (!rutingResultater.isEmpty()) {
                LOG.info("RUTING diff nasjonal vs resultat {}", rutingResultater);
            }
            return NASJONAL_ENHET;
            // Beholde ut 2025
            // var enheter = hentEnheterFor(geografiskTilknytning, behandlingTema);
            // return enheter.isEmpty() ? NASJONAL_ENHET : velgEnhet(enheter.get(0), null);
        }
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkOppgittePersoner(String enhetId, FagsakYtelseType ytelseType, AktørId hovedAktør,
                                                                    Set<AktørId> alleAktører, Collection<FagsakMarkering> saksmarkering) {
        if (SPESIALENHETER.contains(enhetId)) {
            return Optional.empty();
        }
        var rutingResultater = finnRuting(alleAktører);
        if (harNoenDiskresjonskode6(ytelseType, alleAktører)) {
            LOG.info("RUTING {}", rutingResultater.contains(RutingResultat.STRENGTFORTROLIG) ? "ok1" : "diff sf");
            return Optional.of(KODE6_ENHET);
        }
        if (erNoenSkjermetPerson(alleAktører)) {
            LOG.info("RUTING {}", rutingResultater.contains(RutingResultat.SKJERMING) ? "ok skjerm" : "diff skjerm");
            return Optional.of(SKJERMET_ENHET);
        }
        if (saksmarkering.contains(FagsakMarkering.SAMMENSATT_KONTROLL)) {
            return !KONTROLL_ENHET.enhetId().equals(enhetId) ? Optional.of(KONTROLL_ENHET) : Optional.empty();
        }
        if (saksmarkering.contains(FagsakMarkering.PRAKSIS_UTSETTELSE)) {
            return !MIDLERTIDIG_ENHET.enhetId().equals(enhetId) ? Optional.of(MIDLERTIDIG_ENHET) : Optional.empty();
        }
        if (saksmarkering.contains(FagsakMarkering.BOSATT_UTLAND)) {
            return !UTLAND_ENHET.enhetId().equals(enhetId) ? Optional.of(UTLAND_ENHET) : Optional.empty();
        }
        if (FLYTTE_MAP.get(enhetId) == null) {
            return Optional.of(hentEnhetSjekkKunAktør(hovedAktør, ytelseType));
        }
        return Optional.of(FLYTTE_MAP.get(enhetId)).filter(ny -> !ny.enhetId().equals(enhetId));
    }

    public Set<RutingResultat> finnRuting(Set<AktørId> aktørIds) {
        try {
            return rutingKlient.finnRutingEgenskaper(aktørIds.stream().map(AktørId::getId).collect(Collectors.toSet()));
        } catch (Exception e) {
            LOG.info("RUTING feil", e);
            return Set.of();
        }
    }

    private boolean harNoenDiskresjonskode6(FagsakYtelseType ytelseType, Set<AktørId> aktører) {
        return aktører.stream()
            .map(a -> personinfoAdapter.hentDiskresjonskode(ytelseType, a))
            .anyMatch(Diskresjonskode.KODE6::equals);
    }

    private boolean erNoenSkjermetPerson(Set<AktørId> aktører) {
        var identer = aktører.stream()
            .map(a -> personinfoAdapter.hentFnr(a))
            .flatMap(Optional::stream)
            .map(PersonIdent::getIdent)
            .toList();
        return skjermetPersonKlient.erNoenSkjermet(identer);
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

    static OrganisasjonsEnhet getEnhetNasjonal() {
        return NASJONAL_ENHET;
    }

    // Behold ut 2025
    private List<OrganisasjonsEnhet> hentEnheterFor(String geografi, FagsakYtelseType ytelseType) {
        var brukBTema = ytelseType == null || FagsakYtelseType.UDEFINERT.equals(ytelseType) ?
            BehandlingTema.FORELDREPENGER : BehandlingTema.fraFagsak(ytelseType, null);
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
