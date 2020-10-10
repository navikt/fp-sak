package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.util.Optional;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

@ApplicationScoped
@Alternative
@Priority(1)
public class TpsTjenesteMock implements TpsTjeneste {
    private static final PersonIdent PERSON_IDENT = PersonIdent.fra("12345678901");

    @Override
    public Optional<Personinfo> hentBrukerForAktør(AktørId aktørId) {
        return Optional.empty();
    }

    @Override
    public PersonIdent hentFnrForAktør(AktørId aktørId) {
        return PERSON_IDENT;
    }

    @Override
    public Optional<Personinfo> hentBrukerForFnr(PersonIdent fnr) {
        return Optional.empty();
    }

    @Override
    public Optional<AktørId> hentAktørForFnr(PersonIdent fnr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> hentDiskresjonskodeForAktør(PersonIdent fnr) {
        return Optional.empty();
    }

    @Override
    public GeografiskTilknytning hentGeografiskTilknytning(PersonIdent fnr) {
        return null;
    }

    @Override
    public Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return Optional.empty();
    }
}
