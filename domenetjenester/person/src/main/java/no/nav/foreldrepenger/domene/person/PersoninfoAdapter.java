package no.nav.foreldrepenger.domene.person;

import java.util.Collections;
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
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoSpråk;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
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
        if(personIdent.erFdatNummer()) {
            return Collections.emptyList();
        }
        var foreldre = tpsAdapter.hentForeldreTil(personIdent);
        if (fødselTjeneste != null)
            fødselTjeneste.hentForeldreTil(personIdent, foreldre);
        return foreldre.stream()
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
        PersonIdent personIdent = tpsAdapter.hentIdentForAktørId(aktørId).orElseThrow(() -> TpsFeilmeldinger.FACTORY.fantIkkePersonForAktørId().toException());
        List<FødtBarnInfo> barneListe = tpsAdapter.hentFødteBarn(personIdent);
        if (fødselTjeneste != null)
            fødselTjeneste.hentFødteBarnInfoFor(aktørId, barneListe, intervaller);
        return barneListe.stream().filter(p -> intervaller.stream().anyMatch(i -> i.encloses(p.getFødselsdato()))).collect(Collectors.toList());
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

    private Optional<Personinfo> hentBrukerForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr = hentFnr(aktørId);
        return funnetFnr.map(fnr -> tpsAdapter.hentKjerneinformasjon(fnr, aktørId));
    }

    public Optional<PersoninfoBasis> hentBrukerBasisForAktør(AktørId aktørId) {
        Optional<PersonIdent> funnetFnr = hentFnr(aktørId);
        Optional<PersoninfoBasis> pi = funnetFnr.map(fnr -> tpsAdapter.hentKjerneinformasjonBasis(fnr, aktørId));
        pi.ifPresent(p -> basisTjeneste.hentBasisPersoninfo(aktørId, p.getPersonIdent(), p));
        return pi;
    }

    public GeografiskTilknytning hentGeografiskTilknytning(AktørId aktørId) {
        var gt = hentFnr(aktørId).map(fnr -> tpsAdapter.hentGeografiskTilknytning(fnr))
            .orElseGet(() -> new GeografiskTilknytning(null, null));
        if (tilknytningTjeneste != null)
            tilknytningTjeneste.hentGeografiskTilknytning(aktørId, gt);
        return gt;
    }

    public Optional<String> hentDiskresjonskodeForAktør(AktørId aktørId) {
        return Optional.ofNullable(hentGeografiskTilknytning(aktørId).getDiskresjonskode());
    }

    public PersoninfoSpråk hentForetrukketSpråk(AktørId aktørId) {
        var person = hentBrukerForAktør(aktørId);
        var fraTPS = person.map(p -> new PersoninfoSpråk(aktørId, p.getForetrukketSpråk())).orElseGet(() -> new PersoninfoSpråk(aktørId, Språkkode.NB));
        if (dkifSpråkKlient != null && person.isPresent()) {
            try {
                var fraDkif = dkifSpråkKlient.finnSpråkkodeForBruker(person.get().getPersonIdent().getIdent());
                if (fraTPS.getForetrukketSpråk().equals(fraDkif)) {
                    LOG.info("FPSAK PDL DKIF: like svar");
                } else {
                    LOG.info("FPSAK PDL DKIF: ulike svar TPS {} og DKIF {}", fraTPS.getForetrukketSpråk(), fraDkif);
                }
            } catch (Exception e) {
                LOG.info("FPSAK PDL DKIF: feil", e);
            }
        }
        return fraTPS;
    }

}
