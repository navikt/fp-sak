package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.DokumentertBarnDto;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FAKTA_OM_FØDSEL_KODE)
public class OverstyringFaktaOmFødselDto extends OverstyringAksjonspunktDto {

    private LocalDate termindato;

    private List<DokumentertBarnDto> barn;

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

    public List<DokumentertBarnDto> getBarn() {
        return barn == null ? List.of() : barn;
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
