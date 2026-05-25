package no.nav.foreldrepenger.mottak.fyllutsendinn.frontend;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER_KODE)
public class PapirsoknadForeldrepengerMellomlagreDto extends PapirsoknadMedInntektArbeidYtelseMellomlagreDto {

    // Frontend lagrer dekningsgrad som string, f.eks. "100_PROSENT"
    private String dekningsgrad;

    @Valid
    private TidsromPermisjonFormValues tidsromPermisjon;

    private Boolean annenForelderInformert;

    public String getDekningsgrad() {
        return dekningsgrad;
    }

    public void setDekningsgrad(String dekningsgrad) {
        this.dekningsgrad = dekningsgrad;
    }

    public TidsromPermisjonFormValues getTidsromPermisjon() {
        return tidsromPermisjon;
    }

    public void setTidsromPermisjon(TidsromPermisjonFormValues tidsromPermisjon) {
        this.tidsromPermisjon = tidsromPermisjon;
    }

    public Boolean getAnnenForelderInformert() {
        return annenForelderInformert;
    }

    public void setAnnenForelderInformert(Boolean annenForelderInformert) {
        this.annenForelderInformert = annenForelderInformert;
    }
}
