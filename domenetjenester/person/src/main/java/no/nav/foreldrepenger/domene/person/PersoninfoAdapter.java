package no.nav.foreldrepenger.domene.person;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.ws.soap.SOAPFaultException;

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
import no.nav.foreldrepenger.domene.person.pdl.FødselTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersonBasisTjeneste;
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

    private TpsAdapter tpsAdapter;
    private FødselTjeneste fødselTjeneste;
    private TilknytningTjeneste tilknytningTjeneste;
    private PersonBasisTjeneste basisTjeneste;
    private DkifSpråkKlient dkifSpråkKlient;

    public PersoninfoAdapter() {
        // for CDI proxy
    }

    // Midlertidig under refaktorering
    public PersoninfoAdapter(TpsAdapter tpsAdapter) {
        this.tpsAdapter = tpsAdapter;
    }

    @Inject
    public PersoninfoAdapter(TpsAdapter tpsAdapter,
                             FødselTjeneste fødselTjeneste,
                             TilknytningTjeneste tilknytningTjeneste,
                             PersonBasisTjeneste basisTjeneste,
                             DkifSpråkKlient dkifSpråkKlient) {
        this.tpsAdapter = tpsAdapter;
        this.fødselTjeneste = fødselTjeneste;
        this.tilknytningTjeneste = tilknytningTjeneste;
        this.basisTjeneste = basisTjeneste;
        this.dkifSpråkKlient = dkifSpråkKlient;
    }

    public Personinfo innhentSaksopplysningerForSøker(AktørId aktørId) {
        return hentKjerneinformasjon(aktørId).orElse(null);
    }

    public Optional<Personinfo> innhentSaksopplysningerForEktefelle(AktørId aktørId) {
        return hentKjerneinformasjon(aktørId);
    }

    public Optional<Personinfo> innhentSaksopplysninger(PersonIdent personIdent) {
        Optional<AktørId> aktørId = tpsAdapter.hentAktørIdForPersonIdent(personIdent);
        return aktørId.flatMap(a -> hentKjerneinformasjonForBarn(a, personIdent));
    }

    public Personhistorikkinfo innhentPersonopplysningerHistorikk(AktørId aktørId, Interval interval) {
        return tpsAdapter.hentPersonhistorikk(aktørId, interval);
    }

    /** Henter PersonInfo for barn, gitt at det ikke er FDAT nummer (sjekkes på format av PersonIdent, evt. ved feilhåndtering fra TPS). Hvis FDAT nummer returneres {@link Optional#empty()} */
    public Optional<Personinfo> innhentSaksopplysningerForBarn(PersonIdent personIdent) {
        if(personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        Optional<AktørId> optAktørId = tpsAdapter.hentAktørIdForPersonIdent(personIdent);
        if (optAktørId.isPresent()) {
            return hentKjerneinformasjonForBarn(optAktørId.get(), personIdent);
        }
        return Optional.empty();
    }

    public List<AktørId> finnAktørIdForForeldreTil(PersonIdent personIdent) {
        return fødselTjeneste.hentForeldreTil(personIdent).stream()
            .flatMap(p -> tpsAdapter.hentAktørIdForPersonIdent(p).stream())
            .collect(Collectors.toList());
    }

    private Optional<Personinfo> hentKjerneinformasjonForBarn(AktørId aktørId, PersonIdent personIdent) {
        if(personIdent.erFdatNummer()) {
            return Optional.empty();
        }
        try {
            return Optional.of(hentKjerneinformasjon(aktørId, personIdent)
            );
            // TODO Lag en skikkelig fiks på dette
            //Her sorterer vi ut dødfødte barn
        } catch (SOAPFaultException e) {
            if (e.getCause().getMessage().contains("status: S610006F")) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private Optional<Personinfo> hentKjerneinformasjon(AktørId aktørId) {
        return tpsAdapter.hentIdentForAktørId(aktørId).map(i -> tpsAdapter.hentKjerneinformasjon(i, aktørId));
    }

    private Personinfo hentKjerneinformasjon(AktørId aktørId, PersonIdent personIdent) {
        return tpsAdapter.hentKjerneinformasjon(personIdent, aktørId);
    }

    public List<FødtBarnInfo> innhentAlleFødteForBehandlingIntervaller(AktørId aktørId, List<LocalDateInterval> intervaller) {
        return fødselTjeneste.hentFødteBarnInfoFor(aktørId, intervaller);
    }

    public Optional<AktørId> hentAktørForFnr(PersonIdent fnr) {
        return tpsAdapter.hentAktørIdForPersonIdent(fnr);
    }

    public PersonIdent hentFnrForAktør(AktørId aktørId) {
        return hentFnr(aktørId).orElseThrow(() -> TpsFeilmeldinger.FACTORY.fantIkkePersonForAktørId().toException());
    }

    public Optional<PersonIdent> hentFnr(AktørId aktørId) {
        return tpsAdapter.hentIdentForAktørId(aktørId);
    }

    public Optional<PersoninfoBasis> hentBrukerBasisForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr = hentFnr(aktørId);
        Optional<PersoninfoBasis> pi = funnetFnr.map(fnr -> tpsAdapter.hentKjerneinformasjonBasis(fnr, aktørId));
        pi.ifPresent(p -> basisTjeneste.hentBasisPersoninfo(aktørId, p.getPersonIdent(), p));
        return pi;
    }

    public Optional<PersoninfoArbeidsgiver> hentBrukerArbeidsgiverForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr = hentFnr(aktørId);
        return funnetFnr.flatMap(fnr -> basisTjeneste.hentArbeidsgiverPersoninfo(aktørId, fnr));
    }

    public Optional<PersoninfoKjønn> hentBrukerKjønnForAktør(AktørId aktørId) {
        return basisTjeneste.hentKjønnPersoninfo(aktørId);
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
