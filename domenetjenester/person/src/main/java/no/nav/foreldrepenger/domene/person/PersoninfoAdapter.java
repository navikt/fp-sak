package no.nav.foreldrepenger.domene.person;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.aktør.*;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.krr.KrrSpråkKlient;
import no.nav.foreldrepenger.domene.person.pdl.*;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.util.LRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class PersoninfoAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(PersoninfoAdapter.class);

    private static final LRUCache<AktørId, Boolean> MANGLER_ADRESSE = new LRUCache<>(1000, TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS));

    private AktørTjeneste aktørConsumer;
    private FødselTjeneste fødselTjeneste;
    private TilknytningTjeneste tilknytningTjeneste;
    private PersonBasisTjeneste basisTjeneste;
    private PersoninfoTjeneste personinfoTjeneste;
    private KrrSpråkKlient krrSpråkKlient;

    public PersoninfoAdapter() {
        // for CDI proxy
    }

    @Inject
    public PersoninfoAdapter(AktørTjeneste aktørConsumer,
                             FødselTjeneste fødselTjeneste,
                             TilknytningTjeneste tilknytningTjeneste,
                             PersonBasisTjeneste basisTjeneste,
                             PersoninfoTjeneste personinfoTjeneste,
                             KrrSpråkKlient krrSpråkKlient) {
        this.aktørConsumer = aktørConsumer;
        this.fødselTjeneste = fødselTjeneste;
        this.tilknytningTjeneste = tilknytningTjeneste;
        this.basisTjeneste = basisTjeneste;
        this.personinfoTjeneste = personinfoTjeneste;
        this.krrSpråkKlient = krrSpråkKlient;
    }

    public Optional<Personinfo> innhentPersonopplysningerFor(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId).map(i -> hentKjerneinformasjon(aktørId, i));
    }

    public Optional<Personinfo> innhentPersonopplysningerFor(PersonIdent personIdent) {
        return aktørConsumer.hentAktørIdForPersonIdent(personIdent).map(a -> hentKjerneinformasjon(a, personIdent));
    }

    public boolean sjekkOmBrukerManglerAdresse(AktørId aktørId) {
        var manglerAdresse = Optional.ofNullable(MANGLER_ADRESSE.get(aktørId))
            .or(() -> aktørConsumer.hentPersonIdentForAktørId(aktørId).map(i -> personinfoTjeneste.brukerManglerAdresse(i)))
            .orElse(Boolean.TRUE);
        MANGLER_ADRESSE.put(aktørId, manglerAdresse);
        return manglerAdresse;
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
            .toList();
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
        var tilknytning = tilknytningTjeneste.hentGeografiskTilknytning(aktørId);
        if (tilknytning != null && tilknytningTjeneste.erIkkeBosattFreg(aktørId)) {
            return null;
        }
        return tilknytning;
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
            return new PersoninfoSpråk(aktørId, krrSpråkKlient.finnSpråkkodeForBruker(personIdent.getIdent()));
        } catch (Exception e) {
            LOG.warn("KRR feiler, defaulter til NB", e);
        }
        return new PersoninfoSpråk(aktørId, Språkkode.NB);
    }

}
