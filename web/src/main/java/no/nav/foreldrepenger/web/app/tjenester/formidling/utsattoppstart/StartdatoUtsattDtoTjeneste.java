package no.nav.foreldrepenger.web.app.tjenester.formidling.utsattoppstart;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.UtsettelseCore2021;

@ApplicationScoped
public class StartdatoUtsattDtoTjeneste {

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public StartdatoUtsattDtoTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    }

    public StartdatoUtsattDtoTjeneste() {
    }

    public StartdatoUtsattDto getInformasjonOmUtsettelseFraStart(Behandling behandling) {
        var resultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(r -> BehandlingResultatType.FORELDREPENGER_SENERE.equals(r.getBehandlingResultatType()));
        if (resultat.isEmpty()) {
            return new StartdatoUtsattDto(false, null);
        }
        var oppgitt = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(YtelseFordelingAggregat::getOppgittFordeling);
        var tidligsteUttaksperiode = UtsettelseCore2021.finnFørsteDatoFraSøknad(oppgitt, false);
        return new StartdatoUtsattDto(true, tidligsteUttaksperiode.orElse(null));
    }


}
