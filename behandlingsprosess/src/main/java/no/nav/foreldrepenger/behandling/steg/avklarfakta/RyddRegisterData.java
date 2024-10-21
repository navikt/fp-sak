package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

public class RyddRegisterData {
    private final BehandlingRepository behandlingRepository;
    private final BehandlingskontrollKontekst kontekst;

    public RyddRegisterData(BehandlingRepositoryProvider repositoryProvider, BehandlingskontrollKontekst kontekst) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kontekst = kontekst;
    }

    /**
     * @deprecated Erstatt med {@link #ryddRegisterdata()}
     */
    @Deprecated
    public void ryddRegisterdataLegacyEngangsstønad() {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandling.nullstillToTrinnsBehandling();
        behandlingRepository.slettTidligereBeregningerES(behandling, kontekst.getSkriveLås());
    }

    public void ryddRegisterdata() {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandling.nullstillToTrinnsBehandling();
    }

    public void ryddRegisterdataStartpunktRevurdering() {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        behandling.nullstillToTrinnsBehandling();
        behandling.setStartpunkt(StartpunktType.UDEFINERT);
    }

}
