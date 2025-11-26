package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public record MottaPapirsøknadDto(@NotNull @QueryParam("saksnummer") @Digits(integer = 18, fraction = 0) String saksnummer,
                                  @NotNull @QueryParam("journalpostId") @Digits(integer = 18, fraction = 0) String journalpostId,
                                  @NotNull @QueryParam("søknadType") @Valid SøknadType søknadType,
                                  @NotNull @QueryParam("forsendelseMottatt") LocalDate forsendelseMottatt) implements AbacDto {

    public enum SøknadType { ENGANGSSTØNAD_ADOPSJON, ENGANGSSTØNAD_FØDSEL,
        FORELDREPENGER_ADOPSJON, FORELDREPENGER_FØDSEL, ENDRING_FORELDREPENGER, SVANGERSKAPSPENGER }



    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer);
    }

}
