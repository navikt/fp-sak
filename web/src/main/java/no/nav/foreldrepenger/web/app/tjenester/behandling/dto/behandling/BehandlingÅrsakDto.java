package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
public class BehandlingÅrsakDto {

    @JsonProperty("behandlingArsakType")
    private BehandlingÅrsakType behandlingÅrsakType;

    @JsonProperty("manueltOpprettet")
    private boolean manueltOpprettet;

    public BehandlingÅrsakDto() {
        // trengs for deserialisering av JSON
    }

    void setBehandlingArsakType(BehandlingÅrsakType behandlingArsakType) {
        this.behandlingÅrsakType = behandlingArsakType;
    }

    public void setManueltOpprettet(boolean manueltOpprettet) {
        this.manueltOpprettet = manueltOpprettet;
    }

    @JsonGetter
    public Boolean getErAutomatiskRevurdering(){
        if(behandlingÅrsakType == null){
            return false;
        }
        return BehandlingÅrsakType.årsakerForAutomatiskRevurdering().stream().anyMatch(årsak -> årsak.equals(this.behandlingÅrsakType));
    }

    public BehandlingÅrsakType getBehandlingArsakType() {
        return behandlingÅrsakType;
    }

    public boolean isManueltOpprettet() {
        return manueltOpprettet;
    }


}
