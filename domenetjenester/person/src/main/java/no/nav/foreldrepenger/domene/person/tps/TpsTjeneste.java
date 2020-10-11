package no.nav.foreldrepenger.domene.person.tps;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
public interface TpsTjeneste {

    Optional<AktørId> hentAktørForFnr(PersonIdent fnr);

    PersonIdent hentFnrForAktør(AktørId aktørId);
    Optional<PersonIdent> hentFnr(AktørId aktørId);

    Optional<Personinfo> hentBrukerForFnr(PersonIdent fnr);
    Optional<Personinfo> hentBrukerForAktør(AktørId aktørId);

    Optional<String> hentDiskresjonskodeForAktør(PersonIdent fnr);
    GeografiskTilknytning hentGeografiskTilknytning(PersonIdent fnr);

}
