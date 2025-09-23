package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.kontrakter.formidling.v3.BrevmalDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOperasjonerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.dto.KontrollresultatDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollSkjermlenkeContextDto;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FagsakBehandlingDto extends BehandlingDto {

    @JsonProperty("behandlingTillatteOperasjoner")
    private BehandlingOperasjonerDto behandlingTillatteOperasjoner;

    @JsonProperty("brevmaler") @NotNull
    private List<BrevmalDto> brevmaler = new ArrayList<>();

    @JsonProperty("totrinnskontrollÅrsaker") @NotNull
    private List<TotrinnskontrollSkjermlenkeContextDto> totrinnskontrollÅrsaker = new ArrayList<>();

    @JsonProperty("totrinnskontrollReadonly")
    private boolean totrinnskontrollReadonly = true;

    @JsonProperty("risikoAksjonspunkt")
    private AksjonspunktDto risikoAksjonspunkt;

    @JsonProperty("kontrollResultat")
    private KontrollresultatDto kontrollResultat;

    @JsonProperty("ugunstAksjonspunkt")
    private Boolean ugunstAksjonspunkt;

    public BehandlingOperasjonerDto getBehandlingTillatteOperasjoner() {
        return behandlingTillatteOperasjoner;
    }

    public void setBehandlingTillatteOperasjoner(BehandlingOperasjonerDto behandlingTillatteOperasjoner) {
        this.behandlingTillatteOperasjoner = behandlingTillatteOperasjoner;
    }

    public List<BrevmalDto> getBrevmaler() {
        return brevmaler;
    }

    public void setBrevmaler(List<BrevmalDto> brevmaler) {
        this.brevmaler = brevmaler;
    }

    public List<TotrinnskontrollSkjermlenkeContextDto> getTotrinnskontrollÅrsaker() {
        return totrinnskontrollÅrsaker;
    }

    public void setTotrinnskontrollÅrsaker(List<TotrinnskontrollSkjermlenkeContextDto> totrinnskontrollÅrsaker) {
        this.totrinnskontrollÅrsaker = totrinnskontrollÅrsaker;
    }

    public boolean isTotrinnskontrollReadonly() {
        return totrinnskontrollReadonly;
    }

    public void setTotrinnskontrollReadonly(boolean totrinnskontrollReadonly) {
        this.totrinnskontrollReadonly = totrinnskontrollReadonly;
    }

    public AksjonspunktDto getRisikoAksjonspunkt() {
        return risikoAksjonspunkt;
    }

    public void setRisikoAksjonspunkt(AksjonspunktDto risikoAksjonspunkt) {
        this.risikoAksjonspunkt = risikoAksjonspunkt;
    }

    public Boolean getUgunstAksjonspunkt() {
        return ugunstAksjonspunkt;
    }

    public void setUgunstAksjonspunkt(Boolean ugunstAksjonspunkt) {
        this.ugunstAksjonspunkt = ugunstAksjonspunkt;
    }

    public KontrollresultatDto getKontrollResultat() {
        return kontrollResultat;
    }

    public void setKontrollResultat(KontrollresultatDto kontrollResultat) {
        this.kontrollResultat = kontrollResultat;
    }
}
