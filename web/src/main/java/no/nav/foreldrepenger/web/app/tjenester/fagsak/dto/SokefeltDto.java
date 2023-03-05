package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

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
        return searchString;
    }


}
