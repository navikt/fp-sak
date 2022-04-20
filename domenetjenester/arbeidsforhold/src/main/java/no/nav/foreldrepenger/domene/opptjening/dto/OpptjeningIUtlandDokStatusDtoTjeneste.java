package no.nav.foreldrepenger.domene.opptjening.dto;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;

@ApplicationScoped
public class OpptjeningIUtlandDokStatusDtoTjeneste {
    private OpptjeningIUtlandDokStatusTjeneste opptjeningIUtlandDokStatusTjeneste;

    @Inject
    public OpptjeningIUtlandDokStatusDtoTjeneste(OpptjeningIUtlandDokStatusTjeneste opptjeningIUtlandDokStatusTjeneste) {
        this.opptjeningIUtlandDokStatusTjeneste = opptjeningIUtlandDokStatusTjeneste;
    }

    OpptjeningIUtlandDokStatusDtoTjeneste() {
        // CDI
    }

    public Optional<OpptjeningIUtlandDokStatusDto> mapFra(BehandlingReferanse ref) {
        var dokStatus = opptjeningIUtlandDokStatusTjeneste.hentStatus(ref.behandlingId());
        return dokStatus.map(OpptjeningIUtlandDokStatusDto::new);
    }
}
