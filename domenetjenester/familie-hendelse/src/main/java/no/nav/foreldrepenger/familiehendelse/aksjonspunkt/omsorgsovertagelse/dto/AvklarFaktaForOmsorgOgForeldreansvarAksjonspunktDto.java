package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertagelse.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.OmsorgsOvertakelse;
import no.nav.foreldrepenger.validering.ValidKodeverk;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE_KODE)
public class AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto extends BekreftetAksjonspunktDto implements OmsorgsOvertakelse {


    @JsonProperty("omsorgsovertakelseDato")
    @NotNull
    private LocalDate omsorgsovertakelseDato;

    @JsonProperty("vilkarType")
    @NotNull
    @ValidKodeverk
    private VilkårType vilkårType;


    @Override
    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public void setOmsorgsovertakelseDato(LocalDate omsorgsovertakelseDato) {
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    public void setVilkårType(VilkårType vilkarType) {
        this.vilkårType = vilkarType;
    }

}
