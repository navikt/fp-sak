package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyEnum;

import java.util.UUID;

public class BehandlingOperasjonerDto {

    private UUID uuid;
    private boolean behandlingKanBytteEnhet;
    private boolean behandlingKanHenlegges;
    private boolean behandlingKanGjenopptas;
    private boolean behandlingKanOpnesForEndringer;
    private boolean behandlingKanSettesPaVent;
    private boolean behandlingKanSendeMelding;
    private boolean behandlingFraBeslutter;
    private boolean behandlingTilGodkjenning;
    private VergeBehandlingsmenyEnum vergeBehandlingsmeny;

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

    public boolean isBehandlingKanSendeMelding() {
        return behandlingKanSendeMelding;
    }

    public boolean isBehandlingFraBeslutter() {
        return behandlingFraBeslutter;
    }

    public boolean isBehandlingTilGodkjenning() {
        return behandlingTilGodkjenning;
    }

    public VergeBehandlingsmenyEnum getVergeBehandlingsmeny() {
        return vergeBehandlingsmeny;
    }

    public static Builder builder(UUID uuid) {
        return new Builder(uuid);
    }

    public static class Builder {
        private BehandlingOperasjonerDto kladd;

        private Builder(UUID uuid) {
            kladd = new BehandlingOperasjonerDto();
            kladd.uuid = uuid;
            kladd.vergeBehandlingsmeny = VergeBehandlingsmenyEnum.SKJUL;
        }

        public Builder medKanBytteEnhet(boolean bytteEnhet) {
            this.kladd.behandlingKanBytteEnhet = bytteEnhet;
            return this;
        }

        public Builder medKanHenlegges(boolean henlegges) {
            this.kladd.behandlingKanHenlegges = henlegges;
            return this;
        }

        public Builder medKanGjenopptas(boolean gjenopptas) {
            this.kladd.behandlingKanGjenopptas = gjenopptas;
            return this;
        }

        public Builder medKanSettesPaVent(boolean settVent) {
            this.kladd.behandlingKanSettesPaVent = settVent;
            return this;
        }

        public Builder medKanOpnesForEndringer(boolean opnes) {
            this.kladd.behandlingKanOpnesForEndringer = opnes;
            return this;
        }

        public Builder medKanSendeMelding(boolean sendeMelding) {
            this.kladd.behandlingKanSendeMelding = sendeMelding;
            return this;
        }

        public Builder medVergemeny(VergeBehandlingsmenyEnum vergeMeny) {
            this.kladd.vergeBehandlingsmeny = vergeMeny;
            return this;
        }

        public Builder medFraBeslutter(boolean fraBeslutter) {
            this.kladd.behandlingFraBeslutter = fraBeslutter;
            return this;
        }

        public Builder medTilGodkjenning(boolean tilGodkjenning) {
            this.kladd.behandlingTilGodkjenning = tilGodkjenning;
            return this;
        }

        public BehandlingOperasjonerDto build() {
            return kladd;
        }
    }
}
