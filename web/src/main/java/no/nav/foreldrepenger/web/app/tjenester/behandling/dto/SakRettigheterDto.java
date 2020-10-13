package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

public class SakRettigheterDto {

    private List<BehandlingType> behandlingTypeKanOpprettes;
    private List<BehandlingType> behandlingTypeKanIkkeOpprettes;

    private boolean sakSkalTilInfotrygd;

    private List<UUID> behandlingKanBytteEnhet;

    private List<UUID> behandlingKanHenlegges;

    private List<UUID> behandlingKanGjenopptas;

    private List<UUID> behandlingKanOpnesForEndringer;

    private List<UUID> behandlingKanSettesPaVent;

    private SakRettigheterDto() {
    }

    public List<BehandlingType> getBehandlingTypeKanOpprettes() {
        return behandlingTypeKanOpprettes;
    }

    public List<BehandlingType> getBehandlingTypeKanIkkeOpprettes() {
        return behandlingTypeKanIkkeOpprettes;
    }

    public boolean isSakSkalTilInfotrygd() {
        return sakSkalTilInfotrygd;
    }

    public List<UUID> getBehandlingKanBytteEnhet() {
        return behandlingKanBytteEnhet;
    }

    public List<UUID> getBehandlingKanHenlegges() {
        return behandlingKanHenlegges;
    }

    public List<UUID> getBehandlingKanGjenopptas() {
        return behandlingKanGjenopptas;
    }

    public List<UUID> getBehandlingKanOpnesForEndringer() {
        return behandlingKanOpnesForEndringer;
    }

    public List<UUID> getBehandlingKanSettesPaVent() {
        return behandlingKanSettesPaVent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SakRettigheterDto dto;

        private Builder() {
            dto = new SakRettigheterDto();
            dto.behandlingKanBytteEnhet = new ArrayList<>();
            dto.behandlingKanHenlegges = new ArrayList<>();
            dto.behandlingKanGjenopptas = new ArrayList<>();
            dto.behandlingKanOpnesForEndringer = new ArrayList<>();
            dto.behandlingKanSettesPaVent = new ArrayList<>();
            dto.behandlingTypeKanOpprettes = new ArrayList<>();
            dto.behandlingTypeKanIkkeOpprettes = new ArrayList<>();

        }

        public Builder skalTilInfotrygd() {
            dto.sakSkalTilInfotrygd = true;
            return this;
        }

        public Builder behandlingTypeKanOpprettes(BehandlingType type, boolean kanopprettes) {
            if (kanopprettes) {
                dto.behandlingTypeKanOpprettes.add(type);
            } else {
                dto.behandlingTypeKanIkkeOpprettes.add(type);
            }
            return this;
        }

        public Builder kanBytteEnhet(UUID behandling) {
            dto.behandlingKanBytteEnhet.add(behandling);
            return this;
        }

        public Builder kanHenlegges(UUID behandling) {
            dto.behandlingKanHenlegges.add(behandling);
            return this;
        }

        public Builder kanSettesPaVent(UUID behandling) {
            dto.behandlingKanSettesPaVent.add(behandling);
            return this;
        }

        public Builder kanGjenopptas(UUID behandling) {
            dto.behandlingKanGjenopptas.add(behandling);
            return this;
        }

        public Builder kanOpnesForEndringer(UUID behandling) {
            dto.behandlingKanOpnesForEndringer.add(behandling);
            return this;
        }

        public SakRettigheterDto build() {
            return dto;
        }
    }
}
