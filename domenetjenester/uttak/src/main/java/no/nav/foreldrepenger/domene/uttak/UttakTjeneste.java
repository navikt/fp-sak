package no.nav.foreldrepenger.domene.uttak;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class UttakTjeneste {

    private BehandlingRepository behandlingRepository;
    private ForeldrepengerUttakTjeneste foreldrepengerTjeneste;
    private SvangerskapspengerUttakTjeneste svangerskapspengerTjeneste;

    @Inject
    public UttakTjeneste(BehandlingRepository behandlingRepository,
                         ForeldrepengerUttakTjeneste foreldrepengerTjeneste,
                         SvangerskapspengerUttakTjeneste svangerskapspengerTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.foreldrepengerTjeneste = foreldrepengerTjeneste;
        this.svangerskapspengerTjeneste = svangerskapspengerTjeneste;
    }

    UttakTjeneste() {
        //CDI
    }

    public Optional<Uttak> hentHvisEksisterer(long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return switch (behandling.getFagsakYtelseType()) {
            case FORELDREPENGER -> foreldrepengerTjeneste.hentHvisEksisterer(behandlingId).map(u -> u);
            case SVANGERSKAPSPENGER -> svangerskapspengerTjeneste.hentHvisEksisterer(behandling.getId()).map(u -> u);
            case UDEFINERT, ENGANGSTØNAD -> throw new IllegalStateException("Unexpected value: " + behandling.getFagsakYtelseType());
        };
    }
}
