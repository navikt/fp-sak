package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.FamilierelasjonVL;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.ForelderBarnRelasjon;
import no.nav.pdl.ForelderBarnRelasjonResponseProjection;
import no.nav.pdl.ForelderBarnRelasjonRolle;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.KjoennType;
import no.nav.pdl.Navn;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.Sivilstand;
import no.nav.pdl.SivilstandResponseProjection;
import no.nav.pdl.Sivilstandstype;

@ApplicationScoped
public class PersoninfoTjeneste {

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

    private PdlKlientLogCause pdlKlient;
    private AdresseMapper adresseMapper;

    PersoninfoTjeneste() {
        // CDI
    }

    @Inject
    public PersoninfoTjeneste(PdlKlientLogCause pdlKlient, AdresseMapper mapper) {
        this.pdlKlient = pdlKlient;
        this.adresseMapper = mapper;
    }

    public boolean brukerManglerAdresse(FagsakYtelseType ytelseType, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(personIdent.getIdent());

        var person = pdlKlient.hentPerson(ytelseType, query, AdresseMapper.leggTilAdresseQuery(new PersonResponseProjection(), false));

        var adresser = adresseMapper.mapAdresser(person, Gyldighetsperiode.innenfor(null, null), null);

        return adresser.isEmpty() || adresser.stream().map(ap -> ap.adresse().getAdresseType()).allMatch(AdresseType.UKJENT_ADRESSE::equals);
    }

    public Personinfo hentPersoninfo(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent, boolean erBarn) {

        var query = new HentPersonQueryRequest();
        query.setIdent(personIdent.getIdent());

        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .doedsfall(new DoedsfallResponseProjection().doedsdato())
            .kjoenn(new KjoennResponseProjection().kjoenn())
            .sivilstand(new SivilstandResponseProjection().relatertVedSivilstand().type())
            .forelderBarnRelasjon(new ForelderBarnRelasjonResponseProjection().relatertPersonsRolle().relatertPersonsIdent().minRolleForPerson());

        projection = AnnetPeriodisertMapper.leggTilAnnetPeriodisertQuery(projection, false);
        projection = AdresseMapper.leggTilAdresseQuery(projection, false);
        if (erBarn) {
            projection = AdresseMapper.leggTilDeltAdresseQuery(projection);
        }

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var fødselsdato = person.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var dødssdato = person.getDoedsfall().stream()
            .map(Doedsfall::getDoedsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        var sivilstand = person.getSivilstand().stream()
            .map(Sivilstand::getType)
            .findFirst()
            .map(st -> SIVSTAND_FRA_FREG.getOrDefault(st, SivilstandType.UOPPGITT)).orElse(SivilstandType.UOPPGITT);
        var familierelasjoner = mapFamilierelasjoner(person.getForelderBarnRelasjon(), person.getSivilstand());

        var filter = Gyldighetsperiode.innenfor(null, null);
        var pdlStatus = AnnetPeriodisertMapper.mapPersonstatus(person.getFolkeregisterpersonstatus(), filter, fødselsdato);
        var statsborgerskap = AnnetPeriodisertMapper.mapStatsborgerskap(person.getStatsborgerskap(), filter, fødselsdato);
        var adresser = adresseMapper.mapAdresser(person, filter, fødselsdato);

        // Opphørte personer kan mangle fødselsdato mm. Håndtere dette + gi feil hvis fødselsdato mangler i andre tilfelle
        if (fødselsdato == null && !pdlStatus.isEmpty() && pdlStatus.stream().allMatch(ps -> PersonstatusType.UTPE.equals(ps.personstatus()))) {
            return null;
        }

        return new Personinfo.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
            .medNavn(person.getNavn().stream().map(PersoninfoTjeneste::mapNavn).findFirst().orElse("MANGLER NAVN"))
            .medFødselsdato(fødselsdato)
            .medDødsdato(dødssdato)
            .medNavBrukerKjønn(mapKjønn(person))
            .medPersonstatusPerioder(pdlStatus)
            .medSivilstandType(sivilstand)
            .medLandkoder(statsborgerskap)
            .medFamilierelasjon(familierelasjoner)
            .medAdressePerioder(adresser)
            .build();
    }

    public Personhistorikkinfo hentPersoninfoHistorikk(FagsakYtelseType ytelseType, AktørId aktørId, LocalDate fødselsdato, AbstractLocalDateInterval intervall) {

        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());

        var projection = new PersonResponseProjection();
        projection = AnnetPeriodisertMapper.leggTilAnnetPeriodisertQuery(projection, true);
        projection = AnnetPeriodisertMapper.leggTilOppholdQuery(projection, true);
        projection = AdresseMapper.leggTilAdresseQuery(projection, true);

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var filter = Gyldighetsperiode.innenfor(intervall.getFomDato(), intervall.getTomDato());
        return new Personhistorikkinfo(AnnetPeriodisertMapper.mapPersonstatus(person.getFolkeregisterpersonstatus(), filter, fødselsdato),
            AnnetPeriodisertMapper.mapOpphold(person.getOpphold(), filter, fødselsdato),
            AnnetPeriodisertMapper.mapStatsborgerskap(person.getStatsborgerskap(), filter, fødselsdato),
            adresseMapper.mapAdresser(person, filter, fødselsdato));
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

}
