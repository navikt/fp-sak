package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TilbakeBehandlingDto {

    @JsonProperty("id")
    private Long id;
    @JsonProperty("uuid")
    private UUID uuid;
    @JsonProperty("type")
    private BehandlingType type;
    @JsonProperty("originalVedtaksDato")
    private LocalDate originalVedtaksDato;

    public TilbakeBehandlingDto(Long id, UUID uuid, BehandlingType type, LocalDate originalVedtaksDato) {
        this.id = id;
        this.uuid = uuid;
        this.type = type;
        this.originalVedtaksDato = originalVedtaksDato;
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public BehandlingType getType() {
        return type;
    }

    public LocalDate getOriginalVedtaksDato() {
        return originalVedtaksDato;
    }
}
