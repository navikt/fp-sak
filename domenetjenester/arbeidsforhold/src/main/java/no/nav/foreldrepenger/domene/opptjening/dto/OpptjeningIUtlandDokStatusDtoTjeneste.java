package no.nav.foreldrepenger.domene.opptjening.dto;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;

@ApplicationScoped
public class OpptjeningIUtlandDokStatusDtoTjeneste {
    private OpptjeningIUtlandDokStatusTjeneste opptjeningIUtlandDokStatusTjeneste;
    private BehandlingRepository behandlingRepository;

    @Inject
    public OpptjeningIUtlandDokStatusDtoTjeneste(BehandlingRepository behandlingRepository,
                                                 OpptjeningIUtlandDokStatusTjeneste opptjeningIUtlandDokStatusTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.opptjeningIUtlandDokStatusTjeneste = opptjeningIUtlandDokStatusTjeneste;
    }

    OpptjeningIUtlandDokStatusDtoTjeneste() {
        // CDI
    }

    public Optional<OpptjeningIUtlandDokStatusDto> mapFra(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        var dokStatus = opptjeningIUtlandDokStatusTjeneste.hentStatus(behandling).orElse(null);
        var markering = opptjeningIUtlandDokStatusTjeneste.hentUtlandMarkering(behandling).orElse(null);
        return markering != null || dokStatus != null ? Optional.of(new OpptjeningIUtlandDokStatusDto(dokStatus, markering)) : Optional.empty();
    }
}
