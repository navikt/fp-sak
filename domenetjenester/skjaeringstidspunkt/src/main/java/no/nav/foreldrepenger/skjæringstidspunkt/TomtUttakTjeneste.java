package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;

@ApplicationScoped
public class TomtUttakTjeneste {


    private FpUttakRepository fpUttakRepository;
    private BehandlingRepository behandlingRepository;
    private UtsettelseBehandling2021 utsettelseBehandling2021;

    TomtUttakTjeneste() {
        // CDI
    }

    @Inject
    public TomtUttakTjeneste(BehandlingRepositoryProvider repositoryProvider,
                             UtsettelseBehandling2021 utsettelseBehandling2021) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.utsettelseBehandling2021 = utsettelseBehandling2021;
    }

    public Optional<LocalDate> startdatoUttakResultatFrittUttak(Fagsak fagsak) {
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .filter(b -> FagsakYtelseType.FORELDREPENGER.equals(b.getFagsakYtelseType()))
            .filter(b -> !utsettelseBehandling2021.kreverSammenhengendeUttak(b))
            .map(b -> fpUttakRepository.hentUttakResultat(b.getId()).getGjeldendePerioder())
            .flatMap(ur -> UtsettelseCore2021.finnFørsteDatoFraUttakResultat(ur.getPerioder(), false));
    }
}
