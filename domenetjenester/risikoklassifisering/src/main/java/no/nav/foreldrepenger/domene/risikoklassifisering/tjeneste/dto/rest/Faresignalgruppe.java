package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Faresignalgruppe {

    @JsonProperty(value="risikoklasse", required = true)
    @NotNull
    private String risikoklasse;

    @JsonProperty(value="faresignaler", required = true)
    private List<String> faresignaler;

    public String getRisikoklasse() {
        return risikoklasse;
    }

    public void setRisikoklasse(String risikoklasse) {
        this.risikoklasse = risikoklasse;
    }

    public List<String> getFaresignaler() {
        if (faresignaler == null) {
            return Collections.emptyList();
        }
        return faresignaler;
    }

    public void setFaresignaler(List<String> faresignaler) {
        this.faresignaler = faresignaler;
    }
}
