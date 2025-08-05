package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class SokefeltDto {

    @NotNull
    @Digits(integer = 18, fraction = 0)
    private String searchString;

    @SuppressWarnings("unused")
    private SokefeltDto() {
    }

    public SokefeltDto(String searchString) {
        this.searchString = searchString;
    }

    public SokefeltDto(Saksnummer saksnummer) {
        this.searchString = saksnummer.getVerdi();
    }

    public String getSearchString() {
        // fjerner alle space-tegn fra søkestrengen - også unicode-spaces
        return searchString != null ? searchString.replaceAll("\\p{IsWhite_Space}", "") : "";
    }


}
