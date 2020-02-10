package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.time.LocalDate;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_LØPENDE_KODE)
public class OverstyringMedlemskapsvilkåretLøpendeDto extends OverstyringAksjonspunktDto {


    @JsonProperty("avslagDato")
    private LocalDate overstryingsdato;

    @JsonProperty("avslagskode")
    @Size(min = 4, max = 4)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    @JsonProperty("erVilkarOk")
    private boolean erVilkarOk;


    @SuppressWarnings("unused") // NOSONAR
    private OverstyringMedlemskapsvilkåretLøpendeDto() {
        super();
        // For Jackson
    }

    public OverstyringMedlemskapsvilkåretLøpendeDto(boolean erVilkarOk, String begrunnelse, String avslagskode) { // NOSONAR
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
        this.avslagskode = avslagskode;
    }

    @Override
    public String getAvslagskode() {
        return avslagskode;
    }

    @Override
    public boolean getErVilkarOk() {
        return erVilkarOk;
    }

    public LocalDate getOverstryingsdato() {
        return overstryingsdato;
    }
}
