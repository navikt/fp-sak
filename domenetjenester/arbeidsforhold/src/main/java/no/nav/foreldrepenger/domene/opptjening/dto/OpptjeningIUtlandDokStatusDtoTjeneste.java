package no.nav.foreldrepenger.domene.opptjening.dto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;

import java.util.Optional;

@ApplicationScoped
public class OpptjeningIUtlandDokStatusDtoTjeneste {

    private FagsakEgenskapRepository fagsakEgenskapRepository;

    @Inject
    public OpptjeningIUtlandDokStatusDtoTjeneste(FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    OpptjeningIUtlandDokStatusDtoTjeneste() {
        // CDI
    }

    public Optional<OpptjeningIUtlandDokStatusDto> mapFra(Behandling behandling) {
        return fagsakEgenskapRepository.finnUtlandDokumentasjonStatus(behandling.getFagsakId()).map(OpptjeningIUtlandDokStatusDto::new);
    }
}
