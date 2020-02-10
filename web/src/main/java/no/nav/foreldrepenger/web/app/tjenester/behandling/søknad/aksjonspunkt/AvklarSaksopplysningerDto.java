package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS_KODE)
public class AvklarSaksopplysningerDto extends BekreftetAksjonspunktDto {


    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String personstatus;

    private boolean fortsettBehandling;

    AvklarSaksopplysningerDto() {
        //For Jackson
    }


    public AvklarSaksopplysningerDto(String begrunnelse, String personstatus,
                                     boolean fortsettBehandling) {
        super(begrunnelse);
        this.personstatus = personstatus;
        this.fortsettBehandling = fortsettBehandling;
    }


    public String getPersonstatus() {
        return personstatus;
    }

    public boolean isFortsettBehandling() {
        return fortsettBehandling;
    }

}
