package no.nav.foreldrepenger.domene.person.tps;

import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class TpsFamilieTjeneste {

    private TpsTjeneste tpsTjeneste;
    private SøknadRepository søknadRepository;
    private Period etterkontrollTidsromFørSøknadsdato;
    private Period etterkontrollTidsromEtterTermindato;

    TpsFamilieTjeneste() {
        // for CDI proxy
    }

    /**
     * @param etterkontrollTidsromFørSøknadsdato - Periode før søknadsdato hvor det skal etterkontrolleres barn er født
     * @param etterkontrollTidsromEtterTermindato - Periode etter termindato hvor det skal etterkontrolleres barn er født
     */
    @Inject
    public TpsFamilieTjeneste(TpsTjeneste tpsTjeneste,
                              BehandlingRepositoryProvider repositoryProvider,
                              @KonfigVerdi(value = "etterkontroll.førsøknad.periode", defaultVerdi = "P1W") Period etterkontrollTidsromFørSøknadsdato,
                              @KonfigVerdi(value = "etterkontroll.ettertermin.periode", defaultVerdi = "P4W") Period etterkontrollTidsromEtterTermindato) {
        this.tpsTjeneste = tpsTjeneste;
        this.etterkontrollTidsromEtterTermindato = etterkontrollTidsromEtterTermindato;
        this.etterkontrollTidsromFørSøknadsdato = etterkontrollTidsromFørSøknadsdato;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
    }

    public List<FødtBarnInfo> getFødslerRelatertTilBehandling(Behandling behandling, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        final SøknadEntitet søknad = finnOrginalSøknad(behandling);
        DatoIntervallEntitet forventetFødselIntervall = TpsFødselUtil.forventetFødselIntervall(familieHendelseGrunnlag,
            etterkontrollTidsromFørSøknadsdato, etterkontrollTidsromEtterTermindato, søknad);

        List<FødtBarnInfo> barneListe = tpsTjeneste.hentFødteBarn(behandling.getAktørId());
        return barneListe.stream().filter(p -> forventetFødselIntervall.inkluderer(p.getFødselsdato())).collect(Collectors.toList());
    }

    public boolean harBrukerDnr(Behandling behandling) {
        Optional<PersonIdent> bruker = tpsTjeneste.hentFnr(behandling.getAktørId());
        return bruker.map(PersonIdent::erDnr).orElse(false);
    }

    private SøknadEntitet finnOrginalSøknad(Behandling behandling) {
        return søknadRepository.hentFørstegangsSøknad(behandling);
    }
}
