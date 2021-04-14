package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class AksjonspunktKodeDto implements AbacDto {

    @NotNull
    @QueryParam("aksjonspunktKode")
    @Digits(integer = 4, fraction = 0)
    private String aksjonspunktKode;

    public AksjonspunktKodeDto(@NotNull String aksjonspunktKode) {
        this.aksjonspunktKode = aksjonspunktKode;
    }

    public AksjonspunktKodeDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();
        if (aksjonspunktKode != null) {
            abac.leggTil(AppAbacAttributtType.AKSJONSPUNKT_KODE, aksjonspunktKode);
        }
        return abac;
    }

    public String getAksjonspunktKode() {
        return aksjonspunktKode;
    }
}
