package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import java.time.LocalDate;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_VILKÅR_FOR_FORELDREANSVAR_KODE)
public class AvklarFaktaForForeldreansvarAksjonspunktDto extends BekreftetAksjonspunktDto implements OmsorgsOvertakelse {


    @NotNull
    private LocalDate omsorgsovertakelseDato;


    @NotNull
    private LocalDate foreldreansvarDato;


    @Override
    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public void setOmsorgsovertakelseDato(LocalDate omsorgsovertakelseDato) {
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
    }

    public LocalDate getForeldreansvarDato() {
        return foreldreansvarDato;
    }

    public void setForeldreansvarDato(LocalDate foreldreansvarDato) {
        this.foreldreansvarDato = foreldreansvarDato;
    }

}
