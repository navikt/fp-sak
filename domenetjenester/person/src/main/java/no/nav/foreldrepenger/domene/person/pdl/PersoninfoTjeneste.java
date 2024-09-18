package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.FamilierelasjonVL;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.OppholdstillatelsePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Poststed;
import no.nav.foreldrepenger.behandlingslager.geografisk.PoststedKodeverkRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.pdl.Bostedsadresse;
import no.nav.pdl.BostedsadresseResponseProjection;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.FolkeregistermetadataResponseProjection;
import no.nav.pdl.Folkeregisterpersonstatus;
import no.nav.pdl.FolkeregisterpersonstatusResponseProjection;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Kontaktadresse;
import no.nav.pdl.KontaktadresseResponseProjection;
import no.nav.pdl.Matrikkeladresse;
import no.nav.pdl.MatrikkeladresseResponseProjection;
import no.nav.pdl.Metadata;
import no.nav.pdl.Navn;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Opphold;
import no.nav.pdl.OppholdResponseProjection;
import no.nav.pdl.Oppholdsadresse;
import no.nav.pdl.OppholdsadresseResponseProjection;
import no.nav.pdl.Oppholdstillatelse;
import no.nav.pdl.Person;
import no.nav.pdl.PersonBostedsadresseParametrizedInput;
import no.nav.pdl.PersonFolkeregisterpersonstatusParametrizedInput;
import no.nav.pdl.PersonKontaktadresseParametrizedInput;
import no.nav.pdl.PersonOppholdParametrizedInput;
import no.nav.pdl.PersonOppholdsadresseParametrizedInput;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.PersonStatsborgerskapParametrizedInput;
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
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class PersoninfoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PersoninfoTjeneste.class);

    private static final String HARDKODET_POSTNR = "XXXX";
    private static final String HARDKODET_POSTSTED = "UKJENT";

    private static final Set<Sivilstandstype> JURIDISK_GIFT = Set.of(Sivilstandstype.GIFT, Sivilstandstype.SEPARERT,
            Sivilstandstype.REGISTRERT_PARTNER, Sivilstandstype.SEPARERT_PARTNER);

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
            Map.entry(Sivilstandstype.GJENLEVENDE_PARTNER, SivilstandType.GJENLEVENDE_PARTNER));

    private static final Map<ForelderBarnRelasjonRolle, RelasjonsRolleType> ROLLE_FRA_FREG_ROLLE = Map.ofEntries(
            Map.entry(ForelderBarnRelasjonRolle.BARN, RelasjonsRolleType.BARN),
            Map.entry(ForelderBarnRelasjonRolle.MOR, RelasjonsRolleType.MORA),
            Map.entry(ForelderBarnRelasjonRolle.FAR, RelasjonsRolleType.FARA),
            Map.entry(ForelderBarnRelasjonRolle.MEDMOR, RelasjonsRolleType.MEDMOR));

    private static final Map<Sivilstandstype, RelasjonsRolleType> ROLLE_FRA_FREG_STAND = Map.ofEntries(
            Map.entry(Sivilstandstype.GIFT, RelasjonsRolleType.EKTE),
            Map.entry(Sivilstandstype.SEPARERT, RelasjonsRolleType.EKTE),
            Map.entry(Sivilstandstype.REGISTRERT_PARTNER, RelasjonsRolleType.REGISTRERT_PARTNER),
            Map.entry(Sivilstandstype.SEPARERT_PARTNER, RelasjonsRolleType.REGISTRERT_PARTNER));

    private static final Map<Oppholdstillatelse, OppholdstillatelseType> TILLATELSE_FRA_FREG_OPPHOLD = Map.ofEntries(
            Map.entry(Oppholdstillatelse.PERMANENT, OppholdstillatelseType.PERMANENT),
            Map.entry(Oppholdstillatelse.MIDLERTIDIG, OppholdstillatelseType.MIDLERTIDIG));

    private PdlKlientLogCause pdlKlient;
    private PoststedKodeverkRepository poststedKodeverkRepository;

    PersoninfoTjeneste() {
        // CDI
    }

    @Inject
    public PersoninfoTjeneste(PdlKlientLogCause pdlKlient, PoststedKodeverkRepository repository) {
        this.pdlKlient = pdlKlient;
        this.poststedKodeverkRepository = repository;
    }

    public boolean brukerManglerAdresse(FagsakYtelseType ytelseType, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(personIdent.getIdent());

        var projection = new PersonResponseProjection()
            .bostedsadresse(new BostedsadresseResponseProjection().gyldigFraOgMed().angittFlyttedato()
                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                .ukjentBosted(new UkjentBostedResponseProjection().bostedskommune())
                .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn()
                    .bySted().regionDistriktOmraade().postkode().landkode()))
            .oppholdsadresse(new OppholdsadresseResponseProjection().gyldigFraOgMed()
                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn()
                    .bySted().regionDistriktOmraade().postkode().landkode()))
            .kontaktadresse(new KontaktadresseResponseProjection().type().gyldigFraOgMed()
                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                .postboksadresse(new PostboksadresseResponseProjection().postboks().postbokseier().postnummer())
                .postadresseIFrittFormat(
                    new PostadresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().postnummer())
                .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn()
                    .bySted().regionDistriktOmraade().postkode().landkode())
                .utenlandskAdresseIFrittFormat(new UtenlandskAdresseIFrittFormatResponseProjection().adresselinje1().adresselinje2()
                    .adresselinje3().byEllerStedsnavn().postkode().landkode()));

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var adresser = mapAdresser(person.getBostedsadresse(), person.getKontaktadresse(), person.getOppholdsadresse(), true);

        return adresser.stream().map(Adresseinfo::getGjeldendePostadresseType).allMatch(AdresseType.UKJENT_ADRESSE::equals);
    }

    public Personinfo hentPersoninfo(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent) {

        var query = new HentPersonQueryRequest();
        query.setIdent(personIdent.getIdent());

        var projection = new PersonResponseProjection()
                .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
                .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
                .doedsfall(new DoedsfallResponseProjection().doedsdato())
                .folkeregisterpersonstatus(new FolkeregisterpersonstatusResponseProjection().forenkletStatus().status())
                .kjoenn(new KjoennResponseProjection().kjoenn())
                .sivilstand(new SivilstandResponseProjection().relatertVedSivilstand().type())
                .statsborgerskap(new StatsborgerskapResponseProjection().land())
                .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsRolle().relatertPersonsIdent().minRolleForPerson())
                .bostedsadresse(new BostedsadresseResponseProjection().gyldigFraOgMed().angittFlyttedato()
                        .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                        .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                        .ukjentBosted(new UkjentBostedResponseProjection().bostedskommune())
                        .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn()
                                .bySted().regionDistriktOmraade().postkode().landkode()))
                .oppholdsadresse(new OppholdsadresseResponseProjection().gyldigFraOgMed()
                        .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                        .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                        .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn()
                                .bySted().regionDistriktOmraade().postkode().landkode()))
                .kontaktadresse(new KontaktadresseResponseProjection().type().gyldigFraOgMed()
                        .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                        .postboksadresse(new PostboksadresseResponseProjection().postboks().postbokseier().postnummer())
                        .postadresseIFrittFormat(
                                new PostadresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().postnummer())
                        .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn()
                                .bySted().regionDistriktOmraade().postkode().landkode())
                        .utenlandskAdresseIFrittFormat(new UtenlandskAdresseIFrittFormatResponseProjection().adresselinje1().adresselinje2()
                                .adresselinje3().byEllerStedsnavn().postkode().landkode()));

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var fødselsdato = person.getFoedselsdato().stream()
                .map(Foedselsdato::getFoedselsdato)
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
        var familierelasjoner = mapFamilierelasjoner(person.getForelderBarnRelasjon(), person.getSivilstand());
        var adresser = mapAdresser(person.getBostedsadresse(), person.getKontaktadresse(), person.getOppholdsadresse(), true);

        // Opphørte personer kan mangle fødselsdato mm. Håndtere dette + gi feil hvis fødselsdato mangler i andre tilfelle
        if (PersonstatusType.UTPE.equals(pdlStatus) && fødselsdato == null) {
            return null;
        }

        return new Personinfo.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
                .medNavn(person.getNavn().stream().map(PersoninfoTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElse("MANGLER NAVN"))
                .medFødselsdato(fødselsdato)
                .medDødsdato(dødssdato)
                .medNavBrukerKjønn(mapKjønn(person))
                .medPersonstatusType(pdlStatus)
                .medSivilstandType(sivilstand)
                .medLandkoder(statsborgerskap)
                .medFamilierelasjon(familierelasjoner)
                .medAdresseInfoList(adresser)
                .build();
    }

    public Personhistorikkinfo hentPersoninfoHistorikk(FagsakYtelseType ytelseType, AktørId aktørId, LocalDate fom, LocalDate tom) {

        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());

        var projection = new PersonResponseProjection()
                .folkeregisterpersonstatus(new PersonFolkeregisterpersonstatusParametrizedInput().historikk(true),
                        new FolkeregisterpersonstatusResponseProjection()
                                .forenkletStatus().status()
                                .folkeregistermetadata(new FolkeregistermetadataResponseProjection().ajourholdstidspunkt().gyldighetstidspunkt()
                                        .opphoerstidspunkt()))
                .opphold(new PersonOppholdParametrizedInput().historikk(true), new OppholdResponseProjection().type().oppholdFra().oppholdTil())
                .statsborgerskap(new PersonStatsborgerskapParametrizedInput().historikk(true),
                        new StatsborgerskapResponseProjection().land().gyldigFraOgMed().gyldigTilOgMed())
                .bostedsadresse(new PersonBostedsadresseParametrizedInput().historikk(true),
                        new BostedsadresseResponseProjection().angittFlyttedato().gyldigFraOgMed().gyldigTilOgMed()
                                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                                .matrikkeladresse(
                                        new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                                .ukjentBosted(new UkjentBostedResponseProjection().bostedskommune())
                                .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet()
                                        .postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode()))
                .oppholdsadresse(new PersonOppholdsadresseParametrizedInput().historikk(true), new OppholdsadresseResponseProjection()
                        .gyldigFraOgMed().gyldigTilOgMed()
                        .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                        .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                        .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn()
                                .bySted().regionDistriktOmraade().postkode().landkode()))
                .kontaktadresse(new PersonKontaktadresseParametrizedInput().historikk(true),
                        new KontaktadresseResponseProjection().type().gyldigFraOgMed().gyldigTilOgMed()
                                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                                .postboksadresse(new PostboksadresseResponseProjection().postboks().postbokseier().postnummer())
                                .postadresseIFrittFormat(
                                        new PostadresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().postnummer())
                                .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet()
                                        .postboksNummerNavn().bySted().regionDistriktOmraade().postkode().landkode())
                                .utenlandskAdresseIFrittFormat(new UtenlandskAdresseIFrittFormatResponseProjection().adresselinje1().adresselinje2()
                                        .adresselinje3().byEllerStedsnavn().postkode().landkode()));

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var builder = Personhistorikkinfo.builder().medAktørId(aktørId.getId());
        var personStatusPerioder = person.getFolkeregisterpersonstatus().stream()
                .map(PersoninfoTjeneste::mapPersonstatusHistorisk)
                .toList();
        periodiserPersonstatus(personStatusPerioder).stream()
                .filter(p -> p.getGyldighetsperiode().getTom().isAfter(fom) && p.getGyldighetsperiode().getFom().isBefore(tom))
                .forEach(builder::leggTil);

        person.getStatsborgerskap().stream()
                .map(PersoninfoTjeneste::mapStatsborgerskapHistorikk)
                .filter(p -> p.getGyldighetsperiode().getTom().isAfter(fom) && p.getGyldighetsperiode().getFom().isBefore(tom))
                .forEach(builder::leggTil);

        person.getOpphold().stream()
                .filter(PersoninfoTjeneste::relevantOppholdstillatelse)
                .map(PersoninfoTjeneste::mapOppholdsHistorikk)
                .filter(p -> p.getGyldighetsperiode().getTom().isAfter(fom) && p.getGyldighetsperiode().getFom().isBefore(tom))
                .forEach(builder::leggTil);

        var adressePerioder = mapAdresserHistorikk(person.getBostedsadresse(), person.getKontaktadresse(), person.getOppholdsadresse());
        var adressePerioderPeriodisert = periodiserAdresse(adressePerioder);
        try {
            var sistePeriodiserteErUkjent = adressePerioderPeriodisert.stream()
                .max(Comparator.comparing(a -> a.getGyldighetsperiode().getFom()))
                .filter(a -> AdresseType.UKJENT_ADRESSE.equals(a.getAdresse().getAdresseType())).isPresent();
            if (sistePeriodiserteErUkjent) {
                var aplist = adressePerioder.stream().map(AdressePeriode.AdresseTypePeriode::new).toList();
                var applist = adressePerioderPeriodisert.stream().map(AdressePeriode.AdresseTypePeriode::new).toList();
                LOG.info("UKJENT ADRESSE odditet: OPfom {} OPtom {} bosted {} kontakt {} opphold {} mapped {} periodisert {}", fom, tom,
                    person.getBostedsadresse().size(), person.getKontaktadresse().size(), person.getOppholdsadresse().size(),
                    aplist, applist);
                var kommune = person.getBostedsadresse().stream()
                    .map(Bostedsadresse::getUkjentBosted).filter(Objects::nonNull)
                    .map(UkjentBosted::getBostedskommune).filter(Objects::nonNull)
                    .toList();
                if (!kommune.isEmpty())
                    LOG.info("UKJENT ADRESSE kommune: {}", kommune);
            }
        } catch (Exception e) {
            // Intentional
        }
        adressePerioderPeriodisert.stream()
                .filter(p -> p.getGyldighetsperiode().getTom().isAfter(fom) && p.getGyldighetsperiode().getFom().isBefore(tom))
                .forEach(builder::leggTil);

        return builder.build();
    }

    static String mapNavn(Navn navn) {
        return navn.getFornavn() + leftPad(navn.getMellomnavn()) + leftPad(navn.getEtternavn());
    }

    private static String leftPad(String navn) {
        return Optional.ofNullable(navn).map(n -> " " + navn).orElse("");
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

    private static PersonstatusPeriode mapPersonstatusHistorisk(Folkeregisterpersonstatus status) {
        var ajourFom = status.getFolkeregistermetadata().getAjourholdstidspunkt(); // TODO evaluer
        var gyldigFom = status.getFolkeregistermetadata().getGyldighetstidspunkt();
        Date brukFom;
        if (ajourFom != null && gyldigFom != null) {
            brukFom = ajourFom.before(gyldigFom) ? ajourFom : gyldigFom;
        } else {
            brukFom = gyldigFom != null ? gyldigFom : ajourFom;
        }
        var periode = periodeFraDates(brukFom, status.getFolkeregistermetadata().getOpphoerstidspunkt());
        return new PersonstatusPeriode(periode, PersonstatusType.fraFregPersonstatus(status.getStatus()));
    }

    private static List<PersonstatusPeriode> periodiserPersonstatus(List<PersonstatusPeriode> perioder) {
        var gyldighetsperioder = perioder.stream().map(PersonstatusPeriode::getGyldighetsperiode).toList();
        return perioder.stream()
                .map(p -> new PersonstatusPeriode(finnFraPerioder(gyldighetsperioder, p.getGyldighetsperiode()), p.getPersonstatus()))
                .toList();
    }

    private static List<AdressePeriode> periodiserAdresse(List<AdressePeriode> perioder) {
        var adresseTypePerioder = perioder.stream()
                .collect(Collectors.groupingBy(ap -> forSortering(ap.getAdresse().getAdresseType()),
                        Collectors.mapping(AdressePeriode::getGyldighetsperiode, Collectors.toList())));
        return perioder.stream()
                .map(p -> new AdressePeriode(
                        finnFraPerioder(adresseTypePerioder.get(forSortering(p.getAdresse().getAdresseType())), p.getGyldighetsperiode()),
                        p.getAdresse()))
                .filter(a -> !a.getGyldighetsperiode().getFom().isAfter(a.getGyldighetsperiode().getTom()))
                .toList();
    }

    private static AdresseType forSortering(AdresseType type) {
        if (Set.of(AdresseType.BOSTEDSADRESSE, AdresseType.UKJENT_ADRESSE).contains(type))
            return AdresseType.BOSTEDSADRESSE;
        if (Set.of(AdresseType.POSTADRESSE, AdresseType.POSTADRESSE_UTLAND).contains(type))
            return AdresseType.POSTADRESSE;
        return AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE;
    }

    private static Gyldighetsperiode finnFraPerioder(List<Gyldighetsperiode> alleperioder, Gyldighetsperiode periode) {
        if (alleperioder.stream().noneMatch(p -> p.getFom().isAfter(periode.getFom()) && p.getFom().isBefore(periode.getTom())))
            return periode;
        var tom = alleperioder.stream()
                .map(Gyldighetsperiode::getFom)
                .filter(d -> d.isAfter(periode.getFom()))
                .min(Comparator.naturalOrder())
                .map(d -> d.minusDays(1)).orElse(Tid.TIDENES_ENDE);
        return Gyldighetsperiode.innenfor(periode.getFom(), tom);
    }

    private static StatsborgerskapPeriode mapStatsborgerskapHistorikk(Statsborgerskap statsborgerskap) {
        var gyldigTil = statsborgerskap.getGyldigTilOgMed() == null ? null
                : LocalDate.parse(statsborgerskap.getGyldigTilOgMed(), DateTimeFormatter.ISO_LOCAL_DATE);
        var gyldigFra = statsborgerskap.getGyldigFraOgMed() == null ? null
                : LocalDate.parse(statsborgerskap.getGyldigFraOgMed(), DateTimeFormatter.ISO_LOCAL_DATE);
        return new StatsborgerskapPeriode(Gyldighetsperiode.innenfor(gyldigFra, gyldigTil),
                new no.nav.foreldrepenger.behandlingslager.aktør.Statsborgerskap(statsborgerskap.getLand()));
    }

    private static List<Landkoder> mapStatsborgerskap(List<Statsborgerskap> statsborgerskap) {
        var alleLand = statsborgerskap.stream()
                .map(Statsborgerskap::getLand)
                .map(Landkoder::fraKodeDefaultUdefinert)
                .toList();
        return alleLand.stream().anyMatch(Landkoder.NOR::equals) ? List.of(Landkoder.NOR) : alleLand;
    }

    private static Set<FamilierelasjonVL> mapFamilierelasjoner(List<ForelderBarnRelasjon> familierelasjoner, List<Sivilstand> sivilstandliste) {
        Set<FamilierelasjonVL> relasjoner = new HashSet<>();

        familierelasjoner.stream()
                .filter(r -> r.getRelatertPersonsIdent() != null)
                .map(r -> new FamilierelasjonVL(new PersonIdent(r.getRelatertPersonsIdent()), mapRelasjonsrolle(r.getRelatertPersonsRolle())))
                .forEach(relasjoner::add);
        sivilstandliste.stream()
                .filter(rel -> JURIDISK_GIFT.contains(rel.getType()))
                .filter(rel -> rel.getRelatertVedSivilstand() != null)
                .map(r -> new FamilierelasjonVL(new PersonIdent(r.getRelatertVedSivilstand()), mapRelasjonsrolle(r.getType())))
                .forEach(relasjoner::add);
        return relasjoner;
    }

    private static RelasjonsRolleType mapRelasjonsrolle(ForelderBarnRelasjonRolle type) {
        return ROLLE_FRA_FREG_ROLLE.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    private static RelasjonsRolleType mapRelasjonsrolle(Sivilstandstype type) {
        return ROLLE_FRA_FREG_STAND.getOrDefault(type, RelasjonsRolleType.UDEFINERT);
    }

    private List<AdressePeriode> mapAdresserHistorikk(List<Bostedsadresse> bostedsadresser, List<Kontaktadresse> kontaktadresser,
            List<Oppholdsadresse> oppholdsadresser) {
        List<AdressePeriode> adresser = new ArrayList<>();
        bostedsadresser.forEach(b -> {
            var periode = periodeFraDates(b.getGyldigFraOgMed(), b.getGyldigTilOgMed());
            var flyttedato = b.getAngittFlyttedato() != null ? LocalDate.parse(b.getAngittFlyttedato(), DateTimeFormatter.ISO_LOCAL_DATE)
                    : periode.getFom();
            var periode2 = flyttedato.isBefore(periode.getFom()) ? Gyldighetsperiode.innenfor(flyttedato, periode.getTom()) : periode;
            mapAdresser(List.of(b), List.of(), List.of(), true).forEach(a -> adresser.add(mapAdresseinfoTilAdressePeriode(periode2, a, flyttedato, periode.getFom(), b.getMetadata())));
        });
        kontaktadresser.forEach(k -> {
            var periode = periodeFraDates(k.getGyldigFraOgMed(), k.getGyldigTilOgMed());
            mapAdresser(List.of(), List.of(k), List.of(), false).forEach(a -> adresser.add(mapAdresseinfoTilAdressePeriode(periode, a, periode.getFom(), periode.getFom(), k.getMetadata())));
        });
        oppholdsadresser.forEach(o -> {
            var periode = periodeFraDates(o.getGyldigFraOgMed(), o.getGyldigTilOgMed());
            mapAdresser(List.of(), List.of(), List.of(o), false).forEach(a -> adresser.add(mapAdresseinfoTilAdressePeriode(periode, a, periode.getFom(), periode.getFom(), o.getMetadata())));
        });
        return adresser;
    }

    private static Gyldighetsperiode periodeFraDates(Date dateFom, Date dateTom) {
        var gyldigTil = dateTom == null ? null : LocalDateTime.ofInstant(dateTom.toInstant(), ZoneId.systemDefault()).toLocalDate();
        var gyldigFra = dateFom == null ? null : LocalDateTime.ofInstant(dateFom.toInstant(), ZoneId.systemDefault()).toLocalDate();
        return Gyldighetsperiode.innenfor(gyldigFra, gyldigTil);
    }

    private static AdressePeriode mapAdresseinfoTilAdressePeriode(Gyldighetsperiode periode, Adresseinfo adresseinfo, LocalDate flyttedato, LocalDate gyldigFom, Metadata metadata) {
        var ap = AdressePeriode.builder().medGyldighetsperiode(periode)
                .medMatrikkelId(adresseinfo.getMatrikkelId())
                .medAdresselinje1(adresseinfo.getAdresselinje1())
                .medAdresselinje2(adresseinfo.getAdresselinje2())
                .medAdresselinje3(adresseinfo.getAdresselinje3())
                .medAdresselinje4(adresseinfo.getAdresselinje4())
                .medAdresseType(adresseinfo.getGjeldendePostadresseType())
                .medPostnummer(adresseinfo.getPostNr())
                .medPoststed(adresseinfo.getPoststed())
                .medLand(adresseinfo.getLand())
                .build();
        ap.setFlyttedato(flyttedato);
        ap.setGyldigFomDato(gyldigFom);
        ap.setHistorisk(metadata != null && metadata.getHistorisk());
        return ap;
    }

    private List<Adresseinfo> mapAdresser(List<Bostedsadresse> bostedsadresser,
                                          List<Kontaktadresse> kontaktadresser,
                                          List<Oppholdsadresse> oppholdsadresser,
                                          boolean leggTilUkjentHvisIngenAdresser) {
        List<Adresseinfo> resultat = new ArrayList<>();
        bostedsadresser.stream().map(Bostedsadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.BOSTEDSADRESSE, a)).filter(Objects::nonNull)
                .forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getMatrikkeladresse).map(a -> mapMatrikkeladresse(AdresseType.BOSTEDSADRESSE, a))
                .filter(Objects::nonNull).forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getUkjentBosted).filter(Objects::nonNull).map(PersoninfoTjeneste::mapUkjentadresse)
                .forEach(resultat::add);
        bostedsadresser.stream().map(Bostedsadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.BOSTEDSADRESSE_UTLAND, a))
                .filter(Objects::nonNull).forEach(resultat::add);

        oppholdsadresser.stream().map(Oppholdsadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a))
                .filter(Objects::nonNull).forEach(resultat::add);
        oppholdsadresser.stream().map(Oppholdsadresse::getMatrikkeladresse)
                .map(a -> mapMatrikkeladresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).filter(Objects::nonNull).forEach(resultat::add);
        oppholdsadresser.stream().map(Oppholdsadresse::getUtenlandskAdresse)
                .map(a -> mapUtenlandskadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);

        kontaktadresser.stream().map(Kontaktadresse::getVegadresse).map(a -> mapVegadresse(AdresseType.POSTADRESSE, a)).filter(Objects::nonNull)
                .forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getPostboksadresse).map(a -> mapPostboksadresse(AdresseType.POSTADRESSE, a))
                .filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getPostadresseIFrittFormat).map(a -> mapFriAdresseNorsk(AdresseType.POSTADRESSE, a))
                .filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getUtenlandskAdresse).map(a -> mapUtenlandskadresse(AdresseType.POSTADRESSE_UTLAND, a))
                .filter(Objects::nonNull).forEach(resultat::add);
        kontaktadresser.stream().map(Kontaktadresse::getUtenlandskAdresseIFrittFormat)
                .map(a -> mapFriAdresseUtland(AdresseType.POSTADRESSE_UTLAND, a)).filter(Objects::nonNull).forEach(resultat::add);
        if (resultat.isEmpty() && leggTilUkjentHvisIngenAdresser) {
            resultat.add(mapUkjentadresse(null));
        }
        return resultat;
    }

    private Adresseinfo mapVegadresse(AdresseType type, Vegadresse vegadresse) {
        if (vegadresse == null)
            return null;
        var postnummer = Optional.ofNullable(vegadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var gateadresse = vegadresse.getAdressenavn().toUpperCase() + hvisfinnes2(vegadresse.getHusnummer(), vegadresse.getHusbokstav());
        return Adresseinfo.builder(type)
                .medMatrikkelId(vegadresse.getMatrikkelId())
                .medAdresselinje1(gateadresse)
                .medPostNr(postnummer)
                .medPoststed(tilPoststed(postnummer))
                .medLand(Landkoder.NOR.getKode())
                .build();
    }

    private Adresseinfo mapMatrikkeladresse(AdresseType type, Matrikkeladresse matrikkeladresse) {
        if (matrikkeladresse == null)
            return null;
        var postnummer = Optional.ofNullable(matrikkeladresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(type)
                .medMatrikkelId(matrikkeladresse.getMatrikkelId())
                .medAdresselinje1(matrikkeladresse.getTilleggsnavn() != null ? matrikkeladresse.getTilleggsnavn().toUpperCase()
                        : matrikkeladresse.getBruksenhetsnummer())
                .medAdresselinje2(matrikkeladresse.getTilleggsnavn() != null ? matrikkeladresse.getBruksenhetsnummer() : null)
                .medPostNr(postnummer)
                .medPoststed(tilPoststed(postnummer))
                .medLand(Landkoder.NOR.getKode())
                .build();
    }

    private Adresseinfo mapPostboksadresse(AdresseType type, Postboksadresse postboksadresse) {
        if (postboksadresse == null)
            return null;
        var postnummer = Optional.ofNullable(postboksadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var postboks = "Postboks" + hvisfinnes(postboksadresse.getPostboks());
        return Adresseinfo.builder(type)
                .medAdresselinje1(postboksadresse.getPostbokseier() != null ? postboksadresse.getPostbokseier().toUpperCase() : postboks)
                .medAdresselinje2(postboksadresse.getPostbokseier() != null ? postboks : null)
                .medPostNr(postnummer)
                .medPoststed(tilPoststed(postnummer))
                .medLand(Landkoder.NOR.getKode())
                .build();
    }

    private Adresseinfo mapFriAdresseNorsk(AdresseType type, PostadresseIFrittFormat postadresse) {
        if (postadresse == null)
            return null;
        var postnummer = Optional.ofNullable(postadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(type)
                .medAdresselinje1(postadresse.getAdresselinje1() != null ? postadresse.getAdresselinje1().toUpperCase() : null)
                .medAdresselinje2(postadresse.getAdresselinje2() != null ? postadresse.getAdresselinje2().toUpperCase() : null)
                .medAdresselinje3(postadresse.getAdresselinje3() != null ? postadresse.getAdresselinje3().toUpperCase() : null)
                .medPostNr(postnummer)
                .medPoststed(tilPoststed(postnummer))
                .medLand(Landkoder.NOR.getKode())
                .build();
    }

    private static Adresseinfo mapUkjentadresse(UkjentBosted ukjentBosted) {
        return Adresseinfo.builder(AdresseType.UKJENT_ADRESSE).medLand(Landkoder.XUK.getKode()).build();
    }

    private static Adresseinfo mapUtenlandskadresse(AdresseType type, UtenlandskAdresse utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var linje1 = hvisfinnes(utenlandskAdresse.getAdressenavnNummer()) + hvisfinnes(utenlandskAdresse.getBygningEtasjeLeilighet())
                + hvisfinnes(utenlandskAdresse.getPostboksNummerNavn());
        var linje2 = hvisfinnes(utenlandskAdresse.getPostkode()) + hvisfinnes(utenlandskAdresse.getBySted())
                + hvisfinnes(utenlandskAdresse.getRegionDistriktOmraade());
        return Adresseinfo.builder(type)
                .medAdresselinje1(linje1)
                .medAdresselinje2(linje2)
                .medAdresselinje3(utenlandskAdresse.getLandkode())
                .medLand(utenlandskAdresse.getLandkode())
                .build();
    }

    private static Adresseinfo mapFriAdresseUtland(AdresseType type, UtenlandskAdresseIFrittFormat utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var postlinje = hvisfinnes(utenlandskAdresse.getPostkode()) + hvisfinnes(utenlandskAdresse.getByEllerStedsnavn());
        var sisteline = utenlandskAdresse.getAdresselinje3() != null ?
            postlinje + utenlandskAdresse.getLandkode() : utenlandskAdresse.getAdresselinje2() != null ? utenlandskAdresse.getLandkode() : null;
        return Adresseinfo.builder(type)
                .medAdresselinje1(utenlandskAdresse.getAdresselinje1())
                .medAdresselinje2(utenlandskAdresse.getAdresselinje2() != null ? utenlandskAdresse.getAdresselinje2().toUpperCase() : postlinje)
                .medAdresselinje3(utenlandskAdresse.getAdresselinje3() != null ? utenlandskAdresse.getAdresselinje3().toUpperCase() :
                    utenlandskAdresse.getAdresselinje2() != null ? postlinje : utenlandskAdresse.getLandkode())
                .medAdresselinje4(sisteline)
                .medLand(utenlandskAdresse.getLandkode())
                .build();
    }

    private static String hvisfinnes(Object object) {
        return object == null ? "" : " " + object.toString().trim().toUpperCase();
    }

    private static String hvisfinnes2(Object object1, Object object2) {
        if (object1 == null && object2 == null)
            return "";
        if (object1 != null && object2 != null)
            return " " + object1.toString().trim().toUpperCase() + object2.toString().trim().toUpperCase();
        return object2 == null ? " " + object1.toString().trim().toUpperCase() : " " + object2.toString().trim().toUpperCase();
    }

    private String tilPoststed(String postnummer) {
        if (HARDKODET_POSTNR.equals(postnummer)) {
            return HARDKODET_POSTSTED;
        }
        return poststedKodeverkRepository.finnPoststedReadOnly(postnummer).map(Poststed::getPoststednavn).orElse(HARDKODET_POSTSTED);
    }

    private static boolean relevantOppholdstillatelse(Opphold opphold) {
        return Oppholdstillatelse.PERMANENT.equals(opphold.getType()) || Oppholdstillatelse.MIDLERTIDIG.equals(opphold.getType());

    }

    private static OppholdstillatelsePeriode mapOppholdsHistorikk(Opphold opphold) {
        var type = TILLATELSE_FRA_FREG_OPPHOLD.get(opphold.getType());
        var gyldigTil = opphold.getOppholdTil() == null ? null : LocalDate.parse(opphold.getOppholdTil(), DateTimeFormatter.ISO_LOCAL_DATE);
        var gyldigFra = opphold.getOppholdFra() == null ? null : LocalDate.parse(opphold.getOppholdFra(), DateTimeFormatter.ISO_LOCAL_DATE);
        return new OppholdstillatelsePeriode(Gyldighetsperiode.innenfor(gyldigFra, gyldigTil), type);
    }

}
