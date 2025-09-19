package no.nav.foreldrepenger.behandling.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
/** Husk @JsonTypeName på alle sublasser!! */
public abstract class BekreftetAksjonspunktDto implements AksjonspunktKode {

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
    public AksjonspunktDefinisjon getAksjonspunktDefinisjon() {
        if (this.getClass().isAnnotationPresent(JsonTypeName.class)) {
            var kode = this.getClass().getDeclaredAnnotation(JsonTypeName.class).value();
            return AksjonspunktDefinisjon.fraKode(kode);
        }
        throw new IllegalStateException("Utvikler-feil:" + this.getClass().getSimpleName() + " er uten JsonTypeName annotation.");
    }
}
