package no.nav.foreldrepenger.domene.person.tps;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
public interface TpsTjeneste {

    Optional<Personinfo> hentBrukerForAktør(AktørId aktørId);

    /**
     * Hent PersonIdent (FNR) for gitt aktørId.
     *
     * @throws TekniskException hvis ikke finner.
     */
    PersonIdent hentFnrForAktør(AktørId aktørId);

    Optional<Personinfo> hentBrukerForFnr(PersonIdent fnr);

    Optional<AktørId> hentAktørForFnr(PersonIdent fnr);

    Optional<String> hentDiskresjonskodeForAktør(PersonIdent fnr);

    GeografiskTilknytning hentGeografiskTilknytning(PersonIdent fnr);

    List<GeografiskTilknytning> hentDiskresjonskoderForFamilierelasjoner(PersonIdent fnr);

    Optional<PersonIdent> hentFnr(AktørId aktørId);

    Adresseinfo hentAdresseinformasjon(PersonIdent personIdent);

    List<FødtBarnInfo> hentFødteBarn(AktørId aktørId);
}
