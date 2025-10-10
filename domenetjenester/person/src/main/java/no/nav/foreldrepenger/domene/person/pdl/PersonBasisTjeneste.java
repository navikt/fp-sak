package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.FolkeregisteridentifikatorResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;

@ApplicationScoped
public class PersonBasisTjeneste {

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
            .folkeregisteridentifikator(new FolkeregisteridentifikatorResponseProjection().identifikasjonsnummer().status())
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        if (!PersonMappers.harIdentifikator(person)) {
            var falskIdent = pdlKlient.sjekkUtenIdentifikatorFalskIdentitet(ytelseType, aktørId, personIdent);
            if (falskIdent != null) {
                return new PersoninfoVisning(aktørId, falskIdent.personIdent(), falskIdent.navn(), Diskresjonskode.UDEFINERT);
            }
        }

        return new PersoninfoVisning(aktørId, personIdent, PersonMappers.mapNavn(person, aktørId), getDiskresjonskode(person));
    }

    public PersoninfoBasis hentBasisPersoninfo(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .folkeregisteridentifikator(new FolkeregisteridentifikatorResponseProjection().identifikasjonsnummer().status())
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato())
            .doedsfall(new DoedsfallResponseProjection().doedsdato())
            .kjoenn(new KjoennResponseProjection().kjoenn())
            .adressebeskyttelse(new AdressebeskyttelseResponseProjection().gradering());

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        if (!PersonMappers.harIdentifikator(person)) {
            var falskIdent = pdlKlient.sjekkUtenIdentifikatorFalskIdentitet(ytelseType, aktørId, personIdent);
            if (falskIdent != null) {
                return new PersoninfoBasis(aktørId, falskIdent.personIdent(), falskIdent.navn(),
                    falskIdent.fødselsdato(), null, falskIdent.kjønn(), Diskresjonskode.UDEFINERT.getKode());
            }
        }

        var fødselsdato = Optional.ofNullable(PersonMappers.mapFødselsdato(person))
            .orElseGet(() -> IS_PROD ? null : LocalDate.now().minusDays(1));
        var dødsdato = PersonMappers.mapDødsdato(person);
        return new PersoninfoBasis(aktørId, personIdent, PersonMappers.mapNavn(person, aktørId), fødselsdato, dødsdato,
            PersonMappers.mapKjønn(person), getDiskresjonskode(person).getKode());
    }

    public Optional<PersoninfoArbeidsgiver> hentPrivatArbeidsgiverPersoninfo(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());

        var person = pdlKlient.hentPersonTilgangsnektSomInfo(ytelseType, query, projection);

        var fødselsdato = PersonMappers.mapFødselsdato(person);

        var arbeidsgiver = new PersoninfoArbeidsgiver.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
            .medNavn(PersonMappers.mapNavn(person, aktørId))
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

        var fødselsdato = PersonMappers.mapFødselsdato(person);

        var arbeidsgiver = new PersoninfoArbeidsgiver.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
            .medNavn(PersonMappers.mapNavn(person, aktørId))
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
            .medNavBrukerKjønn(PersonMappers.mapKjønn(person))
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

}
