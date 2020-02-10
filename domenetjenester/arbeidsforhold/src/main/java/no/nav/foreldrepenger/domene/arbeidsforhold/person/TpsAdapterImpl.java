package no.nav.foreldrepenger.domene.arbeidsforhold.person;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.felles.integrasjon.aktør.klient.DetFinnesFlereAktørerMedSammePersonIdentException;
import no.nav.vedtak.felles.integrasjon.person.PersonConsumer;

@ApplicationScoped
class TpsAdapterImpl {

    private AktørConsumerMedCache aktørConsumer;
    private PersonConsumer personConsumer;
    private TpsOversetter tpsOversetter;

    public TpsAdapterImpl() {
    }

    @Inject
    TpsAdapterImpl(AktørConsumerMedCache aktørConsumer,
                          PersonConsumer personConsumer,
                          TpsOversetter tpsOversetter) {
        this.aktørConsumer = aktørConsumer;
        this.personConsumer = personConsumer;
        this.tpsOversetter = tpsOversetter;
    }

    public Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            // har ikke tildelt personnr
            return Optional.empty();
        }
        try {
            return aktørConsumer.hentAktørIdForPersonIdent(personIdent.getIdent()).map(AktørId::new);
        } catch (DetFinnesFlereAktørerMedSammePersonIdentException e) { // NOSONAR
            // Her sorterer vi ut dødfødte barn
            return Optional.empty();
        }
    }

    public Optional<PersonIdent> hentIdentForAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(f -> new PersonIdent(f));
    }

    private Personinfo håndterPersoninfoRespons(AktørId aktørId, HentPersonRequest request)
        throws HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning {
        HentPersonResponse response = personConsumer.hentPersonResponse(request);
        Person person = response.getPerson();
        if (!(person instanceof Bruker)) {
            throw TpsFeilmeldinger.FACTORY.ukjentBrukerType().toException();
        }
        return tpsOversetter.tilBrukerInfo(aktørId, (Bruker) person);
    }

    public Personinfo hentKjerneinformasjon(PersonIdent personIdent, AktørId aktørId) {
        HentPersonRequest request = new HentPersonRequest();
        request.setAktoer(lagPersonIdent(personIdent.getIdent()));
        try {
            return håndterPersoninfoRespons(aktørId, request);
        } catch (HentPersonPersonIkkeFunnet e) {
            throw TpsFeilmeldinger.FACTORY.fantIkkePerson(e).toException();
        } catch (HentPersonSikkerhetsbegrensning e) {
            throw TpsFeilmeldinger.FACTORY.tpsUtilgjengeligSikkerhetsbegrensning(e).toException();
        }
    }
    
    private static no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent lagPersonIdent(String fnr) {
        if (fnr == null || fnr.isEmpty()) {
            throw new IllegalArgumentException("Fødselsnummer kan ikke være null eller tomt");
        }

        no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent personIdent = new no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent();
        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(fnr);

        Personidenter type = new Personidenter();
        type.setValue(erDNr(fnr) ? "DNR" : "FNR");
        norskIdent.setType(type);

        personIdent.setIdent(norskIdent);
        return personIdent;
    }

    private static boolean erDNr(String fnr) {
        //D-nummer kan indentifiseres ved at første siffer er 4 større enn hva som finnes i fødselsnumre
        char førsteTegn = fnr.charAt(0);
        return førsteTegn >= '4' && førsteTegn <= '7';
    }

}
