package no.nav.foreldrepenger.domene.opptjening.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatus;

public class OpptjeningIUtlandDokStatusDto {

    private final OpptjeningIUtlandDokStatus dokStatus;

    public OpptjeningIUtlandDokStatusDto(OpptjeningIUtlandDokStatus dokStatus) {
        this.dokStatus = dokStatus;
    }

    public OpptjeningIUtlandDokStatus getDokStatus() {
        return dokStatus;
    }
}
