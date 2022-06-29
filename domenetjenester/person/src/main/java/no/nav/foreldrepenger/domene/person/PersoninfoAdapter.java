package no.nav.foreldrepenger.domene.person;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoSpråk;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoVisning;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.dkif.DkifSpråkKlient;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.FødselTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersonBasisTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersoninfoTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.TilknytningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class PersoninfoAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(PersoninfoAdapter.class);

    private AktørTjeneste aktørConsumer;
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
                             DkifSpråkKlient dkifSpråkKlient) {
        this.aktørConsumer = aktørConsumer;
        this.fødselTjeneste = fødselTjeneste;
        this.tilknytningTjeneste = tilknytningTjeneste;
        this.basisTjeneste = basisTjeneste;
        this.personinfoTjeneste = personinfoTjeneste;
        this.dkifSpråkKlient = dkifSpråkKlient;
    }

    public Optional<Personinfo> innhentPersonopplysningerFor(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId).map(i -> hentKjerneinformasjon(aktørId, i));
    }

    public Optional<Personinfo> innhentPersonopplysningerFor(PersonIdent personIdent) {
        return aktørConsumer.hentAktørIdForPersonIdent(personIdent).map(a -> hentKjerneinformasjon(a, personIdent));
    }

    public boolean sjekkOmBrukerManglerAdresse(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId).map(i -> personinfoTjeneste.brukerManglerAdresse(i)).orElse(true);
    }

    private Personinfo hentKjerneinformasjon(AktørId aktørId, PersonIdent personIdent) {
        return personinfoTjeneste.hentPersoninfo(aktørId, personIdent);
    }

    public Personhistorikkinfo innhentPersonopplysningerHistorikk(AktørId aktørId, SimpleLocalDateInterval interval) {
        return personinfoTjeneste.hentPersoninfoHistorikk(aktørId, interval.getFomDato(), interval.getTomDato());
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
        return hentFnr(aktørId).orElseThrow(() -> new IllegalArgumentException("Fant ikke ident for aktør"));
    }

    public Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId);
    }

    public Optional<PersoninfoBasis> hentBrukerBasisForAktør(AktørId aktørId) {
        var funnetFnr = hentFnr(aktørId);
        return funnetFnr.map(fnr -> basisTjeneste.hentBasisPersoninfo(aktørId, fnr));
    }

    public Optional<PersoninfoArbeidsgiver> hentBrukerArbeidsgiverForAktør(AktørId aktørId) {
        var funnetFnr = hentFnr(aktørId);
        return funnetFnr.flatMap(fnr -> basisTjeneste.hentArbeidsgiverPersoninfo(aktørId, fnr));
    }

    public Optional<PersoninfoKjønn> hentBrukerKjønnForAktør(AktørId aktørId) {
        return basisTjeneste.hentKjønnPersoninfo(aktørId);
    }

    public String hentGeografiskTilknytning(AktørId aktørId) {
        return tilknytningTjeneste.hentGeografiskTilknytning(aktørId);
    }

    public Diskresjonskode hentDiskresjonskode(AktørId aktørId) {
        return tilknytningTjeneste.hentDiskresjonskode(aktørId);
    }

    public Optional<PersoninfoVisning> hentPersoninfoForVisning(AktørId aktørId) {
        return hentFnr(aktørId).map(f -> basisTjeneste.hentVisningsPersoninfo(aktørId, f));
    }

    public PersoninfoSpråk hentForetrukketSpråk(AktørId aktørId) {
        var personIdent = hentFnr(aktørId).orElse(null);
        if (personIdent == null) {
            return new PersoninfoSpråk(aktørId, Språkkode.NB);
        }
        try {
            return new PersoninfoSpråk(aktørId, dkifSpråkKlient.finnSpråkkodeForBruker(personIdent.getIdent()));
        } catch (Exception e) {
            LOG.warn("DKIF feiler, defaulter til NB", e);
        }
        return new PersoninfoSpråk(aktørId, Språkkode.NB);
    }

}
