package no.nav.foreldrepenger.domene.arbeidsforhold.person;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class PersonIdentTjeneste {

    private PersoninfoAdapter personinfoAdapter;

    public PersonIdentTjeneste() {
        // for CDI proxy
    }

    @Inject
    public PersonIdentTjeneste(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    public Optional<PersoninfoBasis> hentBrukerForAktør(AktørId aktørId) {
        return personinfoAdapter.hentBrukerBasisForAktør(aktørId);
    }


}
