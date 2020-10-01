package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FaresignalerRespons {

    @JsonProperty(value = "risikoklasse", required = true)
    @NotNull
    private String risikoklasse;

    @JsonProperty(value = "medlFaresignaler")
    @Valid
    private Faresignalgruppe medlFaresignaler;

    @JsonProperty(value = "iayFaresignaler")
    @Valid
    private Faresignalgruppe iayFaresignaler;

    public String getRisikoklasse() {
        return risikoklasse;
    }

    public void setRisikoklasse(String risikoklasse) {
        this.risikoklasse = risikoklasse;
    }

    public Faresignalgruppe getMedlFaresignaler() {
        return medlFaresignaler;
    }

    public void setMedlFaresignaler(Faresignalgruppe medlFaresignaler) {
        this.medlFaresignaler = medlFaresignaler;
    }

    public Faresignalgruppe getIayFaresignaler() {
        return iayFaresignaler;
    }

    public void setIayFaresignaler(Faresignalgruppe iayFaresignaler) {
        this.iayFaresignaler = iayFaresignaler;
    }
}
