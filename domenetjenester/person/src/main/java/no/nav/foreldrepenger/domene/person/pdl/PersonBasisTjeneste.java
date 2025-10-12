package no.nav.foreldrepenger.domene.person.pdl;

import java.time.LocalDate;
import java.time.Month;
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
import no.nav.pdl.AdressebeskyttelseResponseProjection;
import no.nav.pdl.DoedsfallResponseProjection;
import no.nav.pdl.FoedselsdatoResponseProjection;
import no.nav.pdl.FolkeregisteridentifikatorResponseProjection;
import no.nav.pdl.HentPersonQueryRequest;
import no.nav.pdl.KjoennResponseProjection;
import no.nav.pdl.NavnResponseProjection;
import no.nav.pdl.Person;
import no.nav.pdl.PersonResponseProjection;
import no.nav.vedtak.felles.integrasjon.person.PersonMappers;

@ApplicationScoped
public class PersonBasisTjeneste {

    private static final boolean IS_PROD = Environment.current().isProd();
    private static final LocalDate DUMMY_VOKSEN_FØDT = LocalDate.of(1900, Month.JANUARY, 1);

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

        if (PersonMappers.manglerIdentifikator(person)) {
            var falskIdent = pdlKlient.sjekkUtenIdentifikatorFalskIdentitet(aktørId);
            if (falskIdent != null) {
                return new PersoninfoVisning(aktørId, personIdent, falskIdent.navn(), Diskresjonskode.UDEFINERT);
            }
        }

        return new PersoninfoVisning(aktørId, personIdent, LokalPersonMapper.mapNavn(person), getDiskresjonskode(person));
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

        if (PersonMappers.manglerIdentifikator(person)) {
            var falskIdent = pdlKlient.sjekkUtenIdentifikatorFalskIdentitet(aktørId);
            if (falskIdent != null) {
                return new PersoninfoBasis(aktørId, personIdent, falskIdent.navn(),
                    falskIdent.fødselsdato(), null, falskIdent.kjønn(), Diskresjonskode.UDEFINERT.getKode());
            }
        }

        var fødselsdato = PersonMappers.mapFødselsdato(person)
            .or(() -> IS_PROD ? Optional.empty() : Optional.of(LocalDate.now().minusDays(1)))
            .orElseThrow(() -> new IllegalStateException("Fødselsdato mangler i PDL")); // Behold denne for videre analyse
        var dødsdato = LokalPersonMapper.mapDødsdato(person);
        return new PersoninfoBasis(aktørId, personIdent, LokalPersonMapper.mapNavn(person), fødselsdato, dødsdato,
            LokalPersonMapper.mapKjønn(person), getDiskresjonskode(person).getKode());
    }

    public PersoninfoArbeidsgiver hentPrivatArbeidsgiverPersoninfo(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());

        var person = pdlKlient.hentPersonTilgangsnektSomInfo(ytelseType, query, projection);

        var fødselsdato = LokalPersonMapper.mapFødselsdatoEllerDefault(person, DUMMY_VOKSEN_FØDT);

        return new PersoninfoArbeidsgiver.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
            .medNavn(LokalPersonMapper.mapNavn(person))
            .medFødselsdato(fødselsdato)
            .build();
    }

    public PersoninfoArbeidsgiver hentVergePersoninfo(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .navn(new NavnResponseProjection().fornavn().mellomnavn().etternavn())
            .foedselsdato(new FoedselsdatoResponseProjection().foedselsdato());

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var fødselsdato = LokalPersonMapper.mapFødselsdatoEllerDefault(person, DUMMY_VOKSEN_FØDT);

        return new PersoninfoArbeidsgiver.Builder().medAktørId(aktørId).medPersonIdent(personIdent)
            .medNavn(LokalPersonMapper.mapNavn(person))
            .medFødselsdato(fødselsdato)
            .build();
    }

    public Optional<PersoninfoKjønn> hentKjønnPersoninfo(FagsakYtelseType ytelseType, AktørId aktørId) {
        var query = new HentPersonQueryRequest();
        query.setIdent(aktørId.getId());
        var projection = new PersonResponseProjection()
            .kjoenn(new KjoennResponseProjection().kjoenn());

        var person = pdlKlient.hentPerson(ytelseType, query, projection);

        var kjønn = new PersoninfoKjønn.Builder().medAktørId(aktørId)
            .medNavBrukerKjønn(LokalPersonMapper.mapKjønn(person))
            .build();
        return person.getKjoenn().isEmpty() ? Optional.empty() : Optional.of(kjønn);
    }

    private Diskresjonskode getDiskresjonskode(Person person) {
        var kode = PersonMappers.mapAdressebeskyttelse(person);
        return switch (kode) {
            case STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND -> Diskresjonskode.KODE6;
            case FORTROLIG -> Diskresjonskode.KODE7;
            case null, default -> Diskresjonskode.UDEFINERT;
        };

    }

}
