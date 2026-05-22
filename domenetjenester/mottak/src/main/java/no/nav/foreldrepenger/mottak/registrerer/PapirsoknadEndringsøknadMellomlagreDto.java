package no.nav.foreldrepenger.mottak.registrerer;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AksjonspunktKodeDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER_KODE)
public class PapirsoknadEndringsøknadMellomlagreDto extends PapirsoknadMellomlagreDto {

    @Valid
    private TidsromPermisjonFormValues tidsromPermisjon;

    private Boolean annenForelderInformert;

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
