package no.nav.foreldrepenger.domene.person.tps;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.ws.soap.SOAPFaultException;

import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class PersoninfoAdapter {

    private TpsAdapter tpsAdapter;

    public PersoninfoAdapter() {
        // for CDI proxy
    }

    @Inject
    public PersoninfoAdapter(TpsAdapter tpsAdapter) {
        this.tpsAdapter = tpsAdapter;
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
        return tpsAdapter.hentForeldreTil(personIdent).stream()
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
        List<FødtBarnInfo> barneListe = tpsAdapter.hentFødteBarn(aktørId);
        return barneListe.stream().filter(p -> intervaller.stream().anyMatch(i -> i.encloses(p.getFødselsdato()))).collect(Collectors.toList());
    }

    public Optional<AktørId> hentAktørIdForPersonIdent(PersonIdent personIdent) {
        return tpsAdapter.hentAktørIdForPersonIdent(personIdent);
    }

    public Optional<PersonIdent> hentAktørIdForPersonIdent(AktørId aktørId) {
        return tpsAdapter.hentIdentForAktørId(aktørId);
    }
}
