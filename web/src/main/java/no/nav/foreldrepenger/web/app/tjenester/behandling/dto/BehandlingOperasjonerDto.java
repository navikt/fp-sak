package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.UUID;

public class BehandlingOperasjonerDto {

    private UUID uuid;
    private boolean behandlingKanBytteEnhet;
    private boolean behandlingKanHenlegges;
    private boolean behandlingKanGjenopptas;
    private boolean behandlingKanOpnesForEndringer;
    private boolean behandlingKanSettesPaVent;

    public BehandlingOperasjonerDto(UUID uuid, boolean behandlingKanBytteEnhet, boolean behandlingKanHenlegges,
                                    boolean behandlingKanGjenopptas, boolean behandlingKanSettesPaVent, boolean behandlingKanOpnesForEndringer) {
        this.uuid = uuid;
        this.behandlingKanBytteEnhet = behandlingKanBytteEnhet;
        this.behandlingKanHenlegges = behandlingKanHenlegges;
        this.behandlingKanGjenopptas = behandlingKanGjenopptas;
        this.behandlingKanOpnesForEndringer = behandlingKanOpnesForEndringer;
        this.behandlingKanSettesPaVent = behandlingKanSettesPaVent;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isBehandlingKanBytteEnhet() {
        return behandlingKanBytteEnhet;
    }

    public boolean isBehandlingKanHenlegges() {
        return behandlingKanHenlegges;
    }

    public boolean isBehandlingKanGjenopptas() {
        return behandlingKanGjenopptas;
    }

    public boolean isBehandlingKanOpnesForEndringer() {
        return behandlingKanOpnesForEndringer;
    }

    public boolean isBehandlingKanSettesPaVent() {
        return behandlingKanSettesPaVent;
    }
}
