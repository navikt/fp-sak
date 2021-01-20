package no.nav.foreldrepenger.domene.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoSpråk;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.dkif.DkifSpråkKlient;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.FødselTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersonBasisTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersoninfoTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.TilknytningTjeneste;
import no.nav.foreldrepenger.domene.person.tps.TpsAdapter;
import no.nav.foreldrepenger.domene.person.tps.TpsFeilmeldinger;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
public class PersoninfoAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(PersoninfoAdapter.class);

    private AktørTjeneste aktørConsumer;
    private TpsAdapter tpsAdapter;
    private FødselTjeneste fødselTjeneste;
    private TilknytningTjeneste tilknytningTjeneste;
    private PersonBasisTjeneste basisTjeneste;
    private PersoninfoTjeneste personinfoTjeneste;
    private DkifSpråkKlient dkifSpråkKlient;

    public PersoninfoAdapter() {
        // for CDI proxy
    }

    @Inject
    public PersoninfoAdapter(AktørTjeneste aktørConsumer,
                             FødselTjeneste fødselTjeneste,
                             TilknytningTjeneste tilknytningTjeneste,
                             PersonBasisTjeneste basisTjeneste,
                             PersoninfoTjeneste personinfoTjeneste,
                             DkifSpråkKlient dkifSpråkKlient,
                             TpsAdapter tpsAdapter) {
        this.aktørConsumer = aktørConsumer;
        this.fødselTjeneste = fødselTjeneste;
        this.tilknytningTjeneste = tilknytningTjeneste;
        this.basisTjeneste = basisTjeneste;
        this.personinfoTjeneste = personinfoTjeneste;
        this.dkifSpråkKlient = dkifSpråkKlient;
        this.tpsAdapter = tpsAdapter;
    }

    public Optional<Personinfo> innhentPersonopplysningerFor(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId).filter(i -> !i.erFdatNummer()).map(i -> hentKjerneinformasjon(aktørId, i));
    }

    public Optional<Personinfo> innhentPersonopplysningerFor(PersonIdent personIdent) {
        if (personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        return aktørConsumer.hentAktørIdForPersonIdent(personIdent).map(a -> hentKjerneinformasjon(a, personIdent));
    }

    private Personinfo hentKjerneinformasjon(AktørId aktørId, PersonIdent personIdent) {
        return personinfoTjeneste.hentPersoninfo(aktørId, personIdent);
    }

    public Personhistorikkinfo innhentPersonopplysningerHistorikk(AktørId aktørId, Interval interval) {
        return personinfoTjeneste.hentPersoninfoHistorikk(aktørId, interval);
    }

    public List<AktørId> finnAktørIdForForeldreTil(PersonIdent personIdent) {
        return fødselTjeneste.hentForeldreTil(personIdent).stream()
            .flatMap(p -> aktørConsumer.hentAktørIdForPersonIdent(p).stream())
            .collect(Collectors.toList());
    }

    public List<FødtBarnInfo> innhentAlleFødteForBehandlingIntervaller(AktørId aktørId, List<LocalDateInterval> intervaller) {
        return fødselTjeneste.hentFødteBarnInfoFor(aktørId, intervaller);
    }

    public Optional<AktørId> hentAktørForFnr(PersonIdent fnr) {
        return aktørConsumer.hentAktørIdForPersonIdent(fnr);
    }

    public PersonIdent hentFnrForAktør(AktørId aktørId) {
        return hentFnr(aktørId).orElseThrow(() -> TpsFeilmeldinger.FACTORY.fantIkkePersonForAktørId().toException());
    }

    public Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId);
    }

    public Optional<PersoninfoBasis> hentBrukerBasisForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr = hentFnr(aktørId);
        return funnetFnr.map(fnr -> basisTjeneste.hentBasisPersoninfo(aktørId, fnr));
    }

    public Optional<PersoninfoArbeidsgiver> hentBrukerArbeidsgiverForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr = hentFnr(aktørId);
        return funnetFnr.flatMap(fnr -> basisTjeneste.hentArbeidsgiverPersoninfo(aktørId, fnr));
    }

    public Optional<PersoninfoKjønn> hentBrukerKjønnForAktør(AktørId aktørId) {
        return basisTjeneste.hentKjønnPersoninfo(aktørId);
    }

    public Optional<LocalDate> hentFødselsdato(PersonIdent ident) {
        return basisTjeneste.hentFødselsdato(ident);
    }

    public GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId) {
        // Bruk TPS og PersonV3 inntil PDL er modent.
        return hentFnr(aktørId).map(f -> tpsAdapter.hentGeografiskTilknytning(f))
            .orElse(new GeografiskTilknytning(null, Diskresjonskode.UDEFINERT));
    }

    public Optional<Tuple<PersonIdent, Diskresjonskode>> hentPersonIdentMedDiskresjonskode(AktørId aktørId) {
        return hentFnr(aktørId).map(f -> new Tuple<>(f, tilknytningTjeneste.hentDiskresjonskode(aktørId)));
    }

    public PersoninfoSpråk hentForetrukketSpråk(AktørId aktørId) {
        var personIdent = hentFnr(aktørId).orElse(null);
        if (personIdent == null) {
            return new PersoninfoSpråk(aktørId, Språkkode.NB);
        }
        return new PersoninfoSpråk(aktørId, dkifSpråkKlient.finnSpråkkodeForBruker(personIdent.getIdent()));
    }

}
