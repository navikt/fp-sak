package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.DekningsgradDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.MedInntektArbeidYtelseRegistrering;

@JsonTypeName(AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER_KODE)
public class ManuellRegistreringForeldrepengerDto extends MedInntektArbeidYtelseRegistrering {


    @Valid
    private DekningsgradDto dekningsgrad;

    @Valid
    private TidsromPermisjonDto tidsromPermisjon;

    private boolean annenForelderInformert;

    public DekningsgradDto getDekningsgrad() {
        return dekningsgrad;
    }

    public void setDekningsgrad(DekningsgradDto dekningsgrad) {
        this.dekningsgrad = dekningsgrad;
    }

    public TidsromPermisjonDto getTidsromPermisjon() {
        return tidsromPermisjon;
    }

    public void setTidsromPermisjon(TidsromPermisjonDto tidsromPermisjon) {
        this.tidsromPermisjon = tidsromPermisjon;
    }

    public boolean getAnnenForelderInformert() {
        return annenForelderInformert;
    }

    public void setAnnenForelderInformert(Boolean annenForelderInformert) {
        this.annenForelderInformert = annenForelderInformert;
    }

}
