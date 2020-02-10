package no.nav.foreldrepenger.domene.arbeidsforhold.person;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

@ApplicationScoped
class PersonIdentTjenesteImpl implements PersonIdentTjeneste {

    private TpsAdapterImpl tpsAdapter;

    public PersonIdentTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public PersonIdentTjenesteImpl(TpsAdapterImpl tpsAdapter) {
        this.tpsAdapter = tpsAdapter;
    }

    @Override
    public PersonIdent hentFnrForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr;
        funnetFnr = hentFnr(aktørId);
        if (funnetFnr.isPresent()) {
            return funnetFnr.get();
        }
        throw TpsFeilmeldinger.FACTORY.fantIkkePersonForAktørId().toException();
    }

    @Override
    public Optional<AktørId> hentAktørForFnr(PersonIdent fnr) {
        return tpsAdapter.hentAktørIdForPersonIdent(fnr);
    }

    private Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return tpsAdapter.hentIdentForAktørId(aktørId);
    }

    @Override
    public Optional<Personinfo> hentBrukerForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr = hentFnr(aktørId);
        return funnetFnr.map(fnr -> tpsAdapter.hentKjerneinformasjon(fnr, aktørId));
    }

}
