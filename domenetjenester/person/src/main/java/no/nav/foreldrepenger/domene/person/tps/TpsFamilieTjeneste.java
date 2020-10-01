package no.nav.foreldrepenger.domene.person.tps;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class TpsFamilieTjeneste {

    private TpsTjeneste tpsTjeneste;

    TpsFamilieTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TpsFamilieTjeneste(TpsTjeneste tpsTjeneste) {
        this.tpsTjeneste = tpsTjeneste;
    }

    public List<FødtBarnInfo> getFødslerRelatertTilBehandling(AktørId aktørId, List<LocalDateInterval> intervaller) {
        List<FødtBarnInfo> barneListe = tpsTjeneste.hentFødteBarn(aktørId);
        return barneListe.stream().filter(p -> intervaller.stream().anyMatch(i -> i.encloses(p.getFødselsdato()))).collect(Collectors.toList());
    }

    public boolean harBrukerDnr(AktørId aktørId) {
        return tpsTjeneste.hentFnr(aktørId).map(PersonIdent::erDnr).orElse(false);
    }

}
