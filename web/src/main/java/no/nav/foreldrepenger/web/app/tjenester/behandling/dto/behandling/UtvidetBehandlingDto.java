package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UtvidetBehandlingDto extends BehandlingDto {

    @JsonProperty("ansvarligBeslutter")
    private String ansvarligBeslutter;

    @JsonProperty("behandlingHenlagt")
    private boolean behandlingHenlagt;

    /** Eventuelt async status på tasks. */
    @JsonProperty("taskStatus")
    private AsyncPollingStatus taskStatus;

    public String getAnsvarligBeslutter() {
        return ansvarligBeslutter;
    }

    public AsyncPollingStatus getTaskStatus() {
        return taskStatus;
    }

    public boolean isBehandlingHenlagt() {
        return behandlingHenlagt;
    }

    public void setAnsvarligBeslutter(String ansvarligBeslutter) {
        this.ansvarligBeslutter = ansvarligBeslutter;
    }

    void setBehandlingHenlagt(boolean behandlingHenlagt) {
        this.behandlingHenlagt = behandlingHenlagt;
    }

    public void setAsyncStatus(AsyncPollingStatus asyncStatus) {
        this.taskStatus = asyncStatus;
    }
}
