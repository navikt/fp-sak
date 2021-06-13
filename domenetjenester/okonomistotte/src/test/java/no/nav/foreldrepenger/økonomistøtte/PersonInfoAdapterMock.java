package no.nav.foreldrepenger.økonomistøtte;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonIdentMedDiskresjonskode;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoSpråk;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
@Alternative
@Priority(1)
public class PersonInfoAdapterMock extends PersoninfoAdapter {
    private static final PersonIdent PERSON_IDENT = PersonIdent.fra("12345678901");

    @Override
    public PersonIdent hentFnrForAktør(AktørId aktørId) {
        return PERSON_IDENT;
    }

    @Override
    public Optional<PersoninfoBasis> hentBrukerBasisForAktør(AktørId aktørId) {
        return Optional.empty();
    }

    @Override
    public Optional<PersoninfoKjønn> hentBrukerKjønnForAktør(AktørId aktørId) {
        return Optional.empty();
    }

    @Override
    public Optional<PersoninfoArbeidsgiver> hentBrukerArbeidsgiverForAktør(AktørId aktørId) {
        return Optional.empty();
    }

    @Override
    public Optional<AktørId> hentAktørForFnr(PersonIdent fnr) {
        return Optional.empty();
    }

    @Override
    public Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return Optional.empty();
    }

    @Override
    public GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId) {
        return null;
    }

    @Override
    public Optional<PersonIdentMedDiskresjonskode> hentPersonIdentMedDiskresjonskode(AktørId aktørId) {
        return Optional.empty();
    }

    @Override
    public Personhistorikkinfo innhentPersonopplysningerHistorikk(AktørId aktørId, SimpleLocalDateInterval interval) {
        return null;
    }

    @Override
    public List<AktørId> finnAktørIdForForeldreTil(PersonIdent personIdent) {
        return Collections.emptyList();
    }

    @Override
    public List<FødtBarnInfo> innhentAlleFødteForBehandlingIntervaller(AktørId aktørId, List<LocalDateInterval> intervaller) {
        return Collections.emptyList();
    }

    @Override
    public Optional<Personinfo> innhentPersonopplysningerFor(AktørId aktørId) {
        return Optional.empty();
    }

    @Override
    public Optional<Personinfo> innhentPersonopplysningerFor(PersonIdent personIdent) {
        return Optional.empty();
    }

    @Override
    public PersoninfoSpråk hentForetrukketSpråk(AktørId aktørId) {
        return null;
    }

}
