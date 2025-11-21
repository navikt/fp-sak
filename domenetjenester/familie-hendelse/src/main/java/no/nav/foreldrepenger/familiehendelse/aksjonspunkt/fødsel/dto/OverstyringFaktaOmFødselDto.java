package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FAKTA_OM_FØDSEL_KODE)
public class OverstyringFaktaOmFødselDto extends OverstyringAksjonspunktDto {

    @NotNull
    private LocalDate termindato;

    @Size(min = 1, max = 9)
    private List<@Valid DokumentertBarnDto> barn;

    public OverstyringFaktaOmFødselDto(String begrunnelse, LocalDate termindato, List<DokumentertBarnDto> barn) {
        super(begrunnelse);
        this.termindato = termindato;
        this.barn = barn;
    }

    @SuppressWarnings("unused")
    private OverstyringFaktaOmFødselDto() {
        super();
        // For Jackson
    }

    public LocalDate getTermindato() {
        return termindato;
    }


    public Optional<List<DokumentertBarnDto>> getBarn() {
        return Optional.ofNullable(barn);
    }

    @Override
    @JsonIgnore
    public String getAvslagskode() {
        return null;
    }

    @Override
    @JsonIgnore
    public boolean getErVilkarOk() {
        return false;
    }

    @Override
    public String toString() {
        return "OverstyringFaktaOmFødselDto{" + "termindato=" + termindato + ", barn=" + barn + '}';
    }
}
