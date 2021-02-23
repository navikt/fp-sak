package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.familiehendelse.rest.AvklartDataBarnDto;
import no.nav.foreldrepenger.familiehendelse.rest.AvklartDataForeldreDto;
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

    @Deprecated
    @JsonProperty("antallBarn")
    @Min(1)
    @Max(9)
    private Integer antallBarn;

    @Deprecated
    @JsonProperty("barn")
    @Valid
    @NotNull
    @Size(max = 9)
    private List<AvklartDataBarnDto> barn = new ArrayList<>();

    @Deprecated
    @JsonProperty("foreldre")
    @Valid
    @NotNull
    @Size(max = 9)
    private List<AvklartDataForeldreDto> foreldre = new ArrayList<>();

    @JsonProperty("fødselsdatoer")
    @Valid
    @Size(max = 9)
    private Map<Integer, LocalDate> fødselsdatoer = new HashMap<>();


    @Override
    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    public void setOmsorgsovertakelseDato(LocalDate omsorgsovertakelseDato) {
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
    }

    public Integer getAntallBarn() {
        return antallBarn;
    }

    public void setAntallBarn(Integer antallBarn) {
        this.antallBarn = antallBarn;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    public void setVilkårType(VilkårType vilkarType) {
        this.vilkårType = vilkarType;
    }

    public List<AvklartDataBarnDto> getBarn() {
        return barn;
    }

    public void setBarn(List<AvklartDataBarnDto> barn) {
        this.barn = barn;
    }

    public List<AvklartDataForeldreDto> getForeldre() {
        return foreldre;
    }

    public void setForeldre(List<AvklartDataForeldreDto> foreldre) {
        this.foreldre = foreldre;
    }

    public Map<Integer, LocalDate> getFødselsdatoer() {
        return fødselsdatoer;
    }

    public void setFødselsdatoer(Map<Integer, LocalDate> fødselsdatoer) {
        this.fødselsdatoer = fødselsdatoer;
    }
}
