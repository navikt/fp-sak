package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FaresignalerRequest {

    @JsonProperty(value = "konsumentId")
    private UUID konsumentId;

    public UUID getKonsumentId() {
        return konsumentId;
    }

    public void setKonsumentId(UUID konsumentId) {
        this.konsumentId = konsumentId;
    }
}
