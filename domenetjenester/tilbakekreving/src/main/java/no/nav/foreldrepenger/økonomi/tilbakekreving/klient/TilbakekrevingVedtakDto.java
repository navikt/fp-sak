package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TilbakekrevingVedtakDto {

    private Long behandlingId;

    @JsonProperty("tilbakekrevingVedtakDato")
    private LocalDate tilbakekrevingVedtakDato;

    @JsonProperty("tilbakekrevingBehandlingType")
    private String tilbakekrevingBehandlingType;

    TilbakekrevingVedtakDto(){
        // for CDI
    }

    public TilbakekrevingVedtakDto(Long behandlingId, LocalDate tilbakekrevingVedtakDato, String tilbakekrevingBehandlingType) {
        this.behandlingId = behandlingId;
        this.tilbakekrevingVedtakDato = tilbakekrevingVedtakDato;
        this.tilbakekrevingBehandlingType = tilbakekrevingBehandlingType;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public LocalDate getTilbakekrevingVedtakDato() {
        return tilbakekrevingVedtakDato;
    }

    public String getTilbakekrevingBehandlingType() {
        return tilbakekrevingBehandlingType;
    }

}
