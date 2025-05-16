package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY) // TODO: Hva er dette?
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FAKTA_OM_FØDSEL_KODE)
public class OverstyringFaktaOmFødselDto extends OverstyringAksjonspunktDto {

    @JsonProperty("termindato")
    private LocalDate termindato;

    // TODO: Undersøk om denne trengs eller ikke
    @JsonProperty("omsorgsovertakelsesdato")
    private LocalDate omsorgsovertakelsesdato;

    // TODO: Kan vi bruke UidentifisertBarnDto eller bør vi lage en egen? Eller er det noe
    @JsonProperty("barn")
    private List<UidentifisertBarnDto> barn;

    // TODO: Skal man kunne overstyre dødsdato også?

    public OverstyringFaktaOmFødselDto(String begrunnelse, LocalDate termindato, LocalDate omsorgsovertakelsesdato, List<UidentifisertBarnDto> barn) {
        super(begrunnelse);
        this.termindato = termindato;
        this.omsorgsovertakelsesdato = omsorgsovertakelsesdato;
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

    public LocalDate getOmsorgsovertakelsesdato() {
        return omsorgsovertakelsesdato;
    }

    public List<UidentifisertBarnDto> getBarn() {
        return barn;
    }

    public int getAntallBarn() {
        return barn.size();
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        //Brukes ikke
        return null;
    }

    @JsonIgnore
    @Override
    public boolean getErVilkarOk() {
        //Brukes ikke
        return false;
    }

    @Override
    public String toString() {
        return "OverstyringFaktaOmFødselDto{" + "termindato=" + termindato + ", omsorgsovertakelsesdato=" + omsorgsovertakelsesdato + ", barna="
            + barn + '}';
    }
}
