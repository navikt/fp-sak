package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonIgnore;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
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
            abac.leggTil(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON, getAksjonspunktDefinisjon());
        }
        return abac;
    }

    public String getAksjonspunktKode() {
        return aksjonspunktKode;
    }

    @JsonIgnore
    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return AksjonspunktDefinisjon.fraKode(aksjonspunktKode);
    }
}
