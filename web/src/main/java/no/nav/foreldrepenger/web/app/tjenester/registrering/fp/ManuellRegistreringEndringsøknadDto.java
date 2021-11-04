package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER_KODE)
public class ManuellRegistreringEndringsøknadDto extends ManuellRegistreringDto {

    @Valid
    private TidsromPermisjonDto tidsromPermisjon;

    @NotNull
    private Boolean annenForelderInformert;

    public TidsromPermisjonDto getTidsromPermisjon() {
        return tidsromPermisjon;
    }

    public void setTidsromPermisjon(TidsromPermisjonDto tidsromPermisjon) {
        this.tidsromPermisjon = tidsromPermisjon;
    }

    public Boolean getAnnenForelderInformert() {
        return annenForelderInformert;
    }

    public void setAnnenForelderInformert(Boolean annenForelderInformert) {
        this.annenForelderInformert = annenForelderInformert;
    }
}
