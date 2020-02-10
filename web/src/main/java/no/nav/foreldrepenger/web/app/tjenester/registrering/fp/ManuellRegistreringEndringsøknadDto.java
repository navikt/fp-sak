package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER_KODE)
public class ManuellRegistreringEndringsøknadDto extends ManuellRegistreringDto {


    @Valid
    private TidsromPermisjonDto tidsromPermisjon;

    public TidsromPermisjonDto getTidsromPermisjon() {
        return tidsromPermisjon;
    }

    public void setTidsromPermisjon(TidsromPermisjonDto tidsromPermisjon) {
        this.tidsromPermisjon = tidsromPermisjon;
    }


}
