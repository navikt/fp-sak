package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class RisikovurderingRequest {

    private AktoerIdDto soekerAktoerId;

    private LocalDate skjæringstidspunkt;

    private Opplysningsperiode opplysningsperiode;

    private String behandlingstema;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AnnenPart annenPart;

    private UUID konsumentId;

    public RisikovurderingRequest() {
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public AktoerIdDto getSoekerAktoerId() {
        return soekerAktoerId;
    }

    public Opplysningsperiode getOpplysningsperiode() {
        return opplysningsperiode;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public AnnenPart getAnnenPart() {
        return annenPart;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getKonsumentId() {
        return konsumentId;
    }

    public static class Builder{
        private RisikovurderingRequest mal;

        public Builder(){
            this.mal = new RisikovurderingRequest();
        }

        public Builder medSoekerAktoerId(AktoerIdDto soekerAktoerId){
            mal.soekerAktoerId = soekerAktoerId;
            return this;
        }

        public Builder medSkjæringstidspunkt(LocalDate skjæringstidspunkt){
            mal.skjæringstidspunkt = skjæringstidspunkt;
            return this;
        }

        public Builder medOpplysningsperiode(Opplysningsperiode opplysningsperiode){
            mal.opplysningsperiode = opplysningsperiode;
            return this;
        }

        public Builder medBehandlingstema(String behandlingstema){
            mal.behandlingstema = behandlingstema;
            return this;
        }

        public Builder medAnnenPart(AnnenPart annenPart){
            mal.annenPart = annenPart;
            return this;
        }

        public Builder medKonsumentId(UUID konsumentId){
            mal.konsumentId = konsumentId;
            return this;
        }

        public RisikovurderingRequest build(){
            verifyStateForBuild();
            return mal;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(mal.soekerAktoerId, "soekerAktoerId");
            Objects.requireNonNull(mal.konsumentId, "konsumentid");
            Objects.requireNonNull(mal.skjæringstidspunkt, "skjæringstidspunkt");
            Objects.requireNonNull(mal.opplysningsperiode, "opplysningsperiode");
            Objects.requireNonNull(mal.behandlingstema, "behandlingstema");
        }

    }

}
