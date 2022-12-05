package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import io.swagger.v3.oas.annotations.Parameter;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class BeskyttAktørDto implements AbacDto {


    @NotNull
    @Parameter(description = "Aktørid")
    @QueryParam("aktørId")
    @Digits(integer = 13, fraction = 0)
    private String aktørId;

    public BeskyttAktørDto(@NotNull String aktørId) {
        this.aktørId = aktørId;
    }

    public BeskyttAktørDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett();
    }

    public String aktørId() {
        return aktørId;
    }
}
