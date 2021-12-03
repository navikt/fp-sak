package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class LagreVurderingRequest {

    @JsonProperty(value = "konsumentId")
    private UUID konsumentId;

    @JsonProperty(value = "risikovurderingKode")
    private String risikovurderingKode;

    public UUID getKonsumentId() {
        return konsumentId;
    }

    public void setKonsumentId(UUID konsumentId) {
        this.konsumentId = konsumentId;
    }

    public String getRisikovurderingKode() {
        return risikovurderingKode;
    }

    public void setRisikovurderingKode(String risikovurderingKode) {
        this.risikovurderingKode = risikovurderingKode;
    }
}
