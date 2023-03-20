package no.nav.foreldrepenger.domene.opptjening.dto;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;

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
