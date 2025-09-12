package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.integrasjon.ruting.RutingResultat;

@ApplicationScoped
public class EnhetsTjeneste {

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

    private static final Map<String, OrganisasjonsEnhet> ENHET_MAP = Map.ofEntries(
        Map.entry(NASJONAL_ENHET.enhetId(), NASJONAL_ENHET),
        Map.entry(KLAGE_ENHET.enhetId(), KLAGE_ENHET),
        Map.entry(SKJERMET_ENHET.enhetId(), SKJERMET_ENHET),
        Map.entry(KODE6_ENHET.enhetId(), KODE6_ENHET)
    );

    private static final Set<OrganisasjonsEnhet> ALLEBEHANDLENDEENHETER = Set.of(NASJONAL_ENHET, KLAGE_ENHET, SKJERMET_ENHET, UTLAND_ENHET,
        KONTROLL_ENHET, KODE6_ENHET, MIDLERTIDIG_ENHET);

    private static final Set<OrganisasjonsEnhet> IKKE_MENY = Set.of(KLAGE_ENHET, MIDLERTIDIG_ENHET);

    private RutingKlient rutingKlient;

    public EnhetsTjeneste() {
        // For CDI proxy
    }

    @Inject
    public EnhetsTjeneste(RutingKlient rutingKlient) {
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
            return ENHET_MAP.get(enhetId);
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
        return NASJONAL_ENHET;
    }

    static List<OrganisasjonsEnhet> hentEnhetListe() {
        return ALLEBEHANDLENDEENHETER.stream().filter(e -> !IKKE_MENY.contains(e)).toList();
    }

    OrganisasjonsEnhet hentEnhetSjekkKunAktør(AktørId aktørId) {
        var rutingResultater = finnRuting(Set.of(aktørId));
        if (rutingResultater.contains(RutingResultat.STRENGTFORTROLIG) ) {
            return KODE6_ENHET;
        } else if (rutingResultater.contains(RutingResultat.SKJERMING)) {
            return SKJERMET_ENHET;
        } else if (rutingResultater.contains(RutingResultat.UTLAND) ) {
            return UTLAND_ENHET;
        } else {
            return NASJONAL_ENHET;
        }
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkOppgittePersoner(String enhetId,
                                                                    Set<AktørId> alleAktører, Collection<FagsakMarkering> saksmarkering) {
        if (SPESIALENHETER.contains(enhetId)) {
            return Optional.empty();
        }
        var rutingResultater = finnRuting(alleAktører);
        if (rutingResultater.contains(RutingResultat.STRENGTFORTROLIG)) {
            return Optional.of(KODE6_ENHET);
        }
        if (rutingResultater.contains(RutingResultat.SKJERMING)) {
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
        return NASJONAL_ENHET.enhetId().equals(enhetId) ? Optional.empty() : Optional.of(NASJONAL_ENHET);
    }

    public Set<RutingResultat> finnRuting(Set<AktørId> aktørIds) {
        return rutingKlient.finnRutingEgenskaper(aktørIds.stream().map(AktørId::getId).collect(Collectors.toSet()));
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

    static OrganisasjonsEnhet getEnhetMidlertidig() {
        return MIDLERTIDIG_ENHET;
    }

    static OrganisasjonsEnhet getEnhetNasjonal() {
        return NASJONAL_ENHET;
    }

}
