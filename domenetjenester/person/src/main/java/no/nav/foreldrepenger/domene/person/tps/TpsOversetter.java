package no.nav.foreldrepenger.domene.person.tps;

import static java.util.stream.Collectors.toSet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.Familierelasjon;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Aktoer;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Diskresjonskoder;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Spraak;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Statsborgerskap;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;

@ApplicationScoped
public class TpsOversetter {

    private TpsAdresseOversetter tpsAdresseOversetter;

    TpsOversetter() {
        // for CDI proxy
    }

    @Inject
    public TpsOversetter(TpsAdresseOversetter tpsAdresseOversetter) {

        this.tpsAdresseOversetter = tpsAdresseOversetter;
    }

    private Landkoder utledLandkode(Statsborgerskap statsborgerskap) {
        Landkoder landkode = Landkoder.UDEFINERT;
        if (Optional.ofNullable(statsborgerskap).isPresent()) {
            landkode = Landkoder.fraKode(statsborgerskap.getLand().getValue());
        }
        return landkode;
    }

    public Personinfo tilBrukerInfo(AktørId aktørId, Bruker bruker) { // NOSONAR - ingen forbedring å forkorte metoden her
        String navn = bruker.getPersonnavn().getSammensattNavn();
        String adresse = tpsAdresseOversetter.finnAdresseFor(bruker);
        String adresseLandkode = tpsAdresseOversetter.finnAdresseLandkodeFor(bruker);
        String utlandsadresse = tpsAdresseOversetter.finnUtlandsadresseFor(bruker);

        LocalDate fødselsdato = finnFødselsdato(bruker);
        LocalDate dødsdato = finnDødsdato(bruker);

        Aktoer aktoer = bruker.getAktoer();
        PersonIdent pi = (PersonIdent) aktoer;
        String ident = pi.getIdent().getIdent();
        NavBrukerKjønn kjønn = tilBrukerKjønn(bruker.getKjoenn());
        PersonstatusType personstatus = tilPersonstatusType(bruker.getPersonstatus());
        Set<Familierelasjon> familierelasjoner = bruker.getHarFraRolleI().stream()
            .map(this::tilRelasjon)
            .collect(toSet());

        Landkoder landkoder = utledLandkode(bruker.getStatsborgerskap());
        Region region = MapRegionLandkoder.mapLandkode(landkoder.getKode());

        String diskresjonskode = bruker.getDiskresjonskode() == null ? null : bruker.getDiskresjonskode().getValue();
        String geografiskTilknytning = bruker.getGeografiskTilknytning() != null ? bruker.getGeografiskTilknytning().getGeografiskTilknytning() : null;

        List<Adresseinfo> adresseinfoList = tpsAdresseOversetter.lagListeMedAdresseInfo(bruker);
        SivilstandType sivilstandType = bruker.getSivilstand() == null ? null : SivilstandType.fraKode(bruker.getSivilstand().getSivilstand().getValue());

        return new Personinfo.Builder()
            .medAktørId(aktørId)
            .medPersonIdent(no.nav.foreldrepenger.domene.typer.PersonIdent.fra(ident))
            .medNavn(navn)
            .medAdresse(adresse)
            .medAdresseLandkode(adresseLandkode)
            .medFødselsdato(fødselsdato)
            .medDødsdato(dødsdato)
            .medNavBrukerKjønn(kjønn)
            .medPersonstatusType(personstatus)
            .medStatsborgerskap(new no.nav.foreldrepenger.behandlingslager.aktør.Statsborgerskap(landkoder.getKode()))
            .medRegion(region)
            .medFamilierelasjon(familierelasjoner)
            .medUtlandsadresse(utlandsadresse)
            .medForetrukketSpråk(bestemForetrukketSpråk(bruker))
            .medGegrafiskTilknytning(geografiskTilknytning)
            .medDiskresjonsKode(diskresjonskode)
            .medAdresseInfoList(adresseinfoList)
            .medSivilstandType(sivilstandType)
            .medLandkode(landkoder)
            .build();
    }

    public Personhistorikkinfo tilPersonhistorikkInfo(String aktørId, HentPersonhistorikkResponse response) {

        Personhistorikkinfo.Builder builder = Personhistorikkinfo
            .builder()
            .medAktørId(aktørId);

        konverterPersonstatusPerioder(response, builder);

        konverterStatsborgerskapPerioder(response, builder);

        tpsAdresseOversetter.konverterBostedadressePerioder(response, builder);

        tpsAdresseOversetter.konverterPostadressePerioder(response, builder);

        tpsAdresseOversetter.konverterMidlertidigAdressePerioder(response, builder);

        return builder.build();
    }

    private void konverterPersonstatusPerioder(HentPersonhistorikkResponse response, Personhistorikkinfo.Builder builder) {
        Optional.ofNullable(response.getPersonstatusListe()).ifPresent(list -> {
            list.forEach(e -> {
                Personstatus personstatus = new Personstatus();
                personstatus.setPersonstatus(e.getPersonstatus());
                PersonstatusType personstatusType = tilPersonstatusType(personstatus);

                Gyldighetsperiode gyldighetsperiode = Gyldighetsperiode.innenfor(
                    DateUtil.convertToLocalDate(e.getPeriode().getFom()),
                    DateUtil.convertToLocalDate(e.getPeriode().getTom()));

                PersonstatusPeriode periode = new PersonstatusPeriode(gyldighetsperiode, personstatusType);
                builder.leggTil(periode);
            });
        });
    }

    private void konverterStatsborgerskapPerioder(HentPersonhistorikkResponse response, Personhistorikkinfo.Builder builder) {
        Optional.ofNullable(response.getStatsborgerskapListe()).ifPresent(list -> {
            list.forEach(e -> {
                Gyldighetsperiode gyldighetsperiode = Gyldighetsperiode.innenfor(
                    DateUtil.convertToLocalDate(e.getPeriode().getFom()),
                    DateUtil.convertToLocalDate(e.getPeriode().getTom()));

                Landkoder landkoder = Landkoder.fraKode(e.getStatsborgerskap().getLand().getValue());
                StatsborgerskapPeriode element = new StatsborgerskapPeriode(gyldighetsperiode,
                    new no.nav.foreldrepenger.behandlingslager.aktør.Statsborgerskap(landkoder.getKode()));
                builder.leggTil(element);
            });
        });
    }

    private LocalDate finnDødsdato(Bruker person) {
        LocalDate dødsdato = null;
        Doedsdato dødsdatoJaxb = person.getDoedsdato();
        if (dødsdatoJaxb != null) {
            dødsdato = DateUtil.convertToLocalDate(dødsdatoJaxb.getDoedsdato());
        }
        return dødsdato;
    }

    private LocalDate finnFødselsdato(Bruker person) {
        LocalDate fødselsdato = null;
        Foedselsdato fødselsdatoJaxb = person.getFoedselsdato();
        if (fødselsdatoJaxb != null) {
            fødselsdato = DateUtil.convertToLocalDate(fødselsdatoJaxb.getFoedselsdato());
        }
        return fødselsdato;
    }

    private Språkkode bestemForetrukketSpråk(Bruker person) {
        Språkkode defaultSpråk = Språkkode.NB;
        Spraak språk = person.getMaalform();
        // For å slippe å håndtere foreldet forkortelse "NO" andre steder i løsningen
        if (språk == null || "NO".equals(språk.getValue())) {
            return defaultSpråk;
        }
        return Språkkode.finnForKodeverkEiersKode(språk.getValue());
    }

    GeografiskTilknytning tilGeografiskTilknytning(no.nav.tjeneste.virksomhet.person.v3.informasjon.GeografiskTilknytning geografiskTilknytning,
                                                   Diskresjonskoder diskresjonskoder) {
        String geoTilkn = geografiskTilknytning != null ? geografiskTilknytning.getGeografiskTilknytning() : null;
        String diskKode = diskresjonskoder != null ? diskresjonskoder.getValue() : null;
        return new GeografiskTilknytning(geoTilkn, diskKode);
    }

    public List<GeografiskTilknytning> tilDiskresjonsKoder(Person person) {
        List<String> foreldreKoder = Arrays.asList(RelasjonsRolleType.MORA.getKode(), RelasjonsRolleType.FARA.getKode());

        return person.getHarFraRolleI().stream()
            .filter(rel -> !foreldreKoder.contains(rel.getTilRolle().getValue()))
            .map(this::relasjonTilGeoMedDiskresjon)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private GeografiskTilknytning relasjonTilGeoMedDiskresjon(no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon familierelasjon) {
        Person person = familierelasjon.getTilPerson();

        String diskresjonskode = person.getDiskresjonskode() == null ? null : person.getDiskresjonskode().getValue();

        return diskresjonskode == null ? null : new GeografiskTilknytning(null, diskresjonskode);
    }

    private Familierelasjon tilRelasjon(no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon familierelasjon) {
        String rollekode = familierelasjon.getTilRolle().getValue();
        RelasjonsRolleType relasjonsrolle = RelasjonsRolleType.fraKode(rollekode);
        String adresse = tpsAdresseOversetter.finnAdresseFor(familierelasjon.getTilPerson());
        PersonIdent personIdent = (PersonIdent) familierelasjon.getTilPerson().getAktoer();
        no.nav.foreldrepenger.domene.typer.PersonIdent ident = no.nav.foreldrepenger.domene.typer.PersonIdent.fra(personIdent.getIdent().getIdent());
        Boolean harSammeBosted = familierelasjon.isHarSammeBosted();

        return new Familierelasjon(ident, relasjonsrolle,
            tilLocalDate(familierelasjon.getTilPerson().getFoedselsdato()), adresse, harSammeBosted);
    }

    private NavBrukerKjønn tilBrukerKjønn(Kjoenn kjoenn) {
        return Optional.ofNullable(kjoenn)
            .map(Kjoenn::getKjoenn)
            .map(kj -> NavBrukerKjønn.fraKode(kj.getValue()))
            .orElse(NavBrukerKjønn.UDEFINERT);
    }

    private PersonstatusType tilPersonstatusType(Personstatus personstatus) {
        return PersonstatusType.fraKode(personstatus.getPersonstatus().getValue());
    }

    private LocalDate tilLocalDate(Foedselsdato fødselsdatoJaxb) {
        if (fødselsdatoJaxb != null) {
            return DateUtil.convertToLocalDate(fødselsdatoJaxb.getFoedselsdato());
        }
        return null;
    }

    public Adresseinfo tilAdresseInfo(Person person) {
        return tpsAdresseOversetter.tilAdresseInfo(person);
    }

    public List<FødtBarnInfo> tilFødteBarn(Bruker person) {

        return person.getHarFraRolleI().stream()
            .filter(f -> erBarnRolle(f.getTilRolle()))
            .map(this::relasjonTilPersoninfo)
            .collect(Collectors.toList());
    }

    boolean erBarnRolle(Familierelasjoner familierelasjoner) {
        return familierelasjoner.getValue().matches(RelasjonsRolleType.BARN.getKode());
    }

    FødtBarnInfo relasjonTilPersoninfo(no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon familierelasjon) {
        Person person = familierelasjon.getTilPerson();

        String identNr = ((PersonIdent) person.getAktoer()).getIdent().getIdent();
        no.nav.foreldrepenger.domene.typer.PersonIdent ident = no.nav.foreldrepenger.domene.typer.PersonIdent.fra(identNr);
        Foedselsdato foedselsdato = person.getFoedselsdato();
        LocalDate fødselLocalDate;
        if (foedselsdato == null) {
            fødselLocalDate = utledFødselsDatoFraIdent(ident);
        } else {
            fødselLocalDate = tilLocalDate(foedselsdato);
        }
        Doedsdato doedsdato = person.getDoedsdato();
        LocalDate dødsLocalDate = null;
        if (doedsdato != null) {
            GregorianCalendar gregCal = doedsdato.getDoedsdato().toGregorianCalendar();
            dødsLocalDate = gregCal.toZonedDateTime().toLocalDate();
        } else if (ident.erFdatNummer() && ident.getIdent().endsWith("1")) { //Dødfødt barn
            dødsLocalDate = fødselLocalDate;
        }
        String navn = person.getPersonnavn() != null ? person.getPersonnavn().getSammensattNavn() : FødtBarnInfo.UTEN_NAVN;
        NavBrukerKjønn kjønn = tilBrukerKjønn(person.getKjoenn());

        return new FødtBarnInfo.Builder()
            .medIdent(ident)
            .medNavn(navn)
            .medNavBrukerKjønn(kjønn)
            .medFødselsdato(fødselLocalDate)
            .medDødsdato(dødsLocalDate)
            .build();
    }

    private LocalDate utledFødselsDatoFraIdent(no.nav.foreldrepenger.domene.typer.PersonIdent ident) {
        if (ident.erFdatNummer()) {
            DateTimeFormatter identFormatter = DateTimeFormatter.ofPattern("ddMMyy");

            return LocalDate.parse(ident.getIdent().substring(0, 6), identFormatter);
        }
        throw new IllegalArgumentException("Kan bare utledes basert på fdatnr.");
    }

    FødtBarnInfo tilFødteBarn(Personinfo personinfo) {
        return new FødtBarnInfo.Builder()
            .medIdent(personinfo.getPersonIdent())
            .medNavn(personinfo.getNavn())
            .medNavBrukerKjønn(personinfo.getKjønn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medDødsdato(personinfo.getDødsdato())
            .build();
    }
}
