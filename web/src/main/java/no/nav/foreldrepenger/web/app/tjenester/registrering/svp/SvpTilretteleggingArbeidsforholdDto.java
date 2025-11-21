package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.validation.Valid;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SvpTilretteleggingFrilanserDto.class, name = "FR"),
    @JsonSubTypes.Type(value = SvpTilretteleggingVirksomhetDto.class, name = "VI"),
    @JsonSubTypes.Type(value = SvpTilretteleggingPrivatArbeidsgiverDto.class, name = "PA"),
    @JsonSubTypes.Type(value = SvpTilretteleggingSelvstendigNæringsdrivendeDto.class, name = "SN")
})
public abstract class SvpTilretteleggingArbeidsforholdDto {

    private LocalDate behovsdato;
    private List<@Valid SvpTilretteleggingDto> tilrettelegginger = new ArrayList<>();

    public LocalDate getBehovsdato() {
        return behovsdato;
    }

    public void setBehovsdato(LocalDate behovsdato) {
        this.behovsdato = behovsdato;
    }

    public List<SvpTilretteleggingDto> getTilrettelegginger() {
        return tilrettelegginger;
    }

    public void setTilrettelegginger(List<SvpTilretteleggingDto> tilrettelegginger) {
        this.tilrettelegginger = tilrettelegginger;
    }
}
