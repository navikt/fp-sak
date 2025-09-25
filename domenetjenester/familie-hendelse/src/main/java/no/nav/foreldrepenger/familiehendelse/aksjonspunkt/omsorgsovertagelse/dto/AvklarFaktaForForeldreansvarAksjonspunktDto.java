package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertagelse.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.OmsorgsOvertakelse;

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
