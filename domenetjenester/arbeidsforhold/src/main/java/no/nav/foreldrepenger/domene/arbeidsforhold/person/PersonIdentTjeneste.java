package no.nav.foreldrepenger.domene.arbeidsforhold.person;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
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

    public Optional<PersoninfoArbeidsgiver> hentBrukerForAktør(AktørId aktørId) {
        return personinfoAdapter.hentBrukerArbeidsgiverForAktør(aktørId);
    }

}
