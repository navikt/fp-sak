package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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
        var abac = AbacDataAttributter.opprett();
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

    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        return AksjonspunktDefinisjon.fraKode(aksjonspunktKode);
    }
}
