package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.FamilierelasjonVL;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Poststed;
import no.nav.foreldrepenger.behandlingslager.geografisk.PoststedKodeverkRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.pdl.Bostedsadresse;
import no.nav.pdl.BostedsadresseResponseProjection;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.Familierelasjon;
import no.nav.pdl.FamilierelasjonResponseProjection;
import no.nav.pdl.Familierelasjonsrolle;
import no.nav.pdl.Foedsel;
import no.nav.pdl.FoedselResponseProjection;
import no.nav.pdl.FolkeregistermetadataResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Kontaktadresse;
import no.nav.pdl.KontaktadresseResponseProjection;
import no.nav.pdl.Matrikkeladresse;
import no.nav.pdl.MatrikkeladresseResponseProjection;
import no.nav.pdl.MetadataResponseProjection;
import no.nav.pdl.Navn;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Opphold;
import no.nav.pdl.OppholdResponseProjection;
import no.nav.pdl.Oppholdsadresse;
import no.nav.pdl.OppholdsadresseResponseProjection;
import no.nav.pdl.Oppholdstillatelse;
import no.nav.pdl.Person;
import no.nav.pdl.PersonOppholdParametrizedInput;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.PostadresseIFrittFormat;
import no.nav.pdl.PostadresseIFrittFormatResponseProjection;
import no.nav.pdl.Postboksadresse;
import no.nav.pdl.PostboksadresseResponseProjection;
import no.nav.pdl.Sivilstand;
import no.nav.pdl.SivilstandResponseProjection;
import no.nav.pdl.Sivilstandstype;
import no.nav.pdl.Statsborgerskap;
import no.nav.pdl.StatsborgerskapResponseProjection;
import no.nav.pdl.UkjentBosted;
import no.nav.pdl.UkjentBostedResponseProjection;
import no.nav.pdl.UtenlandskAdresse;
import no.nav.pdl.UtenlandskAdresseIFrittFormat;
import no.nav.pdl.UtenlandskAdresseIFrittFormatResponseProjection;
import no.nav.pdl.UtenlandskAdresseResponseProjection;
import no.nav.pdl.Vegadresse;
import no.nav.pdl.VegadresseResponseProjection;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;
import no.nav.vedtak.felles.integrasjon.pdl.Tema;

@ApplicationScoped
public class PersoninfoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PersoninfoTjeneste.class);

    private static final String HARDKODET_POSTNR = "XXXX";
    private static final String HARDKODET_POSTSTED = "UKJENT";

    private static final Map<Sivilstandstype, SivilstandType> SIVSTAND_FRA_FREG = Map.ofEntries(
        Map.entry(Sivilstandstype.UOPPGITT, SivilstandType.UOPPGITT),
        Map.entry(Sivilstandstype.UGIFT, SivilstandType.UGIFT),
        Map.entry(Sivilstandstype.GIFT, SivilstandType.GIFT),
        Map.entry(Sivilstandstype.ENKE_ELLER_ENKEMANN, SivilstandType.ENKEMANN),
        Map.entry(Sivilstandstype.SKILT, SivilstandType.SKILT),
        Map.entry(Sivilstandstype.SEPARERT, SivilstandType.SEPARERT),
        Map.entry(Sivilstandstype.REGISTRERT_PARTNER, SivilstandType.REGISTRERT_PARTNER),
        Map.entry(Sivilstandstype.SEPARERT_PARTNER, SivilstandType.SEPARERT_PARTNER),
        Map.entry(Sivilstandstype.SKILT_PARTNER, SivilstandType.SKILT_PARTNER),
        Map.entry(Sivilstandstype.GJENLEVENDE_PARTNER, SivilstandType.GJENLEVENDE_PARTNER)
    );

    private static final Map<Familierelasjonsrolle, RelasjonsRolleType> ROLLE_FRA_FREG_ROLLE = Map.ofEntries(
        Map.entry(Familierelasjonsrolle.BARN, RelasjonsRolleType.BARN),
        Map.entry(Familierelasjonsrolle.MOR, RelasjonsRolleType.MORA),
        Map.entry(Familierelasjonsrolle.FAR, RelasjonsRolleType.FARA),
        Map.entry(Familierelasjonsrolle.MEDMOR, RelasjonsRolleType.MEDMOR)
    );

    private static final Map<Sivilstandstype, RelasjonsRolleType> ROLLE_FRA_FREG_STAND = Map.ofEntries(
        Map.entry(Sivilstandstype.GIFT, RelasjonsRolleType.EKTE),
        Map.entry(Sivilstandstype.REGISTRERT_PARTNER, RelasjonsRolleType.REGISTRERT_PARTNER)
    );


    private PdlKlient pdlKlient;
    private PoststedKodeverkRepository poststedKodeverkRepository;

    PersoninfoTjeneste() {
        // CDI
    }

    @Inject
    public PersoninfoTjeneste(PdlKlient pdlKlient, PoststedKodeverkRepository repository) {
        this.pdlKlient = pdlKlient;
        this.poststedKodeverkRepository = repository;
    }

    public void hentPersoninfo(AktørId aktørId, PersonIdent personIdent, Personinfo fraTPS) {
        try {
            var query = new HentPersonQueryRequest();
            query.setIdent(aktørId.getId());
            var projection = new PersonResponseProjection()
                    .navn(new NavnResponseProjection().forkortetNavn().fornavn().mellomnavn().etternavn())
                    .foedsel(new FoedselResponseProjection().foedselsdato())
                    .doedsfall(new DoedsfallResponseProjection().doedsdato())
                    .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().forenkletStatus().status())
                    .opphold(new PersonOppholdParametrizedInput().historikk(true), new OppholdResponseProjection().type().oppholdFra().oppholdTil().metadata(new MetadataResponseProjection().historisk()).folkeregistermetadata(new FolkeregistermetadataResponseProjection().ajourholdstidspunkt()))
                    .kjoenn(new KjoennResponseProjection().kjoenn())
                    .sivilstand(new SivilstandResponseProjection().relatertVedSivilstand().type())
                    .statsborgerskap(new StatsborgerskapResponseProjection().land())
                    .familierelasjoner(new FamilierelasjonResponseProjection().relatertPersonsRolle().relatertPersonsIdent().minRolleForPerson())
                    .bostedsadresse(new BostedsadresseResponseProjection()
                        .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().tilleggsnavn().postnummer())
                        .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                        .ukjentBosted(new UkjentBostedResponseProjection().bostedskommune())
                        .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
                    .oppholdsadresse(new OppholdsadresseResponseProjection()
                        .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().tilleggsnavn().postnummer())
                        .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                        .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
                    .kontaktadresse(new KontaktadresseResponseProjection().type()
                        .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().tilleggsnavn().postnummer())
                        .postboksadresse(new PostboksadresseResponseProjection().postboks().postbokseier().postnummer())
                        .postadresseIFrittFormat(new PostadresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().postnummer())
                        .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode())
                        .utenlandskAdresseIFrittFormat(new UtenlandskAdresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().byEllerStedsnavn().postkode().landkode()))
                ;

            var person = pdlKlient.hentPerson(query, projection, Tema.FOR);

            var fødselsdato = person.getFoedsel().stream()
                .map(Foedsel::getFoedselsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
            var dødssdato = person.getDoedsfall().stream()
                .map(Doedsfall::getDoedsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
            var pdlStatus = person.getFolkeregisterpersonstatus().stream()
                .map(Folkeregisterpersonstatus::getStatus)
                .findFirst().map(PersonstatusType::fraFregPersonstatus).orElse(PersonstatusType.UDEFINERT);
            var sivilstand = person.getSivilstand().stream()
                .map(Sivilstand::getType)
                .findFirst()
                .map(st -> SIVSTAND_FRA_FREG.getOrDefault(st, SivilstandType.UOPPGITT)).orElse(SivilstandType.UOPPGITT);
            var statsborgerskap = mapStatsborgerskap(person.getStatsborgerskap());
            var familierelasjoner = mapFamilierelasjoner(person.getFamilierelasjoner(), person.getSivilstand());
            var adresser = mapAdresser(person.getBostedsadresse(), person.getKontaktadresse(), person.getOppholdsadresse());
            var fraPDL = new Personinfo.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
                .medNavn(person.getNavn().stream().map(PersoninfoTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElse("MANGLER NAVN"))
                .medFødselsdato(fødselsdato)
                .medDødsdato(dødssdato)
                .medNavBrukerKjønn(mapKjønn(person))
                .medPersonstatusType(pdlStatus)
                .medSivilstandType(sivilstand)
                .medLandkode(statsborgerskap)
                .medRegion(MapRegionLandkoder.mapLandkode(statsborgerskap.getKode()))
                .medFamilierelasjon(familierelasjoner)
                .medAdresseInfoList(adresser)
                .build();

            logInnUtOpp(person.getOpphold());
            if (!erLike(fraPDL, fraTPS)) {
                var avvik = finnAvvik(fraTPS, fraPDL);
                LOG.info("FPSAK PDL FULL: avvik {}", avvik);
            }
        } catch (Exception e) {
            LOG.info("FPSAK PDL FULL error", e);
        }
    }

    private static String mapNavn(Navn navn) {
        if (navn.getForkortetNavn() != null)
            return navn.getForkortetNavn();
        return navn.getEtternavn() + " " + navn.getFornavn() + (navn.getMellomnavn() == null ? "" : " " + navn.getMellomnavn());
    }

    private static NavBrukerKjønn mapKjønn(Person person) {
        var kode = person.getKjoenn().stream()
            .map(Kjoenn::getKjoenn)
            .filter(Objects::nonNull)
            .findFirst().orElse(KjoennType.UKJENT);
        if (KjoennType.MANN.equals(kode))
            return NavBrukerKjønn.MANN;
        return KjoennType.KVINNE.equals(kode) ? NavBrukerKjønn.KVINNE : NavBrukerKjønn.UDEFINERT;
    }

    private static Landkoder mapStatsborgerskap(List<Statsborgerskap> statsborgerskap) {
        List<Landkoder> alleLand = statsborgerskap.stream()
            .map(Statsborgerskap::getLand)
            .map(Landkoder::fraKodeDefaultUdefinert)
            .collect(Collectors.toList());
        return alleLand.stream().anyMatch(Landkoder.NOR::equals) ? Landkoder.NOR : alleLand.stream().findFirst().orElse(Landkoder.UOPPGITT);
    }

    private static Set<FamilierelasjonVL> mapFamilierelasjoner(List<Familierelasjon> familierelasjoner, List<Sivilstand> sivilstandliste) {
        Set<FamilierelasjonVL> relasjoner = new HashSet<>();
        // TODO: utled samme bosted ut fra adresse

        familierelasjoner.stream()
            .map(r -> new FamilierelasjonVL(new PersonIdent(r.getRelatertPersonsIdent()), mapRelasjonsrolle(r.getRelatertPersonsRolle()), false))
            .forEach(relasjoner::add);
        sivilstandliste.stream()
            .filter(rel -> Sivilstandstype.GIFT.equals(rel.getType()) || Sivilstandstype.REGISTRERT_PARTNER.equals(rel.getType()))
            .filter(rel -> rel.getRelatertVedSivilstand() != null)
            .map(r -> new FamilierelasjonVL(new PersonIdent(r.getRelatertVedSivilstand()), mapRelasjonsrolle(r.getType()), false))
            .forEach(relasjoner::add);
        return relasjoner;
    }

    private static RelasjonsRolleType mapRelasjonsrolle(Familierelasjonsrolle type) {
        return ROLLE_FRA_FREG_ROLLE.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    private static RelasjonsRolleType mapRelasjonsrolle(Sivilstandstype type) {
        return ROLLE_FRA_FREG_STAND.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    private List<Adresseinfo> mapAdresser(List<Bostedsadresse> bostedsadresser, List<Kontaktadresse> kontaktadresser, List<Oppholdsadresse> oppholdsadresser) {
        List<Adresseinfo> resultat = new ArrayList<>();
        bostedsadresser.stream().map(Bostedsadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getMatrikkeladresse).map(a -> mapMatrikkeladresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getUkjentBosted).map(this::mapUkjentadresse).filter(Objects::nonNull).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);

        oppholdsadresser.stream().map(Oppholdsadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).filter(Objects::nonNull).forEach(resultat::add);
        oppholdsadresser.stream().map(Oppholdsadresse::getMatrikkeladresse).map(a -> mapMatrikkeladresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).filter(Objects::nonNull).forEach(resultat::add);
        oppholdsadresser.stream().map(Oppholdsadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);

        kontaktadresser.stream().map(Kontaktadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.POSTADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getPostboksadresse).map(a -> mapPostboksadresse(AdresseType.POSTADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getPostadresseIFrittFormat).map(a -> mapFriAdresseNorsk(AdresseType.POSTADRESSE, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getUtenlandskAdresseIFrittFormat).map(a -> mapFriAdresseUtland(AdresseType.POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);

        return resultat;
    }

    private Adresseinfo mapVegadresse(AdresseType type, Vegadresse vegadresse) {
        if (vegadresse == null)
            return null;
        String postnummer = Optional.ofNullable(vegadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var gateadresse = vegadresse.getAdressenavn() + hvisfinnes(vegadresse.getHusnummer()) + hvisfinnes(vegadresse.getHusbokstav());
        return Adresseinfo.builder(type)
            .medMatrikkelId(vegadresse.getMatrikkelId())
            .medAdresselinje1(vegadresse.getTilleggsnavn() != null ? vegadresse.getTilleggsnavn() : gateadresse)
            .medAdresselinje2(vegadresse.getTilleggsnavn() != null ? gateadresse : null)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapMatrikkeladresse(AdresseType type, Matrikkeladresse matrikkeladresse) {
        if (matrikkeladresse == null)
            return null;
        String postnummer = Optional.ofNullable(matrikkeladresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(type)
            .medMatrikkelId(matrikkeladresse.getMatrikkelId())
            .medAdresselinje1(matrikkeladresse.getTilleggsnavn() != null ? matrikkeladresse.getTilleggsnavn() : matrikkeladresse.getBruksenhetsnummer())
            .medAdresselinje2(matrikkeladresse.getTilleggsnavn() != null ? matrikkeladresse.getBruksenhetsnummer() : null)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapPostboksadresse(AdresseType type, Postboksadresse postboksadresse) {
        if (postboksadresse == null)
            return null;
        String postnummer = Optional.ofNullable(postboksadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var postboks = "Postboks" + hvisfinnes(postboksadresse.getPostboks());
        return Adresseinfo.builder(type)
            .medAdresselinje1(postboksadresse.getPostbokseier() != null ? postboksadresse.getPostbokseier() : postboks)
            .medAdresselinje2(postboksadresse.getPostbokseier() != null ? postboks : null)
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapFriAdresseNorsk(AdresseType type, PostadresseIFrittFormat postadresse) {
        if (postadresse == null)
            return null;
        String postnummer = Optional.ofNullable(postadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(type)
            .medAdresselinje1(postadresse.getAdresselinje1())
            .medAdresselinje2(postadresse.getAdresselinje2())
            .medAdresselinje3(postadresse.getAdresselinje3())
            .medPostNr(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapUkjentadresse(UkjentBosted ukjentBosted) {
        return ukjentBosted == null ? null : Adresseinfo.builder(AdresseType.UKJENT_ADRESSE).build();
    }

    private Adresseinfo mapUtenlandskadresse(AdresseType type, UtenlandskAdresse utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var linje1 = hvisfinnes(utenlandskAdresse.getAdressenavnNummer()) + hvisfinnes(utenlandskAdresse.getBygningEtasjeLeilighet()) + hvisfinnes(utenlandskAdresse.getPostboksNummerNavn());
        var linje2 = hvisfinnes(utenlandskAdresse.getPostkode()) + hvisfinnes(utenlandskAdresse.getBySted()) + hvisfinnes(utenlandskAdresse.getRegionDistriktOmraade());
        return Adresseinfo.builder(type)
            .medAdresselinje1(linje1)
            .medAdresselinje2(linje2)
            .medAdresselinje3(utenlandskAdresse.getLandkode())
            .medLand(utenlandskAdresse.getLandkode())
            .build();
    }

    private Adresseinfo mapFriAdresseUtland(AdresseType type, UtenlandskAdresseIFrittFormat utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var postlinje = hvisfinnes(utenlandskAdresse.getPostkode()) + hvisfinnes(utenlandskAdresse.getByEllerStedsnavn());
        var sisteline = utenlandskAdresse.getAdresselinje3() != null ? postlinje + utenlandskAdresse.getLandkode()
            : (utenlandskAdresse.getAdresselinje2() != null ? utenlandskAdresse.getLandkode() : null);
        return Adresseinfo.builder(type)
            .medAdresselinje1(utenlandskAdresse.getAdresselinje1())
            .medAdresselinje2(utenlandskAdresse.getAdresselinje2() != null ? utenlandskAdresse.getAdresselinje2() : postlinje)
            .medAdresselinje3(utenlandskAdresse.getAdresselinje3() != null ? utenlandskAdresse.getAdresselinje3() : (utenlandskAdresse.getAdresselinje2() != null ? postlinje : utenlandskAdresse.getLandkode()))
            .medAdresselinje4(sisteline)
            .medLand(utenlandskAdresse.getLandkode())
            .build();
    }

    private static String hvisfinnes(Object object) {
        return object == null ? "" : " " + object.toString().trim();
    }

    private String tilPoststed(String postnummer) {
        if (HARDKODET_POSTNR.equals(postnummer)) {
            return HARDKODET_POSTSTED;
        }
        return poststedKodeverkRepository.finnPoststed(postnummer).map(Poststed::getPoststednavn).orElse(HARDKODET_POSTSTED);
    }

    private boolean erLike(Personinfo pdl, Personinfo tps) {
        if (tps == null && pdl == null) return true;
        if (pdl == null || tps == null || tps.getClass() != pdl.getClass()) return false;
        var likerels = pdl.getFamilierelasjoner().size() == tps.getFamilierelasjoner().size() &&
            pdl.getFamilierelasjoner().containsAll(tps.getFamilierelasjoner());
        var likeadresser = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() &&
            pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList());
        return // Objects.equals(pdl.getNavn(), tps.getNavn()) && - avvik skyldes tegnsett
            Objects.equals(pdl.getFødselsdato(), tps.getFødselsdato()) &&
            Objects.equals(pdl.getDødsdato(), tps.getDødsdato()) &&
            pdl.getPersonstatus() == tps.getPersonstatus() &&
            pdl.getKjønn() == tps.getKjønn() &&
            likerels &&
            likeadresser &&
            pdl.getRegion() == tps.getRegion() &&
            pdl.getLandkode() == tps.getLandkode() &&
            pdl.getSivilstandType() == tps.getSivilstandType();
    }

    private String finnAvvik(Personinfo tps, Personinfo pdl) {
        //String navn = Objects.equals(tps.getNavn(), pdl.getNavn()) ? "" : " navn ";
        String kjonn = Objects.equals(tps.getKjønn(), pdl.getKjønn()) ? "" : " kjønn ";
        String fdato = Objects.equals(tps.getFødselsdato(), pdl.getFødselsdato()) ? "" : " fødsel ";
        String ddato = Objects.equals(tps.getDødsdato(), pdl.getDødsdato()) ? "" : " død ";
        String status = Objects.equals(tps.getPersonstatus(), pdl.getPersonstatus()) ? "" : " status " + tps.getPersonstatus().getKode() + " PDL " + pdl.getPersonstatus().getKode();
        String sivstand = Objects.equals(tps.getSivilstandType(), pdl.getSivilstandType()) ? "" : " sivilst " + tps.getSivilstandType().getKode() + " PDL " + pdl.getSivilstandType().getKode();
        String land = Objects.equals(tps.getLandkode(), pdl.getLandkode()) ? "" : " land " + tps.getLandkode().getKode() + " PDL " + pdl.getLandkode().getKode();
        String region = Objects.equals(tps.getRegion(), pdl.getRegion()) ? "" : " region " + tps.getRegion().getKode() + " PDL " + pdl.getRegion().getKode();
        String frel = pdl.getFamilierelasjoner().size() == tps.getFamilierelasjoner().size() && pdl.getFamilierelasjoner().containsAll(tps.getFamilierelasjoner()) ? ""
            : " famrel " + tps.getFamilierelasjoner().stream().map(FamilierelasjonVL::getRelasjonsrolle).collect(Collectors.toList()) + " PDL " + pdl.getFamilierelasjoner().stream().map(FamilierelasjonVL::getRelasjonsrolle).collect(Collectors.toList());
        String adresse = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() && pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList())  ? ""
            : " adresse " + tps.getAdresseInfoList().stream().map(Adresseinfo::getGjeldendePostadresseType).collect(Collectors.toList()) + " PDL " + pdl.getAdresseInfoList().stream().map(Adresseinfo::getGjeldendePostadresseType).collect(Collectors.toList());
        String adresse2 = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() && pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList())  ? ""
            : " adresse2 " + tps.getAdresseInfoList().stream().map(Adresseinfo::getPostNr).collect(Collectors.toList()) + " PDL " + pdl.getAdresseInfoList().stream().map(Adresseinfo::getPostNr).collect(Collectors.toList());
        String adresse3 = pdl.getAdresseInfoList().size() == tps.getAdresseInfoList().size() && pdl.getAdresseInfoList().containsAll(tps.getAdresseInfoList())  ? ""
            : " adresse3 " + tps.getAdresseInfoList().stream().map(Adresseinfo::getLand).collect(Collectors.toList()) + " PDL " + pdl.getAdresseInfoList().stream().map(Adresseinfo::getLand).collect(Collectors.toList());
        return "Avvik" + kjonn + fdato + ddato + status + sivstand + land + region + frel + adresse + adresse2 + adresse3;
    }

    private void logInnUtOpp(List<Opphold> opp) {
        String opps = opp.stream()
            .filter(o -> !Oppholdstillatelse.OPPLYSNING_MANGLER.equals(o.getType()))
            .map(o -> "OppholdType="+o.getType().toString()+" historisk "+o.getMetadata().getHistorisk()+
                " Fra=" + (o.getOppholdFra() == null ? o.getFolkeregistermetadata().getAjourholdstidspunkt().toString() : o.getOppholdFra()) +" Til="+o.getOppholdTil())
            .collect(Collectors.joining(", "));
        if (!opp.isEmpty()) {
            LOG.info("FPSAK PDL FULL opphold {}", opps);
        }
    }

}
