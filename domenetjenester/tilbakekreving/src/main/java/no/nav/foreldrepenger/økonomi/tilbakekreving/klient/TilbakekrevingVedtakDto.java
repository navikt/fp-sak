package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import java.time.LocalDate;

public record TilbakekrevingVedtakDto(Long behandlingId, LocalDate tilbakekrevingVedtakDato, String tilbakekrevingBehandlingType) {
}
