package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Poststed;
import no.nav.foreldrepenger.behandlingslager.geografisk.PoststedKodeverkRepository;
import no.nav.pdl.Bostedsadresse;
import no.nav.pdl.BostedsadresseResponseProjection;
import no.nav.pdl.DeltBosted;
import no.nav.pdl.DeltBostedResponseProjection;
import no.nav.pdl.Kontaktadresse;
import no.nav.pdl.KontaktadresseResponseProjection;
import no.nav.pdl.Matrikkeladresse;
import no.nav.pdl.MatrikkeladresseResponseProjection;
import no.nav.pdl.Oppholdsadresse;
import no.nav.pdl.OppholdsadresseResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonBostedsadresseParametrizedInput;
import no.nav.pdl.PersonKontaktadresseParametrizedInput;
import no.nav.pdl.PersonOppholdsadresseParametrizedInput;
import no.nav.pdl.PersonResponseProjection;
import no.nav.pdl.PostadresseIFrittFormat;
import no.nav.pdl.PostadresseIFrittFormatResponseProjection;
import no.nav.pdl.Postboksadresse;
import no.nav.pdl.PostboksadresseResponseProjection;
import no.nav.pdl.UkjentBosted;
import no.nav.pdl.UkjentBostedResponseProjection;
import no.nav.pdl.UtenlandskAdresse;
import no.nav.pdl.UtenlandskAdresseIFrittFormat;
import no.nav.pdl.UtenlandskAdresseIFrittFormatResponseProjection;
import no.nav.pdl.UtenlandskAdresseResponseProjection;
import no.nav.pdl.Vegadresse;
import no.nav.pdl.VegadresseResponseProjection;

@Dependent
public class AdresseMapper {

    private static final String HARDKODET_POSTNR = "XXXX";
    private static final String HARDKODET_POSTSTED = "UKJENT";

    private final PoststedKodeverkRepository poststedKodeverkRepository;

    @Inject
    public AdresseMapper(PoststedKodeverkRepository poststedKodeverkRepository) {
        this.poststedKodeverkRepository = poststedKodeverkRepository;
    }

    static PersonResponseProjection leggTilAdresseQuery(PersonResponseProjection query, boolean historikk) {
        return query
            .bostedsadresse(new PersonBostedsadresseParametrizedInput().historikk(historikk), new BostedsadresseResponseProjection()
                .angittFlyttedato().gyldigFraOgMed().gyldigTilOgMed()
                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                .ukjentBosted(new UkjentBostedResponseProjection().bostedskommune())
                .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn()
                    .bySted().regionDistriktOmraade().postkode().landkode()))
            .oppholdsadresse(new PersonOppholdsadresseParametrizedInput().historikk(historikk), new OppholdsadresseResponseProjection()
                .gyldigFraOgMed().gyldigTilOgMed()
                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer())
                .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn()
                    .bySted().regionDistriktOmraade().postkode().landkode()))
            .kontaktadresse(new PersonKontaktadresseParametrizedInput().historikk(historikk), new KontaktadresseResponseProjection()
                .type().gyldigFraOgMed().gyldigTilOgMed()
                .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
                .postboksadresse(new PostboksadresseResponseProjection().postboks().postbokseier().postnummer())
                .postadresseIFrittFormat(new PostadresseIFrittFormatResponseProjection().adresselinje1().adresselinje2().adresselinje3().postnummer())
                .utenlandskAdresse(new UtenlandskAdresseResponseProjection().adressenavnNummer().bygningEtasjeLeilighet().postboksNummerNavn()
                    .bySted().regionDistriktOmraade().postkode().landkode())
                .utenlandskAdresseIFrittFormat(new UtenlandskAdresseIFrittFormatResponseProjection().adresselinje1().adresselinje2()
                    .adresselinje3().byEllerStedsnavn().postkode().landkode()));
    }

    static PersonResponseProjection leggTilDeltAdresseQuery(PersonResponseProjection query) {
        return query.deltBosted(new DeltBostedResponseProjection().startdatoForKontrakt().sluttdatoForKontrakt()
            .vegadresse(new VegadresseResponseProjection().matrikkelId().adressenavn().husnummer().husbokstav().postnummer())
            .matrikkeladresse(new MatrikkeladresseResponseProjection().matrikkelId().bruksenhetsnummer().tilleggsnavn().postnummer()));
    }


    List<AdressePeriode> mapAdresser(Person person, Gyldighetsperiode filter, LocalDate fødsel) {

        var bp = person.getBostedsadresse().stream()
            .flatMap(b -> {
                var periode = Gyldighetsperiode.fraDates(b.getGyldigFraOgMed(), b.getGyldigTilOgMed());
                var flyttedato = Optional.ofNullable(b.getAngittFlyttedato())
                    .map(f -> LocalDate.parse(f, DateTimeFormatter.ISO_LOCAL_DATE)).orElseGet(periode::fom);
                var periode2 = flyttedato.isBefore(periode.fom()) ? Gyldighetsperiode.innenfor(flyttedato, periode.tom()) : periode;
                return mapBostedsadresse(b).stream().map(a -> new AdressePeriode(periode2, a));
            })
            .collect(Collectors.collectingAndThen(Collectors.toList(), AdresseMapper::periodiserAdresse));
        var adresser = new ArrayList<>(bp);

        var kp = person.getKontaktadresse().stream()
            .flatMap(k -> {
                var periode = Gyldighetsperiode.fraDates(k.getGyldigFraOgMed(), k.getGyldigTilOgMed());
                return mapKontaktadresse(k).stream().map(a -> new AdressePeriode(periode, a));
            })
            .collect(Collectors.collectingAndThen(Collectors.toList(), AdresseMapper::periodiserAdresse));
        adresser.addAll(kp);

        var op = person.getOppholdsadresse().stream()
            .flatMap(o -> {
                var periode = Gyldighetsperiode.fraDates(o.getGyldigFraOgMed(), o.getGyldigTilOgMed());
                return mapOppholdsadresse(o).stream().map(a -> new AdressePeriode(periode, a));
            })
            .collect(Collectors.collectingAndThen(Collectors.toList(), AdresseMapper::periodiserAdresse));
        adresser.addAll(op);

        var dbp = person.getDeltBosted().stream()
            .flatMap(d -> {
                var fom = Optional.ofNullable(d.getStartdatoForKontrakt())
                    .map(s -> LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
                var tom = Optional.ofNullable(d.getSluttdatoForKontrakt())
                    .map(s -> LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
                var periode = Gyldighetsperiode.innenfor(fom, tom);
                return mapDeltBosted(d).stream().map(a -> new AdressePeriode(periode, a));
            })
            .collect(Collectors.collectingAndThen(Collectors.toList(), AdresseMapper::periodiserAdresse));
        adresser.addAll(dbp);

        return adresser.stream()
            .filter(p -> p.gyldighetsperiode().overlapper(filter))
            .map(p -> new AdressePeriode(AnnetPeriodisertMapper.fødselsjuster(p.gyldighetsperiode(), fødsel), p.adresse()))
            .filter(p -> p.gyldighetsperiode() != null)
            .toList();
    }

    private static List<AdressePeriode> periodiserAdresse(List<AdressePeriode> perioder) {
        var allePerioder = perioder.stream().map(AdressePeriode::gyldighetsperiode).toList();
        return perioder.stream()
            .map(p -> new AdressePeriode(Gyldighetsperiode.justerForSenere(allePerioder, p.gyldighetsperiode()), p.adresse()))
            .filter(a -> !a.gyldighetsperiode().fom().isAfter(a.gyldighetsperiode().tom()))
            .toList();
    }

    private List<Adresseinfo> mapBostedsadresse(Bostedsadresse bostedsadresse) {
        List<Adresseinfo> resultat = new ArrayList<>();
        Optional.ofNullable(bostedsadresse.getVegadresse()).map(a -> mapVegadresse(AdresseType.BOSTEDSADRESSE, a)).ifPresent(resultat::add);
        Optional.ofNullable(bostedsadresse.getMatrikkeladresse()).map(a -> mapMatrikkeladresse(AdresseType.BOSTEDSADRESSE, a)).ifPresent(resultat::add);
        Optional.ofNullable(bostedsadresse.getUkjentBosted()).map(AdresseMapper::mapUkjentadresse).ifPresent(resultat::add);
        Optional.ofNullable(bostedsadresse.getUtenlandskAdresse()).map(a -> mapUtenlandskadresse(AdresseType.BOSTEDSADRESSE_UTLAND, a)).ifPresent(resultat::add);

        if (resultat.isEmpty()) {
            resultat.add(mapUkjentadresse(null));
        }
        return resultat;
    }

    private List<Adresseinfo> mapOppholdsadresse(Oppholdsadresse oppholdsadresse) {
        List<Adresseinfo> resultat = new ArrayList<>();
        Optional.ofNullable(oppholdsadresse.getVegadresse()).map(a -> mapVegadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).ifPresent(resultat::add);
        Optional.ofNullable(oppholdsadresse.getMatrikkeladresse()).map(a -> mapMatrikkeladresse(AdresseType.MIDLERTIDIG_POSTADRESSE_NORGE, a)).ifPresent(resultat::add);
        Optional.ofNullable(oppholdsadresse.getUtenlandskAdresse()).map(a -> mapUtenlandskadresse(AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, a)).ifPresent(resultat::add);

        return resultat;
    }

    private List<Adresseinfo> mapKontaktadresse(Kontaktadresse kontaktadresse) {
        List<Adresseinfo> resultat = new ArrayList<>();
        Optional.ofNullable(kontaktadresse.getVegadresse()).map(a -> mapVegadresse(AdresseType.POSTADRESSE, a)).ifPresent(resultat::add);
        Optional.ofNullable(kontaktadresse.getPostboksadresse()).map(this::mapPostboksadresse).ifPresent(resultat::add);
        Optional.ofNullable(kontaktadresse.getPostadresseIFrittFormat()).map(this::mapFriAdresseNorsk).ifPresent(resultat::add);
        Optional.ofNullable(kontaktadresse.getUtenlandskAdresse()).map(a -> mapUtenlandskadresse(AdresseType.POSTADRESSE_UTLAND, a)).ifPresent(resultat::add);
        Optional.ofNullable(kontaktadresse.getUtenlandskAdresseIFrittFormat()).map(AdresseMapper::mapFriAdresseUtland).ifPresent(resultat::add);
        return resultat;
    }

    private List<Adresseinfo> mapDeltBosted(DeltBosted deltBostedadresse) {
        List<Adresseinfo> resultat = new ArrayList<>();
        Optional.ofNullable(deltBostedadresse.getVegadresse()).map(a -> mapVegadresse(AdresseType.BOSTEDSADRESSE, a)).ifPresent(resultat::add);
        Optional.ofNullable(deltBostedadresse.getMatrikkeladresse()).map(a -> mapMatrikkeladresse(AdresseType.BOSTEDSADRESSE, a)).ifPresent(resultat::add);

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
            .medPostnummer(postnummer)
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
            .medPostnummer(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapPostboksadresse(Postboksadresse postboksadresse) {
        if (postboksadresse == null)
            return null;
        var postnummer = Optional.ofNullable(postboksadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        var postboks = "Postboks" + hvisfinnes(postboksadresse.getPostboks());
        return Adresseinfo.builder(AdresseType.POSTADRESSE)
            .medAdresselinje1(postboksadresse.getPostbokseier() != null ? postboksadresse.getPostbokseier().toUpperCase() : postboks)
            .medAdresselinje2(postboksadresse.getPostbokseier() != null ? postboks : null)
            .medPostnummer(postnummer)
            .medPoststed(tilPoststed(postnummer))
            .medLand(Landkoder.NOR.getKode())
            .build();
    }

    private Adresseinfo mapFriAdresseNorsk(PostadresseIFrittFormat postadresse) {
        if (postadresse == null)
            return null;
        var postnummer = Optional.ofNullable(postadresse.getPostnummer()).orElse(HARDKODET_POSTNR);
        return Adresseinfo.builder(AdresseType.POSTADRESSE)
            .medAdresselinje1(postadresse.getAdresselinje1() != null ? postadresse.getAdresselinje1().toUpperCase() : null)
            .medAdresselinje2(postadresse.getAdresselinje2() != null ? postadresse.getAdresselinje2().toUpperCase() : null)
            .medAdresselinje3(postadresse.getAdresselinje3() != null ? postadresse.getAdresselinje3().toUpperCase() : null)
            .medPostnummer(postnummer)
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
            .medLand(utenlandskAdresse.getLandkode())
            .build();
    }

    private static Adresseinfo mapFriAdresseUtland(UtenlandskAdresseIFrittFormat utenlandskAdresse) {
        if (utenlandskAdresse == null)
            return null;
        var postlinje = hvisfinnes(utenlandskAdresse.getPostkode()) + hvisfinnes(utenlandskAdresse.getByEllerStedsnavn());
        var sisteline = utenlandskAdresse.getAdresselinje3() != null ? postlinje : null;
        return Adresseinfo.builder(AdresseType.POSTADRESSE_UTLAND)
            .medAdresselinje1(utenlandskAdresse.getAdresselinje1())
            .medAdresselinje2(utenlandskAdresse.getAdresselinje2() != null ? utenlandskAdresse.getAdresselinje2() : postlinje)
            .medAdresselinje3(utenlandskAdresse.getAdresselinje3() != null ? utenlandskAdresse.getAdresselinje3() : utenlandskAdresse.getAdresselinje2() != null ? postlinje : null)
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

    public String tilPoststed(String postnummer) {
        if (HARDKODET_POSTNR.equals(postnummer)) {
            return HARDKODET_POSTSTED;
        }
        return poststedKodeverkRepository.finnPoststedReadOnly(postnummer).map(Poststed::getPoststednavn).orElse(HARDKODET_POSTSTED);
    }
}
