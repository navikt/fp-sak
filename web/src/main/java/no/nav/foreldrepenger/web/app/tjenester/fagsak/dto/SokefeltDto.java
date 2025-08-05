package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record SokefeltDto(@NotNull
                          @Digits(integer = 18, fraction = 0)
                          String searchString) {

    public String getSearchString() {
        // fjerner alle space-tegn fra søkestrengen - også unicode-spaces
        return searchString != null ? searchString.replaceAll("\\p{IsWhite_Space}", "") : "";
    }

}
