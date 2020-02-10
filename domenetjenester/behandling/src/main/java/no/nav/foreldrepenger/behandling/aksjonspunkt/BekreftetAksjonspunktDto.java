package no.nav.foreldrepenger.behandling.aksjonspunkt;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
/** Husk @JsonTypeName på alle sublasser!! */
public abstract class BekreftetAksjonspunktDto implements AksjonspunktKode, AbacDto {

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    protected BekreftetAksjonspunktDto() {
        // For Jackson
    }

    protected BekreftetAksjonspunktDto(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.AKSJONSPUNKT_KODE, getKode());
    }

    @Override
    public String getKode() {
        if (this.getClass().isAnnotationPresent(JsonTypeName.class)) {
            return this.getClass().getDeclaredAnnotation(JsonTypeName.class).value();
        }
        throw new IllegalStateException("Utvikler-feil:" +this.getClass().getSimpleName() +" er uten JsonTypeName annotation.");
    }
}
