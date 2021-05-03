package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class SokefeltDto implements AbacDto {

    @NotNull
    @Digits(integer = 18, fraction = 0)
    private String searchString;

    @SuppressWarnings("unused")
    private SokefeltDto() { // NOSONAR
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

    @Override
    public AbacDataAttributter abacAttributter() {
        var attributter = AbacDataAttributter.opprett();
        if (searchString.length() == 11 /* guess - fødselsnummer */) {
            attributter.leggTil(AppAbacAttributtType.FNR, searchString)
                .leggTil(AppAbacAttributtType.SAKER_MED_FNR, searchString);
        } else if (searchString.length() == 13 /* guess - aktørId */) {
            attributter.leggTil(AppAbacAttributtType.AKTØR_ID, searchString)
                .leggTil(AppAbacAttributtType.SAKER_FOR_AKTØR, searchString);
        } else {
            attributter.leggTil(AppAbacAttributtType.SAKSNUMMER, searchString);
        }
        return attributter;
    }

}
