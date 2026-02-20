package no.nav.foreldrepenger.behandling.aksjonspunkt;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public abstract class OverstyringAksjonspunktDto implements AksjonspunktKode, OverstyringAksjonspunkt {

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    protected OverstyringAksjonspunktDto() {
        // For Jackson
    }

    protected OverstyringAksjonspunktDto(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    @Override
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
