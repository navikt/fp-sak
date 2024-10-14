package no.nav.foreldrepenger.domene.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.krr.KrrSpråkKlient;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.FødselTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersonBasisTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersoninfoTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.TilknytningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.util.LRUCache;

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

    public Optional<Personinfo> innhentPersonopplysningerFor(FagsakYtelseType ytelseType, AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId).map(i -> hentKjerneinformasjon(ytelseType, aktørId, i, false));
    }

    public Optional<Personinfo> innhentPersonopplysningerFor(FagsakYtelseType ytelseType, PersonIdent personIdent, boolean erBarn) {
        return aktørConsumer.hentAktørIdForPersonIdent(personIdent).map(a -> hentKjerneinformasjon(ytelseType, a, personIdent, erBarn));
    }

    public boolean sjekkOmBrukerManglerAdresse(FagsakYtelseType ytelseType, AktørId aktørId) {
        var manglerAdresse = Optional.ofNullable(MANGLER_ADRESSE.get(aktørId))
            .or(() -> aktørConsumer.hentPersonIdentForAktørId(aktørId).map(i -> personinfoTjeneste.brukerManglerAdresse(ytelseType, i)))
            .orElse(Boolean.TRUE);
        MANGLER_ADRESSE.put(aktørId, manglerAdresse);
        return manglerAdresse;
    }

    private Personinfo hentKjerneinformasjon(FagsakYtelseType ytelseType, AktørId aktørId, PersonIdent personIdent, boolean erBarn) {
        return personinfoTjeneste.hentPersoninfo(ytelseType, aktørId, personIdent, erBarn);
    }

    public Personhistorikkinfo innhentPersonopplysningerHistorikk(FagsakYtelseType ytelseType, AktørId aktørId,
                                                                  SimpleLocalDateInterval interval, LocalDate fødselsdato) {
        return personinfoTjeneste.hentPersoninfoHistorikk(ytelseType, aktørId, fødselsdato, interval);
    }

    public List<AktørId> finnAktørIdForForeldreTil(FagsakYtelseType ytelseType, PersonIdent personIdent) {
        return fødselTjeneste.hentForeldreTil(ytelseType, personIdent).stream()
            .flatMap(p -> aktørConsumer.hentAktørIdForPersonIdent(p).stream())
            .toList();
    }

    public List<FødtBarnInfo> innhentAlleFødteForBehandlingIntervaller(FagsakYtelseType ytelseType, AktørId aktørId, List<LocalDateInterval> intervaller) {
        return fødselTjeneste.hentFødteBarnInfoFor(ytelseType, aktørId, intervaller);
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

    public Optional<PersoninfoBasis> hentBrukerBasisForAktør(FagsakYtelseType ytelseType, AktørId aktørId) {
        var funnetFnr = hentFnr(aktørId);
        return funnetFnr.map(fnr -> basisTjeneste.hentBasisPersoninfo(ytelseType, aktørId, fnr));
    }

    public Optional<PersoninfoArbeidsgiver> hentBrukerArbeidsgiverForAktør(AktørId aktørId) {
        var funnetFnr = hentFnr(aktørId);
        return funnetFnr.flatMap(fnr -> basisTjeneste.hentArbeidsgiverPersoninfo(FagsakYtelseType.FORELDREPENGER, aktørId, fnr));
    }

    public Optional<PersoninfoKjønn> hentBrukerKjønnForAktør(FagsakYtelseType ytelseType, AktørId aktørId) {
        return basisTjeneste.hentKjønnPersoninfo(ytelseType, aktørId);
    }

    public String hentGeografiskTilknytning(FagsakYtelseType ytelseType, AktørId aktørId) {
        var tilknytning = tilknytningTjeneste.hentGeografiskTilknytning(ytelseType, aktørId);
        if (tilknytning != null && tilknytningTjeneste.erIkkeBosattFreg(ytelseType, aktørId)) {
            return null;
        }
        return tilknytning;
    }

    public Diskresjonskode hentDiskresjonskode(FagsakYtelseType ytelseType, AktørId aktørId) {
        return tilknytningTjeneste.hentDiskresjonskode(ytelseType, aktørId);
    }

    public Optional<PersoninfoVisning> hentPersoninfoForVisning(FagsakYtelseType ytelseType, AktørId aktørId) {
        return hentFnr(aktørId).map(f -> basisTjeneste.hentVisningsPersoninfo(ytelseType, aktørId, f));
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
