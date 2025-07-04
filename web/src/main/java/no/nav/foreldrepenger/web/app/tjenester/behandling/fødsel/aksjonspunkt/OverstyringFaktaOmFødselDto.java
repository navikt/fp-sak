package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.constraints.NotNull;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.familiehendelse.rest.BarnInfoProvider;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FAKTA_OM_FØDSEL_KODE)
public class OverstyringFaktaOmFødselDto extends OverstyringAksjonspunktDto implements BarnInfoProvider {

    private LocalDate termindato;

    @NotNull
    private Boolean erBarnFødt;

    private List<DokumentertBarnDto> barn;

    public OverstyringFaktaOmFødselDto(String begrunnelse, LocalDate termindato, List<DokumentertBarnDto> barn, Boolean erBarnFødt) {
        super(begrunnelse);
        this.termindato = termindato;
        this.barn = barn;
        this.erBarnFødt = erBarnFødt;
    }

    @SuppressWarnings("unused")
    private OverstyringFaktaOmFødselDto() {
        super();
        // For Jackson
    }

    public Boolean getErBarnFødt() {
        return erBarnFødt;
    }

    public LocalDate getTermindato() {
        return termindato;
    }

    @Override
    public List<DokumentertBarnDto> getBarn() {
        return barn;
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
