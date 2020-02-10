package no.nav.foreldrepenger.mottak.vedtak;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;

/**
 * Lytter på henleggelse av behandling for å kunne sette igang berørt behandling hvis det finnes.
 *
 */
@ApplicationScoped
class HenleggelseEventObserver {

    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private KøKontroller køKontroller;

    public HenleggelseEventObserver() {
    }

    @Inject
    public HenleggelseEventObserver(BehandlingRepositoryProvider repositoryProvider, KøKontroller køKontroller) {
        this.behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.køKontroller = køKontroller;
    }

    public void observerBehandlingSteg(@Observes BehandlingTransisjonEvent event) {
        if (FellesTransisjoner.FREMHOPP_TIL_IVERKSETT_VEDTAK.equals(event.getTransisjonIdentifikator())) {
            // merge og henlegg forutsettes håndtert av dokumentmottak (sic)
            boolean erMergetHenlagt = behandlingsresultatRepository.hentHvisEksisterer(event.getBehandlingId())
                .map(Behandlingsresultat::getBehandlingResultatType)
                .filter(BehandlingResultatType.MERGET_OG_HENLAGT::equals)
                .isPresent();
            if (!erMergetHenlagt) {
                // dekø annen forelders berørte behandling hvis den finnes
                Optional<Behandling> køetBehandlingMedforelder = behandlingRevurderingRepository.finnKøetBehandlingMedforelderFraId(event.getFagsakId());
                køetBehandlingMedforelder.ifPresent(behandling1 -> køKontroller.dekøFørsteBehandlingISakskompleks(behandling1));
            }
        }
    }

}
