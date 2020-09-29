package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class KlageTilbakekrevingDto {

    @JsonProperty("tilbakekrevingUuid")
    private UUID påklagdEksternBehandlingUuid;

    @JsonProperty("tilbakekrevingVedtakDato")
    private LocalDate tilbakekrevingVedtakDato;

    @JsonProperty("tilbakekrevingBehandlingType")
    private String tilbakekrevingBehandlingType;

    KlageTilbakekrevingDto(){
        // for CDI
    }

    public KlageTilbakekrevingDto(UUID påklagdEksternBehandlingUuid, LocalDate tilbakekrevingVedtakDato, String tilbakekrevingBehandlingType) {
        this.påklagdEksternBehandlingUuid = påklagdEksternBehandlingUuid;
        this.tilbakekrevingVedtakDato = tilbakekrevingVedtakDato;
        this.tilbakekrevingBehandlingType = tilbakekrevingBehandlingType;
    }

    public UUID getPåklagdEksternBehandlingUuid() {
        return påklagdEksternBehandlingUuid;
    }

    public LocalDate getTilbakekrevingVedtakDato() {
        return tilbakekrevingVedtakDato;
    }

    public String getTilbakekrevingBehandlingType() {
        return tilbakekrevingBehandlingType;
    }

}
