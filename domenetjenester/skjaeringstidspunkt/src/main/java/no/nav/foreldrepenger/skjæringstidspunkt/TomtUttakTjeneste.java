package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

@ApplicationScoped
public class TomtUttakTjeneste {


    private FpUttakRepository fpUttakRepository;
    private BehandlingRepository behandlingRepository;

    TomtUttakTjeneste() {
        // CDI
    }

    @Inject
    public TomtUttakTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
    }

    public Optional<LocalDate> startdatoUttakResultatFrittUttak(Fagsak fagsak) {
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .filter(b -> FagsakYtelseType.FORELDREPENGER.equals(b.getFagsakYtelseType()))
            .flatMap(b -> fpUttakRepository.hentUttakResultatHvisEksisterer(b.getId()))
            .flatMap(ur -> UtsettelseCore2021.finnFørsteDatoFraUttakResultat(ur.getGjeldendePerioder().getPerioder()))
            .filter(startdato -> !UtsettelseCore2021.kreverSammenhengendeUttak(startdato));
    }
}
