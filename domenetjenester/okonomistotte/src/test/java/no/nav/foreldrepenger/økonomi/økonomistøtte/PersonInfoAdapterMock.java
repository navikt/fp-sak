package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.xml.ws.soap.SOAPFaultException;

import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.tps.TpsFeilmeldinger;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
@Alternative
@Priority(1)
public class PersonInfoAdapterMock extends PersoninfoAdapter {
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
    public Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return Optional.empty();
    }

    @Override
    public GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId) {
        return null;
    }
    @Override
    public Optional<String> hentDiskresjonskodeForAktør(AktørId aktørId) {
        return Optional.empty();
    }


    @Override
    public Personinfo innhentSaksopplysningerForSøker(AktørId aktørId) {
        return null;
    }

    @Override
    public Optional<Personinfo> innhentSaksopplysningerForEktefelle(AktørId aktørId) {
        return Optional.empty();
    }

    @Override
    public Optional<Personinfo> innhentSaksopplysninger(PersonIdent personIdent) {
        return Optional.empty();
    }

    @Override
    public Personhistorikkinfo innhentPersonopplysningerHistorikk(AktørId aktørId, Interval interval) {
        return null;
    }

    @Override
    public Optional<Personinfo> innhentSaksopplysningerForBarn(PersonIdent personIdent) {
        return Optional.empty();
    }

    @Override
    public List<AktørId> finnAktørIdForForeldreTil(PersonIdent personIdent) {
        return Collections.emptyList();
    }

    @Override
    public List<FødtBarnInfo> innhentAlleFødteForBehandlingIntervaller(AktørId aktørId, List<LocalDateInterval> intervaller) {
        return Collections.emptyList();
    }

}
