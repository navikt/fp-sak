package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO_KODE)
public class OverstyringAvklarStartdatoForPeriodenDto extends OverstyringAksjonspunktDto {


    @JsonProperty("startdatoFraSoknad")
    private LocalDate startdatoFraSoknad;

    @JsonProperty("opprinneligDato")
    private LocalDate opprinneligDato;

    public OverstyringAvklarStartdatoForPeriodenDto(String begrunnelse, LocalDate startdatoFraSoknad, LocalDate opprinneligDato) {
        super(begrunnelse);
        this.startdatoFraSoknad = startdatoFraSoknad;
        this.opprinneligDato = opprinneligDato;
    }

    @SuppressWarnings("unused")
    private OverstyringAvklarStartdatoForPeriodenDto() {
        super();
        // For Jackson
    }

    public LocalDate getStartdatoFraSoknad() {
        return startdatoFraSoknad;
    }

    public LocalDate getOpprinneligDato() {
        return opprinneligDato;
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
}
