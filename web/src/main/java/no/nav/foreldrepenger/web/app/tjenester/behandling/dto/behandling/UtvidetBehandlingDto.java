package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UtvidetBehandlingDto extends BehandlingDto {

    @JsonProperty("ansvarligBeslutter")
    private String ansvarligBeslutter;

    @JsonProperty("aksjonspunkt")
    @NotNull private Set<AksjonspunktDto> aksjonspunkt;

    @JsonProperty("harSøknad") @NotNull
    private boolean harSøknad;

    @JsonProperty("harRegisterdata")
    private boolean harRegisterdata;

    @JsonProperty("harSattEndringsdato") @NotNull
    private boolean harSattEndringsdato;

    @JsonProperty("alleUttaksperioderAvslått")
    private boolean alleUttaksperioderAvslått;

    @Deprecated(forRemoval = true) // Sjekk heller om lenken "simuleringResultat" er inkludert i dto
    @JsonProperty("sjekkSimuleringResultat")
    private boolean sjekkSimuleringResultat;

    /** Eventuelt async status på tasks. */
    @JsonProperty("taskStatus")
    private AsyncPollingStatus taskStatus;

    public String getAnsvarligBeslutter() {
        return ansvarligBeslutter;
    }

    public AsyncPollingStatus getTaskStatus() {
        return taskStatus;
    }

    public Set<AksjonspunktDto> getAksjonspunkt() {
        return aksjonspunkt;
    }

    public boolean getHarSøknad() {
        return harSøknad;
    }

    public boolean getHarRegisterdata() {
        return harRegisterdata;
    }

    public boolean getHarSattEndringsdato() {
        return harSattEndringsdato;
    }

    public boolean getAlleUttaksperioderAvslått() {
        return alleUttaksperioderAvslått;
    }

    public boolean getSjekkSimuleringResultat() {
        return sjekkSimuleringResultat;
    }

    public void setAnsvarligBeslutter(String ansvarligBeslutter) {
        this.ansvarligBeslutter = ansvarligBeslutter;
    }

    public void setAsyncStatus(AsyncPollingStatus asyncStatus) {
        this.taskStatus = asyncStatus;
    }

    public void setAksjonspunkt(Set<AksjonspunktDto> aksjonspunkt) {
        this.aksjonspunkt = aksjonspunkt;
    }

    public void setHarSøknad(boolean harSøknad) {
        this.harSøknad = harSøknad;
    }

    public void setHarRegisterdata(boolean harRegisterdata) {
        this.harRegisterdata = harRegisterdata;
    }

    public void setHarSattEndringsdato(boolean harSattEndringsdato) {
        this.harSattEndringsdato = harSattEndringsdato;
    }

    public void setAlleUttaksperioderAvslått(boolean alleUttaksperioderAvslått) {
        this.alleUttaksperioderAvslått = alleUttaksperioderAvslått;
    }

    public void setSjekkSimuleringResultat(boolean sjekkSimuleringResultat) {
        this.sjekkSimuleringResultat = sjekkSimuleringResultat;
    }
}
