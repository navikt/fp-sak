package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.validering.ValidKodeverk;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_OMSORGSOVERTAKELSEVILKÅRET)
public class VurderOmsorgsovertakelseVilkårAksjonspunktDto extends BekreftetAksjonspunktDto {

    @ValidKodeverk
    private Avslagsårsak avslagskode;

    @NotNull
    @ValidKodeverk
    private OmsorgsovertakelseVilkårType delvilkår;

    @NotNull
    private LocalDate omsorgsovertakelseDato;

    @Valid
    @NotNull
    @Size(max = 9)
    private List<OmsorgsovertakelseBarnDto> barn;

    @NotNull
    private Boolean ektefellesBarn;

    VurderOmsorgsovertakelseVilkårAksjonspunktDto() {
        // For Jackson
    }

    public VurderOmsorgsovertakelseVilkårAksjonspunktDto(String begrunnelse,
                                                         LocalDate omsorgsovertakelseDato,
                                                         List<OmsorgsovertakelseBarnDto> fødselsdatoer,
                                                         Avslagsårsak avslagskode,
                                                         OmsorgsovertakelseVilkårType delvilkår,
                                                         Boolean ektefellesBarn) {

        super(begrunnelse);
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
        this.barn = fødselsdatoer;
        this.avslagskode = avslagskode;
        this.delvilkår = delvilkår;
        this.ektefellesBarn = ektefellesBarn;
    }


    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public List<OmsorgsovertakelseBarnDto> getBarn() {
        return barn;
    }

    public Avslagsårsak getAvslagskode() {
        return avslagskode;
    }

    public OmsorgsovertakelseVilkårType getDelvilkår() {
        return delvilkår;
    }

    public Boolean getEktefellesBarn() {
        return ektefellesBarn;
    }
}
