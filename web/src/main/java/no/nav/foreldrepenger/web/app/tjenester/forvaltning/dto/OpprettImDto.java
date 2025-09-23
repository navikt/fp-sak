package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonIgnore;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
//Midlertidig dto for forvaltningsendepunktet /opprettIMForesporselForBehandling i klassen ForvaltningsUttrekkRestTjeneste
public class OpprettImDto implements AbacDto {
    @Valid
    @QueryParam("behandlingUuid")
    private UUID behandlingUuid;

    @Valid
    @Digits(integer = 4, fraction = 0)
    @QueryParam("aksjonspunktKode")
    private String aksjonspunktKode;

    @Override
    public AbacDataAttributter abacAttributter() {
        return TilbakeRestTjeneste.opprett();
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
