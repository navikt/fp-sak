package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class BehandlingAksjonspunktDto implements AbacDto {

    @NotNull
    @QueryParam("behandlingId")
    @DefaultValue("0")
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @NotNull
    @QueryParam("aksjonspunktKode")
    @Digits(integer = 4, fraction = 0)
    private String aksjonspunktKode;

    public BehandlingAksjonspunktDto(@NotNull Long behandlingId, @NotNull String aksjonspunktKode) {
        this.behandlingId = behandlingId;
        this.aksjonspunktKode = aksjonspunktKode;
    }

    public BehandlingAksjonspunktDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();
        if (behandlingId != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
        }
        if (aksjonspunktKode != null) {
            abac.leggTil(AppAbacAttributtType.AKSJONSPUNKT_KODE, aksjonspunktKode);
        }
        return abac;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getAksjonspunktKode() {
        return aksjonspunktKode;
    }
}
