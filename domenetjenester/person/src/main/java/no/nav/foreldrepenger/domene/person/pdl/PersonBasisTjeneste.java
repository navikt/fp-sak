package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoVisning;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.pdl.Adressebeskyttelse;
import no.nav.pdl.AdressebeskyttelseGradering;
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.Doedsfall;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.Foedselsdato;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.Kjoenn;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.KjoennType;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PersonBasisTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PersonBasisTjeneste.class);
    private static final boolean IS_PROD = Environment.current().isProd();

    private PdlKlientLogCause pdlKlient;

    PersonBasisTjeneste() {
        // CDI
    }

    @Inject
    public PersonBasisTjeneste(PdlKlientLogCause pdlKlient) {
        this.pdlKlient = pdlKlient;
    }

    public PersoninfoVisning hentVisningsPersoninfo(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        return new PersoninfoVisning(aktørId, personIdent, mapNavnVisning(person, aktørId), getDiskresjonskode(person));
    }

    public PersoninfoBasis hentBasisPersoninfo(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
                .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
                .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
                .doedsfall(new DoedsfallResponseProjection().doedsdato())
                .kjoenn(new KjoennResponseProjection().kjoenn())
                .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var fødselsdato = person.getFoedselsdato().stream()
                .map(Foedselsdato::getFoedselsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE))
                .orElseGet(() -> IS_PROD ? null : LocalDate.now().minusDays(1));
        var dødsdato = person.getDoedsfall().stream()
                .map(Doedsfall::getDoedsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);
        return new PersoninfoBasis(aktørId, personIdent, mapNavn(person, aktørId), fødselsdato, dødsdato,
            mapKjønn(person), getDiskresjonskode(person).getKode());
    }

    public Optional<PersoninfoArbeidsgiver> hentPrivatArbeidsgiverPersoninfo(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
                .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
                .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());

        var person = pdlKlient.hentPersonTilgangsnektSomInfo(ytelseType, query, projection);

        var fødselsdato = person.getFoedselsdato().stream()
                .map(Foedselsdato::getFoedselsdato)
                .filter(Objects::nonNull)
                .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);

        var arbeidsgiver = new PersoninfoArbeidsgiver.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
                .medNavn(person.getNavn().stream().map(PersoninfoTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElse(null))
                .medFødselsdato(fødselsdato)
                .build();

        return person.getNavn().isEmpty() || person.getFoedselsdato().isEmpty() ? Optional.empty() : Optional.of(arbeidsgiver);
    }

    public Optional<PersoninfoArbeidsgiver> hentVergePersoninfo(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var fødselsdato = person.getFoedselsdato().stream()
            .map(Foedselsdato::getFoedselsdato)
            .filter(Objects::nonNull)
            .findFirst().map(d -> LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE)).orElse(null);

        var arbeidsgiver = new PersoninfoArbeidsgiver.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
            .medNavn(person.getNavn().stream().map(PersoninfoTjeneste::mapNavn).filter(Objects::nonNull).findFirst().orElse(null))
            .medFødselsdato(fødselsdato)
            .build();

        return person.getNavn().isEmpty() || person.getFoedselsdato().isEmpty() ? Optional.empty() : Optional.of(arbeidsgiver);
    }

    public Optional<PersoninfoKjønn> hentKjønnPersoninfo(FagsakYtelseType ytelseType, AktørId aktørId) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
                .kjoenn(new KjoennResponseProjection().kjoenn());

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var kjønn = new PersoninfoKjønn.Builder().medAktørId(aktørId)
                .medNavBrukerKjønn(mapKjønn(person))
                .build();
        return person.getKjoenn().isEmpty() ? Optional.empty() : Optional.of(kjønn);
    }

    private Diskresjonskode getDiskresjonskode(Person person) {
        var kode = person.getAdressebeskyttelse().stream()
            .map(Adressebeskyttelse::getGradering)
            .filter(g -> !AdressebeskyttelseGradering.UGRADERT.equals(g))
            .findFirst().orElse(null);
        if (AdressebeskyttelseGradering.STRENGT_FORTROLIG.equals(kode) || AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND.equals(kode))
            return Diskresjonskode.KODE6;
        return AdressebeskyttelseGradering.FORTROLIG.equals(kode) ? Diskresjonskode.KODE7 : Diskresjonskode.UDEFINERT;
    }

    private String mapNavnVisning(Person person, AktørId aktørId) {
        return person.getNavn().stream()
            .filter(Objects::nonNull)
            .map(PersoninfoTjeneste::mapNavn)
            .findFirst().orElseGet(() -> {
                LOG.info("PDL mangler navn for aktørId={}", aktørId);
                return "Navn ikke tilgjengelig";
            });
    }

    private String mapNavn(Person person, AktørId aktørId) {
        return person.getNavn().stream()
            .filter(Objects::nonNull)
            .map(PersoninfoTjeneste::mapNavn)
            .findFirst().orElseGet(() -> {
                LOG.warn("PDL mangler navn for aktørId={}", aktørId);
                return IS_PROD ? null : "Navn ikke tilgjengelig";
            });
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

}
