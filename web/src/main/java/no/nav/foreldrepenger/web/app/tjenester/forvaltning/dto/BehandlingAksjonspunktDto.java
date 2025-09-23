package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonIgnore;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class BehandlingAksjonspunktDto implements AbacDto {

    @NotNull
    @QueryParam("behandlingUuid")
    @Valid
    private UUID behandlingUuid;

    @NotNull
    @QueryParam("aksjonspunktKode")
    @Digits(integer = 4, fraction = 0)
    private String aksjonspunktKode;

    public BehandlingAksjonspunktDto(@NotNull UUID behandlingUuid, @NotNull String aksjonspunktKode) {
        this.behandlingUuid = behandlingUuid;
        this.aksjonspunktKode = aksjonspunktKode;
    }

    public BehandlingAksjonspunktDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = TilbakeRestTjeneste.opprett();
        if (behandlingUuid != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, behandlingUuid);
        }
        if (aksjonspunktKode != null) {
            abac.leggTil(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON, getAksjonspunktDefinisjon());
        }
        return abac;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public String getAksjonspunktKode() {
        return aksjonspunktKode;
    }

    @JsonIgnore
    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return AksjonspunktDefinisjon.fraKode(aksjonspunktKode);
    }
}
