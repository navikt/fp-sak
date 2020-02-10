package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt.fp;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_STARTDATO_FOR_FORELDREPENGEPERIODEN_KODE)
public class AvklarStartdatoForPeriodenDto extends BekreftetAksjonspunktDto {


    @NotNull
    private LocalDate startdatoFraSoknad;
    
    public AvklarStartdatoForPeriodenDto(String begrunnelse, LocalDate startdatoFraSoknad) {
        super(begrunnelse);
        this.startdatoFraSoknad = startdatoFraSoknad;
    }

    @SuppressWarnings("unused") // NOSONAR
    private AvklarStartdatoForPeriodenDto() {
        super();
        // For Jackson
    }


    public AvklarStartdatoForPeriodenDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }



    public LocalDate getStartdatoFraSoknad() {
        return startdatoFraSoknad;
    }
}
